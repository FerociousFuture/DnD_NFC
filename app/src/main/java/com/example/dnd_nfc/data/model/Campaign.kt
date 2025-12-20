package com.example.dnd_nfc.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Modelo de la campaña.
 * @param id ID único de Firebase.
 * @param name Nombre de la partida (ej: "La Mina Perdida").
 * @param joinCode Código de 6 dígitos para que los jugadores se unan.
 * @param dmId ID del usuario que creó la partida (el Dungeon Master).
 * @param logs Lista de eventos (ej: "Grog entró a la partida").
 */
data class Campaign(
    val id: String = "",
    val name: String = "",
    val joinCode: String = "",
    val dmId: String = "",
    val logs: List<GameLog> = emptyList()
)

/**
 * Un evento individual en la bitácora.
 */
data class GameLog(
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)