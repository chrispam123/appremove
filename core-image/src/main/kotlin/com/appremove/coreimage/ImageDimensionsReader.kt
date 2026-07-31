package com.appremove.coreimage

import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

/** Ancho y alto de una imagen, en píxeles. */
data class ImageDimensions(
    val width: Int,
    val height: Int,
)

/**
 * Lee el ancho/alto de un archivo de imagen sin decodificar los píxeles
 * (usa el header que exponen los `ImageReader` de ImageIO), para poder
 * mostrar las dimensiones originales en la UI apenas se elige un archivo,
 * sin pagar el costo de decodificar la imagen completa solo para eso.
 */
class ImageDimensionsReader {
    fun read(input: File): ImageDimensions {
        val stream = ImageIO.createImageInputStream(input) ?: throw IOException("No se pudo leer la imagen: ${input.path}")
        stream.use {
            val readers = ImageIO.getImageReaders(it)
            if (!readers.hasNext()) {
                throw IOException("No se pudo leer la imagen: ${input.path}")
            }
            val reader = readers.next()
            try {
                reader.input = it
                return ImageDimensions(width = reader.getWidth(0), height = reader.getHeight(0))
            } finally {
                reader.dispose()
            }
        }
    }
}
