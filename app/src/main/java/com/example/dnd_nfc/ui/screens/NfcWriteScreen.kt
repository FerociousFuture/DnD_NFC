package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.model.PlayerCharacter

@Composable
fun NfcWriteScreen(
    characterToWrite: PlayerCharacter?, // Recibe el personaje completo de la DB
    onReadyToWrite: (String) -> Unit,   // Envía el CSV a MainActivity
    onBack: () -> Unit
) {
    // Si entramos aquí sin personaje (error de flujo), mostramos aviso
    if (characterToWrite == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: No hay personaje seleccionado para vincular.")
            Button(onClick = onBack) { Text("Volver") }
        }
        return
    }

    // LÓGICA AUTOMÁTICA: Preparar los datos en cuanto carga la pantalla
    LaunchedEffect(characterToWrite) {
        // Limpiamos comas del nombre para no romper el formato CSV
        val safeName = characterToWrite.name.replace(",", " ").trim()

        // CONSTRUCCIÓN DEL CSV (Formato Ligero)
        // Estructura: ID, Nombre, FUE, DES, CON, INT, SAB, CAR
        val csvData = buildString {
            append(characterToWrite.id)
            append(",")
            append(safeName)
            append(",")
            append(characterToWrite.str)
            append(",")
            append(characterToWrite.dex)
            append(",")
            append(characterToWrite.con)
            append(",")
            append(characterToWrite.int)
            append(",")
            append(characterToWrite.wis)
            append(",")
            append(characterToWrite.cha)
        }

        // Enviamos los datos listos a MainActivity para que el adaptador NFC los espere
        onReadyToWrite(csvData)
    }

    // INTERFAZ VISUAL
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Nfc,
            contentDescription = "NFC Icon",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Modo Vinculación",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Acerca una etiqueta NFC vacía para grabar la llave de:",
            textAlign = TextAlign.Center
        )

        // Tarjeta visual del personaje a grabar
        Card(
            modifier = Modifier
                .padding(vertical = 24.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = characterToWrite.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${characterToWrite.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }

        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Esperando etiqueta...", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedButton(onClick = onBack) {
            Text("Cancelar")
        }
    }
}