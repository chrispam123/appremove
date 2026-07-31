package com.appremove.domain.resize

import java.io.File

/**
 * Puerto (patrón Strategy) que define cómo redimensionar una imagen.
 * `domain` no sabe nada de ImageIO ni de ninguna librería concreta: quien
 * implemente esta interfaz (por ejemplo `core-image`) decide el algoritmo real.
 */
fun interface ImageResizer {
    /**
     * Redimensiona [input] a exactamente [targetWidth]x[targetHeight] px y escribe
     * el resultado en [output]. Si esas medidas no respetan la proporción original,
     * la imagen sale deformada (mantener o no la proporción es una decisión de
     * quien llama, ver [AspectRatio]); acá se permite tanto achicar como agrandar.
     * Devuelve las dimensiones y el peso del archivo antes/después del proceso.
     */
    fun resize(
        input: File,
        output: File,
        targetWidth: Int,
        targetHeight: Int,
    ): ImageResizeResult
}
