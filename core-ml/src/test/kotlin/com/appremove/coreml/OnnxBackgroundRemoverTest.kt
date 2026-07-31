package com.appremove.coreml

import com.appremove.domain.bgremoval.BackgroundChoice
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Prueba [OnnxBackgroundRemover] de punta a punta contra el modelo real.
 *
 * En CI el `.onnx` es solo el puntero de texto que deja Git LFS (a propósito
 * no se baja el contenido real ahí, ver `.github/workflows/ci.yml`), así que
 * este test se salta cuando corre ahí en vez de fallar — solo corre de
 * verdad en una máquina que tenga el modelo real presente (`git lfs pull`).
 *
 * Los tests de reemplazo de fondo verifican un píxel de una esquina (zona de
 * fondo segura, lejos del sujeto sintético dibujado en el centro): prueban que
 * la composición (y el resize "cover" en el caso de imagen) funcionan, no la
 * calidad de la segmentación en sí — eso se valida a mano con fotos reales.
 */
class OnnxBackgroundRemoverTest {
    @Test
    fun `removes the background end to end and produces a PNG with alpha`() {
        assumeTrue(hasRealModel(), "Modelo real no disponible (solo el puntero de Git LFS) - se salta este test")

        val input = File.createTempFile("bg-remover-test-input", ".png").apply { deleteOnExit() }
        writeSampleImage(input, width = 300, height = 200)
        val output = File.createTempFile("bg-remover-test-output", ".png").apply { deleteOnExit() }

        val result = OnnxBackgroundRemover().removeBackground(input, output, BackgroundChoice.Transparent)

        assertEquals(300, result.width)
        assertEquals(200, result.height)
        assertTrue(output.exists() && output.length() > 0)

        val written = ImageIO.read(output)
        assertEquals(300, written.width)
        assertEquals(200, written.height)
        assertTrue(written.colorModel.hasAlpha(), "La salida tiene que tener canal alfa")
    }

    @Test
    fun `replaces the background with a solid color`() {
        assumeTrue(hasRealModel(), "Modelo real no disponible (solo el puntero de Git LFS) - se salta este test")

        val input = File.createTempFile("bg-remover-test-color-input", ".png").apply { deleteOnExit() }
        writeSampleImage(input, width = 300, height = 200)
        val output = File.createTempFile("bg-remover-test-color-output", ".png").apply { deleteOnExit() }

        OnnxBackgroundRemover().removeBackground(input, output, BackgroundChoice.SolidColor(255, 0, 0))

        val corner = Color(ImageIO.read(output).getRGB(2, 2))
        assertTrue(corner.red > 200 && corner.green < 60 && corner.blue < 60, "Esquina esperada en rojo, salió $corner")
    }

    @Test
    fun `replaces the background with another image, resized to cover`() {
        assumeTrue(hasRealModel(), "Modelo real no disponible (solo el puntero de Git LFS) - se salta este test")

        val backgroundFile = File.createTempFile("bg-remover-test-bg", ".png").apply { deleteOnExit() }
        writeSolidImage(backgroundFile, width = 50, height = 50, color = Color.GREEN)

        val input = File.createTempFile("bg-remover-test-image-input", ".png").apply { deleteOnExit() }
        writeSampleImage(input, width = 300, height = 200)
        val output = File.createTempFile("bg-remover-test-image-output", ".png").apply { deleteOnExit() }

        OnnxBackgroundRemover().removeBackground(input, output, BackgroundChoice.Image(backgroundFile))

        val corner = Color(ImageIO.read(output).getRGB(2, 2))
        assertTrue(corner.green > 200 && corner.red < 60 && corner.blue < 60, "Esquina esperada en verde, salió $corner")
    }

    /** Distingue el modelo real (~170MB) del puntero de Git LFS (unos pocos cientos de bytes). */
    private fun hasRealModel(): Boolean {
        val resource = javaClass.getResourceAsStream("/models/isnet-general-use.onnx") ?: return false
        return resource.use { it.readBytes().size } > 10_000_000
    }

    private fun writeSampleImage(
        file: File,
        width: Int,
        height: Int,
    ) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.BLUE
        graphics.fillRect(0, 0, width, height)
        graphics.color = Color.RED
        graphics.fillOval(width / 4, height / 4, width / 2, height / 2)
        graphics.dispose()
        ImageIO.write(image, "png", file)
    }

    private fun writeSolidImage(
        file: File,
        width: Int,
        height: Int,
        color: Color,
    ) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = color
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        ImageIO.write(image, "png", file)
    }
}
