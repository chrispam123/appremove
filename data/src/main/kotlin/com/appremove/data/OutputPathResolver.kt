package com.appremove.data

import java.io.File

/**
 * Repositorio de filesystem: decide dónde debe guardarse el resultado de un
 * resize a partir del archivo original, sin pisar un archivo que ya exista.
 */
class OutputPathResolver {
    /**
     * Genera el [File] de salida para [input] agregando el sufijo "_resized" antes
     * de la extensión (ej. "foto.jpg" -> "foto_resized.jpg"), en la misma carpeta.
     * Si ese nombre ya existe (por ejemplo porque ya se corrió antes), agrega un
     * número ("_resized_2", "_resized_3", ...) hasta encontrar uno libre, en vez
     * de sobreescribir un resultado anterior.
     */
    fun resolve(input: File): File {
        val parent = input.parentFile
        val baseName = input.nameWithoutExtension
        val extension = input.extension

        var candidate = buildFile(parent, baseName, "_resized", extension)
        var attempt = 2
        while (candidate.exists()) {
            candidate = buildFile(parent, baseName, "_resized_$attempt", extension)
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
