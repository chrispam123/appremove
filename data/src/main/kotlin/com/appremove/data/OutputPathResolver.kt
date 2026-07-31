package com.appremove.data

import java.io.File

/**
 * Repositorio de filesystem: decide dónde debe guardarse el resultado de un
 * resize a partir del archivo original, sin pisar un archivo que ya exista.
 */
class OutputPathResolver {
    /**
     * Genera el [File] de salida para [input] agregando [suffix] antes de la
     * extensión (ej. suffix="_resized" -> "foto.jpg" -> "foto_resized.jpg"),
     * en la misma carpeta. Por defecto conserva la extensión de [input], pero
     * se puede forzar otra con [extension] (la remoción de fondo, por ejemplo,
     * siempre escribe PNG sea cual sea el formato original). Si el nombre
     * resultante ya existe (porque ya se corrió antes), agrega un número
     * (ej. "_resized_2", "_resized_3", ...) hasta encontrar uno libre, en vez
     * de sobreescribir un resultado anterior.
     */
    fun resolve(
        input: File,
        suffix: String,
        extension: String = input.extension,
    ): File {
        val parent = input.parentFile
        val baseName = input.nameWithoutExtension

        var candidate = buildFile(parent, baseName, suffix, extension)
        var attempt = 2
        while (candidate.exists()) {
            candidate = buildFile(parent, baseName, "${suffix}_$attempt", extension)
            attempt++
        }
        return candidate
    }

    private fun buildFile(
        parent: File?,
        baseName: String,
        suffix: String,
        extension: String,
    ): File {
        val fileName = if (extension.isBlank()) "$baseName$suffix" else "$baseName$suffix.$extension"
        return if (parent != null) File(parent, fileName) else File(fileName)
    }
}
