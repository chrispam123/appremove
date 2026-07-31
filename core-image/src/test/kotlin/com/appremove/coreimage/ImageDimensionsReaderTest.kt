package com.appremove.coreimage

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Prueba [ImageDimensionsReader] contra un archivo real generado en memoria.
 */
class ImageDimensionsReaderTest {
    @Test
    fun `reads width and height from a real file`() {
        val file = File.createTempFile("dimensions-test", ".png").apply { deleteOnExit() }
        val image = BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(image, "png", file)

        val dimensions = ImageDimensionsReader().read(file)

        assertEquals(640, dimensions.width)
        assertEquals(480, dimensions.height)
    }
}
