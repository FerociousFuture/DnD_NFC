package com.example.dnd_nfc.data.model

data class PlayerCharacter(
    val id: String = "",

    // --- IDENTIDAD ---
    val name: String = "",
    val shortDescription: String = "", // Reemplaza a Raza/Clase en la UI

    // Mantenemos estos dos vacíos por compatibilidad con lecturas antiguas si es necesario,
    // pero ya no se usan en la edición.
    val charClass: String = "",
    val race: String = "",

    // --- ESTADÍSTICAS (STR, DEX, CON, INT, WIS, CHA) ---
    val str: Int = 10,
    val dex: Int = 10,
    val con: Int = 10,
    val int: Int = 10,
    val wis: Int = 10,
    val cha: Int = 10,

    // --- COMBATE ---
    val ac: Int = 10,          // Clase de Armadura
    val hpMax: Int = 10,       // Vida Total
    val hpCurrent: Int = 10,   // Vida Actual
    val status: String = "Normal", // Estado (Veneno, Aturdido...)
    val spellSaveDC: Int = 10  // Dificultad de Conjuros (CD)
)