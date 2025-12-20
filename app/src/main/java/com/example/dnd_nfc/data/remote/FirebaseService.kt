package com.example.dnd_nfc.data.remote

import com.example.dnd_nfc.data.model.Campaign
import com.example.dnd_nfc.data.model.GameLog
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object FirebaseService {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Referencia a la colección
    private val campaignsRef = db.collection("campaigns")

    /**
     * Crea una nueva campaña y devuelve el código de unión.
     */
    suspend fun createCampaign(campaignName: String): String? {
        val user = auth.currentUser ?: return null

        // Generamos un código de 6 dígitos aleatorio
        val code = (100000..999999).random().toString()
        val id = campaignsRef.document().id

        val newCampaign = Campaign(
            id = id,
            name = campaignName,
            joinCode = code,
            dmId = user.uid,
            logs = listOf(GameLog("Campaña iniciada: $campaignName"))
        )

        campaignsRef.document(id).set(newCampaign).await()
        return code
    }

    /**
     * Busca una campaña por su código (para unirse).
     */
    suspend fun getCampaignByCode(code: String): Campaign? {
        val snapshot = campaignsRef
            .whereEqualTo("joinCode", code)
            .limit(1) // Importante para ahorrar: solo traemos 1
            .get()
            .await()

        return if (!snapshot.isEmpty) {
            snapshot.documents[0].toObject(Campaign::class.java)
        } else {
            null
        }
    }

    /**
     * Agrega un log a la campaña (Ej: cuando escaneas un NFC).
     * Usamos 'arrayUnion' para añadirlo a la lista existente sin borrar lo anterior.
     */
    suspend fun addLogToCampaign(campaignId: String, message: String) {
        val newLog = GameLog(message = message)

        campaignsRef.document(campaignId)
            .update("logs", FieldValue.arrayUnion(newLog))
            .await()
    }

    /**
     * Login anónimo rápido para que el usuario no pierda tiempo registrándose
     */
    suspend fun signInAnonymously() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }
}