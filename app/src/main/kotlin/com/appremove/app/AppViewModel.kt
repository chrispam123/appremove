package com.appremove.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appremove.coreimage.BufferedImageResizer
import com.appremove.coreimage.ImageDimensions
import com.appremove.coreimage.ImageDimensionsReader
import com.appremove.data.OutputPathResolver
import com.appremove.domain.resize.AspectRatio
import com.appremove.domain.resize.ImageResizeResult
import com.appremove.domain.resize.ResizeImageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    private val imageDimensionsReader = ImageDimensionsReader()

    var selectedFile by mutableStateOf<File?>(null)
        private set

    /** Ancho/alto originales de la imagen elegida; null hasta que se terminan de leer. */
    var originalDimensions by mutableStateOf<ImageDimensions?>(null)
        private set

    /** Contenido de los campos de ancho/alto de la UI (texto, para permitir edición libre). */
    var widthInput by mutableStateOf("")
        private set

    var heightInput by mutableStateOf("")
        private set

    /** Si está activo, editar un campo recalcula el otro para no deformar la imagen. */
    var keepAspectRatio by mutableStateOf(true)
        private set

    var status by mutableStateOf<ResizeStatus>(ResizeStatus.Idle)
        private set

    /**
     * Se llama cuando el usuario elige un archivo en el selector de la UI.
     * Lee sus dimensiones originales en segundo plano y precarga los campos
     * de ancho/alto con esos valores.
     */
    fun onImageSelected(file: File) {
        selectedFile = file
        originalDimensions = null
        widthInput = ""
        heightInput = ""
        status = ResizeStatus.Idle

        scope.launch {
            val dimensions =
                withContext(Dispatchers.IO) {
                    runCatching { imageDimensionsReader.read(file) }.getOrNull()
                }
            if (dimensions == null) {
                status = ResizeStatus.Error("No se pudieron leer las dimensiones de la imagen")
                return@launch
            }
            originalDimensions = dimensions
            widthInput = dimensions.width.toString()
            heightInput = dimensions.height.toString()
        }
    }

    /** Se llama al tipear en el campo de ancho. */
    fun onWidthChanged(value: String) {
        widthInput = value
        val original = originalDimensions ?: return
        val newWidth = value.toIntOrNull() ?: return
        if (keepAspectRatio && newWidth > 0) {
            heightInput = AspectRatio.linkedHeight(original.width, original.height, newWidth).toString()
        }
    }

    /** Se llama al tipear en el campo de alto. */
    fun onHeightChanged(value: String) {
        heightInput = value
        val original = originalDimensions ?: return
        val newHeight = value.toIntOrNull() ?: return
        if (keepAspectRatio && newHeight > 0) {
            widthInput = AspectRatio.linkedWidth(original.width, original.height, newHeight).toString()
        }
    }

    /** Se llama al tildar/destildar "mantener proporción". */
    fun onKeepAspectRatioChanged(value: Boolean) {
        keepAspectRatio = value
        val original = originalDimensions ?: return
        val width = widthInput.toIntOrNull() ?: return
        // Al reactivarla, recalcula el alto a partir del ancho actual para que
        // ambos campos vuelvan a quedar coherentes entre sí.
        if (value && width > 0) {
            heightInput = AspectRatio.linkedHeight(original.width, original.height, width).toString()
        }
    }

    /**
     * Corre el resize del archivo seleccionado, con el ancho/alto tipeados,
     * fuera del hilo de UI (Dispatchers.IO), y actualiza [status] con el
     * resultado (o el error) cuando termina.
     */
    fun resizeSelected() {
        val input = selectedFile ?: return
        val width = widthInput.toIntOrNull()
        val height = heightInput.toIntOrNull()
        if (width == null || width <= 0 || height == null || height <= 0) {
            status = ResizeStatus.Error("Ingresá un ancho y un alto válidos (mayores a 0)")
            return
        }

        status = ResizeStatus.Processing
        scope.launch {
            status =
                withContext(Dispatchers.IO) {
                    val output = outputPathResolver.resolve(input)
                    resizeImageUseCase(input, output, width, height).fold(
                        onSuccess = { ResizeStatus.Success(output, it) },
                        onFailure = { ResizeStatus.Error(it.message ?: "Error desconocido al redimensionar la imagen") },
                    )
                }
        }
    }
}
