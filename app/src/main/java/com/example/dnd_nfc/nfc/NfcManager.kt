package com.example.dnd_nfc.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.example.dnd_nfc.data.model.CharacterSheet
import java.nio.charset.Charset

object NfcManager {

    /**
     * Lee el Intent NFC y procesa el formato CSV compacto:
     * "Nombre,Clase,Raza,FUE,DES,CON,INT,SAB,CAR"
     */
    fun readFromIntent(intent: Intent): CharacterSheet? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

        if (rawMessages != null && rawMessages.isNotEmpty()) {
            val msg = rawMessages[0] as NdefMessage

            // Verificamos que el registro no esté vacío
            if (msg.records.isEmpty()) return null

            val payloadBytes = msg.records[0].payload

            // Lógica para saltar la cabecera de idioma (Status byte + Language code) estándar de NDEF
            // El primer byte indica la longitud del código de idioma (bits 0-5)
            val languageCodeLength = payloadBytes[0].toInt() and 0x3F

            // El contenido real empieza después del status byte y del código de idioma
            val payload = String(
                payloadBytes,
                languageCodeLength + 1,
                payloadBytes.size - languageCodeLength - 1,
                Charset.forName("UTF-8")
            )

            // Dividimos por coma para el formato CSV
            val parts = payload.split(",")

            // Verificamos que existan al menos los 3 campos base + 6 estadísticas = 9 campos
            if (parts.size >= 9) {
                return CharacterSheet(
                    n = parts[0].trim(),
                    c = parts[1].trim(),
                    r = parts[2].trim(),
                    // Unimos las estadísticas con guiones para el modelo interno
                    s = parts.subList(3, 9).joinToString("-") { it.trim() }
                )
            }
        }
        return null
    }

    /**
     * Escribe los datos en la etiqueta NFC en formato CSV.
     * Retorna true si la escritura fue exitosa.
     */
    fun writeToTag(tag: Tag, data: String): Boolean {
        val ndefRecord = createTextRecord(data)
        val ndefMessage = NdefMessage(arrayOf(ndefRecord))

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return false
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                return true
            } else {
                // Si la etiqueta no está formateada como NDEF, intentamos formatearla
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(ndefMessage)
                    formatable.close()
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Crea un registro NDEF de texto plano (RTD_TEXT).
     */
    private fun createTextRecord(text: String): NdefRecord {
        val langBytes = "en".toByteArray(Charset.forName("US-ASCII"))
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val payload = ByteArray(1 + langBytes.size + textBytes.size)

        // Status byte: Bit 7 = 0 (UTF-8), Bits 0-5 = longitud del código de idioma
        payload[0] = langBytes.size.toByte()

        // Copiamos el código de idioma
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)

        // Copiamos el texto
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
    }
}