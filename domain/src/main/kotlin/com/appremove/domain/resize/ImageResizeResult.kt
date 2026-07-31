package com.appremove.domain.resize

/**
 * Resultado de una operación de resize: guarda las dimensiones y el peso en bytes
 * antes y después, para que la UI pueda mostrarle al usuario cuánto se redujo.
 */
data class ImageResizeResult(
    val originalWidth: Int,
    val originalHeight: Int,
    val originalBytes: Long,
    val resizedWidth: Int,
    val resizedHeight: Int,
    val resizedBytes: Long,
)
