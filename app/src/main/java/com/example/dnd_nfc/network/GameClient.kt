package com.example.dnd_nfc.network

import android.util.Log
import com.example.dnd_nfc.data.model.BattleState
import com.google.gson.Gson
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

object GameClient {
    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private val gson = Gson()

    // Callback para cuando recibimos datos del servidor (Actualizar UI)
    var onGameStateReceived: ((List<BattleState>) -> Unit)? = null

    // Conectarse a la IP del DM (Host)
    fun connect(ip: String) {
        thread {
            try {
                socket = Socket(ip, 4444)
                out = PrintWriter(socket!!.getOutputStream(), true)

                // Escuchar actualizaciones del servidor
                val input = socket!!.getInputStream().bufferedReader()
                while (true) {
                    val json = input.readLine() ?: break
                    // Aquí recibimos la lista actualizada de todos los monstruos
                    // y notificamos a la UI para que se repinte
                    try {
                        val listType = object : com.google.gson.reflect.TypeToken<List<BattleState>>() {}.type
                        val list = gson.fromJson<List<BattleState>>(json, listType)
                        onGameStateReceived?.invoke(list)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) {
                Log.e("Network", "Error de conexión", e)
            }
        }
    }

    // Enviar una actualización (ej: "Le bajé 5 de vida a este ID")
    fun sendUpdate(updatedCharacter: BattleState) {
        thread {
            if (out != null) {
                val json = gson.toJson(updatedCharacter)
                out!!.println(json)
            }
        }
    }
}