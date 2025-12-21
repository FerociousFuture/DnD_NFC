package com.example.dnd_nfc.data.local

import android.content.Context
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

object CharacterManager {
    private const val FOLDER_NAME = "characters"
    private val gson = Gson()

    /**
     * Guarda un personaje en un archivo JSON local.
     */
    fun saveCharacter(context: Context, character: PlayerCharacter): Boolean {
        return try {
            // Si es nuevo, le damos un ID único
            val charToSave = if (character.id.isEmpty()) {
                character.copy(id = UUID.randomUUID().toString())
            } else {
                character
            }

            val folder = File(context.filesDir, FOLDER_NAME)
            if (!folder.exists()) folder.mkdirs()

            val file = File(folder, "${charToSave.id}.json")
            file.writeText(gson.toJson(charToSave))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Carga todos los personajes guardados.
     */
    fun getCharacters(context: Context): List<PlayerCharacter> {
        val list = mutableListOf<PlayerCharacter>()
        val folder = File(context.filesDir, FOLDER_NAME)

        if (folder.exists()) {
            folder.listFiles()?.forEach { file ->
                try {
                    val json = file.readText()
                    val character = gson.fromJson(json, PlayerCharacter::class.java)
                    list.add(character)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return list
    }

    /**
     * Obtiene un solo personaje por ID.
     */
    fun getCharacterById(context: Context, id: String): PlayerCharacter? {
        val file = File(File(context.filesDir, FOLDER_NAME), "$id.json")
        return if (file.exists()) {
            gson.fromJson(file.readText(), PlayerCharacter::class.java)
        } else {
            null
        }
    }

    /**
     * Borra un personaje.
     */
    fun deleteCharacter(context: Context, id: String) {
        val file = File(File(context.filesDir, FOLDER_NAME), "$id.json")
        if (file.exists()) file.delete()
    }
}