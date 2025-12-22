package com.example.dnd_nfc.data.local

import android.util.Base64 // Usar android.util en lugar de java.util para compatibilidad
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.nio.charset.StandardCharsets

object DataCompressor {

    fun compress(data: String): String {
        if (data.isEmpty()) return ""
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzip ->
            gzip.write(data.toByteArray(StandardCharsets.UTF_8))
        }
        // Usamos NO_WRAP para evitar saltos de línea
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun decompress(compressedData: String): String {
        if (compressedData.isEmpty()) return ""
        return try {
            val bytes = Base64.decode(compressedData, Base64.NO_WRAP)
            val inputStream = ByteArrayInputStream(bytes)
            GZIPInputStream(inputStream).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}