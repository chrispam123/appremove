package com.appremove.coreimage

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Prueba [BufferedImageResizer] contra archivos reales en un directorio temporal
 * (sin mocks): genera una imagen en memoria, la escribe a disco y valida el resultado.
 */
class BufferedImageResizerTest {
    @Test
    fun `scales down a large image and reduces its file size`() {
        val input = File.createTempFile("resizer-test-input", ".jpg").apply { deleteOnExit() }
        writeNoisyImage(input, width = 2000, height = 1000)
        val originalBytes = input.length()
        val output = File.createTempFile("resizer-test-output", ".jpg").apply { deleteOnExit() }

        val result = BufferedImageResizer().resize(input, output, maxDimension = 1000)

        assertEquals(2000, result.originalWidth)
        assertEquals(1000, result.originalHeight)
        assertEquals(1000, result.resizedWidth)
        assertEquals(500, result.resizedHeight)
        assertTrue(output.exists() && output.length() > 0)
        assertEquals(originalBytes, input.length())
        // La razón de ser del bug que motivó este test: el resultado nunca debe
        // pesar igual o más que el original.
        assertTrue(result.resizedBytes < result.originalBytes)

        val writtenImage = ImageIO.read(output)
        assertEquals(1000, writtenImage.width)
        assertEquals(500, writtenImage.height)
    }

    @Test
    fun `does not upscale an image already smaller than maxDimension`() {
        val input = File.createTempFile("resizer-test-small-input", ".png").apply { deleteOnExit() }
        writeSolidImage(input, width = 200, height = 100)
        val output = File.createTempFile("resizer-test-small-output", ".png").apply { deleteOnExit() }

        val result = BufferedImageResizer().resize(input, output, maxDimension = 1280)

        assertEquals(200, result.resizedWidth)
        assertEquals(100, result.resizedHeight)
        assertTrue(result.resizedBytes <= result.originalBytes)
    }

    @Test
    fun `never returns a file heavier than the original, even on a second pass`() {
        val original = File.createTempFile("resizer-test-twice-original", ".jpg").apply { deleteOnExit() }
        writeNoisyImage(original, width = 1200, height = 800)

        val firstPass = File.createTempFile("resizer-test-twice-first", ".jpg").apply { deleteOnExit() }
        BufferedImageResizer().resize(original, firstPass, maxDimension = 1280)

        // Segunda pasada sobre un archivo que ya fue comprimido antes: es el
        // escenario real que exponía el bug (re-codificar algo ya optimizado).
        val secondPass = File.createTempFile("resizer-test-twice-second", ".jpg").apply { deleteOnExit() }
        val result = BufferedImageResizer().resize(firstPass, secondPass, maxDimension = 1280)

        assertTrue(result.resizedBytes <= result.originalBytes)
    }

    private fun writeSolidImage(
        file: File,
        width: Int,
        height: Int,
    ) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        ImageIO.write(image, file.extension, file)
    }

    /**
     * Genera ruido aleatorio píxel a píxel (sin ningún patrón repetible), para que
     * el peso del archivo JPEG escale de forma predecible con la cantidad de
     * píxeles. Un patrón con bloques alineados a 8px (el tamaño de bloque de
     * JPEG) comprimiría casi a cero sin importar el tamaño, lo cual no sirve para
     * probar que reducir la resolución realmente reduce el peso del archivo.
     */
    private fun writeNoisyImage(
        file: File,
        width: Int,
        height: Int,
    ) {
        val random = Random(seed = 42)
        val pixels = IntArray(width * height) { random.nextInt() }
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, width, height, pixels, 0, width)
        ImageIO.write(image, file.extension, file)
    }
}
