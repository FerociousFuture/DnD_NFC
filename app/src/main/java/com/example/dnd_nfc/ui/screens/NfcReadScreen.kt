package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.model.CharacterSheet

@Composable
fun NfcReadScreen(
    character: CharacterSheet?,
    isWaiting: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "D&D Mini Reader",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (isWaiting && character == null) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Acerca la miniatura al teléfono...")
        } else if (character != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Personaje detectado:", style = MaterialTheme.typography.titleMedium)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Nombre: ${character.n}")
                    Text("Clase: ${character.c}")
                    Text("Raza: ${character.r}")
                    Text("Estadísticas: ${character.s}")
                }
            }
        }
    }
}