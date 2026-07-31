package com.appremove.domain.bgremoval

import java.io.File

/**
 * Qué poner detrás del sujeto una vez removido el fondo original. `domain` no
 * depende de `java.awt.Color` ni de nada gráfico: el color se representa como
 * tres componentes 0-255, y quien implemente [BackgroundRemover] decide cómo
 * componerlo (ver `core-ml`).
 */
sealed interface BackgroundChoice {
    /** Fondo transparente (el comportamiento original: solo remover, sin reemplazar). */
    data object Transparent : BackgroundChoice

    /** Fondo de un único color sólido, ej. para fotos de perfil o de producto. */
    data class SolidColor(
        val red: Int,
        val green: Int,
        val blue: Int,
    ) : BackgroundChoice

    /** Fondo tomado de otra imagen, reescalada para cubrir el lienzo completo. */
    data class Image(
        val file: File,
    ) : BackgroundChoice
}
