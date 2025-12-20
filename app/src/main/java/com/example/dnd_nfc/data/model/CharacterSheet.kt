package com.example.dnd_nfc.data.model

/**
 * Representa los datos básicos de la hoja de personaje de D&D de forma compacta para ahorrar espacio en NFC.
 * * @param n Nombre del personaje.
 * @param c Clase del personaje.
 * @param r Raza del personaje.
 * @param s Estadísticas principales separadas por guiones.
 * El orden estándar procesado es: FUE-DES-CON-INT-SAB-CAR.
 */
data class CharacterSheet(
    val n: String,
    val c: String,
    val r: String,
    val s: String
)