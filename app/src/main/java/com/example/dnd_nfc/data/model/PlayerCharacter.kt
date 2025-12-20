package com.example.dnd_nfc.data.model

/**
 * Modelo completo de la hoja de personaje (D&D 5e / 2024).
 * Contiene todos los campos del PDF para guardar en Firebase.
 */
data class PlayerCharacter(
    val id: String = "",
    val userId: String = "",

    // --- CABECERA ---
    val name: String = "",
    val charClass: String = "",
    val subclass: String = "",
    val level: Int = 1,
    val race: String = "", // Especie
    val background: String = "",
    val xp: String = "",

    // --- ESTADÍSTICAS (Base) ---
    val str: Int = 10,
    val dex: Int = 10,
    val con: Int = 10,
    val int: Int = 10,
    val wis: Int = 10,
    val cha: Int = 10,

    // --- COMBATE ---
    val ac: Int = 10,
    val initiative: Int = 0,
    val speed: Int = 30,
    val hpMax: Int = 10,
    val hpCurrent: Int = 10,
    val tempHp: Int = 0,
    val hitDice: String = "1d8",
    val deathSavesSuccess: Int = 0,
    val deathSavesFail: Int = 0,

    // --- COMPETENCIAS Y HABILIDADES ---
    val proficiencyBonus: Int = 2,
    val passivePerception: Int = 10,
    // Guardamos las skills en un mapa: "Acrobacias" -> true (es competente)
    val skillProficiencies: Map<String, Boolean> = emptyMap(),
    val savingThrowProficiencies: Map<String, Boolean> = emptyMap(),

    // --- OTROS ---
    val attacksAndSpells: String = "", // Texto libre para armas/ataques
    val featuresAndTraits: String = "", // Rasgos de clase/raza
    val inventory: String = "", // Equipo y monedas
    val proficienciesAndLanguages: String = "", // Idiomas y herramientas

    // --- TRASFONDO ---
    val personality: String = "",
    val ideals: String = "",
    val bonds: String = "",
    val flaws: String = "",
    val backstory: String = ""
)