package com.example.dnd_nfc.data.model

/**
 * Clase contenedora para eventos de escaneo NFC.
 * Ahora incluye el personaje completo leído directamente del NFC.
 */
data class ScanEvent(
    val character: CharacterSheet, // Datos ligeros
    val fullCharacter: PlayerCharacter? = null, // Datos completos leídos del tag
    val timestamp: Long = System.currentTimeMillis()
)