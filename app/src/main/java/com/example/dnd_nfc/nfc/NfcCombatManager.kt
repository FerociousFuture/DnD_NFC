package com.example.dnd_nfc.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.example.dnd_nfc.data.model.BattleState
import com.example.dnd_nfc.network.GameClient
import com.google.gson.Gson
import java.nio.charset.Charset
import kotlin.random.Random

object NfcCombatManager {
    private val gson = Gson()

    data class AttackRequest(
        val attackBonus: Int,   // Tu bono de ataque (+5)
        val damageDice: String, // Dados de daño ("1d8")
        val damageBonus: Int,   // Bono de daño (+3)
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
        // 1. LEER EL ESTADO ACTUAL DE LA FIGURA
        val currentState = readTag(tag) ?: return null

        // 2. TIRADA DE ATAQUE (Con Ventaja si aplica)
        val roll1 = Random.nextInt(1, 21)
        val roll2 = Random.nextInt(1, 21)
        val rawRoll = if (request.hasAdvantage) maxOf(roll1, roll2) else roll1
        val totalAttack = rawRoll + request.attackBonus

        // 3. COMPARAR CON LA ARMADURA (AC) QUE ESTABA EN EL NFC
        val isHit = totalAttack >= currentState.ac || rawRoll == 20
        var damage = 0

        var newState = currentState

        if (isHit) {
            // 4. CALCULAR DAÑO
            damage = rollDamage(request.damageDice) + request.damageBonus
            // Critico duplica dados (simplificado)
            if (rawRoll == 20) damage += rollDamage(request.damageDice)

            var newHp = currentState.hp - damage
            if (newHp < 0) newHp = 0

            newState = currentState.copy(hp = newHp)

            // 5. ESCRIBIR EL NUEVO HP EN LA FIGURA (TIEMPO REAL FÍSICO)
            val written = writeTag(tag, newState)
            if (!written) return null

            // 6. ENVIAR A LA RED (TIEMPO REAL DIGITAL)
            // Esto actualiza las pantallas de todos los demás en la sala
            GameClient.sendUpdate(newState)
        }

        return AttackResult(
            hit = isHit,
            damageDealt = damage,
            attackRoll = totalAttack,
            enemyState = newState,
            message = if (isHit) "¡Impacto! ($totalAttack vs AC ${currentState.ac})" else "Fallo... ($totalAttack vs AC ${currentState.ac})"
        )
    }

    // --- UTILS DE DADOS Y NFC ---

    private fun rollDamage(dice: String): Int {
        // Parsea "2d6"
        return try {
            val parts = dice.lowercase().split("d")
            val count = parts[0].toInt()
            val faces = parts[1].toInt()
            (1..count).sumOf { Random.nextInt(1, faces + 1) }
        } catch (e: Exception) { 0 }
    }

    private fun readTag(tag: Tag): BattleState? {
        try {
            val ndef = Ndef.get(tag)
            ndef?.connect()
            val msg = ndef?.ndefMessage
            ndef?.close()
            if (msg != null) {
                val json = String(msg.records[0].payload, Charset.forName("UTF-8"))
                return gson.fromJson(json, BattleState::class.java)
            }
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    private fun writeTag(tag: Tag, state: BattleState): Boolean {
        try {
            val json = gson.toJson(state)
            val record = NdefRecord.createMime("application/dnd", json.toByteArray()) // MimeType personalizado
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