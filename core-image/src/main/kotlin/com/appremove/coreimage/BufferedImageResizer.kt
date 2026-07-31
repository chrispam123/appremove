package com.appremove.coreimage

import com.appremove.domain.resize.ImageResizeResult
import com.appremove.domain.resize.ImageResizer
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Implementación real de [ImageResizer] usando solo las clases estándar de la JVM
 * (`javax.imageio` + `java.awt`), sin dependencias externas. Escala la imagen
 * manteniendo la proporción hasta que su lado mayor mida [maxDimension] px o menos;
 * si la imagen ya es más chica que eso, no la agranda.
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
        if (!ImageIO.write(scaled, format, output)) {
            throw IOException("Formato de imagen no soportado para escribir: $format")
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
