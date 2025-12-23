package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.ScanEvent

@Composable
fun NfcReadScreen(
    scanEvent: ScanEvent?,
    onFullCharacterLoaded: (PlayerCharacter) -> Unit,
    onScanAgainClick: () -> Unit
) {
    val nfcChar = scanEvent?.fullCharacter
    var currentData by remember { mutableStateOf(nfcChar) }

    LaunchedEffect(nfcChar) { currentData = nfcChar }

    if (currentData == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Acerca una figura...", style = MaterialTheme.typography.titleLarge)
            }
        }
    } else {
        val char = currentData!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CABECERA
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(char.name, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text("${char.race} ${char.charClass}", style = MaterialTheme.typography.headlineSmall)
                }
            }
            Spacer(Modifier.height(16.dp))

            // --- NUEVO: VISUALIZACIÓN DE ESTADÍSTICAS ---
            Text("Estadísticas", fontWeight = FontWeight.Bold, color = Color.Gray)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBadge("FUE", char.str)
                StatBadge("DES", char.dex)
                StatBadge("CON", char.con)
                StatBadge("INT", char.int)
                StatBadge("SAB", char.wis)
                StatBadge("CAR", char.cha)
            }
            Divider()
            Spacer(Modifier.height(16.dp))

            // INFO VITAL (AC, HP)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // AC
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)) {
                        Text("${char.ac}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Armadura", fontWeight = FontWeight.Bold)
                }
                // HP
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp).clip(CircleShape).background(if(char.hpCurrent > 0) MaterialTheme.colorScheme.errorContainer else Color.Gray)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${char.hpCurrent}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("/${char.hpMax}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Text("Puntos de Golpe", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))

            // MODIFICAR SALUD
            Text("Modificar Salud", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                Button(onClick = { currentData = char.copy(hpCurrent = (char.hpCurrent - 1).coerceAtLeast(0)) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Remove, null) }
                Spacer(Modifier.width(24.dp))
                Button(onClick = { currentData = char.copy(hpCurrent = (char.hpCurrent + 1).coerceAtMost(char.hpMax)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Icon(Icons.Default.Add, null) }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = char.status,
                onValueChange = { currentData = char.copy(status = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Estado") }
            )

            Spacer(Modifier.height(48.dp))

            Button(onClick = { onFullCharacterLoaded(currentData!!) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Default.SaveAs, null)
                Spacer(Modifier.width(8.dp))
                Text("ACTUALIZAR FIGURA (ACERCAR NFC)")
            }

            TextButton(onClick = onScanAgainClick, modifier = Modifier.padding(top=16.dp)) { Text("Cancelar") }
        }
    }
}

@Composable
fun StatBadge(label: String, value: Int) {
    val mod = (value - 10) / 2
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier.size(36.dp).border(1.dp, Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$value", fontWeight = FontWeight.Bold)
        }
        Text(if(mod>=0) "+$mod" else "$mod", style = MaterialTheme.typography.labelSmall, color = if(mod>=0) Color.DarkGray else Color.Red)
    }
}