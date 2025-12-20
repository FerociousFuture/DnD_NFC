package com.example.dnd_nfc.nfc

import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.content.Intent

object NfcManager {
    /**
     * Extrae el texto plano de un Intent NFC.
     */
    fun readFromIntent(intent: Intent): String {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMessages != null) {
            val msg = rawMessages[0] as NdefMessage
            // Retorna el contenido del primer registro como String
            return String(msg.records[0].payload).drop(3) // Quitamos prefijo de idioma (en)
        }
        return ""
    }
}