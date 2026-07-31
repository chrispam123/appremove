package com.appremove.domain.resize

import java.io.File

/**
 * Puerto (patrón Strategy) que define cómo reducir el tamaño de una imagen.
 * `domain` no sabe nada de ImageIO ni de ninguna librería concreta: quien
 * implemente esta interfaz (por ejemplo `core-image`) decide el algoritmo real.
 */
fun interface ImageResizer {
    /**
     * Redimensiona [input] para que su lado mayor mida como máximo [maxDimension] px,
     * manteniendo la proporción original, y escribe el resultado en [output].
     * Devuelve las dimensiones y el peso del archivo antes/después del proceso.
     */
    fun resize(
        input: File,
        output: File,
        maxDimension: Int,
    ): ImageResizeResult
}
