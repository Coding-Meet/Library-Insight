package com.meet.libraryinsight.parser

import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.lexer.KtTokens
import com.meet.libraryinsight.model.*
import java.io.File

object KotlinSourceParser {
    private val disposable = Disposer.newDisposable()
    private val environment = KotlinCoreEnvironment.createForProduction(
        disposable,
        CompilerConfiguration(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
    private val psiFactory = KtPsiFactory(environment.project)

    private class LineColConverter(text: String) {
        private val lineStarts = mutableListOf<Int>()
        init {
            lineStarts.add(0)
            var index = 0
            while (index < text.length) {
                if (text[index] == '\n') {
                    lineStarts.add(index + 1)
                }
                index++
            }
        }
        
        fun convert(offset: Int): Pair<Int, Int> {
            var line = lineStarts.binarySearch(offset)
            if (line < 0) {
                line = -line - 2
            }
            if (line < 0) line = 0
            val lineStart = lineStarts[line]
            val column = offset - lineStart + 1
            return Pair(line + 1, column)
        }
    }

    fun parse(file: File): List<ClassApi> {
        val fileContent = file.readText()
        val ktFile = psiFactory.createFile(file.name, fileContent)
        val packageName = ktFile.packageFqName.asString()
        val imports = ktFile.importDirectives.mapNotNull { it.importPath?.toString() }
        val relPath = try {
            file.canonicalFile.relativeTo(File(".").canonicalFile).path
        } catch (e: Exception) {
            file.path
        }
        val converter = LineColConverter(fileContent)

        fun getLocation(element: KtElement): SourceLocation {
            val offset = element.textRange.startOffset
            val (l, c) = converter.convert(offset)
            return SourceLocation(file = relPath, line = l, column = c)
        }

        val classes = mutableListOf<ClassApi>()
        val packageLevelFunctions = mutableListOf<MethodApi>()
        val packageLevelProperties = mutableListOf<PropertyApi>()

        fun collectDeclarations(decl: KtDeclaration, parentClass: KtClassOrObject?) {
            when (decl) {
                is KtClassOrObject -> {
                    classes.add(mapClass(decl, packageName, ::getLocation, imports))
                    decl.declarations.forEach { collectDeclarations(it, decl) }
                }
                is KtNamedFunction -> {
                    if (parentClass == null) {
                        packageLevelFunctions.add(mapFunction(decl, ::getLocation))
                    }
                }
                is KtProperty -> {
                    if (parentClass == null) {
                        packageLevelProperties.add(mapProperty(decl, ::getLocation))
                    }
                }
            }
        }

        ktFile.declarations.forEach { collectDeclarations(it, null) }

        if (packageLevelFunctions.isNotEmpty() || packageLevelProperties.isNotEmpty()) {
            val facadeName = "${file.nameWithoutExtension}Kt"
            val facadeFqcn = if (packageName.isNotEmpty()) "$packageName.$facadeName" else facadeName
            classes.add(
                ClassApi(
                    name = facadeFqcn,
                    simpleName = facadeName,
                    visibility = Visibility.PUBLIC,
                    kind = ClassKind.CLASS,
                    modifiers = emptyList(),
                    superTypes = emptyList(),
                    annotations = emptyList(),
                    constructors = emptyList(),
                    methods = packageLevelFunctions,
                    properties = packageLevelProperties,
                    nestedClasses = emptyList(),
                    sourceLocation = SourceLocation(file = relPath, line = 1, column = 1),
                    imports = imports
                )
            )
        }

        return classes
    }

    private fun mapClass(
        decl: KtClassOrObject, 
        packageName: String, 
        locGetter: (KtElement) -> SourceLocation,
        imports: List<String>
    ): ClassApi {
        val simpleName = decl.name ?: ""
        val fqName = decl.fqName?.asString() ?: simpleName

        val kind = when {
            decl is KtClass && decl.isInterface() -> ClassKind.INTERFACE
            decl is KtClass && decl.isAnnotation() -> ClassKind.ANNOTATION
            decl is KtClass && decl.isEnum() -> ClassKind.ENUM
            decl is KtObjectDeclaration && decl.isCompanion() -> ClassKind.COMPANION_OBJECT
            decl is KtObjectDeclaration -> ClassKind.OBJECT
            else -> ClassKind.CLASS
        }

        val visibility = getVisibility(decl)
        val modifiers = mapModifiers(decl)
        val annotations = mapAnnotations(decl)
        val doc = decl.docComment?.text

        val nestedDecls = decl.declarations.filterIsInstance<KtClassOrObject>()
        val nestedClassNames = nestedDecls.map { nd ->
            nd.fqName?.asString() ?: "${fqName}.${nd.name}"
        }

        val methods = decl.declarations.filterIsInstance<KtNamedFunction>().map { mapFunction(it, locGetter) }

        val properties = decl.declarations.filterIsInstance<KtProperty>().map { mapProperty(it, locGetter) }.toMutableList()

        decl.primaryConstructor?.valueParameters
            ?.filter { it.hasValOrVar() }
            ?.forEach { param ->
                val propVisibility = getVisibility(param)
                val propType = param.typeReference?.text ?: "Any"
                properties.add(
                    PropertyApi(
                        name = param.name ?: "",
                        visibility = propVisibility,
                        type = propType,
                        isMutable = param.isMutable,
                        annotations = mapAnnotations(param),
                        doc = param.docComment?.text,
                        sourceLocation = locGetter(param)
                    )
                )
            }

        val constructors = mutableListOf<ConstructorApi>()
        decl.primaryConstructor?.let { pc ->
            val params = pc.valueParameters.map { p ->
                ParameterApi(
                    name = p.name ?: "",
                    type = p.typeReference?.text ?: "Any",
                    annotations = mapAnnotations(p)
                )
            }
            constructors.add(
                ConstructorApi(
                    visibility = getVisibility(pc),
                    parameters = params,
                    annotations = mapAnnotations(pc),
                    signature = "",
                    sourceLocation = locGetter(pc)
                )
            )
        }
        decl.secondaryConstructors.forEach { sc ->
            val params = sc.valueParameters.map { p ->
                ParameterApi(
                    name = p.name ?: "",
                    type = p.typeReference?.text ?: "Any",
                    annotations = mapAnnotations(p)
                )
            }
            constructors.add(
                ConstructorApi(
                    visibility = getVisibility(sc),
                    parameters = params,
                    annotations = mapAnnotations(sc),
                    signature = "",
                    sourceLocation = locGetter(sc)
                )
            )
        }

        val superTypes = decl.superTypeListEntries.mapNotNull { it.typeReference?.text }

        return ClassApi(
            name = fqName,
            simpleName = simpleName,
            visibility = visibility,
            kind = kind,
            modifiers = modifiers,
            superTypes = superTypes,
            annotations = annotations,
            constructors = constructors,
            methods = methods,
            properties = properties,
            nestedClasses = nestedClassNames,
            typeParameters = mapTypeParameters(decl.typeParameters),
            doc = doc,
            sourceLocation = locGetter(decl),
            imports = imports
        )
    }

    private fun getVisibility(decl: KtModifierListOwner): Visibility {
        return when {
            decl.hasModifier(KtTokens.PRIVATE_KEYWORD) -> Visibility.PRIVATE
            decl.hasModifier(KtTokens.PROTECTED_KEYWORD) -> Visibility.PROTECTED
            decl.hasModifier(KtTokens.INTERNAL_KEYWORD) -> Visibility.INTERNAL
            else -> Visibility.PUBLIC
        }
    }

    private fun mapModifiers(decl: KtModifierListOwner): List<String> {
        val list = mutableListOf<String>()
        if (decl.hasModifier(KtTokens.DATA_KEYWORD)) list.add("data")
        if (decl.hasModifier(KtTokens.SEALED_KEYWORD)) list.add("sealed")
        if (decl.hasModifier(KtTokens.VALUE_KEYWORD)) list.add("value")
        if (decl.hasModifier(KtTokens.INNER_KEYWORD)) list.add("inner")
        if (decl.hasModifier(KtTokens.OPEN_KEYWORD)) list.add("open")
        if (decl.hasModifier(KtTokens.ABSTRACT_KEYWORD)) list.add("abstract")
        if (decl.hasModifier(KtTokens.INLINE_KEYWORD)) list.add("inline")
        return list
    }

    private fun mapAnnotations(decl: KtModifierListOwner): List<AnnotationApi> {
        return decl.annotationEntries.map { entry ->
            val name = entry.typeReference?.text ?: ""
            val args = entry.valueArguments.associate { arg ->
                val key = arg.getArgumentName()?.asName?.asString() ?: "value"
                val value = arg.getArgumentExpression()?.text ?: ""
                key to value
            }
            AnnotationApi(name = name, arguments = args)
        }
    }

    private fun mapTypeParameters(tps: List<KtTypeParameter>): List<TypeParameterApi> {
        return tps.map { tp ->
            val bounds = mutableListOf<String>()
            tp.extendsBound?.let { bounds.add(it.text) }
            TypeParameterApi(
                name = tp.name ?: "",
                upperBounds = bounds,
                isReified = tp.hasModifier(KtTokens.REIFIED_KEYWORD)
            )
        }
    }

    private fun mapFunction(func: KtNamedFunction, locGetter: (KtElement) -> SourceLocation): MethodApi {
        val name = func.name ?: ""
        val visibility = getVisibility(func)
        val returnType = func.typeReference?.text ?: "Unit"
        val params = func.valueParameters.map { p ->
            ParameterApi(
                name = p.name ?: "",
                type = p.typeReference?.text ?: "Any",
                annotations = mapAnnotations(p)
            )
        }

        val isSuspend = func.hasModifier(KtTokens.SUSPEND_KEYWORD)
        val isInline = func.hasModifier(KtTokens.INLINE_KEYWORD)
        val isOperator = func.hasModifier(KtTokens.OPERATOR_KEYWORD)
        val isInfix = func.hasModifier(KtTokens.INFIX_KEYWORD)
        val isAbstract = func.hasModifier(KtTokens.ABSTRACT_KEYWORD)

        return MethodApi(
            name = name,
            visibility = visibility,
            returnType = returnType,
            parameters = params,
            annotations = mapAnnotations(func),
            flags = MethodFlags(
                isSuspend = isSuspend,
                isInline = isInline,
                isOperator = isOperator,
                isInfix = isInfix,
                isAbstract = isAbstract
            ),
            extensionReceiverType = func.receiverTypeReference?.text,
            signature = "",
            typeParameters = mapTypeParameters(func.typeParameters),
            doc = func.docComment?.text,
            sourceLocation = locGetter(func)
        )
    }

    private fun mapProperty(prop: KtProperty, locGetter: (KtElement) -> SourceLocation): PropertyApi {
        val name = prop.name ?: ""
        val visibility = getVisibility(prop)
        val type = prop.typeReference?.text ?: "Any"
        val isMutable = prop.isVar

        return PropertyApi(
            name = name,
            visibility = visibility,
            type = type,
            isMutable = isMutable,
            annotations = mapAnnotations(prop),
            doc = prop.docComment?.text,
            sourceLocation = locGetter(prop)
        )
    }
}
