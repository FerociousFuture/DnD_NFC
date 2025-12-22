package com.example.dnd_nfc.data.model

import java.util.UUID

data class BattleState(
    val id: String = UUID.randomUUID().toString(), // Identificador único
    val name: String = "Enemigo",
    val hp: Int = 0,        // Vida Actual
    val maxHp: Int = 0,     // Vida Máxima
    val ac: Int = 10,       // Armadura (Para calcular si aciertas)
    val initiative: Int = 0 // Iniciativa
)