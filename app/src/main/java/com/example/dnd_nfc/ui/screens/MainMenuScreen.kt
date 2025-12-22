package com.example.dnd_nfc.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.local.DataCompressor
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.google.gson.Gson // <--- IMPORTANTE
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun MainMenuScreen(
    onNavigateToCharacters: () -> Unit,
    onNavigateToCampaigns: () -> Unit,
    onNavigateToCombat: () -> Unit,
    onCharacterImported: (PlayerCharacter) -> Unit
) {
    val context = LocalContext.current

    // Lanzador de la cámara para escanear QR
    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val qrContent = result.contents
            try {
                // 1. Descomprimimos el texto
                val json = DataCompressor.decompress(qrContent)

                // 2. Convertimos el JSON a Objeto PlayerCharacter (ESTO FALTABA)
                if (json.isNotEmpty()) {
                    val character = Gson().fromJson(json, PlayerCharacter::class.java)
                    Toast.makeText(context, "¡Personaje detectado: ${character.name}!", Toast.LENGTH_SHORT).show()
                    onCharacterImported(character)
                } else {
                    Toast.makeText(context, "Error: Datos QR vacíos o corruptos.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al leer QR: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("D&D NFC", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("Gestor Offline", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(48.dp))

        MenuCard("Mis Personajes", "Fichas e Inventario", Icons.Default.Description, onNavigateToCharacters)
        Spacer(modifier = Modifier.height(16.dp))
        MenuCard("Mis Campañas", "Bitácora y Notas", Icons.Default.Groups, onNavigateToCampaigns)

        Spacer(modifier = Modifier.height(32.dp))

        // BOTÓN ESCANEAR QR
        Button(
            onClick = {
                val options = ScanOptions()
                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                options.setPrompt("Escanea el QR del personaje")
                options.setBeepEnabled(true)
                options.setOrientationLocked(false)
                qrLauncher.launch(options)
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Default.QrCodeScanner, null)
            Spacer(Modifier.width(8.dp))
            Text("Escanear Respaldo QR")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Combate
        Button(
            onClick = onNavigateToCombat,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Casino, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.width(16.dp))
                Text("Mesa de Combate", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

@Composable
fun MenuCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(90.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}