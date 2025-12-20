package com.example.dnd_nfc.data.model

data class CharacterSheet(
    val n: String, // n = Nombre
    val c: String, // c = Clase
    val r: String, // r = Raza
    val s: String  // s = Stats (ej: "18,14,12,10,8,15")
)