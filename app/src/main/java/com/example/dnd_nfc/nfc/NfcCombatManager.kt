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
        val attackBonus: Int,
        val damageDice: String,
        val damageBonus: Int,
        val hasAdvantage: Boolean = false
    )

    data class AttackResult(
        val hit: Boolean,
        val damageDealt: Int,
        val attackRoll: Int,
        val enemyState: BattleState,
        val message: String
    )

    fun performAttack(tag: Tag, request: AttackRequest): AttackResult? {
        // 1. LEER
        val currentState = readTag(tag) ?: return null

        // 2. CALCULAR
        val roll1 = Random.nextInt(1, 21)
        val roll2 = Random.nextInt(1, 21)
        val rawRoll = if (request.hasAdvantage) maxOf(roll1, roll2) else roll1
        val totalAttack = rawRoll + request.attackBonus

        val isHit = totalAttack >= currentState.ac || rawRoll == 20
        var damage = 0
        var newState = currentState

        if (isHit) {
            damage = rollDamage(request.damageDice) + request.damageBonus
            if (rawRoll == 20) damage += rollDamage(request.damageDice)

            var newHp = currentState.hp - damage
            if (newHp < 0) newHp = 0

            newState = currentState.copy(hp = newHp)

            // 3. ESCRIBIR (Solo físico)
            val written = writeTag(tag, newState)
            if (!written) return null

            // ELIMINADO: GameClient.sendUpdate(newState) -> Ya no hay servidor
        }

        return AttackResult(
            hit = isHit,
            damageDealt = damage,
            attackRoll = totalAttack,
            enemyState = newState,
            message = if (isHit) "¡Impacto! ($totalAttack vs AC ${currentState.ac})" else "Fallo... ($totalAttack vs AC ${currentState.ac})"
        )
    }

    private fun rollDamage(dice: String): Int {
        return try {
            val parts = dice.lowercase().split("d")
            val count = parts[0].toInt()
            val faces = parts[1].toInt()
            (1..count).sumOf { Random.nextInt(1, faces + 1) }
        } catch (e: Exception) { 0 }
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