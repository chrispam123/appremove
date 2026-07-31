package com.appremove.domain.resize

import java.io.File

/**
 * Caso de uso: redimensiona una imagen a un ancho/alto exactos, delegando el
 * algoritmo concreto en un [ImageResizer] (Strategy). Se encarga de validar la
 * entrada y de envolver el resultado en un [Result], para que quien lo llame
 * (la UI) no tenga que lidiar con excepciones crudas ni con el detalle de
 * implementación.
 */
class ResizeImageUseCase(
    private val imageResizer: ImageResizer,
) {
    /**
     * Ejecuta el resize de [input] hacia [output] a [targetWidth]x[targetHeight] px.
     * Devuelve [Result.failure], sin llegar a invocar la estrategia concreta, si:
     * - el archivo de entrada no existe o no es un archivo, o
     * - [targetWidth] o [targetHeight] no son valores positivos.
     */
    operator fun invoke(
        input: File,
        output: File,
        targetWidth: Int,
        targetHeight: Int,
    ): Result<ImageResizeResult> {
        if (!input.exists() || !input.isFile) {
            return Result.failure(IllegalArgumentException("El archivo de entrada no existe: ${input.path}"))
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            return Result.failure(IllegalArgumentException("El ancho y el alto deben ser mayores a 0"))
        }
        return runCatching { imageResizer.resize(input, output, targetWidth, targetHeight) }
    }
}
