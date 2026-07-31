package com.appremove.coreimage

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Prueba [BufferedImageResizer] contra archivos reales en un directorio temporal
 * (sin mocks): genera una imagen en memoria, la escribe a disco y valida el resultado.
 */
class BufferedImageResizerTest {
    @Test
    fun `scales down a large image and keeps the original untouched`() {
        val input = File.createTempFile("resizer-test-input", ".jpg").apply { deleteOnExit() }
        writeSolidImage(input, width = 2000, height = 1000)
        val originalBytes = input.length()
        val output = File.createTempFile("resizer-test-output", ".jpg").apply { deleteOnExit() }

        val result = BufferedImageResizer().resize(input, output, maxDimension = 1000)

        assertEquals(2000, result.originalWidth)
        assertEquals(1000, result.originalHeight)
        assertEquals(1000, result.resizedWidth)
        assertEquals(500, result.resizedHeight)
        assertTrue(output.exists() && output.length() > 0)
        assertEquals(originalBytes, input.length())

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
}
