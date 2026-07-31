package com.appremove.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Pantalla principal: selector de imagen, campos de ancho/alto (con opción de
 * mantener la proporción) y botón para redimensionar. Todo el estado vive en
 * [AppViewModel]; este composable solo lo lee y dispara sus funciones ante los
 * eventos del usuario.
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

                Spacer(modifier = Modifier.height(16.dp))
                StatusText(viewModel.status)
            }
        }
    }
}

/** Habilita el botón de resize solo si hay un archivo y un ancho/alto numérico válido. */
private fun canResize(viewModel: AppViewModel): Boolean {
    if (viewModel.selectedFile == null || viewModel.status is ResizeStatus.Processing) return false
    val width = viewModel.widthInput.toIntOrNull()
    val height = viewModel.heightInput.toIntOrNull()
    return width != null && width > 0 && height != null && height > 0
}

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

/** Traduce el [ResizeStatus] actual a un mensaje legible para el usuario. */
@Composable
private fun StatusText(status: ResizeStatus) {
    val message =
        when (status) {
            is ResizeStatus.Idle -> ""
            is ResizeStatus.Processing -> "Redimensionando imagen…"
            is ResizeStatus.Success ->
                "Listo: ${status.output.name} " +
                    "(${status.result.originalWidth}x${status.result.originalHeight} -> " +
                    "${status.result.resizedWidth}x${status.result.resizedHeight})"
            is ResizeStatus.Error -> "Error: ${status.message}"
        }
    Text(message)
}
