package com.appremove.domain.resize

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifica la regla de tres de [AspectRatio] para distintas proporciones.
 */
class AspectRatioTest {
    @Test
    fun `computes linked height for a landscape image`() {
        // 2000x1000 es 2:1 -> un ancho de 800 debería dar un alto de 400.
        assertEquals(400, AspectRatio.linkedHeight(originalWidth = 2000, originalHeight = 1000, newWidth = 800))
    }

    @Test
    fun `computes linked width for a portrait image`() {
        // 1000x2000 es 1:2 -> un alto de 400 debería dar un ancho de 200.
        assertEquals(200, AspectRatio.linkedWidth(originalWidth = 1000, originalHeight = 2000, newHeight = 400))
    }

    @Test
    fun `keeps the same value for a square image`() {
        assertEquals(500, AspectRatio.linkedHeight(originalWidth = 1000, originalHeight = 1000, newWidth = 500))
        assertEquals(500, AspectRatio.linkedWidth(originalWidth = 1000, originalHeight = 1000, newHeight = 500))
    }

    @Test
    fun `never returns less than 1px, even for a tiny target`() {
        assertEquals(1, AspectRatio.linkedHeight(originalWidth = 2000, originalHeight = 1, newWidth = 1))
    }

    @Test
    fun `returns the input as-is when the original dimensions are unknown`() {
        assertEquals(800, AspectRatio.linkedHeight(originalWidth = 0, originalHeight = 0, newWidth = 800))
        assertEquals(400, AspectRatio.linkedWidth(originalWidth = 0, originalHeight = 0, newHeight = 400))
    }
}
