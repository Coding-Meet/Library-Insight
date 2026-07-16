package com.meet.libraryinsight.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignatureParserTest {

    @Test
    fun testParseSimpleDescriptor() {
        assertEquals("java.lang.String", SignatureParser.parseDescriptor("Ljava/lang/String;"))
        assertEquals("int", SignatureParser.parseDescriptor("I"))
        assertEquals("java.lang.String[]", SignatureParser.parseDescriptor("[Ljava/lang/String;"))
        assertEquals("void", SignatureParser.parseDescriptor("V"))
    }

    @Test
    fun testParseMethodSignature() {
        val sig = "(Ljava/util/List<Ljava/lang/String;>;)V"
        val result = SignatureParser.parseMethodSignature(sig, "(Ljava/util/List;)V")
        
        assertEquals(1, result.parameterTypes.size)
        assertEquals("java.util.List<java.lang.String>", result.parameterTypes[0])
        assertEquals("void", result.returnType)
    }

    @Test
    fun testParseClassSignature() {
        val sig = "<T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/lang/Comparable<TT;>;"
        val result = SignatureParser.parseClassSignature(sig, "java/lang/Object", listOf("java/lang/Comparable"))
        
        assertEquals("java.lang.Object", result.superType)
        assertEquals(1, result.interfaces.size)
        assertEquals("java.lang.Comparable<T>", result.interfaces[0])
        assertEquals(1, result.typeParameters.size)
        assertEquals("T", result.typeParameters[0].name)
    }
}
