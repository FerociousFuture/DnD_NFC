package com.example.dnd_nfc.data.local

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.nio.charset.StandardCharsets

object DataCompressor {

    // Comprime un String (JSON) a una cadena Base64 corta
    fun compress(data: String): String {
        if (data.isEmpty()) return ""
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzip ->
            gzip.write(data.toByteArray(StandardCharsets.UTF_8))
        }
        // Usamos Base64 sin saltos de línea para ahorrar espacio
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    // Descomprime la cadena Base64 al String original (JSON)
    fun decompress(compressedData: String): String {
        if (compressedData.isEmpty()) return ""
        return try {
            val bytes = Base64.getDecoder().decode(compressedData)
            val inputStream = ByteArrayInputStream(bytes)
            GZIPInputStream(inputStream).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}