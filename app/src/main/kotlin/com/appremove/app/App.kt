package com.appremove.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.appremove.domain.bgremoval.BackgroundChoice
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Paleta fija de colores de reemplazo (en vez de un color picker completo):
 * alcanza para los casos típicos (foto de perfil/producto) y es mucho más
 * simple de construir y de usar que un selector RGB/hex a medida.
 */
private val BACKGROUND_COLOR_PALETTE =
    listOf(
        BackgroundChoice.SolidColor(255, 255, 255), // blanco
        BackgroundChoice.SolidColor(0, 0, 0), // negro
        BackgroundChoice.SolidColor(0, 177, 64), // verde chroma
        BackgroundChoice.SolidColor(135, 206, 235), // celeste
    )

/**
 * Pantalla principal: selector de imagen, campos de ancho/alto (con opción de
 * mantener la proporción), botón para redimensionar y botón para remover el
 * fondo. Todo el estado vive en [AppViewModel]; este composable solo lo lee y
 * dispara sus funciones ante los eventos del usuario.
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val viewModel = remember { AppViewModel(scope) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("appremove")
                Text("Remové fondos y comprimí tus imágenes", style = MaterialTheme.typography.body2)

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = { pickImageFile()?.let(viewModel::onImageSelected) }) {
                    Text("Elegir imagen")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(viewModel.selectedFile?.name ?: "Ningún archivo seleccionado")

                val original = viewModel.originalDimensions
                if (viewModel.selectedFile != null && original == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Leyendo dimensiones…")
                }

                if (original != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tamaño original: ${original.width}x${original.height}px")

                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = viewModel.widthInput,
                            onValueChange = viewModel::onWidthChanged,
                            modifier = Modifier.width(120.dp),
                            label = { Text("Ancho (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = viewModel.heightInput,
                            onValueChange = viewModel::onHeightChanged,
                            modifier = Modifier.width(120.dp),
                            label = { Text("Alto (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = viewModel.keepAspectRatio,
                            onCheckedChange = viewModel::onKeepAspectRatioChanged,
                        )
                        Text("Mantener proporción")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.resizeSelected() },
                    enabled = canResize(viewModel),
                ) {
                    Text("Reducir tamaño")
                }

                Spacer(modifier = Modifier.height(8.dp))
                StatusText(viewModel.resizeStatus, processingLabel = "Redimensionando imagen…")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Fondo de reemplazo:")
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackgroundOptionButton(
                        label = "Transparente",
                        selected = viewModel.selectedBackground is BackgroundChoice.Transparent,
                        onClick = { viewModel.onBackgroundChoiceSelected(BackgroundChoice.Transparent) },
                    )
                    for (colorChoice in BACKGROUND_COLOR_PALETTE) {
                        ColorSwatch(
                            color = Color(colorChoice.red, colorChoice.green, colorChoice.blue),
                            selected = viewModel.selectedBackground == colorChoice,
                            onClick = { viewModel.onBackgroundChoiceSelected(colorChoice) },
                        )
                    }
                    BackgroundOptionButton(
                        label = "Imagen…",
                        selected = viewModel.selectedBackground is BackgroundChoice.Image,
                        onClick = { pickImageFile()?.let { viewModel.onBackgroundChoiceSelected(BackgroundChoice.Image(it)) } },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.removeBackgroundSelected() },
                    enabled = canRemoveBackground(viewModel),
                ) {
                    Text("Remover fondo")
                }

                Spacer(modifier = Modifier.height(8.dp))
                StatusText(
                    viewModel.backgroundRemovalStatus,
                    processingLabel = "Removiendo fondo (puede tardar varios segundos)…",
                )
            }
        }
    }
}

/** Habilita el botón de resize solo si hay un archivo y un ancho/alto numérico válido. */
private fun canResize(viewModel: AppViewModel): Boolean {
    if (viewModel.selectedFile == null || viewModel.resizeStatus is OperationStatus.Processing) return false
    val width = viewModel.widthInput.toIntOrNull()
    val height = viewModel.heightInput.toIntOrNull()
    return width != null && width > 0 && height != null && height > 0
}

/** Habilita el botón de remover fondo solo si hay un archivo elegido y no hay otra remoción en curso. */
private fun canRemoveBackground(viewModel: AppViewModel): Boolean =
    viewModel.selectedFile != null && viewModel.backgroundRemovalStatus !is OperationStatus.Processing

/**
 * Abre el selector de archivos nativo (Swing) filtrado a formatos de imagen
 * comunes. Devuelve null si el usuario cancela.
 */
private fun pickImageFile(): File? {
    val extensions = AppViewModel.SUPPORTED_EXTENSIONS.toTypedArray()
    val chooser =
        JFileChooser().apply {
            // Sin esto, Windows deja igual la opción "todos los archivos": el
            // filtro por sí solo no alcanza para impedir elegir un formato no soportado.
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter("Imágenes (${extensions.joinToString()})", *extensions)
        }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

/** Botón tipo "toggle": relleno cuando está seleccionado, contorno cuando no. */
@Composable
private fun BackgroundOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

/** Cuadradito clickeable de un color de la paleta; se resalta el borde si está elegido. */
@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .background(color)
                .border(width = if (selected) 3.dp else 1.dp, color = Color.Black)
                .clickable(onClick = onClick),
    )
}

/** Traduce el [OperationStatus] actual a un mensaje legible para el usuario. */
@Composable
private fun StatusText(
    status: OperationStatus,
    processingLabel: String,
) {
    val message =
        when (status) {
            is OperationStatus.Idle -> ""
            is OperationStatus.Processing -> processingLabel
            is OperationStatus.Success -> status.message
            is OperationStatus.Error -> "Error: ${status.message}"
        }
    Text(message)
}
