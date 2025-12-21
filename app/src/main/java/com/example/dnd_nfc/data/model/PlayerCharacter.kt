package com.example.dnd_nfc.data.model

data class PlayerCharacter(
    val id: String = "",
    val userId: String = "",

    // --- CABECERA ---
    val name: String = "",
    val charClass: String = "",
    val subclass: String = "",
    val level: Int = 1,
    val race: String = "",
    val background: String = "",
    val size: String = "Mediano",
    val xp: String = "",
    val alignment: String = "",

    // --- ESTADÍSTICAS ---
    val str: Int = 10, val dex: Int = 10, val con: Int = 10,
    val int: Int = 10, val wis: Int = 10, val cha: Int = 10,

    // --- COMBATE Y ESTADO ---
    val ac: Int = 10,
    val initiative: Int = 0,
    val speed: Int = 30,
    val hpMax: Int = 10,
    val hpCurrent: Int = 10,
    val tempHp: Int = 0,
    val hitDiceTotal: String = "1d8",
    val hitDiceUsed: Int = 0,
    val deathSaveSuccess: Int = 0,
    val deathSaveFail: Int = 0,
    val inspiration: Boolean = false,

    // --- HABILIDADES ---
    val proficiencyBonus: Int = 2,
    val passivePerception: Int = 10,
    val skillProficiencies: Map<String, Boolean> = emptyMap(),
    val savingThrowProficiencies: Map<String, Boolean> = emptyMap(),

    // --- RASGOS ---
    val classFeatures: String = "",
    val speciesTraits: String = "",
    val feats: String = "",
    val otherProficiencies: String = "",

    // --- NUEVO SISTEMA DE COMBATE ---
    val attacks: List<Attack> = emptyList(), // Lista estructurada de armas/ataques

    // --- NUEVO SISTEMA DE EQUIPO ---
    val inventoryItems: List<InventoryItem> = emptyList(), // Lista de objetos
    val attunedItems: List<String> = emptyList(), // Objetos sintonizados (Max 3 típicamente)

    // Monedas (Se mantienen igual)
    val cp: Int = 0, val sp: Int = 0, val ep: Int = 0, val gp: Int = 0, val pp: Int = 0,

    // --- MAGIA ---
    val spellAbility: String = "INT",
    val spellAbilityMod: Int = 0,
    val spellSaveDC: Int = 10,
    val spellAttackBonus: Int = 2,
    val spellSlots: Map<String, Int> = emptyMap(),
    val spells: List<Spell> = emptyList(),

    // --- BIO ---
    val personality: String = "",
    val ideals: String = "",
    val bonds: String = "",
    val flaws: String = "",
    val backstory: String = ""
)

/**
 * Modelo para Ataques Físicos o Conjuros de Ataque
 */
data class Attack(
    val name: String = "",
    val bonus: String = "", // Ej: "+5" o "CD 14"
    val damage: String = "", // Ej: "1d8+3"
    val damageType: String = "", // Ej: "Cortante"
    val notes: String = ""
)

/**
 * Modelo para Objetos del Inventario
 */
data class InventoryItem(
    val name: String = "",
    val quantity: Int = 1,
    val notes: String = ""
)

data class Spell(
    val name: String = "",
    val level: Int = 0,
    val castingTime: String = "",
    val duration: String = "",
    val range: String = "",
    val components: String = "",
    val material: String = "",
    val concentration: Boolean = false,
    val ritual: Boolean = false,
    val notes: String = ""
)