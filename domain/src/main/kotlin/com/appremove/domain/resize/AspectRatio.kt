package com.appremove.domain.resize

import kotlin.math.roundToInt

/**
 * Lógica pura para mantener la proporción original de una imagen cuando el
 * usuario edita solo el ancho o solo el alto en la UI. No depende de ninguna
 * imagen real ni de disco: solo hace la regla de tres a partir de las
 * dimensiones originales.
 */
object AspectRatio {
    /**
     * Dado el [newWidth] que tipeó el usuario, devuelve el alto que le corresponde
     * para conservar la proporción [originalWidth]:[originalHeight].
     * Si no se conoce el original (0 o negativo), devuelve [newWidth] tal cual
     * para no dividir por cero.
     */
    fun linkedHeight(
        originalWidth: Int,
        originalHeight: Int,
        newWidth: Int,
    ): Int {
        if (originalWidth <= 0 || originalHeight <= 0) return newWidth
        return (newWidth.toDouble() * originalHeight / originalWidth).roundToIntAtLeastOne()
    }

    /**
     * Análogo a [linkedHeight] pero partiendo del [newHeight] que tipeó el usuario.
     */
    fun linkedWidth(
        originalWidth: Int,
        originalHeight: Int,
        newHeight: Int,
    ): Int {
        if (originalWidth <= 0 || originalHeight <= 0) return newHeight
        return (newHeight.toDouble() * originalWidth / originalHeight).roundToIntAtLeastOne()
    }

    private fun Double.roundToIntAtLeastOne(): Int = roundToInt().coerceAtLeast(1)
}
