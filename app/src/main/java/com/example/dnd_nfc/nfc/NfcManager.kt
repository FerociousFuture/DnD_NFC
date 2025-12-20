package com.example.dnd_nfc.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import com.example.dnd_nfc.data.model.CharacterSheet

object NfcManager {
    /**
     * Lee el Intent NFC y procesa el formato "Nombre|Clase|Raza|Stats"
     */
    fun readFromIntent(intent: Intent): CharacterSheet? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMessages != null && rawMessages.isNotEmpty()) {
            val msg = rawMessages[0] as NdefMessage
            // Extraemos el texto saltando los bytes de metadatos de idioma
            val payload = String(msg.records[0].payload).drop(3)
            val parts = payload.split("|")

            if (parts.size >= 4) {
                return CharacterSheet(
                    n = parts[0],
                    c = parts[1],
                    r = parts[2],
                    s = parts[3]
                )
            }
        }
        return null
    }
}