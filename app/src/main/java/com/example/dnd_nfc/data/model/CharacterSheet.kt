package com.example.dnd_nfc.data.model

// Modelo ligero para el NFC (Solo llave y referencia visual rápida)
data class CharacterSheet(
    val id: String = "", // ID para buscar en Firebase
    val n: String = "",  // Nombre (para confirmar que es la tarjeta correcta)
    val s: String = ""   // Stats (ej: "10-12-14-10-8-15") para preview rápido
)