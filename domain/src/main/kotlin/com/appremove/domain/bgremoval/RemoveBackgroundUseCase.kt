package com.appremove.domain.bgremoval

import java.io.File

/**
 * Caso de uso: remueve el fondo de una imagen delegando el algoritmo concreto
 * en un [BackgroundRemover] (Strategy). Se encarga de validar la entrada y de
 * envolver el resultado en un [Result], igual que
 * [com.appremove.domain.resize.ResizeImageUseCase] para el resize.
 */
class RemoveBackgroundUseCase(
    private val backgroundRemover: BackgroundRemover,
) {
    /**
     * Ejecuta la remoción de fondo de [input] hacia [output].
     * Devuelve [Result.failure], sin llegar a invocar la estrategia concreta,
     * si el archivo de entrada no existe o no es un archivo.
     */
    operator fun invoke(
        input: File,
        output: File,
    ): Result<BackgroundRemovalResult> {
        if (!input.exists() || !input.isFile) {
            return Result.failure(IllegalArgumentException("El archivo de entrada no existe: ${input.path}"))
        }
        return runCatching { backgroundRemover.removeBackground(input, output) }
    }
}
