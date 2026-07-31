package com.meet.libraryinsight.parser

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.NormalAnnotationExpr
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr
import com.github.javaparser.ast.type.TypeParameter
import com.meet.libraryinsight.model.*
import java.io.File

object JavaSourceParser {
    fun parse(file: File): List<ClassApi> {
        return try {
            val cu = StaticJavaParser.parse(file)
            val packageName = cu.packageDeclaration.map { it.nameAsString }.orElse("")
            val imports = cu.imports.map { it.nameAsString }
            val relPath = try {
                file.canonicalFile.relativeTo(File(".").canonicalFile).path
            } catch (e: Exception) {
                file.path
            }

            fun getLocation(node: Node): SourceLocation? {
                val range = node.range.orElse(null)
                return range?.let {
                    SourceLocation(
                        file = relPath,
                        line = it.begin.line,
                        column = it.begin.column
                    )
                }
            }
            
            cu.findAll(TypeDeclaration::class.java).map { typeDecl ->
                val className = typeDecl.nameAsString
                val kind = when (typeDecl) {
                    is ClassOrInterfaceDeclaration -> {
                        if (typeDecl.isInterface) ClassKind.INTERFACE else ClassKind.CLASS
                    }
                    is EnumDeclaration -> ClassKind.ENUM
                    is AnnotationDeclaration -> ClassKind.ANNOTATION
                    else -> ClassKind.CLASS
                }

                val visibility = when {
                    typeDecl.isPublic -> Visibility.PUBLIC
                    typeDecl.isPrivate -> Visibility.PRIVATE
                    typeDecl.isProtected -> Visibility.PROTECTED
                    else -> Visibility.PUBLIC
                }

                val modifiers = typeDecl.modifiers.map { it.keyword.asString() }
                val annotations = typeDecl.annotations.map { mapJavaAnnotation(it) }
                val doc = typeDecl.javadoc.map { it.toText() }.orElse(null)

                // 1. Constructors
                val constructors = when (typeDecl) {
                    is ClassOrInterfaceDeclaration -> {
                        typeDecl.constructors.map { mapJavaConstructor(it, ::getLocation) }
                    }
                    is RecordDeclaration -> {
                        typeDecl.constructors.map { mapJavaConstructor(it, ::getLocation) }
                    }
                    else -> emptyList()
                }

                // 2. Methods
                val methods = when (typeDecl) {
                    is ClassOrInterfaceDeclaration -> {
                        typeDecl.methods.map { mapJavaMethod(it, typeDecl.isInterface, ::getLocation) }
                    }
                    is EnumDeclaration -> {
                        typeDecl.methods.map { mapJavaMethod(it, false, ::getLocation) }
                    }
                    is RecordDeclaration -> {
                        typeDecl.methods.map { mapJavaMethod(it, false, ::getLocation) }
                    }
                    else -> emptyList()
                }

                // 3. Fields / Properties
                val properties = mutableListOf<PropertyApi>()
                
                if (typeDecl is ClassOrInterfaceDeclaration) {
                    typeDecl.fields.flatMapTo(properties) { fieldDecl ->
                        val fieldVisibility = when {
                            fieldDecl.isPublic -> Visibility.PUBLIC
                            fieldDecl.isPrivate -> Visibility.PRIVATE
                            fieldDecl.isProtected -> Visibility.PROTECTED
                            else -> Visibility.PUBLIC
                        }
                        fieldDecl.variables.map { v ->
                            PropertyApi(
                                name = v.nameAsString,
                                visibility = fieldVisibility,
                                type = v.typeAsString,
                                isMutable = !fieldDecl.isFinal,
                                annotations = fieldDecl.annotations.map { mapJavaAnnotation(it) },
                                doc = fieldDecl.javadoc.map { it.toText() }.orElse(null),
                                sourceLocation = getLocation(v)
                            )
                        }
                    }
                } else if (typeDecl is EnumDeclaration) {
                    typeDecl.entries.mapTo(properties) { entry ->
                        PropertyApi(
                            name = entry.nameAsString,
                            visibility = Visibility.PUBLIC,
                            type = className,
                            isMutable = false,
                            annotations = entry.annotations.map { mapJavaAnnotation(it) },
                            doc = entry.javadoc.map { it.toText() }.orElse(null),
                            sourceLocation = getLocation(entry)
                        )
                    }
                } else if (typeDecl is RecordDeclaration) {
                    typeDecl.parameters.mapTo(properties) { comp ->
                        PropertyApi(
                            name = comp.nameAsString,
                            visibility = Visibility.PUBLIC,
                            type = comp.typeAsString,
                            isMutable = false,
                            annotations = comp.annotations.map { mapJavaAnnotation(it) },
                            sourceLocation = getLocation(comp)
                        )
                    }
                }

                val nested = typeDecl.members
                    .filterIsInstance<TypeDeclaration<*>>()
                    .map { it.nameAsString }

                val typeParams = if (typeDecl is ClassOrInterfaceDeclaration) {
                    mapJavaTypeParameters(typeDecl.typeParameters)
                } else emptyList()

                val superTypes = if (typeDecl is ClassOrInterfaceDeclaration) {
                    typeDecl.extendedTypes.map { it.nameAsString } + typeDecl.implementedTypes.map { it.nameAsString }
                } else emptyList()

                ClassApi(
                    name = if (packageName.isNotEmpty()) "$packageName.$className" else className,
                    simpleName = className,
                    visibility = visibility,
                    kind = kind,
                    modifiers = modifiers,
                    superTypes = superTypes,
                    annotations = annotations,
                    constructors = constructors,
                    methods = methods,
                    properties = properties,
                    nestedClasses = nested,
                    typeParameters = typeParams,
                    doc = doc,
                    sourceLocation = getLocation(typeDecl),
                    imports = imports
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapJavaConstructor(cons: ConstructorDeclaration, locGetter: (Node) -> SourceLocation?): ConstructorApi {
        val params = cons.parameters.map { p ->
            ParameterApi(
                name = p.nameAsString,
                type = p.typeAsString,
                annotations = p.annotations.map { mapJavaAnnotation(it) }
            )
        }
        val visibility = when {
            cons.isPublic -> Visibility.PUBLIC
            cons.isPrivate -> Visibility.PRIVATE
            cons.isProtected -> Visibility.PROTECTED
            else -> Visibility.PUBLIC
        }
        return ConstructorApi(
            visibility = visibility,
            parameters = params,
            annotations = cons.annotations.map { mapJavaAnnotation(it) },
            signature = "",
            sourceLocation = locGetter(cons)
        )
    }

    private fun mapJavaMethod(method: MethodDeclaration, isInterface: Boolean, locGetter: (Node) -> SourceLocation?): MethodApi {
        val params = method.parameters.map { p ->
            ParameterApi(
                name = p.nameAsString,
                type = p.typeAsString,
                annotations = p.annotations.map { mapJavaAnnotation(it) }
            )
        }
        val visibility = when {
            method.isPublic -> Visibility.PUBLIC
            method.isPrivate -> Visibility.PRIVATE
            method.isProtected -> Visibility.PROTECTED
            else -> Visibility.PUBLIC
        }
        return MethodApi(
            name = method.nameAsString,
            visibility = visibility,
            returnType = method.typeAsString,
            parameters = params,
            annotations = method.annotations.map { mapJavaAnnotation(it) },
            flags = MethodFlags(
                isStatic = method.isStatic,
                isAbstract = method.isAbstract || isInterface
            ),
            signature = "",
            typeParameters = mapJavaTypeParameters(method.typeParameters),
            doc = method.javadoc.map { it.toText() }.orElse(null),
            sourceLocation = locGetter(method)
        )
    }

    private fun mapJavaAnnotation(anno: AnnotationExpr): AnnotationApi {
        val name = anno.nameAsString
        val args = mutableMapOf<String, String>()
        if (anno is NormalAnnotationExpr) {
            anno.pairs.forEach { pair ->
                args[pair.nameAsString] = pair.value.toString()
            }
        } else if (anno is SingleMemberAnnotationExpr) {
            args["value"] = anno.memberValue.toString()
        }
        return AnnotationApi(name = name, arguments = args)
    }

    private fun mapJavaTypeParameters(tps: List<TypeParameter>): List<TypeParameterApi> {
        return tps.map { tp ->
            val bounds = tp.typeBound.map { it.nameAsString }
            TypeParameterApi(name = tp.nameAsString, upperBounds = bounds)
        }
    }
}
