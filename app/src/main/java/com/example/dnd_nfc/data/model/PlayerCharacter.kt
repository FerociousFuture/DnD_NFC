package com.example.dnd_nfc.data.model

data class PlayerCharacter(
    val id: String = "",
    val userId: String = "",

    // --- CABECERA (D&D 2024) ---
    val name: String = "",
    val charClass: String = "",
    val subclass: String = "",
    val level: Int = 1,
    val race: String = "", // "Especie" en 2024
    val background: String = "",
    val size: String = "Mediano",
    val xp: String = "",
    val alignment: String = "",

    // --- ESTADÍSTICAS ---
    val str: Int = 10, val dex: Int = 10, val con: Int = 10,
    val int: Int = 10, val wis: Int = 10, val cha: Int = 10,

    // --- COMBATE ---
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
    val inspiration: Boolean = false, // Inspiración Heroica

    // --- HABILIDADES Y COMPETENCIAS ---
    val proficiencyBonus: Int = 2,
    val passivePerception: Int = 10,
    val skillProficiencies: String = "", // Guardado como texto simple por ahora
    val savingThrowProficiencies: String = "",

    // --- RASGOS ---
    val classFeatures: String = "",
    val speciesTraits: String = "",
    val feats: String = "",
    val otherProficiencies: String = "", // Herramientas e Idiomas

    // --- EQUIPO Y MONEDAS ---
    val inventory: String = "",
    val cp: Int = 0, val sp: Int = 0, val ep: Int = 0, val gp: Int = 0, val pp: Int = 0,

    // --- MAGIA ---
    val spellAbility: String = "INT",
    val spellSaveDC: Int = 10,
    val spellAttackBonus: Int = 2,
    val spellSlots: Map<String, Int> = emptyMap(), // Ej: "1"->4, "1_used"->2
    val spells: List<Spell> = emptyList(),

    // --- BIO ---
    val personality: String = "",
    val ideals: String = "",
    val bonds: String = "",
    val flaws: String = "",
    val backstory: String = ""
)

data class Spell(
    val name: String = "",
    val level: Int = 0,
    val time: String = "",
    val range: String = "",
    val description: String = ""
)