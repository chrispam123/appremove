package com.appremove.app.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de color del spec de diseño "Bauhaus cromado" (ver
 * `appremove-design-spec.md` §2). Grafito + cromo cubren el 95% de la
 * superficie; el dorado se reserva para un único elemento "activo" por
 * pantalla (wordmark, la línea de los swatches, el borde de foco de los
 * inputs, y el CTA principal) — nunca más de uno a la vez.
 */
object AppColors {
    val graphite0 = Color(0xFF14161A) // fondo de página
    val graphite1 = Color(0xFF1C1F24) // fondo de pantalla / inputs
    val graphite2 = Color(0xFF24272D)

    val chromeHi = Color(0xFFE9EDF1) // highlight de metal
    val chromeMid = Color(0xFFA7ADB6) // metal medio
    val chromeLow = Color(0xFF3A3E45) // sombra de metal

    val cream = Color(0xFFF2EFE9) // texto principal
    val creamDim = Color(0xFFC7C3BA) // texto secundario

    val gold0 = Color(0xFFF0C987) // oro claro
    val gold1 = Color(0xFFD9A24E) // oro medio — acento por defecto
    val gold2 = Color(0xFFA8721F) // oro oscuro / bronce

    val hairline = Color(0xFF33373D) // bordes hairline
}
