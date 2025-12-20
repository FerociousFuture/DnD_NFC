package com.example.dnd_nfc.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

// Modelo para la Campaña
@IgnoreExtraProperties
data class Campaign(
    val id: String = "",
    val name: String = "",
    val adminId: String = "", // El DM que creó la campaña
    val joinCode: String = "", // Código de 6 dígitos para unirse
    val logs: List<GameLog> = emptyList(),
    val notes: String = "" // Notas generales de la campaña
)

// Modelo para los Logs de personajes
data class GameLog(
    val characterName: String = "",
    val message: String = "", // Ej: "Recibió 10 de daño", "Subió a nivel 5"
    val timestamp: Long = System.currentTimeMillis()
)