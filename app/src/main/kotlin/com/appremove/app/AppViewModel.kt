package com.appremove.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appremove.coreimage.BufferedImageResizer
import com.appremove.data.OutputPathResolver
import com.appremove.domain.resize.ImageResizeResult
import com.appremove.domain.resize.ResizeImageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Tamaño fijo para este primer trazador: se prioriza tener el flujo completo
// funcionando antes que agregar un control de UI para elegirlo.
private const val MAX_DIMENSION_PX = 1280

/**
 * Estados posibles de la pantalla de resize, para que la UI sepa qué mostrar.
 */
sealed interface ResizeStatus {
    data object Idle : ResizeStatus

    data object Processing : ResizeStatus

    data class Success(
        val output: File,
        val result: ImageResizeResult,
    ) : ResizeStatus

    data class Error(
        val message: String,
    ) : ResizeStatus
}

/**
 * Estado y lógica de la pantalla principal (MVVM "liviano": usa Compose runtime
 * State en vez de androidx.lifecycle.ViewModel, alcanza para este trazador).
 *
 * Arma manualmente [ResizeImageUseCase] con las implementaciones concretas de
 * `core-image` y `data` (Strategy + inyección manual, sin framework de DI).
 */
class AppViewModel(
    private val scope: CoroutineScope,
) {
    private val resizeImageUseCase = ResizeImageUseCase(BufferedImageResizer())
    private val outputPathResolver = OutputPathResolver()

    var selectedFile by mutableStateOf<File?>(null)
        private set

    var status by mutableStateOf<ResizeStatus>(ResizeStatus.Idle)
        private set

    /** Se llama cuando el usuario elige un archivo en el selector de la UI. */
    fun onImageSelected(file: File) {
        selectedFile = file
        status = ResizeStatus.Idle
    }

    /**
     * Corre el resize del archivo seleccionado fuera del hilo de UI (Dispatchers.IO)
     * y actualiza [status] con el resultado (o el error) cuando termina.
     */
    fun resizeSelected() {
        val input = selectedFile ?: return
        status = ResizeStatus.Processing

        scope.launch {
            status =
                withContext(Dispatchers.IO) {
                    val output = outputPathResolver.resolve(input)
                    resizeImageUseCase(input, output, MAX_DIMENSION_PX).fold(
                        onSuccess = { ResizeStatus.Success(output, it) },
                        onFailure = { ResizeStatus.Error(it.message ?: "Error desconocido al reducir la imagen") },
                    )
                }
        }
    }
}
