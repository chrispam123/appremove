package com.appremove.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appremove.coreimage.BufferedImageResizer
import com.appremove.coreimage.ImageDimensions
import com.appremove.coreimage.ImageDimensionsReader
import com.appremove.coreml.OnnxBackgroundRemover
import com.appremove.data.OutputPathResolver
import com.appremove.domain.bgremoval.RemoveBackgroundUseCase
import com.appremove.domain.resize.AspectRatio
import com.appremove.domain.resize.ResizeImageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Estados posibles de una operación de la pantalla (resize o remoción de
 * fondo comparten esta misma forma). El mensaje de [Success] ya viene armado
 * por quien dispara la operación, porque cada una tiene su propio tipo de
 * resultado ([com.appremove.domain.resize.ImageResizeResult] vs.
 * [com.appremove.domain.bgremoval.BackgroundRemovalResult]) y no vale la pena
 * que la UI conozca ambos tipos.
 */
sealed interface OperationStatus {
    data object Idle : OperationStatus

    data object Processing : OperationStatus

    data class Success(
        val output: File,
        val message: String,
    ) : OperationStatus

    data class Error(
        val message: String,
    ) : OperationStatus
}

/**
 * Estado y lógica de la pantalla principal (MVVM "liviano": usa Compose runtime
 * State en vez de androidx.lifecycle.ViewModel, alcanza para este trazador).
 *
 * Arma manualmente los casos de uso con las implementaciones concretas de
 * `core-image`/`core-ml` y `data` (Strategy + inyección manual, sin framework
 * de DI).
 */
class AppViewModel(
    private val scope: CoroutineScope,
) {
    companion object {
        /**
         * Únicos formatos que la app sabe procesar. Se mantiene la lista corta
         * a propósito: es la misma que usa el selector de archivos de la UI, y
         * es también la última barrera antes de tocar disco por si algo la
         * esquiva (ej. tipeando una ruta a mano en el diálogo del sistema).
         */
        val SUPPORTED_EXTENSIONS = listOf("jpg", "jpeg", "png")
    }

    private val resizeImageUseCase = ResizeImageUseCase(BufferedImageResizer())

    // OnnxBackgroundRemover no carga el modelo (~170MB) hasta el primer uso
    // real (su sesión de ONNX Runtime es lazy), así que instanciarlo acá no
    // tiene costo hasta que el usuario clickee "Remover fondo".
    private val removeBackgroundUseCase = RemoveBackgroundUseCase(OnnxBackgroundRemover())

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

    var resizeStatus by mutableStateOf<OperationStatus>(OperationStatus.Idle)
        private set

    var backgroundRemovalStatus by mutableStateOf<OperationStatus>(OperationStatus.Idle)
        private set

    /**
     * Se llama cuando el usuario elige un archivo en el selector de la UI.
     * Rechaza formatos fuera de [SUPPORTED_EXTENSIONS]; si no, lee las
     * dimensiones originales en segundo plano y precarga los campos de
     * ancho/alto con esos valores.
     */
    fun onImageSelected(file: File) {
        if (file.extension.lowercase() !in SUPPORTED_EXTENSIONS) {
            resizeStatus =
                OperationStatus.Error(
                    "Formato no soportado. Elegí un archivo ${SUPPORTED_EXTENSIONS.joinToString(" / ")}",
                )
            return
        }

        selectedFile = file
        originalDimensions = null
        widthInput = ""
        heightInput = ""
        resizeStatus = OperationStatus.Idle
        backgroundRemovalStatus = OperationStatus.Idle

        scope.launch {
            val dimensions =
                withContext(Dispatchers.IO) {
                    runCatching { imageDimensionsReader.read(file) }.getOrNull()
                }
            if (dimensions == null) {
                resizeStatus = OperationStatus.Error("No se pudieron leer las dimensiones de la imagen")
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
     * fuera del hilo de UI (Dispatchers.IO), y actualiza [resizeStatus] con el
     * resultado (o el error) cuando termina.
     */
    fun resizeSelected() {
        val input = selectedFile ?: return
        val width = widthInput.toIntOrNull()
        val height = heightInput.toIntOrNull()
        if (width == null || width <= 0 || height == null || height <= 0) {
            resizeStatus = OperationStatus.Error("Ingresá un ancho y un alto válidos (mayores a 0)")
            return
        }

        resizeStatus = OperationStatus.Processing
        scope.launch {
            resizeStatus =
                withContext(Dispatchers.IO) {
                    val output = outputPathResolver.resolve(input, suffix = "_resized")
                    resizeImageUseCase(input, output, width, height).fold(
                        onSuccess = { result ->
                            OperationStatus.Success(
                                output,
                                "Listo: ${output.name} " +
                                    "(${result.originalWidth}x${result.originalHeight} -> " +
                                    "${result.resizedWidth}x${result.resizedHeight})",
                            )
                        },
                        onFailure = { OperationStatus.Error(it.message ?: "Error desconocido al redimensionar la imagen") },
                    )
                }
        }
    }

    /**
     * Corre la remoción de fondo del archivo seleccionado fuera del hilo de UI
     * (Dispatchers.IO) y actualiza [backgroundRemovalStatus] con el resultado
     * (o el error) cuando termina. La salida siempre es PNG, sea cual sea el
     * formato de entrada (única forma de tener canal alfa).
     */
    fun removeBackgroundSelected() {
        val input = selectedFile ?: return

        backgroundRemovalStatus = OperationStatus.Processing
        scope.launch {
            backgroundRemovalStatus =
                withContext(Dispatchers.IO) {
                    val output = outputPathResolver.resolve(input, suffix = "_sinfondo", extension = "png")
                    removeBackgroundUseCase(input, output).fold(
                        onSuccess = { result ->
                            OperationStatus.Success(output, "Listo: ${output.name} (${result.width}x${result.height})")
                        },
                        onFailure = { OperationStatus.Error(it.message ?: "Error desconocido al remover el fondo") },
                    )
                }
        }
    }
}
