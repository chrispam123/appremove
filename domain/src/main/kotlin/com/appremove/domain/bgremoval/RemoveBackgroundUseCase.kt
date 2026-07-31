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
     * Ejecuta la remoción/reemplazo de fondo de [input] hacia [output], con el
     * [background] elegido (transparente, color sólido, u otra imagen).
     * Devuelve [Result.failure], sin llegar a invocar la estrategia concreta,
     * si el archivo de entrada no existe o no es un archivo. La validación de
     * un eventual archivo de fondo (para [BackgroundChoice.Image]) queda del
     * lado de la estrategia concreta, que ya falla con un mensaje claro si no
     * puede leerlo.
     */
    operator fun invoke(
        input: File,
        output: File,
        background: BackgroundChoice,
    ): Result<BackgroundRemovalResult> {
        if (!input.exists() || !input.isFile) {
            return Result.failure(IllegalArgumentException("El archivo de entrada no existe: ${input.path}"))
        }
        return runCatching { backgroundRemover.removeBackground(input, output, background) }
    }
}
