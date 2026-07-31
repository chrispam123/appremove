package com.appremove.coreimage

import com.appremove.domain.resize.ImageResizeResult
import com.appremove.domain.resize.ImageResizer
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

// Calidad de compresión JPEG explícita: el writer por defecto de ImageIO
// no siempre comprime lo suficiente y puede devolver un archivo más pesado
// que el original si no se fija este valor.
private const val JPEG_QUALITY = 0.82f

/**
 * Implementación real de [ImageResizer] usando solo las clases estándar de la JVM
 * (`javax.imageio` + `java.awt`), sin dependencias externas. Escala la imagen
 * manteniendo la proporción hasta que su lado mayor mida [maxDimension] px o menos;
 * si la imagen ya es más chica que eso, no la agranda.
 *
 * Para el usuario "reducir tamaño" significa "que pese menos", y volver a
 * codificar una imagen no siempre logra eso (una imagen ya optimizada, o un PNG
 * sin pérdida, puede terminar pesando más al reescribirse). Por eso, si el
 * resultado no pesa menos que el original, se devuelve una copia del original
 * en vez del reescrito: nunca se entrega un archivo más pesado que el de entrada.
 */
class BufferedImageResizer : ImageResizer {
    override fun resize(
        input: File,
        output: File,
        maxDimension: Int,
    ): ImageResizeResult {
        val original = ImageIO.read(input) ?: throw IOException("No se pudo leer la imagen: ${input.path}")

        // coerceAtMost(1.0) evita agrandar imágenes que ya son más chicas que maxDimension.
        val scale = (maxDimension.toDouble() / max(original.width, original.height)).coerceAtMost(1.0)
        val targetWidth = (original.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (original.height * scale).roundToInt().coerceAtLeast(1)

        // Se conserva el formato del archivo de entrada (jpg, png, etc.) para la salida.
        val format = input.extension.ifBlank { "jpg" }
        val scaled = scaleImage(original, targetWidth, targetHeight, format)

        output.parentFile?.mkdirs()
        writeImage(scaled, format, output)

        if (output.length() >= input.length()) {
            input.copyTo(output, overwrite = true)
            return ImageResizeResult(
                originalWidth = original.width,
                originalHeight = original.height,
                originalBytes = input.length(),
                resizedWidth = original.width,
                resizedHeight = original.height,
                resizedBytes = output.length(),
            )
        }

        return ImageResizeResult(
            originalWidth = original.width,
            originalHeight = original.height,
            originalBytes = input.length(),
            resizedWidth = targetWidth,
            resizedHeight = targetHeight,
            resizedBytes = output.length(),
        )
    }

    /**
     * Escribe [image] en [output] con el [format] pedido. Para JPEG se fija
     * explícitamente [JPEG_QUALITY] vía [ImageWriteParam]; para el resto de los
     * formatos (ej. PNG) se usa el writer por defecto de ImageIO.
     */
    private fun writeImage(
        image: BufferedImage,
        format: String,
        output: File,
    ) {
        val isJpeg = format.equals("jpg", ignoreCase = true) || format.equals("jpeg", ignoreCase = true)
        if (!isJpeg) {
            if (!ImageIO.write(image, format, output)) {
                throw IOException("Formato de imagen no soportado para escribir: $format")
            }
            return
        }

        val writer = ImageIO.getImageWritersByFormatName("jpg").next()
        val writeParam =
            writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = JPEG_QUALITY
            }
        FileImageOutputStream(output).use { stream ->
            writer.output = stream
            writer.write(null, IIOImage(image, null, null), writeParam)
            writer.dispose()
        }
    }

    /**
     * Dibuja [original] escalada a [targetWidth]x[targetHeight] sobre un lienzo nuevo.
     * JPEG no soporta canal alfa, así que para ese formato se rellena un fondo blanco;
     * para el resto de los formatos (png, etc.) se preserva la transparencia.
     */
    private fun scaleImage(
        original: BufferedImage,
        targetWidth: Int,
        targetHeight: Int,
        format: String,
    ): BufferedImage {
        val supportsAlpha = !format.equals("jpg", ignoreCase = true) && !format.equals("jpeg", ignoreCase = true)
        val type = if (supportsAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB

        val scaled = BufferedImage(targetWidth, targetHeight, type)
        val graphics: Graphics2D = scaled.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            if (!supportsAlpha) {
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, targetWidth, targetHeight)
            }
            graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null)
        } finally {
            graphics.dispose()
        }
        return scaled
    }
}
