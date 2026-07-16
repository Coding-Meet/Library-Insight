package com.meet.libraryinsight.parser

import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.Opcodes

object SignatureParser {

    /**
     * Converts a raw JVM internal name (e.g. "java/lang/String") or a descriptor (e.g. "Ljava/lang/String;")
     * to a human-readable type name (e.g. "java.lang.String").
     */
    fun parseDescriptor(descriptor: String): String {
        if (descriptor.isEmpty()) return ""
        return try {
            val type = Type.getType(descriptor)
            formatType(type)
        } catch (e: Exception) {
            // Fallback for simple class internal names
            descriptor.replace('/', '.')
        }
    }

    private fun formatType(type: Type): String {
        return when (type.sort) {
            Type.ARRAY -> formatType(type.elementType) + "[]"
            Type.OBJECT -> type.className
            else -> type.className // Primitive types (int, boolean, void, etc.)
        }
    }

    /**
     * Parses a generic method signature (e.g. "(Ljava/util/List<Ljava/lang/String;>;)V")
     * and extracts the parameter types, return type, and generic parameters.
     */
    fun parseMethodSignature(signature: String?, fallbackDescriptor: String): MethodSignatureResult {
        if (signature == null) {
            // Fallback to standard non-generic descriptor
            val returnType = parseDescriptor(Type.getReturnType(fallbackDescriptor).descriptor)
            val paramTypes = Type.getArgumentTypes(fallbackDescriptor).map { parseDescriptor(it.descriptor) }
            return MethodSignatureResult(paramTypes, returnType, emptyList())
        }

        return try {
            val reader = SignatureReader(signature)
            val visitor = MethodSignatureVisitor()
            reader.accept(visitor)
            visitor.getResult()
        } catch (e: Exception) {
            // Fallback on error
            val returnType = parseDescriptor(Type.getReturnType(fallbackDescriptor).descriptor)
            val paramTypes = Type.getArgumentTypes(fallbackDescriptor).map { parseDescriptor(it.descriptor) }
            MethodSignatureResult(paramTypes, returnType, emptyList())
        }
    }

    /**
     * Parses a generic class signature (e.g. "<T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/lang/Comparable<TT;>;")
     * and extracts superclass, implemented interfaces, and generic type parameters.
     */
    fun parseClassSignature(signature: String?, fallbackSuper: String?, fallbackInterfaces: List<String>): ClassSignatureResult {
        if (signature == null) {
            return ClassSignatureResult(
                superType = fallbackSuper?.replace('/', '.') ?: "java.lang.Object",
                interfaces = fallbackInterfaces.map { it.replace('/', '.') },
                typeParameters = emptyList()
            )
        }

        return try {
            val reader = SignatureReader(signature)
            val visitor = ClassSignatureVisitor()
            reader.accept(visitor)
            visitor.getResult()
        } catch (e: Exception) {
            ClassSignatureResult(
                superType = fallbackSuper?.replace('/', '.') ?: "java.lang.Object",
                interfaces = fallbackInterfaces.map { it.replace('/', '.') },
                typeParameters = emptyList()
            )
        }
    }

    data class MethodSignatureResult(
        val parameterTypes: List<String>,
        val returnType: String,
        val typeParameters: List<TypeParameterResult>
    )

    data class ClassSignatureResult(
        val superType: String,
        val interfaces: List<String>,
        val typeParameters: List<TypeParameterResult>
    )

    data class TypeParameterResult(
        val name: String,
        val bounds: List<String>
    )

    private class TypeSignatureVisitor(val onComplete: (String) -> Unit) : SignatureVisitor(Opcodes.ASM9) {
        private val sb = StringBuilder()
        private var arrayCount = 0
        private val typeArguments = mutableListOf<String>()

        override fun visitBaseType(descriptor: Char) {
            sb.append(Type.getType(descriptor.toString()).className)
            finish()
        }

        override fun visitTypeVariable(name: String) {
            sb.append(name)
            finish()
        }

        override fun visitArrayType(): SignatureVisitor {
            arrayCount++
            return this
        }

        override fun visitClassType(name: String) {
            sb.append(name.replace('/', '.'))
        }

        override fun visitInnerClassType(name: String) {
            sb.append('.').append(name)
        }

        override fun visitTypeArgument() {
            // Wildcard *
            typeArguments.add("?")
        }

        override fun visitTypeArgument(wildcard: Char): SignatureVisitor {
            return TypeSignatureVisitor { arg ->
                val prefix = when (wildcard) {
                    EXTENDS -> "? extends "
                    SUPER -> "? super "
                    else -> ""
                }
                typeArguments.add(prefix + arg)
            }
        }

        override fun visitEnd() {
            if (typeArguments.isNotEmpty()) {
                sb.append(typeArguments.joinToString(prefix = "<", postfix = ">"))
            }
            finish()
        }

        private fun finish() {
            for (i in 0 until arrayCount) {
                sb.append("[]")
            }
            onComplete(sb.toString())
        }
    }

    private class MethodSignatureVisitor : SignatureVisitor(Opcodes.ASM9) {
        private var currentTypeParameterName: String? = null
        private val currentTypeParameterBounds = mutableListOf<String>()
        private val typeParameters = mutableListOf<TypeParameterResult>()

        private val parameterTypes = mutableListOf<String>()
        private var returnType: String = "void"

        override fun visitFormalTypeParameter(name: String) {
            flushTypeParameter()
            currentTypeParameterName = name
        }

        override fun visitClassBound(): SignatureVisitor {
            return TypeSignatureVisitor { bound ->
                if (bound != "java.lang.Object") {
                    currentTypeParameterBounds.add(bound)
                }
            }
        }

        override fun visitInterfaceBound(): SignatureVisitor {
            return TypeSignatureVisitor { bound ->
                currentTypeParameterBounds.add(bound)
            }
        }

        override fun visitParameterType(): SignatureVisitor {
            flushTypeParameter()
            return TypeSignatureVisitor { type ->
                parameterTypes.add(type)
            }
        }

        override fun visitReturnType(): SignatureVisitor {
            flushTypeParameter()
            return TypeSignatureVisitor { type ->
                returnType = type
            }
        }

        private fun flushTypeParameter() {
            val name = currentTypeParameterName
            if (name != null) {
                typeParameters.add(TypeParameterResult(name, currentTypeParameterBounds.toList()))
                currentTypeParameterName = null
                currentTypeParameterBounds.clear()
            }
        }

        fun getResult(): MethodSignatureResult {
            flushTypeParameter()
            return MethodSignatureResult(parameterTypes, returnType, typeParameters)
        }
    }

    private class ClassSignatureVisitor : SignatureVisitor(Opcodes.ASM9) {
        private var currentTypeParameterName: String? = null
        private val currentTypeParameterBounds = mutableListOf<String>()
        private val typeParameters = mutableListOf<TypeParameterResult>()

        private var superType: String = "java.lang.Object"
        private val interfaces = mutableListOf<String>()
        private var visitingSuperclass = false

        override fun visitFormalTypeParameter(name: String) {
            flushTypeParameter()
            currentTypeParameterName = name
        }

        override fun visitClassBound(): SignatureVisitor {
            return TypeSignatureVisitor { bound ->
                if (bound != "java.lang.Object") {
                    currentTypeParameterBounds.add(bound)
                }
            }
        }

        override fun visitInterfaceBound(): SignatureVisitor {
            return TypeSignatureVisitor { bound ->
                currentTypeParameterBounds.add(bound)
            }
        }

        override fun visitSuperclass(): SignatureVisitor {
            flushTypeParameter()
            visitingSuperclass = true
            return TypeSignatureVisitor { type ->
                superType = type
                visitingSuperclass = false
            }
        }

        override fun visitInterface(): SignatureVisitor {
            flushTypeParameter()
            return TypeSignatureVisitor { type ->
                interfaces.add(type)
            }
        }

        private fun flushTypeParameter() {
            val name = currentTypeParameterName
            if (name != null) {
                typeParameters.add(TypeParameterResult(name, currentTypeParameterBounds.toList()))
                currentTypeParameterName = null
                currentTypeParameterBounds.clear()
            }
        }

        fun getResult(): ClassSignatureResult {
            flushTypeParameter()
            return ClassSignatureResult(superType, interfaces, typeParameters)
        }
    }
}
