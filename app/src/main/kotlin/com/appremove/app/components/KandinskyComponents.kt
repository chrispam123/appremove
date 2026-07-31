package com.appremove.app.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appremove.app.theme.AppColors
import com.appremove.app.theme.AppTypography
import com.appremove.domain.bgremoval.BackgroundChoice

/**
 * El "Punkt" de Kandinsky (spec §4): el botón circular que arranca todo —
 * elegir imagen. Gradiente radial simulando luz desde arriba-izquierda
 * (principio no negociable §6.2) + bisel dibujado a mano superponiendo un
 * realce claro cerca de la luz y una sombra oscura del lado opuesto (Compose
 * no tiene una sombra "inset" nativa como CSS).
 */
@Composable
fun PunktButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .shadow(elevation = 10.dp, shape = CircleShape)
                .size(104.dp)
                .clip(CircleShape)
                .drawWithCache {
                    // Degradado base con más contraste: el brillo se concentra cerca
                    // del punto de luz y cae rápido a sombra, en vez de una
                    // transición pareja — así se lee como metal pulido, no como una
                    // esfera gris lisa.
                    val base =
                        Brush.radialGradient(
                            colorStops =
                                arrayOf(
                                    0f to AppColors.chromeHi,
                                    0.22f to AppColors.chromeMid,
                                    0.55f to AppColors.chromeLow,
                                    1f to AppColors.graphite0,
                                ),
                            center = Offset(size.width * 0.35f, size.height * 0.25f),
                            radius = size.maxDimension * 0.95f,
                        )
                    val highlight =
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                            center = Offset(size.width * 0.3f, size.height * 0.18f),
                            radius = size.minDimension * 0.26f,
                        )
                    val innerShadow =
                        Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.9f),
                            radius = size.minDimension * 0.5f,
                        )
                    onDrawBehind {
                        drawCircle(base)
                        drawCircle(highlight)
                        drawCircle(innerShadow)
                    }
                }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        UploadIcon()
    }
}

/** Ícono de "subir imagen": flecha hacia arriba + bandeja, trazo simple (spec §5.3). */
@Composable
private fun UploadIcon() {
    Canvas(modifier = Modifier.size(34.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val color = AppColors.graphite0
        val w = size.width
        val h = size.height

        drawLine(color, Offset(w * 0.5f, h * 0.67f), Offset(w * 0.5f, h * 0.17f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.17f), Offset(w * 0.33f, h * 0.33f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.17f), Offset(w * 0.67f, h * 0.33f), strokeWidth, StrokeCap.Round)

        val tray =
            Path().apply {
                moveTo(w * 0.17f, h * 0.67f)
                lineTo(w * 0.17f, h * 0.83f)
                quadraticTo(w * 0.17f, h * 0.92f, w * 0.25f, h * 0.92f)
                lineTo(w * 0.75f, h * 0.92f)
                quadraticTo(w * 0.83f, h * 0.92f, w * 0.83f, h * 0.83f)
                lineTo(w * 0.83f, h * 0.67f)
            }
        drawPath(tray, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Etiqueta de sección (spec §3): Sora 500, mayúsculas, letter-spacing 1.2px,
 * en dorado — encabeza cada bloque ("Dimensiones", "Fondo de reemplazo").
 */
@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style =
            TextStyle(
                fontFamily = AppTypography.soraFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
            ),
        color = AppColors.gold0,
    )
}

/**
 * El "Fläche" de Kandinsky (spec §4): el marco cuadrado con doble borde que
 * contiene los inputs de ancho/alto. El borde interior (`inset: 6px` en el
 * mockup) es el eco visual del cuadro de Kandinsky — acá se dibuja como un
 * segundo `Box` con padding en vez de un pseudo-elemento CSS.
 */
@Composable
fun FrameSquare(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.03f), Color.Black.copy(alpha = 0.15f))))
                .border(1.dp, AppColors.hairline, RoundedCornerShape(10.dp)),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .padding(6.dp)
                    .border(1.dp, AppColors.gold1.copy(alpha = 0.25f), RoundedCornerShape(6.dp)),
        )
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/**
 * Botón metálico secundario (spec §5.5): gradiente grafito vertical, sin
 * acento dorado propio (no compite con el CTA principal) — el borde solo se
 * pone dorado en hover, como único guiño de interactividad.
 */
@Composable
fun MetalButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val borderColor = if (hovered && enabled) AppColors.gold1 else AppColors.chromeMid

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF464B53), Color(0xFF2C2F34))))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .hoverable(interactionSource)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextStyle(fontFamily = AppTypography.soraFamily, fontWeight = FontWeight.Medium, fontSize = 13.5.sp),
            color = if (enabled) AppColors.cream else AppColors.cream.copy(alpha = 0.4f),
        )
    }
}

/**
 * Campo de ancho/alto dentro del [FrameSquare] (spec §5.4): fondo grafito,
 * borde hairline, foco → borde dorado. Solo acepta números (teclado numérico).
 */
@Composable
fun DimensionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, style = TextStyle(fontFamily = AppTypography.soraFamily, fontSize = 11.sp)) },
        textStyle = TextStyle(fontFamily = AppTypography.soraFamily, fontSize = 15.sp, color = AppColors.cream),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors =
            TextFieldDefaults.outlinedTextFieldColors(
                textColor = AppColors.cream,
                backgroundColor = AppColors.graphite1,
                focusedBorderColor = AppColors.gold1,
                unfocusedBorderColor = AppColors.hairline,
                cursorColor = AppColors.gold1,
                focusedLabelColor = AppColors.creamDim,
                unfocusedLabelColor = AppColors.creamDim,
            ),
    )
}

/**
 * La "Linie" de Kandinsky (spec §4/§5.6): línea dorada horizontal que
 * organiza y conecta las opciones de fondo. El swatch "transparente" es una
 * píldora con patrón de damero (para distinguirlo como opción especial, no
 * un color más); el resto son círculos; el último ("+") abre el selector de
 * imagen de fondo.
 */
@Composable
fun BackgroundSwatchRow(
    selected: BackgroundChoice,
    palette: List<BackgroundChoice.SolidColor>,
    onSelect: (BackgroundChoice) -> Unit,
    onPickImage: () -> Unit,
) {
    Column {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Brush.horizontalGradient(listOf(AppColors.gold2, AppColors.gold0, AppColors.gold2))),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TransparentSwatch(
                selected = selected is BackgroundChoice.Transparent,
                onClick = { onSelect(BackgroundChoice.Transparent) },
            )
            for (colorChoice in palette) {
                ColorCircleSwatch(
                    color = Color(colorChoice.red, colorChoice.green, colorChoice.blue),
                    selected = selected == colorChoice,
                    onClick = { onSelect(colorChoice) },
                )
            }
            MoreSwatch(selected = selected is BackgroundChoice.Image, onClick = onPickImage)
        }
    }
}

/** Píldora con patrón de damero (spec §5.6): representa "sin fondo", no un color. */
@Composable
private fun TransparentSwatch(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .drawWithCache {
                    val tile = 8.dp.toPx()
                    onDrawBehind {
                        drawRect(AppColors.graphite1)
                        var y = 0f
                        var row = 0
                        while (y < size.height) {
                            var x = if (row % 2 == 0) 0f else tile
                            while (x < size.width) {
                                drawRect(Color(0xFF55585E), topLeft = Offset(x, y), size = Size(tile, tile))
                                x += tile * 2
                            }
                            y += tile
                            row++
                        }
                    }
                }.border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) AppColors.gold1 else AppColors.hairline,
                    shape = RoundedCornerShape(15.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Ninguno",
            style = TextStyle(fontFamily = AppTypography.soraFamily, fontWeight = FontWeight.Medium, fontSize = 10.5.sp),
            color = AppColors.cream,
        )
    }
}

/** Swatch circular de un color sólido de la paleta (spec §5.6). */
@Composable
private fun ColorCircleSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 3.dp else 2.dp,
                    color = if (selected) AppColors.gold1 else AppColors.graphite0,
                    shape = CircleShape,
                ).clickable(onClick = onClick),
    )
}

/** Swatch "+": abre el selector de archivos para elegir una imagen de fondo. */
@Composable
private fun MoreSwatch(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(AppColors.graphite1)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) AppColors.gold1 else AppColors.hairline,
                    shape = CircleShape,
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("+", color = AppColors.creamDim, fontSize = 16.sp)
    }
}

/**
 * El CTA principal (spec §5.7): el elemento de mayor lujo de la pantalla —
 * marco degradado tipo joyería, glow pulsante (nunca más rápido que ~3s, para
 * que se sienta como presencia, no alarma) y un brillo diagonal que recorre
 * el botón al pasar el mouse (equivalente al `background-position` animado
 * de CSS, acá logrado desplazando un `Brush` en cada frame).
 */
@Composable
fun LuxuryCta(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowElevation by
        infiniteTransition.animateFloat(
            initialValue = 10f,
            targetValue = 22f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
        )

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val sweep by animateFloatAsState(targetValue = if (hovered) 1f else 0f, animationSpec = tween(500))

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = glowElevation.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = AppColors.gold1,
                    spotColor = AppColors.gold1,
                ).clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AppColors.gold0, AppColors.gold2, Color(0xFF6B4A15), AppColors.gold1),
                    ),
                ).padding(2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(AppColors.gold0, AppColors.gold1, AppColors.gold2)))
                .drawWithCache {
                    // Franja de brillo diagonal que se desliza de derecha a
                    // izquierda en hover (spec §5.7: "simula luz recorriendo metal
                    // pulido"). En reposo queda casi toda fuera del botón.
                    val sheenWidth = size.width * 0.35f
                    val travel = size.width + sheenWidth
                    val startX = travel * (1f - sweep) - sheenWidth
                    val sheen =
                        Brush.linearGradient(
                            colorStops =
                                arrayOf(
                                    0f to Color.Transparent,
                                    0.5f to Color.White.copy(alpha = 0.65f),
                                    1f to Color.Transparent,
                                ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + sheenWidth, size.height),
                        )
                    onDrawBehind { drawRect(sheen) }
                }.hoverable(interactionSource)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextStyle(fontFamily = AppTypography.soraFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
            color = if (enabled) Color(0xFF3A2506) else Color(0xFF3A2506).copy(alpha = 0.5f),
        )
    }
}
