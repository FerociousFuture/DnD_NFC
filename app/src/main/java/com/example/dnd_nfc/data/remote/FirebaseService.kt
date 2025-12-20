package com.example.dnd_nfc.data.remote

import android.util.Log
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Singleton que maneja todas las operaciones con la base de datos Firestore.
 */
object FirebaseService {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // Referencia a la colección de personajes
    private val charactersRef = db.collection("characters")

    // Referencia a la colección de campañas (por si se usa a futuro)
    private val campaignsRef = db.collection("campaigns")

    // --- GESTIÓN DE PERSONAJES (Fichas Completas) ---

    /**
     * Guarda o actualiza un personaje en la nube.
     * Si el personaje no tiene ID, se crea uno nuevo.
     * Se asigna automáticamente al usuario logueado.
     */
    suspend fun saveCharacter(character: PlayerCharacter): Boolean {
        val user = auth.currentUser ?: return false

        try {
            // Si es nuevo (ID vacío), generamos un ID de documento
            val docId = if (character.id.isEmpty()) charactersRef.document().id else character.id

            // Aseguramos que el personaje tenga el ID del documento y el ID del usuario dueño
            val charToSave = character.copy(
                id = docId,
                userId = user.uid
            )

            // Guardamos en Firestore (set reemplaza o crea)
            charactersRef.document(docId).set(charToSave).await()
            return true
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error al guardar personaje: ${e.message}")
            return false
        }
    }

    /**
     * Obtiene la lista de todos los personajes creados por el usuario actual.
     */
    suspend fun getUserCharacters(): List<PlayerCharacter> {
        val user = auth.currentUser ?: return emptyList()

        return try {
            val snapshot = charactersRef
                .whereEqualTo("userId", user.uid)
                .get()
                .await()

            // Convertimos los documentos a objetos PlayerCharacter
            snapshot.toObjects(PlayerCharacter::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error al obtener personajes: ${e.message}")
            emptyList()
        }
    }

    /**
     * Obtiene un personaje específico por su ID.
     */
    suspend fun getCharacterById(charId: String): PlayerCharacter? {
        return try {
            val doc = charactersRef.document(charId).get().await()
            doc.toObject(PlayerCharacter::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error al obtener personaje $charId: ${e.message}")
            null
        }
    }

    /**
     * Elimina un personaje permanentemente.
     */
    suspend fun deleteCharacter(charId: String): Boolean {
        return try {
            charactersRef.document(charId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error al borrar personaje: ${e.message}")
            false
        }
    }
}