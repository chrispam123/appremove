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
            resizedWidth = 800,
            resizedHeight = 400,
            resizedBytes = 120_000,
        )

    @Test
    fun `delegates to the strategy and returns its result when input exists`() {
        val input = File.createTempFile("resize-test-input", ".jpg").apply { deleteOnExit() }
        val output = File.createTempFile("resize-test-output", ".jpg").apply { deleteOnExit() }

        var receivedSize: Pair<Int, Int>? = null
        val fakeResizer =
            ImageResizer { _, _, targetWidth, targetHeight ->
                receivedSize = targetWidth to targetHeight
                fakeResult
            }

        val result = ResizeImageUseCase(fakeResizer)(input, output, targetWidth = 800, targetHeight = 400)

        assertTrue(result.isSuccess)
        assertEquals(fakeResult, result.getOrNull())
        assertEquals(800 to 400, receivedSize)
    }

    @Test
    fun `fails without calling the strategy when input does not exist`() {
        val missingInput = File("this-file-does-not-exist.jpg")
        val output = File.createTempFile("resize-test-output", ".jpg").apply { deleteOnExit() }

        var wasCalled = false
        val fakeResizer =
            ImageResizer { _, _, _, _ ->
                wasCalled = true
                fakeResult
            }

        val result = ResizeImageUseCase(fakeResizer)(missingInput, output, targetWidth = 800, targetHeight = 400)

        assertTrue(result.isFailure)
        assertFalse(wasCalled)
    }

    @Test
    fun `fails without calling the strategy when width or height is not positive`() {
        val input = File.createTempFile("resize-test-input", ".jpg").apply { deleteOnExit() }
        val output = File.createTempFile("resize-test-output", ".jpg").apply { deleteOnExit() }

        var wasCalled = false
        val fakeResizer =
            ImageResizer { _, _, _, _ ->
                wasCalled = true
                fakeResult
            }
        val useCase = ResizeImageUseCase(fakeResizer)

        assertTrue(useCase(input, output, targetWidth = 0, targetHeight = 400).isFailure)
        assertTrue(useCase(input, output, targetWidth = 800, targetHeight = -1).isFailure)
        assertFalse(wasCalled)
    }
}
