package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.local.CharacterManager // <--- Importante
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.ScanEvent

@Composable
fun NfcReadScreen(
    scanEvent: ScanEvent?,
    onFullCharacterLoaded: (PlayerCharacter) -> Unit,
    onScanAgainClick: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Extraemos los datos básicos de la tarjeta
    val nfcCharacter = scanEvent?.character

    // Cuando se detecta una tarjeta, buscamos la ficha completa en el móvil
    LaunchedEffect(scanEvent) {
        if (nfcCharacter != null && nfcCharacter.id.isNotEmpty()) {
            isLoading = true
            errorMsg = null

            // BUSQUEDA LOCAL
            val fullChar = CharacterManager.getCharacterById(context, nfcCharacter.id)

            if (fullChar != null) {
                // ¡Encontrado! Navegamos a la ficha
                onFullCharacterLoaded(fullChar)
            } else {
                // El ID está en la tarjeta, pero no tenemos el archivo en este móvil
                errorMsg = "Personaje no encontrado en este dispositivo.\nID: ${nfcCharacter.id}"
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (nfcCharacter == null) {
            // ESTADO 1: ESPERANDO
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_search),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Acerca una tarjeta...", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            // ESTADO 2: LECTURA EXITOSA (O ERROR)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("¡Etiqueta Detectada!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(nfcCharacter.n, style = MaterialTheme.typography.headlineMedium)
                        Text("ID: ${nfcCharacter.id}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Stats: ${nfcCharacter.s}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onScanAgainClick) { Text("Escanear otra vez") }
                }
            }
        }
    }
}