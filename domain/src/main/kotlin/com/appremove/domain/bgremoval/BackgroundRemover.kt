package com.appremove.domain.bgremoval

import java.io.File

/**
 * Puerto (patrón Strategy) que define cómo remover el fondo de una imagen.
 * `domain` no sabe nada de ONNX Runtime ni de ningún modelo de IA concreto:
 * quien implemente esta interfaz (por ejemplo `core-ml`) decide el algoritmo real.
 */
fun interface BackgroundRemover {
    /**
     * Remueve el fondo de [input] y escribe el resultado en [output]. La salida
     * siempre es PNG: es el único formato que soporta la app con canal alfa,
     * necesario para representar la transparencia donde estaba el fondo.
     * Devuelve las dimensiones y el peso del archivo antes/después del proceso.
     */
    fun removeBackground(
        input: File,
        output: File,
    ): BackgroundRemovalResult
}
