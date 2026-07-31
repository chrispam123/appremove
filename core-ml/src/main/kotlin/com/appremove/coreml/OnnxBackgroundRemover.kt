package com.appremove.coreml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.appremove.domain.bgremoval.BackgroundChoice
import com.appremove.domain.bgremoval.BackgroundRemovalResult
import com.appremove.domain.bgremoval.BackgroundRemover
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.FloatBuffer
import javax.imageio.ImageIO
import kotlin.math.roundToInt

// Tamaño de entrada fijo que espera el modelo IS-Net "general use": toda
// imagen se reescala a este cuadrado antes de inferir, y la máscara resultante
// se vuelve a escalar al tamaño original de la imagen después.
private const val MODEL_INPUT_SIZE = 1024

// Un archivo real de ~170MB nunca puede confundirse con el puntero de texto
// que deja Git LFS cuando no se bajó el contenido real (unos pocos cientos de
// bytes) — este umbral alcanza para distinguir ambos casos sin ambigüedad.
private const val MIN_VALID_MODEL_BYTES = 10_000_000L

/**
 * Implementación real de [BackgroundRemover] usando ONNX Runtime + el modelo
 * IS-Net "general use" embebido en `core-ml/src/main/resources/models/`.
 *
 * El pre/post-procesamiento sigue exactamente al de rembg (el proyecto del
 * que sale este modelo, ver [[project_bg_removal_model]] en la memoria del
 * proyecto), verificado leyendo su código fuente: la imagen se reescala a
 * 1024x1024, se normaliza dividiendo cada canal por el valor de píxel más
 * alto de la propia imagen (no un /255 fijo) y centrando en 0.5; la máscara
 * de salida se normaliza min-max y se reescala de vuelta al tamaño original
 * para usarse como canal alfa.
 *
 * La sesión de ONNX Runtime se crea una sola vez, de forma perezosa, en el
 * primer uso: crearla de nuevo en cada llamada sería carísimo, porque implica
 * volver a parsear los ~170MB del modelo.
 */
class OnnxBackgroundRemover : BackgroundRemover {
    private val environment by lazy { OrtEnvironment.getEnvironment() }
    private val session by lazy { createSession() }

    override fun removeBackground(
        input: File,
        output: File,
        background: BackgroundChoice,
    ): BackgroundRemovalResult {
        val original = ImageIO.read(input) ?: throw IOException("No se pudo leer la imagen: ${input.path}")

        val mask = runInference(original)
        val resultImage =
            when (background) {
                is BackgroundChoice.Transparent -> applyMaskAsAlpha(original, mask)
                is BackgroundChoice.SolidColor -> compositeOverColor(original, mask, background)
                is BackgroundChoice.Image -> compositeOverImage(original, mask, background.file)
            }

        output.parentFile?.mkdirs()
        if (!ImageIO.write(resultImage, "png", output)) {
            throw IOException("No se pudo escribir el PNG de salida: ${output.path}")
        }

        return BackgroundRemovalResult(
            width = original.width,
            height = original.height,
            originalBytes = input.length(),
            resultBytes = output.length(),
        )
    }

    /** Corre [original] a través del modelo y devuelve la máscara en escala de grises, ya reescalada a su tamaño. */
    private fun runInference(original: BufferedImage): BufferedImage {
        val inputName = session.inputNames.iterator().next()
        val shape = longArrayOf(1, 3, MODEL_INPUT_SIZE.toLong(), MODEL_INPUT_SIZE.toLong())

        OnnxTensor.createTensor(environment, toModelInputTensor(original), shape).use { inputTensor ->
            session.run(mapOf(inputName to inputTensor)).use { outputs ->
                @Suppress("UNCHECKED_CAST")
                val rawOutput = outputs[0].value as Array<Array<Array<FloatArray>>>
                // rawOutput tiene forma [batch=1][canal=1][alto][ancho]; a nosotros
                // nos interesa solo la única imagen/canal que produce este modelo.
                val maskAtModelSize = rawOutput[0][0]
                return maskToGrayscaleImage(maskAtModelSize, original.width, original.height)
            }
        }
    }

    /**
     * Convierte [image] al buffer NCHW `[1,3,1024,1024]` que espera el modelo:
     * la reescala a 1024x1024 y normaliza cada canal como `(v/max - 0.5)`,
     * donde `max` es el valor de píxel más alto de la propia imagen (así
     * preprocesa rembg, la referencia de este modelo).
     */
    private fun toModelInputTensor(image: BufferedImage): FloatBuffer {
        val resized = resizeSquare(image, MODEL_INPUT_SIZE)

        var maxValue = 1
        for (y in 0 until MODEL_INPUT_SIZE) {
            for (x in 0 until MODEL_INPUT_SIZE) {
                val rgb = resized.getRGB(x, y)
                maxValue = maxOf(maxValue, (rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)
            }
        }

        val buffer = FloatBuffer.allocate(3 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        // Layout NCHW: primero los MODEL_INPUT_SIZE² valores de R, después G, después B.
        for (channelShift in intArrayOf(16, 8, 0)) {
            for (y in 0 until MODEL_INPUT_SIZE) {
                for (x in 0 until MODEL_INPUT_SIZE) {
                    val value = (resized.getRGB(x, y) shr channelShift) and 0xFF
                    buffer.put((value.toFloat() / maxValue) - 0.5f)
                }
            }
        }
        buffer.rewind()
        return buffer
    }

    /** Máscara cruda de salida del modelo (min-max normalizada a [0,255]) reescalada a [targetWidth]x[targetHeight]. */
    private fun maskToGrayscaleImage(
        rawMask: Array<FloatArray>,
        targetWidth: Int,
        targetHeight: Int,
    ): BufferedImage {
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        for (row in rawMask) {
            for (value in row) {
                if (value < min) min = value
                if (value > max) max = value
            }
        }
        val range = (max - min).takeIf { it > 1e-6f } ?: 1f

        val maskImage = BufferedImage(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, BufferedImage.TYPE_BYTE_GRAY)
        for (y in 0 until MODEL_INPUT_SIZE) {
            for (x in 0 until MODEL_INPUT_SIZE) {
                val normalized = ((rawMask[y][x] - min) / range).coerceIn(0f, 1f)
                maskImage.raster.setSample(x, y, 0, (normalized * 255).toInt())
            }
        }

        return resizeSquareToRect(maskImage, targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY)
    }

    /** Combina el RGB de [original] con [mask] como canal alfa: blanco = opaco, negro = transparente. */
    private fun applyMaskAsAlpha(
        original: BufferedImage,
        mask: BufferedImage,
    ): BufferedImage {
        val result = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until original.height) {
            for (x in 0 until original.width) {
                val rgb = original.getRGB(x, y) and 0x00FFFFFF
                val alpha = mask.raster.getSample(x, y, 0) shl 24
                result.setRGB(x, y, rgb or alpha)
            }
        }
        return result
    }

    /** Compone [original] sobre un fondo de un único [color], usando [mask] como opacidad del sujeto. */
    private fun compositeOverColor(
        original: BufferedImage,
        mask: BufferedImage,
        color: BackgroundChoice.SolidColor,
    ): BufferedImage {
        val backgroundRgb = (color.red shl 16) or (color.green shl 8) or color.blue
        val result = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until original.height) {
            for (x in 0 until original.width) {
                val alpha = mask.raster.getSample(x, y, 0)
                result.setRGB(x, y, blendPixel(original.getRGB(x, y), backgroundRgb, alpha))
            }
        }
        return result
    }

    /**
     * Compone [original] sobre otra imagen ([backgroundFile]) usando [mask] como opacidad
     * del sujeto. El fondo se reescala con la estrategia "cover" (como CSS `background-size:
     * cover`): cubre todo el lienzo recortando el sobrante, en vez de estirarse o dejar bordes.
     */
    private fun compositeOverImage(
        original: BufferedImage,
        mask: BufferedImage,
        backgroundFile: File,
    ): BufferedImage {
        val backgroundOriginal =
            ImageIO.read(backgroundFile)
                ?: throw IOException("No se pudo leer la imagen de fondo: ${backgroundFile.path}")
        val background = resizeCover(backgroundOriginal, original.width, original.height)

        val result = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until original.height) {
            for (x in 0 until original.width) {
                val alpha = mask.raster.getSample(x, y, 0)
                result.setRGB(x, y, blendPixel(original.getRGB(x, y), background.getRGB(x, y), alpha))
            }
        }
        return result
    }

    /**
     * Mezcla [fgRgb] (sujeto) y [bgRgb] (fondo nuevo) según [alpha] (0-255): el operador
     * alfa estándar "over", canal por canal — `resultado = fg×alfa + bg×(1−alfa)`.
     * El resultado siempre es opaco (no hace falta canal alfa: ya no hay transparencia).
     */
    private fun blendPixel(
        fgRgb: Int,
        bgRgb: Int,
        alpha: Int,
    ): Int {
        fun blendChannel(shift: Int): Int {
            val fg = (fgRgb shr shift) and 0xFF
            val bg = (bgRgb shr shift) and 0xFF
            return (fg * alpha + bg * (255 - alpha)) / 255
        }
        val opaqueAlpha = 0xFF shl 24
        return opaqueAlpha or (blendChannel(16) shl 16) or (blendChannel(8) shl 8) or blendChannel(0)
    }

    /**
     * Reescala [image] para cubrir un lienzo de [targetWidth]x[targetHeight], recortando
     * el sobrante centrado (estrategia "cover"): evita deformar la imagen de fondo o
     * dejarla más chica con bordes vacíos.
     */
    private fun resizeCover(
        image: BufferedImage,
        targetWidth: Int,
        targetHeight: Int,
    ): BufferedImage {
        val scale = maxOf(targetWidth.toDouble() / image.width, targetHeight.toDouble() / image.height)
        val scaledWidth = (image.width * scale).roundToInt()
        val scaledHeight = (image.height * scale).roundToInt()
        val offsetX = (scaledWidth - targetWidth) / 2
        val offsetY = (scaledHeight - targetHeight) / 2

        val result = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val graphics: Graphics2D = result.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.drawImage(image, -offsetX, -offsetY, scaledWidth, scaledHeight, null)
        } finally {
            graphics.dispose()
        }
        return result
    }

    private fun resizeSquare(
        image: BufferedImage,
        size: Int,
    ): BufferedImage = resizeSquareToRect(image, size, size, BufferedImage.TYPE_INT_RGB)

    private fun resizeSquareToRect(
        image: BufferedImage,
        width: Int,
        height: Int,
        type: Int,
    ): BufferedImage {
        val resized = BufferedImage(width, height, type)
        val graphics: Graphics2D = resized.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.drawImage(image, 0, 0, width, height, null)
        } finally {
            graphics.dispose()
        }
        return resized
    }

    private fun createSession(): OrtSession {
        val modelBytes = loadModelBytes()
        if (modelBytes.size < MIN_VALID_MODEL_BYTES) {
            throw IOException(
                "El modelo isnet-general-use.onnx no está disponible completo (solo ${modelBytes.size} bytes). " +
                    "¿Faltó bajar el contenido real de Git LFS? Corré 'git lfs pull'.",
            )
        }
        return environment.createSession(modelBytes)
    }

    private fun loadModelBytes(): ByteArray {
        val resource =
            javaClass.getResourceAsStream("/models/isnet-general-use.onnx")
                ?: throw IOException("No se encontró el recurso del modelo isnet-general-use.onnx")
        return resource.use { it.readBytes() }
    }
}
