package com.appremove.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.appremove.app.theme.AppColors

// Ancho del "dispositivo" cromado (spec §5.1: 420px de contenido).
private val DEVICE_WIDTH = 420.dp

// Alto de ventana: valor inicial pensado para el contenido completo del spec;
// se termina de ajustar a ojo en la Tarea F, una vez que todas las secciones
// estén con su tamaño final.
private val DEVICE_HEIGHT = 800.dp

// Margen transparente alrededor del dispositivo para que la sombra exterior
// no quede recortada en el borde de la ventana (la ventana es más grande que
// el "objeto" visible, y el resto queda transparente).
private val WINDOW_MARGIN = 40.dp

fun main() =
    application {
        val windowState =
            rememberWindowState(
                size = DpSize(DEVICE_WIDTH + WINDOW_MARGIN * 2, DEVICE_HEIGHT + WINDOW_MARGIN * 2),
                position = WindowPosition(Alignment.Center),
            )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            undecorated = true,
            transparent = true,
            resizable = false,
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(WINDOW_MARGIN),
                contentAlignment = Alignment.Center,
            ) {
                DeviceFrame(onCloseRequest = ::exitApplication) {
                    App()
                }
            }
        }
    }

/**
 * El marco cromado del "dispositivo" (spec §5.1): borde con degradado de
 * metal (`chrome-hi` → `chrome-low` → `chrome-mid` → `graphite-0`, la luz
 * viene de arriba-izquierda, principio no negociable §6.2), sombra exterior,
 * y la "pantalla" interior con el fondo grafito. Como la ventana no tiene
 * chrome nativo de Windows, acá vive el único control de sistema que hace
 * falta: cerrar.
 */
@Composable
private fun WindowScope.DeviceFrame(
    onCloseRequest: () -> Unit,
    content: @Composable WindowScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(DEVICE_WIDTH, DEVICE_HEIGHT)
                .shadow(elevation = 32.dp, shape = RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors =
                            listOf(
                                AppColors.chromeHi,
                                AppColors.chromeLow,
                                AppColors.chromeMid,
                                AppColors.graphite0,
                                AppColors.chromeLow,
                                AppColors.chromeMid,
                            ),
                    ),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(colors = listOf(AppColors.graphite1, AppColors.graphite0))),
        ) {
            content()
            CloseButton(onClick = onCloseRequest, modifier = Modifier.align(Alignment.TopEnd).padding(14.dp))
        }
    }
}

/**
 * Botón de cerrar propio (discreto: solo una línea en cruz, sin círculo de
 * fondo llamativo — no debe competir con el único acento dorado de la
 * pantalla, principio no negociable §6.1).
 */
@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(11.dp)) {
            val stroke = 1.6.dp.toPx()
            drawLine(AppColors.creamDim, Offset(0f, 0f), Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(AppColors.creamDim, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
        }
    }
}
