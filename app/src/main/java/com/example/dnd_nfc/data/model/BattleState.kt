package com.example.dnd_nfc.data.model

data class BattleState(
    val id: String,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val ac: Int,
    val status: String = "Normal", // Estado (Envenenado, etc)

    // Iniciativa
    var initiativeBonus: Int = 0, // Se edita manualmente antes de empezar
    var initiativeTotal: Int = 0, // Resultado del dado + bono

    // Control de UI
    var isSelected: Boolean = false // Para seleccionar a quién pegar/curar
)