package com.meet.libraryinsight.parser

import com.meet.libraryinsight.model.ClassKind
import com.meet.libraryinsight.model.Visibility
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SourceParserTest {

    @Test
    fun testParseKotlinSource() {
        val ktCode = """
            package com.meet.test
            
            import kotlinx.coroutines.Dispatchers
            import retrofit2.Retrofit
            
            class MyService(val id: Int, var name: String) {
                private val internalKey: String = "secret"
                
                fun executeTask(param: String): Boolean {
                    return true
                }
                
                private fun helper() {}
            }
            
            fun globalUtility(input: Int): String = input.toString()
        """.trimIndent()

        val tempFile = File.createTempFile("MyService", ".kt")
        tempFile.writeText(ktCode)

        try {
            val classes = KotlinSourceParser.parse(tempFile)
            
            // Should have MyService class and MyServiceKt file facade class
            assertEquals(2, classes.size)

            val serviceClass = classes.find { it.name == "com.meet.test.MyService" }
            assertNotNull(serviceClass)
            assertEquals(ClassKind.CLASS, serviceClass.kind)
            assertEquals(Visibility.PUBLIC, serviceClass.visibility)

            // Verify source location
            assertNotNull(serviceClass.sourceLocation)
            assertTrue(serviceClass.sourceLocation!!.file.contains("MyService"))
            // MyService declaration is on line 6 (0-indexed line 5)
            assertEquals(6, serviceClass.sourceLocation!!.line)

            // Verify imports
            assertEquals(2, serviceClass.imports.size)
            assertTrue(serviceClass.imports.contains("kotlinx.coroutines.Dispatchers"))
            assertTrue(serviceClass.imports.contains("retrofit2.Retrofit"))

            // Verify constructor
            assertEquals(1, serviceClass.constructors.size)
            assertEquals(2, serviceClass.constructors[0].parameters.size)
            assertEquals("id", serviceClass.constructors[0].parameters[0].name)
            assertEquals("Int", serviceClass.constructors[0].parameters[0].type)
            // Constructor is on same line as Class
            assertEquals(6, serviceClass.constructors[0].sourceLocation!!.line)

            // Verify properties (should include val/var parameters from constructor and internalKey)
            assertEquals(3, serviceClass.properties.size)
            assertTrue(serviceClass.properties.any { it.name == "id" && it.type == "Int" && !it.isMutable })
            assertTrue(serviceClass.properties.any { it.name == "name" && it.type == "String" && it.isMutable })
            
            val internalKeyProp = serviceClass.properties.find { it.name == "internalKey" }
            assertNotNull(internalKeyProp)
            assertEquals(Visibility.PRIVATE, internalKeyProp.visibility)
            assertEquals(7, internalKeyProp.sourceLocation!!.line)

            // Verify methods
            assertEquals(2, serviceClass.methods.size)
            val taskMethod = serviceClass.methods.find { it.name == "executeTask" }
            assertNotNull(taskMethod)
            assertEquals("Boolean", taskMethod.returnType)
            assertEquals(Visibility.PUBLIC, taskMethod.visibility)
            assertEquals(9, taskMethod.sourceLocation!!.line)

        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testParseJavaSource() {
        val javaCode = """
            package com.meet.test;
            
            import java.util.List;
            import java.util.ArrayList;
            
            public class User {
                private String username;
                public final int id;
                
                public User(int id, String username) {
                    this.id = id;
                    this.username = username;
                }
                
                public String getUsername() {
                    return username;
                }
                
                private void updateInternalState() {}
            }
        """.trimIndent()

        val tempFile = File.createTempFile("User", ".java")
        tempFile.writeText(javaCode)

        try {
            val classes = JavaSourceParser.parse(tempFile)
            assertEquals(1, classes.size)

            val userClass = classes[0]
            assertEquals("com.meet.test.User", userClass.name)
            assertEquals(ClassKind.CLASS, userClass.kind)
            assertEquals(Visibility.PUBLIC, userClass.visibility)

            // Verify source location
            assertNotNull(userClass.sourceLocation)
            assertEquals(6, userClass.sourceLocation!!.line)

            // Verify imports
            assertEquals(2, userClass.imports.size)
            assertTrue(userClass.imports.contains("java.util.List"))
            assertTrue(userClass.imports.contains("java.util.ArrayList"))

            // Verify properties (fields)
            assertEquals(2, userClass.properties.size)
            val usernameProp = userClass.properties.find { it.name == "username" }
            assertNotNull(usernameProp)
            assertEquals(7, usernameProp.sourceLocation!!.line)

            // Verify constructor
            assertEquals(1, userClass.constructors.size)
            assertEquals(10, userClass.constructors[0].sourceLocation!!.line)

            // Verify methods
            assertEquals(2, userClass.methods.size)
            val getUsernameMethod = userClass.methods.find { it.name == "getUsername" }
            assertNotNull(getUsernameMethod)
            assertEquals(15, getUsernameMethod.sourceLocation!!.line)

        } finally {
            tempFile.delete()
        }
    }
}
