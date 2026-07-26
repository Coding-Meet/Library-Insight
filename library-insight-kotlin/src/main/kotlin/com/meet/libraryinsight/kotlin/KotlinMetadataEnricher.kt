package com.meet.libraryinsight.kotlin

import com.meet.libraryinsight.common.Logger
import com.meet.libraryinsight.model.*
import com.meet.libraryinsight.model.Visibility
import com.meet.libraryinsight.model.ClassKind
import com.meet.libraryinsight.parser.RawAnnotation
import com.meet.libraryinsight.parser.RawClassData
import com.meet.libraryinsight.parser.RawMethod
import com.meet.libraryinsight.parser.SignatureParser
import kotlin.metadata.*
import kotlin.metadata.Visibility as KmVisibility
import kotlin.metadata.ClassKind as KmClassKind
import kotlin.metadata.jvm.*
import org.objectweb.asm.Opcodes

object KotlinMetadataEnricher {

    private val logger = Logger

    /**
     * Internal marker: name of kotlin.DslMarker annotation descriptor.
     * Used for DSL scope detection.
     */
    private const val DSL_MARKER_DESC = "Lkotlin/DslMarker;"

    fun enrich(rawClass: RawClassData, metadata: KotlinClassMetadata): ClassApi {
        return when (metadata) {
            is KotlinClassMetadata.Class -> enrichClass(rawClass, metadata.kmClass)
            is KotlinClassMetadata.FileFacade -> enrichPackageFacade(rawClass, metadata.kmPackage)
            is KotlinClassMetadata.MultiFileClassPart -> enrichPackageFacade(rawClass, metadata.kmPackage)
            else -> fallbackToJava(rawClass)
        }
    }

    /**
     * Extracts type aliases from a FileFacade or MultiFileClassPart package metadata.
     * Called externally by LibraryAnalyzer to collect package-level aliases.
     */
    fun extractTypeAliases(rawClass: RawClassData, metadata: KotlinClassMetadata): List<TypeAliasApi> {
        val kmPackage = when (metadata) {
            is KotlinClassMetadata.FileFacade -> metadata.kmPackage
            is KotlinClassMetadata.MultiFileClassPart -> metadata.kmPackage
            else -> return emptyList()
        }
        logger.info("Extracting type aliases from ${rawClass.name}: ${kmPackage.typeAliases.size} found")
        return kmPackage.typeAliases.map { alias ->
            val typeParamsMap = alias.typeParameters.associate { it.id to it.name }
            TypeAliasApi(
                name = alias.name,
                expandedType = formatKmType(alias.expandedType, typeParamsMap),
                typeParameters = alias.typeParameters.map { param ->
                    TypeParameterApi(
                        name = param.name,
                        upperBounds = param.upperBounds.map { formatKmType(it, typeParamsMap) },
                        isReified = param.isReified
                    )
                },
                annotations = alias.annotations.mapNotNull { mapKmAnnotation(it) }
            )
        }
    }

    private fun enrichClass(rawClass: RawClassData, kmClass: KmClass): ClassApi {
        val typeParamsMap = kmClass.typeParameters.associate { it.id to it.name }
        
        // Form Modifiers
        val modifiers = mutableListOf<String>()
        when (kmClass.modality) {
            Modality.ABSTRACT -> modifiers.add("abstract")
            Modality.OPEN -> modifiers.add("open")
            Modality.SEALED -> modifiers.add("sealed")
            Modality.FINAL -> { /* default */ }
        }
        if (kmClass.isData) modifiers.add("data")
        if (kmClass.isValue) modifiers.add("value")
        if (kmClass.isInner) modifiers.add("inner")
        if (kmClass.isFunInterface) modifiers.add("fun")

        val kind = when {
            kmClass.kind == KmClassKind.COMPANION_OBJECT -> ClassKind.COMPANION_OBJECT
            else -> mapClassKind(kmClass.kind)
        }

        // Map supertypes
        val superTypes = kmClass.supertypes.map { formatKmType(it, typeParamsMap) }

        // Constructors
        val constructors = kmClass.constructors.map { kmConstructor ->
            val signature = kmConstructor.signature?.descriptor ?: ""
            val rawMethod = rawClass.methods.firstOrNull { it.name == "<init>" && it.desc == signature }
            
            val parameters = kmConstructor.valueParameters.mapIndexed { index, param ->
                val rawAnnotations = rawMethod?.parameterAnnotations?.getOrNull(index) ?: emptyList()
                ParameterApi(
                    name = param.name,
                    type = formatKmType(param.type, typeParamsMap),
                    annotations = rawAnnotations.map { mapAnnotation(it) },
                    hasDefaultValue = param.declaresDefaultValue,
                    isLambdaReceiver = isLambdaWithReceiver(param.type)
                )
            }

            ConstructorApi(
                visibility = mapVisibility(kmConstructor.visibility),
                parameters = parameters,
                annotations = rawMethod?.annotations?.map { mapAnnotation(it) } ?: emptyList(),
                signature = signature
            )
        }

        // Properties
        val properties = kmClass.properties.map { kmProperty ->
            enrichProperty(rawClass, kmProperty, typeParamsMap)
        }

        // Functions
        val functions = kmClass.functions.map { kmFunction ->
            enrichFunction(rawClass, kmFunction, typeParamsMap)
        }

        // Nested classes
        val nestedClasses = kmClass.nestedClasses.map { "${rawClass.name}\$$it" }

        // Type Parameters — with reified flag
        val typeParams = kmClass.typeParameters.map { param ->
            TypeParameterApi(
                name = param.name,
                upperBounds = param.upperBounds.map { formatKmType(it, typeParamsMap) },
                isReified = param.isReified
            )
        }

        // @DslMarker scope detection
        val dslMarkerAnnotations = detectDslMarkerAnnotations(rawClass)

        logger.info("Enriched class ${rawClass.name}: ${functions.size} functions, ${properties.size} properties, ${dslMarkerAnnotations.size} DSL scopes")

        return ClassApi(
            name = rawClass.name,
            simpleName = rawClass.name.substringAfterLast('.').substringAfterLast('$'),
            visibility = mapVisibility(kmClass.visibility),
            kind = kind,
            modifiers = modifiers,
            superTypes = superTypes,
            annotations = rawClass.annotations.map { mapAnnotation(it) },
            constructors = constructors,
            methods = functions,
            properties = properties,
            nestedClasses = nestedClasses,
            typeParameters = typeParams,
            dslMarkerAnnotations = dslMarkerAnnotations
        )
    }

    private fun enrichPackageFacade(rawClass: RawClassData, kmPackage: KmPackage): ClassApi {
        // Packages hold top-level functions and properties
        val typeParamsMap = emptyMap<Int, String>()
        
        val properties = kmPackage.properties.map { kmProperty ->
            enrichProperty(rawClass, kmProperty, typeParamsMap)
        }

        val functions = kmPackage.functions.map { kmFunction ->
            enrichFunction(rawClass, kmFunction, typeParamsMap)
        }

        return ClassApi(
            name = rawClass.name,
            simpleName = rawClass.name.substringAfterLast('.'),
            visibility = Visibility.PUBLIC,
            kind = ClassKind.CLASS,
            modifiers = listOf("final"), // File facades are final classes
            superTypes = listOf("java.lang.Object"),
            annotations = emptyList(),
            constructors = emptyList(), // Static facades don't have public constructors
            methods = functions,
            properties = properties,
            nestedClasses = emptyList(),
            typeParameters = emptyList()
        )
    }

    private fun enrichProperty(rawClass: RawClassData, kmProperty: KmProperty, typeParamsMap: Map<Int, String>): PropertyApi {
        // Resolve property annotations
        // 1. Check synthetic annotations method: className + "$" + propertyName + "$annotations"
        val syntheticAnnotationsMethodName = "${kmProperty.name}\$annotations"
        val syntheticMethod = rawClass.methods.firstOrNull { it.name == syntheticAnnotationsMethodName }
        val syntheticAnnos = syntheticMethod?.annotations ?: emptyList()

        // 2. Check backing field
        val fieldSig = kmProperty.fieldSignature
        val fieldNode = rawClass.fields.firstOrNull { it.name == fieldSig?.name && it.desc == fieldSig?.descriptor }
        val fieldAnnos = fieldNode?.annotations ?: emptyList()

        // 3. Check getter method
        val getterSig = kmProperty.getterSignature
        val getterMethod = rawClass.methods.firstOrNull { it.name == getterSig?.name && it.desc == getterSig?.descriptor }
        val getterAnnos = getterMethod?.annotations ?: emptyList()

        // 4. Check setter method
        val setterSig = kmProperty.setterSignature
        val setterMethod = rawClass.methods.firstOrNull { it.name == setterSig?.name && it.desc == setterSig?.descriptor }
        val setterAnnos = setterMethod?.annotations ?: emptyList()

        // Merge all annotations, avoiding duplicates
        val allRawAnnos = (syntheticAnnos + fieldAnnos + getterAnnos + setterAnnos).distinctBy { it.desc }

        // Find getter/setter visibilities
        val getterVisibility = if (getterMethod != null) mapJvmAccessVisibility(getterMethod.access) else null
        val setterVisibility = if (setterMethod != null) mapJvmAccessVisibility(setterMethod.access) else null

        return PropertyApi(
            name = kmProperty.name,
            visibility = mapVisibility(kmProperty.visibility),
            type = formatKmType(kmProperty.returnType, typeParamsMap),
            isMutable = kmProperty.isVar,
            annotations = allRawAnnos.map { mapAnnotation(it) },
            getterVisibility = getterVisibility,
            setterVisibility = setterVisibility,
            isConst = kmProperty.isConst,
            isLateinit = kmProperty.isLateinit
        )
    }

    private fun enrichFunction(rawClass: RawClassData, kmFunction: KmFunction, typeParamsMap: Map<Int, String>): MethodApi {
        // Map local type parameters — with reified flag
        val localTypeParamsMap = typeParamsMap + kmFunction.typeParameters.associate { it.id to it.name }
        
        val sig = kmFunction.signature
        val rawMethod = if (sig != null) {
            rawClass.methods.firstOrNull { it.name == sig.name && it.desc == sig.descriptor }
        } else {
            rawClass.methods.firstOrNull { it.name == kmFunction.name }
        }

        val parameters = kmFunction.valueParameters.mapIndexed { index, param ->
            val rawAnnotations = rawMethod?.parameterAnnotations?.getOrNull(index) ?: emptyList()
            ParameterApi(
                name = param.name,
                type = formatKmType(param.type, localTypeParamsMap),
                annotations = rawAnnotations.map { mapAnnotation(it) },
                hasDefaultValue = param.declaresDefaultValue,
                isLambdaReceiver = isLambdaWithReceiver(param.type)
            )
        }

        val returnType = formatKmType(kmFunction.returnType, localTypeParamsMap)
        val receiverType = kmFunction.receiverParameterType?.let { formatKmType(it, localTypeParamsMap) }

        val isStatic = rawMethod?.let { (it.access and Opcodes.ACC_STATIC) != 0 } ?: false
        val isAbstract = kmFunction.modality == Modality.ABSTRACT
        val isOpen = kmFunction.modality == Modality.OPEN
        val isFinal = kmFunction.modality == Modality.FINAL

        val methodFlags = MethodFlags(
            isSuspend = kmFunction.isSuspend,
            isInline = kmFunction.isInline,
            isOperator = kmFunction.isOperator,
            isInfix = kmFunction.isInfix,
            isStatic = isStatic,
            isAbstract = isAbstract,
            isOpen = isOpen,
            isFinal = isFinal
        )

        val signatureString = sig?.descriptor ?: rawMethod?.desc ?: ""

        // Type Parameters — with reified flag
        val typeParameters = kmFunction.typeParameters.map { param ->
            TypeParameterApi(
                name = param.name,
                upperBounds = param.upperBounds.map { formatKmType(it, localTypeParamsMap) },
                isReified = param.isReified
            )
        }

        return MethodApi(
            name = kmFunction.name,
            visibility = mapVisibility(kmFunction.visibility),
            returnType = returnType,
            parameters = parameters,
            annotations = rawMethod?.annotations?.map { mapAnnotation(it) } ?: emptyList(),
            flags = methodFlags,
            extensionReceiverType = receiverType,
            signature = signatureString,
            typeParameters = typeParameters
        )
    }

    fun fallbackToJava(rawClass: RawClassData): ClassApi {
        val isInterface = (rawClass.access and Opcodes.ACC_INTERFACE) != 0
        val isEnum = (rawClass.access and Opcodes.ACC_ENUM) != 0
        val isAnnotation = (rawClass.access and Opcodes.ACC_ANNOTATION) != 0
        
        val kind = when {
            isAnnotation -> ClassKind.ANNOTATION
            isEnum -> ClassKind.ENUM
            isInterface -> ClassKind.INTERFACE
            else -> ClassKind.CLASS
        }

        val parsedSignature = SignatureParser.parseClassSignature(rawClass.signature, rawClass.superName, rawClass.interfaces)

        val modifiers = mutableListOf<String>()
        if ((rawClass.access and Opcodes.ACC_ABSTRACT) != 0 && !isInterface) modifiers.add("abstract")
        if ((rawClass.access and Opcodes.ACC_FINAL) == 0 && !isInterface && !isEnum) modifiers.add("open")

        val constructors = rawClass.methods
            .filter { it.name == "<init>" && isPublicOrProtected(it.access) }
            .map { rawMethod ->
                val parsedMethodSig = SignatureParser.parseMethodSignature(rawMethod.signature, rawMethod.desc)
                val params = parsedMethodSig.parameterTypes.mapIndexed { index, paramType ->
                    val paramName = rawMethod.parameterNames.getOrNull(index) ?: "p$index"
                    ParameterApi(
                        name = paramName,
                        type = paramType,
                        annotations = rawMethod.parameterAnnotations.getOrNull(index)?.map { mapAnnotation(it) } ?: emptyList()
                    )
                }

                ConstructorApi(
                    visibility = mapJvmAccessVisibility(rawMethod.access),
                    parameters = params,
                    annotations = rawMethod.annotations.map { mapAnnotation(it) },
                    signature = rawMethod.desc
                )
            }

        val methods = rawClass.methods
            .filter { it.name != "<init>" && it.name != "<clinit>" && isPublicOrProtected(it.access) && !isSynthetic(it.access) }
            .map { rawMethod ->
                val parsedMethodSig = SignatureParser.parseMethodSignature(rawMethod.signature, rawMethod.desc)
                val params = parsedMethodSig.parameterTypes.mapIndexed { index, paramType ->
                    val paramName = rawMethod.parameterNames.getOrNull(index) ?: "p$index"
                    ParameterApi(
                        name = paramName,
                        type = paramType,
                        annotations = rawMethod.parameterAnnotations.getOrNull(index)?.map { mapAnnotation(it) } ?: emptyList()
                    )
                }

                val flags = MethodFlags(
                    isStatic = (rawMethod.access and Opcodes.ACC_STATIC) != 0,
                    isAbstract = (rawMethod.access and Opcodes.ACC_ABSTRACT) != 0,
                    isOpen = (rawMethod.access and Opcodes.ACC_FINAL) == 0 && (rawMethod.access and Opcodes.ACC_STATIC) == 0,
                    isFinal = (rawMethod.access and Opcodes.ACC_FINAL) != 0
                )

                MethodApi(
                    name = rawMethod.name,
                    visibility = mapJvmAccessVisibility(rawMethod.access),
                    returnType = parsedMethodSig.returnType,
                    parameters = params,
                    annotations = rawMethod.annotations.map { mapAnnotation(it) },
                    flags = flags,
                    signature = rawMethod.desc,
                    typeParameters = parsedMethodSig.typeParameters.map { TypeParameterApi(it.name, it.bounds) }
                )
            }

        val properties = rawClass.fields
            .filter { isPublicOrProtected(it.access) && !isSynthetic(it.access) }
            .map { rawField ->
                val fieldType = SignatureParser.parseDescriptor(rawField.desc)
                val isMutable = (rawField.access and Opcodes.ACC_FINAL) == 0
                PropertyApi(
                    name = rawField.name,
                    visibility = mapJvmAccessVisibility(rawField.access),
                    type = fieldType,
                    isMutable = isMutable,
                    annotations = rawField.annotations.map { mapAnnotation(it) },
                    isConst = (rawField.access and Opcodes.ACC_STATIC) != 0 && (rawField.access and Opcodes.ACC_FINAL) != 0 && rawField.value != null
                )
            }

        val superTypes = mutableListOf<String>()
        if (parsedSignature.superType != "java.lang.Object") {
            superTypes.add(parsedSignature.superType)
        }
        superTypes.addAll(parsedSignature.interfaces)

        val nestedClasses = rawClass.innerClasses
            .filter { it.outerName == rawClass.internalName && isPublicOrProtected(it.access) }
            .map { it.name.replace('/', '.') }

        return ClassApi(
            name = rawClass.name,
            simpleName = rawClass.name.substringAfterLast('.'),
            visibility = mapJvmAccessVisibility(rawClass.access),
            kind = kind,
            modifiers = modifiers,
            superTypes = superTypes,
            annotations = rawClass.annotations.map { mapAnnotation(it) },
            constructors = constructors,
            methods = methods,
            properties = properties,
            nestedClasses = nestedClasses,
            typeParameters = parsedSignature.typeParameters.map { TypeParameterApi(it.name, it.bounds) }
        )
    }

    private fun mapClassKind(kind: KmClassKind): ClassKind {
        return when (kind) {
            KmClassKind.CLASS -> ClassKind.CLASS
            KmClassKind.INTERFACE -> ClassKind.INTERFACE
            KmClassKind.ENUM_CLASS -> ClassKind.ENUM
            KmClassKind.ENUM_ENTRY -> ClassKind.CLASS
            KmClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION
            KmClassKind.OBJECT -> ClassKind.OBJECT
            KmClassKind.COMPANION_OBJECT -> ClassKind.COMPANION_OBJECT
        }
    }

    /**
     * Formats a KmType into human-readable Kotlin syntax.
     *
     * Key DSL improvements:
     * - `kotlin.FunctionN` with arguments is rendered as `(P1, P2) -> R`
     * - `kotlin.ExtensionFunctionType` (lambda-with-receiver) is rendered as `Receiver.(P) -> R`
     *   by detecting the `AnnotationFlag.IS_SUSPEND` on the first argument being the receiver.
     */
    internal fun formatKmType(type: KmType, typeParamsMap: Map<Int, String>): String {
        val classifier = type.classifier

        // Detect kotlin.FunctionN (regular lambdas and extension lambdas)
        if (classifier is KmClassifier.Class) {
            val className = classifier.name
            // Matches kotlin/Function0 .. kotlin/Function22 and kotlin/FunctionN
            val functionArity = extractFunctionArity(className)
            if (functionArity != null && type.arguments.isNotEmpty()) {
                return formatFunctionType(type, typeParamsMap, functionArity)
            }
        }

        val classifierStr = when (classifier) {
            is KmClassifier.Class -> classifier.name.replace('/', '.')
            is KmClassifier.TypeParameter -> typeParamsMap[classifier.id] ?: "T"
            is KmClassifier.TypeAlias -> classifier.name.replace('/', '.')
        }

        val argsStr = if (type.arguments.isNotEmpty()) {
            type.arguments.joinToString(prefix = "<", postfix = ">") { projection ->
                val pType = projection.type
                if (pType == null) {
                    "*"
                } else {
                    val formatted = formatKmType(pType, typeParamsMap)
                    val variancePrefix = when (projection.variance) {
                        KmVariance.IN -> "in "
                        KmVariance.OUT -> "out "
                        else -> ""
                    }
                    variancePrefix + formatted
                }
            }
        } else {
            ""
        }

        val nullableSuffix = if (type.isNullable) "?" else ""
        return classifierStr + argsStr + nullableSuffix
    }

    /**
     * Formats kotlin.FunctionN types into readable Kotlin lambda syntax.
     * Handles both regular lambdas `(A, B) -> R` and extension lambdas `A.(B) -> R`.
     */
    private fun formatFunctionType(type: KmType, typeParamsMap: Map<Int, String>, arity: Int): String {
        val args = type.arguments
        val nullableSuffix = if (type.isNullable) "?" else ""
        val isSuspend = type.annotations.any { it.className == "kotlin/coroutines/SuspendFunction" }

        // Check for extension function type: kotlin.ExtensionFunctionType annotation on the type
        val isExtension = type.annotations.any { it.className == "kotlin/ExtensionFunctionType" }

        val allArgTypes = args.map { it.type?.let { t -> formatKmType(t, typeParamsMap) } ?: "*" }

        return if (isExtension && allArgTypes.size >= 2) {
            // Extension lambda: first arg is receiver, rest are params, last is return type
            val receiver = allArgTypes.first()
            val params = allArgTypes.drop(1).dropLast(1)
            val returnType = allArgTypes.last()
            val suspendPrefix = if (isSuspend) "suspend " else ""
            val paramsStr = params.joinToString(", ")
            "$suspendPrefix$receiver.($paramsStr) -> $returnType$nullableSuffix"
        } else {
            // Regular lambda: all but last are params, last is return type
            val params = allArgTypes.dropLast(1)
            val returnType = allArgTypes.last()
            val suspendPrefix = if (isSuspend) "suspend " else ""
            val paramsStr = params.joinToString(", ")
            "$suspendPrefix($paramsStr) -> $returnType$nullableSuffix"
        }
    }

    /**
     * Detects if a class name is kotlin/FunctionN and returns the arity N, or null if not.
     */
    private fun extractFunctionArity(className: String): Int? {
        if (!className.startsWith("kotlin/Function")) return null
        val suffix = className.removePrefix("kotlin/Function")
        return suffix.toIntOrNull()
    }

    /**
     * Returns true if the KmType represents a lambda-with-receiver (extension function type).
     * Used to mark ParameterApi.isLambdaReceiver.
     */
    private fun isLambdaWithReceiver(type: KmType?): Boolean {
        if (type == null) return false
        val classifier = type.classifier
        if (classifier !is KmClassifier.Class) return false
        if (extractFunctionArity(classifier.name) == null) return false
        return type.annotations.any { it.className == "kotlin/ExtensionFunctionType" }
    }

    /**
     * Detects @DslMarker-annotated annotations applied to this class.
     * A class is a "DSL scope" if any of its annotations is itself annotated with @DslMarker.
     * Returns a list of simple annotation names that carry the @DslMarker meta-annotation.
     */
    private fun detectDslMarkerAnnotations(rawClass: RawClassData): List<String> {
        return rawClass.annotations.filter { annotation ->
            // Check if this annotation class itself has @DslMarker on it
            // We detect by checking if the annotation descriptor has "DslMarker" in its meta-annotations.
            // Since we only have the annotation descriptor here, we use a heuristic:
            // @DslMarker is typically on custom annotation classes applied to DSL builders.
            // We detect it by checking if the annotation's own annotations list contains DslMarker.
            annotation.metaAnnotations.any { meta -> meta.desc == DSL_MARKER_DESC }
        }.map { annotation ->
            SignatureParser.parseDescriptor(annotation.desc).substringAfterLast('.')
        }
    }

    private fun mapVisibility(vis: KmVisibility): Visibility {
        return when (vis) {
            KmVisibility.PUBLIC -> Visibility.PUBLIC
            KmVisibility.PROTECTED -> Visibility.PROTECTED
            KmVisibility.INTERNAL -> Visibility.INTERNAL
            else -> Visibility.PRIVATE
        }
    }

    private fun mapJvmAccessVisibility(access: Int): Visibility {
        return when {
            (access and Opcodes.ACC_PUBLIC) != 0 -> Visibility.PUBLIC
            (access and Opcodes.ACC_PROTECTED) != 0 -> Visibility.PROTECTED
            (access and Opcodes.ACC_PRIVATE) != 0 -> Visibility.PRIVATE
            else -> Visibility.PUBLIC // Package-private is treated as PUBLIC or visible in package indices
        }
    }

    private fun isPublicOrProtected(access: Int): Boolean {
        return (access and Opcodes.ACC_PUBLIC) != 0 || (access and Opcodes.ACC_PROTECTED) != 0
    }

    private fun isSynthetic(access: Int): Boolean {
        return (access and Opcodes.ACC_SYNTHETIC) != 0 || (access and Opcodes.ACC_BRIDGE) != 0
    }

    private fun mapAnnotation(raw: RawAnnotation): AnnotationApi {
        val argsMap = raw.values.mapValues { (_, value) ->
            formatAnnotationValue(value)
        }
        return AnnotationApi(
            name = SignatureParser.parseDescriptor(raw.desc),
            arguments = argsMap
        )
    }

    private fun mapKmAnnotation(annotation: KmAnnotation): AnnotationApi? {
        return try {
            AnnotationApi(
                name = annotation.className.replace('/', '.'),
                arguments = annotation.arguments.mapValues { it.value.toString() }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun formatAnnotationValue(value: Any?): String {
        if (value == null) return "null"
        return when (value) {
            is String -> "\"$value\""
            is List<*> -> value.joinToString(prefix = "[", postfix = "]") { formatAnnotationValue(it) }
            is RawAnnotation -> {
                val args = value.values.map { "${it.key} = ${formatAnnotationValue(it.value)}" }.joinToString()
                "@${SignatureParser.parseDescriptor(value.desc)}($args)"
            }
            else -> value.toString()
        }
    }
}
