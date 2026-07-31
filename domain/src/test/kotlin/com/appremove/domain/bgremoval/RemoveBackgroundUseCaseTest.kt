package com.appremove.domain.bgremoval

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifica la orquestación de [RemoveBackgroundUseCase] usando un
 * [BackgroundRemover] falso, sin tocar ONNX Runtime ni el modelo real
 * (ese trabajo lo hace `core-ml`, con sus propios tests).
 */
class RemoveBackgroundUseCaseTest {
    private val fakeResult =
        BackgroundRemovalResult(
            width = 800,
            height = 600,
            originalBytes = 500_000,
            resultBytes = 650_000,
        )

    @Test
    fun `delegates to the strategy with the chosen background and returns its result`() {
        val input = File.createTempFile("bg-test-input", ".jpg").apply { deleteOnExit() }
        val output = File.createTempFile("bg-test-output", ".png").apply { deleteOnExit() }

        var receivedBackground: BackgroundChoice? = null
        val fakeRemover =
            BackgroundRemover { _, _, background ->
                receivedBackground = background
                fakeResult
            }

        val chosenBackground = BackgroundChoice.SolidColor(255, 255, 255)
        val result = RemoveBackgroundUseCase(fakeRemover)(input, output, chosenBackground)

        assertTrue(result.isSuccess)
        assertEquals(fakeResult, result.getOrNull())
        assertEquals(chosenBackground, receivedBackground)
    }

    @Test
    fun `fails without calling the strategy when input does not exist`() {
        val missingInput = File("this-file-does-not-exist.jpg")
        val output = File.createTempFile("bg-test-output", ".png").apply { deleteOnExit() }

        var wasCalled = false
        val fakeRemover =
            BackgroundRemover { _, _, _ ->
                wasCalled = true
                fakeResult
            }

        val result = RemoveBackgroundUseCase(fakeRemover)(missingInput, output, BackgroundChoice.Transparent)

        assertTrue(result.isFailure)
        assertFalse(wasCalled)
    }
}
