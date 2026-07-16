package com.meet.libraryinsight.model

import kotlinx.serialization.Serializable

@Serializable
data class LibraryApiIndex(
    val libraryName: String,
    val version: String,
    val packages: List<PackageApi>
)

@Serializable
data class PackageApi(
    val name: String,
    val classes: List<ClassApi>
)

@Serializable
data class ClassApi(
    val name: String, // Fully qualified binary name (e.g. com/meet/libraryinsight/ClassApi or com.meet.libraryinsight.ClassApi)
    val simpleName: String,
    val visibility: Visibility,
    val kind: ClassKind,
    val modifiers: List<String>, // "abstract", "sealed", "data", "value", "inner", "open", "fun", etc.
    val superTypes: List<String>, // Superclass and interface names
    val annotations: List<AnnotationApi>,
    val constructors: List<ConstructorApi>,
    val methods: List<MethodApi>,
    val properties: List<PropertyApi>,
    val nestedClasses: List<String>, // Fully qualified names of nested classes
    val typeParameters: List<TypeParameterApi> = emptyList(),
    val doc: String? = null,
    val sourceCode: String? = null
)

@Serializable
enum class Visibility {
    PUBLIC, PROTECTED, INTERNAL, PRIVATE
}

@Serializable
enum class ClassKind {
    CLASS, INTERFACE, ENUM, ANNOTATION, OBJECT, COMPANION_OBJECT
}

@Serializable
data class AnnotationApi(
    val name: String, // Fully qualified name of the annotation class
    val arguments: Map<String, String> = emptyMap() // key to stringified value
)

@Serializable
data class ConstructorApi(
    val visibility: Visibility,
    val parameters: List<ParameterApi>,
    val annotations: List<AnnotationApi>,
    val signature: String // JVM descriptor signature
)

@Serializable
data class MethodApi(
    val name: String,
    val visibility: Visibility,
    val returnType: String,
    val parameters: List<ParameterApi>,
    val annotations: List<AnnotationApi>,
    val flags: MethodFlags,
    val extensionReceiverType: String? = null,
    val signature: String, // JVM descriptor signature
    val typeParameters: List<TypeParameterApi> = emptyList(),
    val doc: String? = null,
    val sourceCode: String? = null
)

@Serializable
data class MethodFlags(
    val isSuspend: Boolean = false,
    val isInline: Boolean = false,
    val isOperator: Boolean = false,
    val isInfix: Boolean = false,
    val isStatic: Boolean = false,
    val isAbstract: Boolean = false,
    val isOpen: Boolean = false,
    val isFinal: Boolean = false
)

@Serializable
data class ParameterApi(
    val name: String,
    val type: String,
    val annotations: List<AnnotationApi> = emptyList(),
    val hasDefaultValue: Boolean = false
)

@Serializable
data class PropertyApi(
    val name: String,
    val visibility: Visibility,
    val type: String,
    val isMutable: Boolean, // var vs val
    val annotations: List<AnnotationApi> = emptyList(),
    val getterVisibility: Visibility? = null,
    val setterVisibility: Visibility? = null,
    val isConst: Boolean = false,
    val isLateinit: Boolean = false,
    val doc: String? = null,
    val sourceCode: String? = null
)

@Serializable
data class TypeParameterApi(
    val name: String,
    val upperBounds: List<String> = emptyList()
)
