package com.example.dnd_nfc.data.model

import java.util.UUID

data class BattleState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Enemigo",
    val hp: Int = 0,
    val maxHp: Int = 0,
    val ac: Int = 10,
    val initiative: Int = 0
)