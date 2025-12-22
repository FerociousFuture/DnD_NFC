package com.example.dnd_nfc.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.network.GameServer
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun HostGameScreen(onBack: () -> Unit) {
    var ipAddress by remember { mutableStateOf("Cargando IP...") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        // 1. Arrancar Servidor
        GameServer.start()

        // 2. Obtener IP y Generar QR
        val ip = GameServer.getLocalIpAddress()
        ipAddress = ip
        qrBitmap = generateQrCode(ip)
    }

    // Al salir de la pantalla, detenemos el servidor (opcional, o dejarlo en background)
    DisposableEffect(Unit) {
        onDispose {
            // GameServer.stop() // Descomentar si quieres que la sala muera al salir
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SALA CREADA", style = MaterialTheme.typography.headlineLarge)
        Text("Dungeon Master", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(32.dp))

        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap!!.asImageBitmap(),
                contentDescription = "QR de Conexión",
                modifier = Modifier.size(250.dp)
            )
        } else {
            CircularProgressIndicator()
        }

        Spacer(Modifier.height(16.dp))

        Text("Pide a los jugadores que escaneen este código", style = MaterialTheme.typography.bodyMedium)
        Text("IP: $ipAddress", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)

        Spacer(Modifier.height(32.dp))

        Button(onClick = onBack) {
            Text("Ir al Tablero de Combate")
        }
    }
}

// Función auxiliar para crear el BitMap del QR
fun generateQrCode(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) { null }
}