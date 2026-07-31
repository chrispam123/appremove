package com.appremove.domain.bgremoval

import java.io.File

/**
 * Puerto (patrón Strategy) que define cómo remover (y opcionalmente reemplazar)
 * el fondo de una imagen. `domain` no sabe nada de ONNX Runtime ni de ningún
 * modelo de IA concreto: quien implemente esta interfaz (por ejemplo `core-ml`)
 * decide el algoritmo real, tanto para detectar el sujeto como para componerlo
 * contra el [background] elegido.
 */
fun interface BackgroundRemover {
    /**
     * Remueve el fondo de [input] y lo reemplaza según [background] (transparente,
     * color sólido, u otra imagen), escribiendo el resultado en [output]. La salida
     * siempre es PNG. Devuelve las dimensiones y el peso del archivo antes/después.
     */
    fun removeBackground(
        input: File,
        output: File,
        background: BackgroundChoice,
    ): BackgroundRemovalResult
}
