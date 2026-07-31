package com.appremove.domain.bgremoval

/**
 * Resultado de una operación de remoción de fondo: dimensiones de la imagen
 * (no cambian, a diferencia del resize) y peso del archivo antes/después,
 * para poder mostrarlo en la UI.
 */
data class BackgroundRemovalResult(
    val width: Int,
    val height: Int,
    val originalBytes: Long,
    val resultBytes: Long,
)
