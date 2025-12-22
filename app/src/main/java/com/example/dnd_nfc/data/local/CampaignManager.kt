package com.example.dnd_nfc.data.local

import android.content.Context
import com.example.dnd_nfc.data.model.Campaign
import com.google.gson.Gson
import java.io.File

object CampaignManager {
    private const val FOLDER_NAME = "campaigns"
    private val gson = Gson()

    fun saveCampaign(context: Context, campaign: Campaign): Boolean {
        return try {
            val folder = File(context.filesDir, FOLDER_NAME)
            if (!folder.exists()) folder.mkdirs()

            val file = File(folder, "${campaign.id}.json")
            file.writeText(gson.toJson(campaign))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getCampaigns(context: Context): List<Campaign> {
        val list = mutableListOf<Campaign>()
        val folder = File(context.filesDir, FOLDER_NAME)
        if (folder.exists()) {
            folder.listFiles()?.forEach { file ->
                try {
                    val campaign = gson.fromJson(file.readText(), Campaign::class.java)
                    list.add(campaign)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return list
    }

    fun getCampaignById(context: Context, id: String): Campaign? {
        val file = File(File(context.filesDir, FOLDER_NAME), "$id.json")
        return if (file.exists()) gson.fromJson(file.readText(), Campaign::class.java) else null
    }

    fun deleteCampaign(context: Context, id: String) {
        val file = File(File(context.filesDir, FOLDER_NAME), "$id.json")
        if (file.exists()) file.delete()
    }
}