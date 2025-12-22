package com.example.dnd_nfc.data.model

data class BattleState(
    val id: String,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val ac: Int,
    val initiativeMod: Int = 0, // NUEVO: Para calcular iniciativa automática
    val currentInitiative: Int? = null // Opcional: para guardar el resultado actual
)