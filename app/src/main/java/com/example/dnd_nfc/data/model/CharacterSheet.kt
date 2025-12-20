package com.example.dnd_nfc.data.model

/**
 * Representa los datos básicos de la hoja de personaje de D&D de forma compacta.
 * n = Nombre, c = Clase, r = Raza, s = Estadísticas (ej: "18,14,12,10,8,15")
 */
data class CharacterSheet(
    val n: String,
    val c: String,
    val r: String,
    val s: String
)