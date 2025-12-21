package com.example.dnd_nfc.data.model

/**
 * Clase contenedora para eventos de escaneo NFC.
 * El timestamp asegura que cada escaneo sea único, incluso si es la misma tarjeta.
 */
data class ScanEvent(
    val character: CharacterSheet,
    val timestamp: Long = System.currentTimeMillis()
)