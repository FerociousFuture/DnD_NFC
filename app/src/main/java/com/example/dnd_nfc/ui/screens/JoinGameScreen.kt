package com.example.dnd_nfc.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.network.GameClient
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun JoinGameScreen(onConnected: () -> Unit) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("No conectado") }

    // Configuramos el lanzador del Escáner de la librería ZXing
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val ip = result.contents // El QR contiene solo la IP
            statusText = "Conectando a $ip..."

            // Conectamos el cliente
            GameClient.connect(ip)

            // (Opcional) Esperamos un poco o asumimos éxito si no hay crash
            Toast.makeText(context, "¡Conectado!", Toast.LENGTH_SHORT).show()
            onConnected()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("UNIRSE A PARTIDA", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                val options = ScanOptions()
                options.setPrompt("Escanea el QR del DM")
                options.setBeepEnabled(true)
                options.setOrientationLocked(true)
                scanLauncher.launch(options)
            },
            modifier = Modifier.height(50.dp)
        ) {
            Text("ESCANEAR CÓDIGO QR")
        }

        Spacer(Modifier.height(16.dp))
        Text(statusText, color = MaterialTheme.colorScheme.secondary)
    }
}