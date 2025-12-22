package com.example.dnd_nfc.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.example.dnd_nfc.data.model.BattleState
import com.google.gson.Gson
import java.nio.charset.Charset
import kotlin.random.Random

object NfcCombatManager {
    private val gson = Gson()

    data class AttackRequest(
        val diceCount: Int,
        val dieFaces: Int,
        val bonus: Int
    )

    data class AttackResult(
        val hit: Boolean, // Siempre true en este modo manual
        val damageDealt: Int,
        val attackRoll: Int, // No se usa, se deja a 0
        val enemyState: BattleState,
        val message: String
    )

    fun performAttack(tag: Tag, request: AttackRequest): AttackResult? {
        // 1. LEER ESTADO ACTUAL DE LA FIGURA
        val currentState = readTag(tag) ?: return null

        // 2. CALCULAR RESULTADO DE DADOS (Sin tirada de ataque contra AC)
        var totalRoll = 0
        repeat(request.diceCount) {
            totalRoll += Random.nextInt(1, request.dieFaces + 1)
        }
        val finalValue = totalRoll + request.bonus

        // Evitamos valores negativos (aunque podría ser curación si se implementara lógica, asumimos daño)
        val damage = if (finalValue < 0) 0 else finalValue

        // 3. APLICAR DAÑO A LA VIDA
        var newHp = currentState.hp - damage
        if (newHp < 0) newHp = 0

        val newState = currentState.copy(hp = newHp)

        // 4. ESCRIBIR NUEVO ESTADO EN LA FIGURA
        val written = writeTag(tag, newState)
        if (!written) return null

        return AttackResult(
            hit = true,
            damageDealt = damage,
            attackRoll = 0,
            enemyState = newState,
            message = "Tirada: ${request.diceCount}d${request.dieFaces}+${request.bonus} = $finalValue"
        )
    }

    fun readTag(tag: Tag): BattleState? {
        try {
            val ndef = Ndef.get(tag)
            ndef?.connect()
            val msg = ndef?.ndefMessage
            ndef?.close()
            if (msg != null && msg.records.isNotEmpty()) {
                val json = String(msg.records[0].payload, Charset.forName("UTF-8"))
                return gson.fromJson(json, BattleState::class.java)
            }
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    fun writeTag(tag: Tag, state: BattleState): Boolean {
        try {
            val json = gson.toJson(state)
            val record = NdefRecord.createMime("application/dnd", json.toByteArray(Charset.forName("UTF-8")))
            val msg = NdefMessage(arrayOf(record))

            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                ndef.writeNdefMessage(msg)
                ndef.close()
                return true
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(msg)
                    formatable.close()
                    return true
                }
            }
        } catch (e: Exception) { return false }
        return false
    }
}