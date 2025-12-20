package com.example.dnd_nfc.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.content.Intent
import com.example.dnd_nfc.data.model.CharacterSheet

object NfcManager {

    // Escribe los datos en formato ultra-compacto: "Nombre|Clase|Raza|10,10,10..."
    fun writeToTag(tag: Tag, character: CharacterSheet): Boolean {
        val payload = "${character.n}|${character.c}|${character.r}|${character.s}"
        val record = NdefRecord.createTextRecord("en", payload)
        val message = NdefMessage(arrayOf(record))

        return try {
            val ndef = Ndef.get(tag)
            ndef?.let {
                it.connect()
                it.writeNdefMessage(message)
                it.close()
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    // Lee y reconstruye el objeto
    fun readFromIntent(intent: Intent): CharacterSheet? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMessages != null) {
            val msg = rawMessages[0] as NdefMessage
            val payload = String(msg.records[0].payload).drop(3) // Quita prefijo de idioma
            val parts = payload.split("|")

            if (parts.size >= 4) {
                return CharacterSheet(n = parts[0], c = parts[1], r = parts[2], s = parts[3])
            }
        }
        return null
    }
}