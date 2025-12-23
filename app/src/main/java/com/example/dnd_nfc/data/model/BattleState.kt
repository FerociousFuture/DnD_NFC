package com.example.dnd_nfc.data.model

data class BattleState(
    val id: String,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val ac: Int,
    val status: String = "Normal",

    // Iniciativa
    var initiativeBonus: Int = 0,
    var initiativeTotal: Int = 0,

    // Control UI
    var isSelected: Boolean = false,

    // --- ESTADÍSTICAS GUARDADAS (Para no escanear cada vez) ---
    val str: Int = 10,
    val dex: Int = 10,
    val con: Int = 10,
    val int: Int = 10,
    val wis: Int = 10,
    val cha: Int = 10
)