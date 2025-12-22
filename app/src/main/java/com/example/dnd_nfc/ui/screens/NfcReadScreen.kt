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
    onFullCharacterLoaded: (PlayerCharacter) -> Unit, // Se usa para actualizar/escribir
    onScanAgainClick: () -> Unit
) {
    // Obtenemos los datos del evento de escaneo
    val nfcChar = scanEvent?.fullCharacter

    // Estado local editable para modificar HP o Estado in-situ
    var currentData by remember { mutableStateOf(nfcChar) }

    // Sincronizar si cambia el escaneo
    LaunchedEffect(nfcChar) {
        currentData = nfcChar
    }

    if (currentData == null) {
        // PANTALLA DE ESPERA
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Acerca una figura...", style = MaterialTheme.typography.titleLarge)
            }
        }
    } else {
        // PANTALLA DE DATOS DE LA FIGURA
        val char = currentData!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CABECERA: Nombre y Clase
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(char.name, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${char.race} ${char.charClass}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
                }
            }

            Spacer(Modifier.height(24.dp))

            // FILA: CD y ESTADO VISUAL
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // CD
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .border(4.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                    ) {
                        Text("${char.spellSaveDC}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("CD", fontWeight = FontWeight.Bold)
                }

                // HP VISUAL
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if(char.hpCurrent > 0) MaterialTheme.colorScheme.errorContainer else Color.Gray)
                            .border(4.dp, MaterialTheme.colorScheme.error, CircleShape)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${char.hpCurrent}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Divider(Modifier.width(40.dp).padding(vertical = 2.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("${char.hpMax}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Puntos de Golpe", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))

            // CONTROLES DE HP
            Text("Modificar Salud", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    val newHp = (char.hpCurrent - 1).coerceAtLeast(0)
                    currentData = char.copy(hpCurrent = newHp)
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Remove, null) }

                Spacer(Modifier.width(24.dp))

                Button(onClick = {
                    val newHp = (char.hpCurrent + 1).coerceAtMost(char.hpMax)
                    currentData = char.copy(hpCurrent = newHp)
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Icon(Icons.Default.Add, null) }
            }

            Spacer(Modifier.height(24.dp))

            // ESTADO DE LA MINI (Editable)
            Text("Estado de la Mini", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = char.status,
                onValueChange = { currentData = char.copy(status = it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Ej: Normal, Aturdido...") }
            )

            Spacer(Modifier.height(48.dp))

            // BOTÓN ACTUALIZAR FIGURA (WRITE BACK)
            Button(
                onClick = { onFullCharacterLoaded(currentData!!) }, // Esto llamará al proceso de escritura en MainActivity
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.SaveAs, null)
                Spacer(Modifier.width(8.dp))
                Text("ACTUALIZAR FIGURA (ACERCAR NFC)", fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onScanAgainClick, modifier = Modifier.padding(top=16.dp)) {
                Text("Cancelar / Escanear otra")
            }
        }
    }
}