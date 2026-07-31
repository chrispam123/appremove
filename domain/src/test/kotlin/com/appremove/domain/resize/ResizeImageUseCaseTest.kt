package com.appremove.domain.resize

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifica la orquestación de [ResizeImageUseCase] usando un [ImageResizer] falso,
 * sin tocar ningún algoritmo real de resize ni depender de core-image.
 */
class ResizeImageUseCaseTest {
    private val fakeResult =
        ImageResizeResult(
            originalWidth = 2000,
            originalHeight = 1000,
            originalBytes = 500_000,
            resizedWidth = 1280,
            resizedHeight = 640,
            resizedBytes = 120_000,
        )

    @Test
    fun `delegates to the strategy and returns its result when input exists`() {
        val input = File.createTempFile("resize-test-input", ".jpg").apply { deleteOnExit() }
        val output = File.createTempFile("resize-test-output", ".jpg").apply { deleteOnExit() }

        var receivedMaxDimension = -1
        val fakeResizer =
            ImageResizer { _, _, maxDimension ->
                receivedMaxDimension = maxDimension
                fakeResult
            }

        val result = ResizeImageUseCase(fakeResizer)(input, output, maxDimension = 1280)

        assertTrue(result.isSuccess)
        assertEquals(fakeResult, result.getOrNull())
        assertEquals(1280, receivedMaxDimension)
    }

    @Test
    fun `fails without calling the strategy when input does not exist`() {
        val missingInput = File("this-file-does-not-exist.jpg")
        val output = File.createTempFile("resize-test-output", ".jpg").apply { deleteOnExit() }

        var wasCalled = false
        val fakeResizer =
            ImageResizer { _, _, _ ->
                wasCalled = true
                fakeResult
            }

        val result = ResizeImageUseCase(fakeResizer)(missingInput, output, maxDimension = 1280)

        assertTrue(result.isFailure)
        assertFalse(wasCalled)
    }
}
