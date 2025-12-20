package com.example.dnd_nfc.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import com.example.dnd_nfc.data.model.CharacterSheet

object NfcManager {
    /**
     * Lee el Intent NFC y procesa el formato CSV: "Nombre,Clase,Raza,Stats"
     */
    fun readFromIntent(intent: Intent): CharacterSheet? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMessages != null && rawMessages.isNotEmpty()) {
            val msg = rawMessages[0] as NdefMessage

            // Obtenemos el payload completo
            val payload = msg.records[0].payload

            // El estándar NDEF suele incluir el código de idioma al principio (ej: "en")
            // Saltamos el primer byte (status) y el código de idioma
            val languageCodeLength = payload[0].toInt() and 0x3F
            val textEncoding = if ((payload[0].toInt() and 0x80) == 0) "UTF-8" else "UTF-16"

            val content = String(
                payload,
                languageCodeLength + 1,
                payload.size - languageCodeLength - 1,
                charset(textEncoding)
            )

            // Dividimos por coma para el formato CSV
            val parts = content.split(",")

            if (parts.size >= 4) {
                return CharacterSheet(
                    n = parts[0].trim(),
                    c = parts[1].trim(),
                    r = parts[2].trim(),
                    s = parts[3].trim()
                )
            }
        }
        return null
    }
}