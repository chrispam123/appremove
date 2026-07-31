package com.appremove.data

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifica la lógica de nombrado de [OutputPathResolver] contra un directorio
 * temporal real (sin mocks, sin tocar carpetas del usuario).
 */
class OutputPathResolverTest {
    private val tempDir = Files.createTempDirectory("output-path-resolver-test").toFile().apply { deleteOnExit() }

    @Test
    fun `appends _resized suffix before the extension`() {
        val input = File(tempDir, "foto.jpg").apply { createNewFile() }

        val output = OutputPathResolver().resolve(input)

        assertEquals("foto_resized.jpg", output.name)
        assertEquals(tempDir, output.parentFile)
    }

    @Test
    fun `adds a number when the default output name is already taken`() {
        val input = File(tempDir, "foto2.jpg").apply { createNewFile() }
        File(tempDir, "foto2_resized.jpg").createNewFile()

        val output = OutputPathResolver().resolve(input)

        assertEquals("foto2_resized_2.jpg", output.name)
    }

    @Test
    fun `keeps working for files without extension`() {
        val input = File(tempDir, "sinextension").apply { createNewFile() }

        val output = OutputPathResolver().resolve(input)

        assertEquals("sinextension_resized", output.name)
    }
}
