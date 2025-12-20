package com.example.dnd_nfc.data.remote

import com.example.dnd_nfc.data.model.Campaign
import com.example.dnd_nfc.data.model.GameLog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private val campaignCollection = db.collection("campaigns")

    // Crear campaña con código único
    suspend fun createCampaign(name: String, adminId: String): String {
        val joinCode = (100000..999999).random().toString()
        val id = campaignCollection.document().id
        val newCampaign = Campaign(id, name, adminId, joinCode)

        campaignCollection.document(id).set(newCampaign).await()
        return joinCode
    }

    // Unirse a campaña mediante código
    suspend fun joinCampaignByCode(code: String): Campaign? {
        val query = campaignCollection.whereEqualTo("joinCode", code).get().await()
        return query.documents.firstOrNull()?.toObject(Campaign::class.java)
    }

    // Añadir un log de personaje (Ej: al leer el NFC)
    suspend fun addLog(campaignId: String, characterName: String, action: String) {
        val docRef = campaignCollection.document(campaignId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val campaign = snapshot.toObject(Campaign::class.java)
            val updatedLogs = campaign?.logs?.toMutableList() ?: mutableListOf()
            updatedLogs.add(GameLog(characterName, action))

            // Limitamos a los últimos 50 logs para no inflar el documento (Ahorro de espacio/dinero)
            val finalLogs = if (updatedLogs.size > 50) updatedLogs.takeLast(50) else updatedLogs

            transaction.update(docRef, "logs", finalLogs)
        }.await()
    }
}