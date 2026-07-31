package com.meet.libraryinsight.model

import kotlinx.serialization.Serializable

@Serializable
enum class ScanMode {
    BYTECODE, SOURCE
}

@Serializable
data class SourceLocation(
    val file: String,
    val line: Int,
    val column: Int
)

@Serializable
data class LibraryApiIndex(
    val libraryName: String,
    val version: String,
    val packages: List<PackageApi>,
    val scanMode: ScanMode = ScanMode.BYTECODE
)

@Serializable
data class PackageApi(
    val name: String,
    val classes: List<ClassApi>,
    /** Top-level type aliases declared in this package (from Kotlin FileFacade metadata). */
    val typeAliases: List<TypeAliasApi> = emptyList()
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
    /** Names of @DslMarker-annotated annotations applied to this class (marks DSL scopes). */
    val dslMarkerAnnotations: List<String> = emptyList(),
    val doc: String? = null,
    val sourceCode: String? = null,
    val documentationExamples: List<String> = emptyList(),
    val sourceLocation: SourceLocation? = null,
    val imports: List<String> = emptyList()
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
    val signature: String, // JVM descriptor signature
    val sourceLocation: SourceLocation? = null
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
    val sourceCode: String? = null,
    val sourceLocation: SourceLocation? = null
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
    val hasDefaultValue: Boolean = false,
    /** True if this parameter is a function type with a receiver (lambda-with-receiver / DSL block). */
    val isLambdaReceiver: Boolean = false
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
    val sourceCode: String? = null,
    val sourceLocation: SourceLocation? = null
)

@Serializable
data class TypeParameterApi(
    val name: String,
    val upperBounds: List<String> = emptyList(),
    /** True if this type parameter is reified (only valid on inline functions). */
    val isReified: Boolean = false
)

/**
 * A Kotlin type alias declaration.
 * e.g. `typealias BuilderBlock = Builder.() -> Unit`
 */
@Serializable
data class TypeAliasApi(
    val name: String,
    /** The fully expanded underlying type after alias resolution. */
    val expandedType: String,
    val typeParameters: List<TypeParameterApi> = emptyList(),
    val annotations: List<AnnotationApi> = emptyList(),
    val doc: String? = null
)
