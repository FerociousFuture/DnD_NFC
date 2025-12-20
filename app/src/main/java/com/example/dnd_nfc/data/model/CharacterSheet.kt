package com.example.dnd_nfc.data.model

/**
 * Representa los datos básicos de la hoja de personaje de D&D.
 */
data class CharacterSheet(
    val name: String = "Aventurero Desconocido",
    val level: Int = 1,
    val hpCurrent: Int = 10,
    val hpMax: Int = 10,
    val armorClass: Int = 10,
    val charClass: String = "Guerrero"
)