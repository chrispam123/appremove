package com.appremove.app.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * Tipografías del spec de diseño: **Sora** (funcional — casi todo el texto de
 * la interfaz) y **Fraunces** itálica (solo el wordmark "appremove" — la
 * "firma humana" cálida sobre la superficie de metal, ver spec §1/§3).
 *
 * Google Fonts distribuye ambas como fuentes variables (un solo archivo con
 * ejes ajustables), pero la API de `Font()` de Compose Desktop disponible acá
 * no permite fijar esos ejes en tiempo de ejecución ni cargar por nombre de
 * resource directamente (esa sobrecarga apunta al sistema de
 * `compose.resources`, que este proyecto no usa). Por eso se generaron
 * instancias estáticas de los pesos exactos que pide el spec (`fonttools
 * varLib.instancer`, una sola vez al bajar las fuentes) a partir de los
 * mismos archivos oficiales de Google Fonts (licencia SIL Open Font
 * License): `sora_regular.ttf` (400), `sora_medium.ttf` (500),
 * `sora_semibold.ttf` (600) y `fraunces_italic.ttf` (peso 500, tamaño óptico
 * 40 — el rango "display" que pide el wordmark a 29px). Se cargan a mano
 * desde bytes del classpath, mismo patrón que el modelo ONNX en `core-ml`.
 */
object AppTypography {
    val soraFamily =
        FontFamily(
            Font(identity = "Sora-Regular", data = loadFontBytes("font/sora_regular.ttf"), weight = FontWeight.Normal),
            Font(identity = "Sora-Medium", data = loadFontBytes("font/sora_medium.ttf"), weight = FontWeight.Medium),
            Font(
                identity = "Sora-SemiBold",
                data = loadFontBytes("font/sora_semibold.ttf"),
                weight = FontWeight.SemiBold,
            ),
        )

    /** Solo para el wordmark "appremove": itálica, ya instanciada al peso/tamaño óptico del spec. */
    val frauncesItalic =
        FontFamily(
            Font(
                identity = "Fraunces-Italic",
                data = loadFontBytes("font/fraunces_italic.ttf"),
                weight = FontWeight.Medium,
                style = FontStyle.Italic,
            ),
        )

    private fun loadFontBytes(resourcePath: String): ByteArray {
        val stream =
            AppTypography::class.java.classLoader.getResourceAsStream(resourcePath)
                ?: error("No se encontró la fuente: $resourcePath")
        return stream.use { it.readBytes() }
    }
}
