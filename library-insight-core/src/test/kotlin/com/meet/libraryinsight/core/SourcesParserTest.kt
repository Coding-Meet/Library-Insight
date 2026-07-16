package com.meet.libraryinsight.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SourcesParserTest {

    @Test
    fun testParseSimpleSource() {
        val source = """
            package com.example
            
            /**
             * This is a sample class comment.
             * Multi-line.
             */
            class Target {
            
                /**
                 * Method comment.
                 */
                fun run(param: String): Int {
                    val x = 10
                    return x
                }
                
                /**
                 * A property comment.
                 */
                val item: String = "test"
            }
        """.trimIndent()

        val parsed = SourcesParser.parse(source)
        val targetClassInfo = parsed.classes["Target"]
        assertNotNull(targetClassInfo)
        assertEquals("This is a sample class comment.\nMulti-line.", targetClassInfo.doc)
        
        val runOverloads = parsed.methods["run"]
        assertNotNull(runOverloads)
        assertEquals(1, runOverloads.size)
        assertEquals("Method comment.", runOverloads[0].doc)
        assertEquals("fun run(param: String): Int {\n        val x = 10\n        return x\n    }", runOverloads[0].sourceCode)

        val itemProp = parsed.properties["item"]
        assertNotNull(itemProp)
        assertEquals("A property comment.", itemProp.doc)
    }
}
