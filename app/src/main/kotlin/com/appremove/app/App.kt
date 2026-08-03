package com.appremove.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import com.appremove.app.components.BackgroundSwatchRow
import com.appremove.app.components.DimensionField
import com.appremove.app.components.FrameSquare
import com.appremove.app.components.LuxuryCta
import com.appremove.app.components.MetalButton
import com.appremove.app.components.PunktButton
import com.appremove.app.components.SectionLabel
import com.appremove.app.theme.AppColors
import com.appremove.app.theme.AppTypography
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
 * Pantalla principal. Requiere [WindowScope] porque el wordmark de arriba es
 * la zona de arrastre de la ventana (no hay barra de título nativa — la
 * ventana es `undecorated`, ver [com.appremove.app.DeviceFrame] en `Main.kt`).
 *
 * por ahora solo el wordmark, el
 * subtítulo y el divisor dorado tienen el acabado final; el resto de la UI
 * (elegir imagen, dimensiones, fondo) sigue con componentes Material simples
 * — se restylean en las Tareas D y E con las piezas  (Punkt,
 * Fläche, Linie, CTA de lujo).
 */
@Composable
fun WindowScope.App() {
    val scope = rememberCoroutineScope()
    val viewModel = remember { AppViewModel(scope) }

    MaterialTheme(
        colors =
            darkColors(
                primary = AppColors.gold1,
                onPrimary = AppColors.graphite0,
                background = AppColors.graphite1,
                onBackground = AppColors.cream,
                surface = AppColors.graphite1,
                onSurface = AppColors.cream,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 30.dp)
                    .padding(top = 36.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WindowDraggableArea {
                Wordmark()
            }

            GoldDivider()

            val original = viewModel.originalDimensions
            val fileStatusLine =
                when {
                    viewModel.selectedFile == null -> "Ningún archivo seleccionado"
                    original == null -> "${viewModel.selectedFile?.name} · leyendo dimensiones…"
                    else -> "${viewModel.selectedFile?.name} · ${original.width}×${original.height}px"
                }

            PunktButton(onClick = { pickImageFile()?.let(viewModel::onImageSelected) })
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Elegir imagen",
                style = TextStyle(fontFamily = AppTypography.soraFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp),
                color = AppColors.cream,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                fileStatusLine,
                style = TextStyle(fontFamily = AppTypography.soraFamily, fontSize = 12.sp),
                color = AppColors.creamDim,
            )

            if (original != null) {
                Spacer(modifier = Modifier.height(26.dp))
                Row(modifier = Modifier.fillMaxWidth()) { SectionLabel("Dimensiones") }
                Spacer(modifier = Modifier.height(12.dp))

                FrameSquare {
                    Row {
                        DimensionField(
                            label = "Ancho (px)",
                            value = viewModel.widthInput,
                            onValueChange = viewModel::onWidthChanged,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        DimensionField(
                            label = "Alto (px)",
                            value = viewModel.heightInput,
                            onValueChange = viewModel::onHeightChanged,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = viewModel.keepAspectRatio,
                        onCheckedChange = viewModel::onKeepAspectRatioChanged,
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor = AppColors.gold1,
                                uncheckedColor = AppColors.chromeMid,
                                checkmarkColor = AppColors.graphite0,
                            ),
                    )
                    Text(
                        "Mantener proporción",
                        style = TextStyle(fontFamily = AppTypography.soraFamily, fontSize = 12.5.sp),
                        color = AppColors.creamDim,
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                MetalButton(text = "Reducir tamaño", onClick = { viewModel.resizeSelected() }, enabled = canResize(viewModel))

                Spacer(modifier = Modifier.height(8.dp))
                StatusText(viewModel.resizeStatus, processingLabel = "Redimensionando imagen…")
            }

            Spacer(modifier = Modifier.height(26.dp))
            Row(modifier = Modifier.fillMaxWidth()) { SectionLabel("Fondo de reemplazo") }
            Spacer(modifier = Modifier.height(12.dp))

            BackgroundSwatchRow(
                selected = viewModel.selectedBackground,
                palette = BACKGROUND_COLOR_PALETTE,
                onSelect = viewModel::onBackgroundChoiceSelected,
                onPickImage = { pickImageFile()?.let { viewModel.onBackgroundChoiceSelected(BackgroundChoice.Image(it)) } },
            )

            Spacer(modifier = Modifier.height(26.dp))

            LuxuryCta(
                text = "Remover fondo",
                onClick = { viewModel.removeBackgroundSelected() },
                enabled = canRemoveBackground(viewModel),
            )

            Spacer(modifier = Modifier.height(8.dp))
            StatusText(
                viewModel.backgroundRemovalStatus,
                processingLabel = "Removiendo fondo (puede tardar varios segundos)…",
            )
        }
    }
}

/**
 * "amover" en Fraunces itálica con degradado dorado (spec §3: el wordmark se
 * pinta con un degradado, nunca color plano) + el subtítulo funcional en Sora.
 */
@Composable
private fun Wordmark() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "amover",
            style =
                TextStyle(
                    fontFamily = AppTypography.frauncesItalic,
                    fontStyle = FontStyle.Italic,
                    fontSize = 29.sp,
                    brush = Brush.verticalGradient(listOf(AppColors.gold0, AppColors.gold2)),
                ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Remover fondos y redimensiona tus imágenes",
            style = TextStyle(fontFamily = AppTypography.soraFamily, fontSize = 12.5.sp),
            color = AppColors.creamDim,
        )
    }
}

/**
 * La "Linie" bajo el wordmark (spec §5.2): línea dorada horizontal con los
 * extremos transparentes, opacidad 0.55 — separa el header del contenido.
 */
@Composable
private fun GoldDivider() {
    Box(
        modifier =
            Modifier
                .padding(vertical = 26.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            AppColors.gold2,
                            AppColors.gold0,
                            AppColors.gold2,
                            Color.Transparent,
                        ),
                    ),
                ),
    )
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

/**
 * Traduce el [OperationStatus] actual a un mensaje legible para el usuario.
 * En éxito, resalta el nombre del archivo de salida en dorado (spec §5.8):
 * es el único dato que importa mirar de un vistazo en ese mensaje.
 */
@Composable
private fun StatusText(
    status: OperationStatus,
    processingLabel: String,
) {
    val style = TextStyle(fontFamily = AppTypography.soraFamily, fontSize = 12.sp)
    when (status) {
        is OperationStatus.Idle -> {}
        is OperationStatus.Processing -> Text(processingLabel, style = style, color = AppColors.creamDim)
        is OperationStatus.Error -> Text("Error: ${status.message}", style = style, color = AppColors.creamDim)
        is OperationStatus.Success -> {
            val fileName = status.output.name
            val nameIndex = status.message.indexOf(fileName)
            val annotated =
                buildAnnotatedString {
                    if (nameIndex < 0) {
                        append(status.message)
                    } else {
                        append(status.message.substring(0, nameIndex))
                        withStyle(SpanStyle(color = AppColors.gold0, fontWeight = FontWeight.Medium)) {
                            append(fileName)
                        }
                        append(status.message.substring(nameIndex + fileName.length))
                    }
                }
            Text(annotated, style = style, color = AppColors.creamDim)
        }
    }
}
