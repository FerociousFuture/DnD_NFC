package com.example.dnd_nfc.network

import com.example.dnd_nfc.data.model.BattleState
import com.google.gson.Gson
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import kotlin.concurrent.thread

object GameServer {
    private const val PORT = 4444
    private var serverSocket: ServerSocket? = null
    private val clients = Collections.synchronizedList(mutableListOf<Socket>())
    private val gson = Gson()

    // ESTADO GLOBAL DE LA BATALLA (Lista de monstruos/jugadores)
    // El servidor es la "Fuente de la Verdad"
    private var gameState = mutableListOf<BattleState>()

    fun start() {
        if (serverSocket != null && !serverSocket!!.isClosed) return

        thread {
            try {
                serverSocket = ServerSocket(PORT)
                println("Servidor iniciado en puerto $PORT")

                while (true) {
                    val client = serverSocket!!.accept()
                    clients.add(client)
                    // Al conectarse alguien, le enviamos el estado actual inmediatamente
                    sendStateToClient(client)

                    // Escuchar a este cliente en un hilo aparte
                    thread { handleClient(client) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = socket.getInputStream().bufferedReader()
            while (true) {
                val json = input.readLine() ?: break

                // Recibimos una actualización de un personaje (BattleState)
                val updatedChar = gson.fromJson(json, BattleState::class.java)
                updateGameState(updatedChar)

                // Reenviamos el estado NUEVO a TODOS (Broadcast)
                broadcastState()
            }
        } catch (e: Exception) {
            clients.remove(socket)
        }
    }

    // Actualiza la lista maestra. Si el ID ya existe, lo reemplaza. Si no, lo añade.
    private fun updateGameState(char: BattleState) {
        val index = gameState.indexOfFirst { it.id == char.id }
        if (index != -1) {
            gameState[index] = char
        } else {
            gameState.add(char)
        }
    }

    private fun broadcastState() {
        val json = gson.toJson(gameState)
        // Iteramos sobre una copia para evitar errores de concurrencia
        val clientsCopy = ArrayList(clients)
        clientsCopy.forEach { client ->
            try {
                val out = PrintWriter(client.getOutputStream(), true)
                out.println(json)
            } catch (e: Exception) { }
        }
    }

    private fun sendStateToClient(client: Socket) {
        try {
            val out = PrintWriter(client.getOutputStream(), true)
            out.println(gson.toJson(gameState))
        } catch (e: Exception) {}
    }

    fun stop() {
        serverSocket?.close()
        clients.forEach { it.close() }
        clients.clear()
        serverSocket = null
    }

    // UTILIDAD: Obtener la IP local para el QR
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress ?: ""
                    }
                }
            }
        } catch (e: Exception) { }
        return "127.0.0.1"
    }
}