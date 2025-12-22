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

    // LEE Y DESCOMPRIME
    fun readCharacterFromIntent(intent: Intent): PlayerCharacter? {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

            if (rawMsgs != null && rawMsgs.isNotEmpty()) {
                val msg = rawMsgs[0] as NdefMessage
                // Leemos el payload crudo
                val compressedPayload = String(msg.records[0].payload, Charset.forName("UTF-8"))

                // Limpiamos cabeceras de idioma si existen (ej: "en" al principio)
                // Buscamos el inicio del Base64 (suele ser alfanumérico o '+', '/')
                val cleanPayload = compressedPayload.dropWhile { !it.isLetterOrDigit() && it != '+' && it != '/' }

                // Descomprimimos
                val json = DataCompressor.decompress(cleanPayload)

                return if (json.isNotEmpty()) {
                    try {
                        gson.fromJson(json, PlayerCharacter::class.java)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null // No era un dato comprimido válido
                }
            }
        }
        return null
    }

    // COMPRIME Y ESCRIBE
    fun writeCharacterToTag(tag: Tag, character: PlayerCharacter): Boolean {
        // 1. Convertir objeto a JSON
        val json = gson.toJson(character)

        // 2. Comprimir JSON
        val compressedData = DataCompressor.compress(json)

        // Verificación de seguridad de tamaño (NTAG215 tiene ~504 bytes)
        // Si se pasa, podríamos avisar al usuario, pero por ahora intentamos escribir.

        val ndefRecord = NdefRecord.createTextRecord("en", compressedData)
        val ndefMessage = NdefMessage(arrayOf(ndefRecord))

        return writeNdefMessage(tag, ndefMessage)
    }

    private fun writeNdefMessage(tag: Tag, message: NdefMessage): Boolean {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) return false
                // Verificamos si cabe
                if (ndef.maxSize < message.byteArrayLength) return false

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