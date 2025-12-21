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

    fun readFromIntent(intent: Intent): CharacterSheet? {
        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return null
        val msgs = rawMsgs.map { it as NdefMessage }
        if (msgs.isEmpty()) return null

        val record = msgs[0].records[0]
        val payload = String(record.payload, Charset.forName("UTF-8"))

        // NUEVO FORMATO: ID,Nombre,F,D,C,I,S,Car (Total 8 partes)
        val parts = payload.split(",")

        // Verificamos que tenga al menos 8 partes (ID + Nombre + 6 Stats)
        return if (parts.size >= 8) {
            CharacterSheet(
                id = parts[0],
                n = parts[1],
                // Construimos el string de stats uniendo las partes 2 a 7
                s = "${parts[2]}-${parts[3]}-${parts[4]}-${parts[5]}-${parts[6]}-${parts[7]}"
            )
        } else {
            null
        }
    }

    fun writeToTag(tag: Tag, csvData: String): Boolean {
        // ... (El resto de la función de escritura se mantiene igual)
        val record = NdefRecord.createMime("text/plain", csvData.toByteArray())
        val message = NdefMessage(arrayOf(record))

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) return false
                ndef.writeNdefMessage(message)
                ndef.close()
                return true
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(message)
                    formatable.close()
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}