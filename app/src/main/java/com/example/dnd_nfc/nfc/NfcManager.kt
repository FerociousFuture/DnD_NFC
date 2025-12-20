package com.example.dnd_nfc.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import com.example.dnd_nfc.data.model.CharacterSheet

object NfcManager {
    /**
     * Lee el Intent NFC y procesa el formato CSV compacto para ahorrar espacio:
     * "Nombre,Clase,Raza,FUE,DES,CON,INT,SAB,CAR"
     */
    fun readFromIntent(intent: Intent): CharacterSheet? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMessages != null && rawMessages.isNotEmpty()) {
            val msg = rawMessages[0] as NdefMessage

            // Extraemos el payload manejando el estándar NDEF (metadatos de idioma)
            val payloadBytes = msg.records[0].payload
            val isUtf8 = (payloadBytes[0].toInt() and 0x80) == 0
            val langCodeLength = payloadBytes[0].toInt() and 0x3F
            val textEncoding = if (isUtf8) "UTF-8" else "UTF-16"

            val payload = String(
                payloadBytes,
                langCodeLength + 1,
                payloadBytes.size - langCodeLength - 1,
                charset(textEncoding)
            )

            // Dividimos por coma para el formato CSV
            val parts = payload.split(",")

            // Verificamos que existan al menos los 3 campos base + 6 estadísticas = 9 campos
            if (parts.size >= 9) {
                return CharacterSheet(
                    n = parts[0].trim(),
                    c = parts[1].trim(),
                    r = parts[2].trim(),
                    // Unimos las estadísticas con guiones para el almacenamiento interno en el modelo
                    s = parts.subList(3, 9).joinToString("-") { it.trim() }
                )
            }
        }
        return null
    }
}