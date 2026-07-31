package com.appremove.domain.resize

import java.io.File

/**
 * Caso de uso: reduce el tamaño de una imagen delegando el algoritmo concreto
 * en un [ImageResizer] (Strategy). Se encarga de validar la entrada y de
 * envolver el resultado en un [Result], para que quien lo llame (la UI) no
 * tenga que lidiar con excepciones crudas ni con el detalle de implementación.
 */
class ResizeImageUseCase(
    private val imageResizer: ImageResizer,
) {
    /**
     * Ejecuta el resize de [input] hacia [output] con el [maxDimension] pedido.
     * Devuelve [Result.failure] si el archivo de entrada no existe o no es un archivo,
     * sin llegar a invocar la estrategia concreta.
     */
    operator fun invoke(
        input: File,
        output: File,
        maxDimension: Int,
    ): Result<ImageResizeResult> {
        if (!input.exists() || !input.isFile) {
            return Result.failure(IllegalArgumentException("El archivo de entrada no existe: ${input.path}"))
        }
        return runCatching { imageResizer.resize(input, output, maxDimension) }
    }
}
