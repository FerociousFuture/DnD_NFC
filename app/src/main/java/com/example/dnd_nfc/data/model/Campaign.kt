package com.example.dnd_nfc.data.model

import java.util.UUID

/**
 * Modelo de la campaña (Versión Local).
 * @param id ID único generado localmente.
 * @param name Nombre de la partida.
 * @param description Descripción o notas del DM.
 * @param logs Lista de eventos (Bitácora).
 * @param players Lista de personajes asociados (copias locales importadas por NFC).
 */
data class Campaign(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val logs: List<GameLog> = emptyList(),
    val players: List<PlayerCharacter> = emptyList()
)

/**
 * Un evento individual en la bitácora.
 */
data class GameLog(
    val id: String = UUID.randomUUID().toString(),
    val message: String = "",
    val type: LogType = LogType.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LogType {
    INFO, COMBAT, LOOT, STORY
}