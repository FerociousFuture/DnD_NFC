package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.local.CharacterManager
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.ScanEvent

@Composable
fun NfcReadScreen(
    scanEvent: ScanEvent?,
    onFullCharacterLoaded: (PlayerCharacter) -> Unit,
    onScanAgainClick: () -> Unit
) {
    val context = LocalContext.current
    // Si la lectura nos trajo el personaje completo, lo usamos.
    val nfcFullChar = scanEvent?.fullCharacter
    val nfcLightChar = scanEvent?.character

    // Estado para saber si ya existe en local
    var isLocal by remember { mutableStateOf(false) }

    LaunchedEffect(nfcFullChar) {
        if (nfcFullChar != null) {
            val local = CharacterManager.getCharacterById(context, nfcFullChar.id)
            isLocal = (local != null)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (nfcFullChar == null && nfcLightChar == null) {
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
        } else if (nfcFullChar != null) {
            // ESTADO 2: LECTURA EXITOSA (DATOS COMPLETOS)
            // Mostramos los datos directamente del NFC
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Contenido de la Tarjeta", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(nfcFullChar.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("Nivel ${nfcFullChar.level} | ${nfcFullChar.race} ${nfcFullChar.charClass}", style = MaterialTheme.typography.titleMedium)
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Stats principales
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatBox("FUE", nfcFullChar.str)
                            StatBox("DES", nfcFullChar.dex)
                            StatBox("CON", nfcFullChar.con)
                            StatBox("INT", nfcFullChar.int)
                            StatBox("SAB", nfcFullChar.wis)
                            StatBox("CAR", nfcFullChar.cha)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("HP Max: ${nfcFullChar.hpMax} | AC: ${nfcFullChar.ac}", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (isLocal) {
                    Button(onClick = { onFullCharacterLoaded(nfcFullChar) }) {
                        Text("Abrir Ficha Local")
                    }
                    Text("Este personaje ya está en tu móvil.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top=8.dp))
                } else {
                    Button(
                        onClick = { onFullCharacterLoaded(nfcFullChar) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Importar Personaje")
                    }
                    Text("Guardar copia en este dispositivo.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top=8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onScanAgainClick) { Text("Escanear otro") }
            }
        } else {
            // ESTADO 3: DATOS PARCIALES (Si falló la descompresión pero leyó algo)
            Text("Error: Datos ilegibles o dañados.", color = MaterialTheme.colorScheme.error)
            Button(onClick = onScanAgainClick) { Text("Intentar de nuevo") }
        }
    }
}

@Composable
fun StatBox(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text("$value", style = MaterialTheme.typography.bodyLarge)
    }
}