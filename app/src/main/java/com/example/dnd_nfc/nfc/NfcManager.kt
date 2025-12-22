package com.example.dnd_nfc.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.example.dnd_nfc.data.local.DataCompressor
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.google.gson.Gson
import java.nio.charset.Charset

object NfcManager {

    private val gson = Gson()
    // Definimos un tipo MIME propio para asegurar que leemos solo nuestras etiquetas
    private const val MIME_TYPE = "application/dnd"

    // LEE Y DESCOMPRIME
    fun readCharacterFromIntent(intent: Intent): PlayerCharacter? {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

            if (rawMsgs != null && rawMsgs.isNotEmpty()) {
                val msg = rawMsgs[0] as NdefMessage
                val record = msg.records[0]

                // Corrección: Leemos el payload directamente sin intentar limpiarlo con lógica compleja
                // Al usar createMime, no hay cabeceras de idioma ("en") que estorben.
                val compressedPayload = String(record.payload, Charset.forName("UTF-8"))

                // Descomprimimos
                val json = DataCompressor.decompress(compressedPayload)

                return if (json.isNotEmpty()) {
                    try {
                        gson.fromJson(json, PlayerCharacter::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else {
                    null // Dato corrupto o vacío
                }
            }
        }
        return null
    }

    // COMPRIME Y ESCRIBE
    fun writeCharacterToTag(tag: Tag, character: PlayerCharacter): Boolean {
        try {
            // 1. Convertir objeto a JSON
            val json = gson.toJson(character)

            // 2. Comprimir JSON
            val compressedData = DataCompressor.compress(json)

            // 3. Crear registro MIME (Más limpio que TextRecord)
            // Esto guarda SOLO los datos, sin prefijos de idioma.
            val payload = compressedData.toByteArray(Charset.forName("UTF-8"))
            val ndefRecord = NdefRecord.createMime(MIME_TYPE, payload)

            val ndefMessage = NdefMessage(arrayOf(ndefRecord))

            return writeNdefMessage(tag, ndefMessage)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun writeNdefMessage(tag: Tag, message: NdefMessage): Boolean {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return false
                }
                // Verificamos espacio (NTAG215 ~504 bytes)
                if (ndef.maxSize < message.byteArrayLength) {
                    ndef.close()
                    return false
                }

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