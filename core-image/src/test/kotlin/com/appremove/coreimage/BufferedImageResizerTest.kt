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
    fun `resizes to the exact target dimensions, even if they do not match the original aspect ratio`() {
        val input = File.createTempFile("resizer-test-input", ".jpg").apply { deleteOnExit() }
        writeNoisyImage(input, width = 2000, height = 1000)
        val output = File.createTempFile("resizer-test-output", ".jpg").apply { deleteOnExit() }

        // 2000x1000 es 2:1; pedimos 900x900 (1:1) a propósito para probar que
        // el resizer no intenta "corregir" la proporción por su cuenta.
        val result = BufferedImageResizer().resize(input, output, targetWidth = 900, targetHeight = 900)

        assertEquals(2000, result.originalWidth)
        assertEquals(1000, result.originalHeight)
        assertEquals(900, result.resizedWidth)
        assertEquals(900, result.resizedHeight)
        assertTrue(output.exists() && output.length() > 0)

        val writtenImage = ImageIO.read(output)
        assertEquals(900, writtenImage.width)
        assertEquals(900, writtenImage.height)
    }

    @Test
    fun `upscales when the target size is bigger than the original`() {
        val input = File.createTempFile("resizer-test-small-input", ".jpg").apply { deleteOnExit() }
        writeNoisyImage(input, width = 100, height = 100)
        val output = File.createTempFile("resizer-test-small-output", ".jpg").apply { deleteOnExit() }

        val result = BufferedImageResizer().resize(input, output, targetWidth = 500, targetHeight = 300)

        assertEquals(500, result.resizedWidth)
        assertEquals(300, result.resizedHeight)

        val writtenImage = ImageIO.read(output)
        assertEquals(500, writtenImage.width)
        assertEquals(300, writtenImage.height)
    }

    /**
     * Genera ruido aleatorio píxel a píxel (sin ningún patrón repetible), para
     * que el resizer trabaje sobre contenido realista en vez de un color sólido.
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
