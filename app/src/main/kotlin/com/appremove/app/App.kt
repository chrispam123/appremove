package com.appremove.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Pantalla principal: selector de imagen + botón para reducir su tamaño.
 * Todo el estado vive en [AppViewModel]; este composable solo lo lee y dispara
 * sus funciones ante los clicks del usuario.
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.resizeSelected() },
                    enabled = viewModel.selectedFile != null && viewModel.status !is ResizeStatus.Processing,
                ) {
                    Text("Reducir tamaño")
                }

                Spacer(modifier = Modifier.height(16.dp))
                StatusText(viewModel.status)
            }
        }
    }
}

/**
 * Abre el selector de archivos nativo (Swing) filtrado a formatos de imagen
 * comunes. Devuelve null si el usuario cancela.
 */
private fun pickImageFile(): File? {
    val chooser =
        JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Imágenes (jpg, jpeg, png)", "jpg", "jpeg", "png")
        }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

/** Traduce el [ResizeStatus] actual a un mensaje legible para el usuario. */
@Composable
private fun StatusText(status: ResizeStatus) {
    val message =
        when (status) {
            is ResizeStatus.Idle -> ""
            is ResizeStatus.Processing -> "Reduciendo imagen…"
            is ResizeStatus.Success ->
                "Listo: ${status.output.name} " +
                    "(${status.result.originalWidth}x${status.result.originalHeight} -> " +
                    "${status.result.resizedWidth}x${status.result.resizedHeight}, " +
                    "${status.result.originalBytes / 1024}KB -> ${status.result.resizedBytes / 1024}KB)"
            is ResizeStatus.Error -> "Error: ${status.message}"
        }
    Text(message)
}
