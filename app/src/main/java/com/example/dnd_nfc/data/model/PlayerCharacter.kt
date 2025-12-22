package com.example.dnd_nfc.data.model

data class PlayerCharacter(
    val id: String = "",

    // --- DATOS DE IDENTIFICACIÓN ---
    val name: String = "",
    val charClass: String = "",
    val race: String = "", // Especie

    // --- ESTADÍSTICAS DE COMBATE ---
    val spellSaveDC: Int = 10, // CD (Clase de Dificultad)
    val hpMax: Int = 10,
    val hpCurrent: Int = 10,

    // --- ESTADO DE LA MINIATURA ---
    val status: String = "Normal", // Ej: "Envenenado", "Concentrando", "Derribado"

    // Mantenemos estos por compatibilidad técnica con GSON/Compresión,
    // pero no los usaremos en la UI principal.
    val userId: String = "",
    val subclass: String = "",
    val level: Int = 1,
    val background: String = "",
    val size: String = "Mediano",
    val xp: String = "",
    val alignment: String = "",
    val str: Int = 10, val dex: Int = 10, val con: Int = 10,
    val int: Int = 10, val wis: Int = 10, val cha: Int = 10,
    val ac: Int = 10,
    val initiative: Int = 0,
    val speed: Int = 30,
    val tempHp: Int = 0,
    val hitDiceTotal: String = "",
    val hitDiceUsed: Int = 0,
    val deathSaveSuccess: Int = 0,
    val deathSaveFail: Int = 0,
    val inspiration: Boolean = false,
    val proficiencyBonus: Int = 2,
    val passivePerception: Int = 10,
    val skillProficiencies: Map<String, Boolean> = emptyMap(),
    val savingThrowProficiencies: Map<String, Boolean> = emptyMap(),
    val classFeatures: String = "",
    val speciesTraits: String = "",
    val feats: String = "",
    val otherProficiencies: String = "",
    val attacks: List<Attack> = emptyList(),
    val inventoryItems: List<InventoryItem> = emptyList(),
    val attunedItems: List<String> = emptyList(),
    val cp: Int = 0, val sp: Int = 0, val ep: Int = 0, val gp: Int = 0, val pp: Int = 0,
    val spellAbility: String = "",
    val spellAbilityMod: Int = 0,
    val spellAttackBonus: Int = 0,
    val spellSlots: Map<String, Int> = emptyMap(),
    val spells: List<Spell> = emptyList(),
    val personality: String = "",
    val ideals: String = "",
    val bonds: String = "",
    val flaws: String = "",
    val backstory: String = ""
)

// Clases auxiliares vacías o mínimas para no romper referencias antiguas si las hubiera
data class Attack(val name: String = "", val damage: String = "", val notes: String = "")
data class InventoryItem(val name: String = "", val quantity: Int = 1)
data class Spell(val name: String = "", val level: Int = 0)