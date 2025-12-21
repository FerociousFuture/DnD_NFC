package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.model.ScanEvent // <--- AHORA SÍ LO IMPORTAMOS
import com.example.dnd_nfc.data.model.PlayerCharacter

@Composable
fun NfcReadScreen(
    scanEvent: ScanEvent?, // <--- CAMBIO CLAVE: Recibimos el evento, no el character directo
    onFullCharacterLoaded: (PlayerCharacter) -> Unit,
    onScanAgainClick: () -> Unit
) {
    var isLoadingCloud by remember { mutableStateOf(false) }
    var cloudError by remember { mutableStateOf<String?>(null) }

    // Extraemos el personaje del evento
    val nfcCharacter = scanEvent?.character

    // EFECTO: Se dispara cada vez que scanEvent cambia (incluso si es la misma tarjeta)
    LaunchedEffect(scanEvent) {
        if (nfcCharacter != null && nfcCharacter.id.isNotEmpty()) {
            isLoadingCloud = true
            cloudError = null

            val fullChar = FirebaseService.getCharacterById(nfcCharacter.id)

            if (fullChar != null) {
                onFullCharacterLoaded(fullChar)
            } else {
                cloudError = "Error: Personaje no encontrado en la nube."
            }
            isLoadingCloud = false
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
                Text("Acerca una miniatura...", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            // ESTADO 2: LECTURA EXITOSA
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
                        Text("Stats (Preview): ${nfcCharacter.s}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoadingCloud) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Sincronizando ficha completa...")
                    }
                } else if (cloudError != null) {
                    Text(cloudError!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onScanAgainClick) { Text("Intentar otra etiqueta") }
                }
            }
        }
    }
}