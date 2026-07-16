package com.meet.libraryinsight.kotlin

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

    fun enrich(rawClass: RawClassData, metadata: KotlinClassMetadata): ClassApi {
        return when (metadata) {
            is KotlinClassMetadata.Class -> enrichClass(rawClass, metadata.kmClass)
            is KotlinClassMetadata.FileFacade -> enrichPackageFacade(rawClass, metadata.kmPackage)
            is KotlinClassMetadata.MultiFileClassPart -> enrichPackageFacade(rawClass, metadata.kmPackage)
            else -> fallbackToJava(rawClass)
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
            // Check flags or kind (OBJECT vs CLASS)
            // Kotlin metadata doesn't have an explicit enum for object, but we can detect it.
            // In 2.0.0, KmClass has ClassKind or similar, wait, kmClass has no direct isObject,
            // but Kotlin ClassKind can be retrieved via flags.
            // Let's check how objects are compiled. Companion objects have isCompanion = true.
            // Standard objects are usually compiled as FINAL classes with static field INSTANCE.
            // Let's inspect the compiled classes to verify if they have INSTANCE field or check kmClass.kind.
            // Actually, in 2.0.0, kmClass.kind is an enum kotlin.metadata.ClassKind:
            // CLASS, INTERFACE, ENUM_CLASS, ENUM_ENTRY, ANNOTATION_CLASS, OBJECT, COMPANION_OBJECT.
            // Let's map it!
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
                    hasDefaultValue = param.declaresDefaultValue
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

        // Type Parameters
        val typeParams = kmClass.typeParameters.map { param ->
            TypeParameterApi(
                name = param.name,
                upperBounds = param.upperBounds.map { formatKmType(it, typeParamsMap) }
            )
        }

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
            typeParameters = typeParams
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
        // Map local type parameters
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
                hasDefaultValue = param.declaresDefaultValue
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

        val typeParameters = kmFunction.typeParameters.map { param ->
            TypeParameterApi(
                name = param.name,
                upperBounds = param.upperBounds.map { formatKmType(it, localTypeParamsMap) }
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

    private fun formatKmType(type: KmType, typeParamsMap: Map<Int, String>): String {
        val classifierStr = when (val classifier = type.classifier) {
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
