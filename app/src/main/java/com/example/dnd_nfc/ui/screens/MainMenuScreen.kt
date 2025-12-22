package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onNavigateToCharacters: () -> Unit,
    onNavigateToNewCharacter: () -> Unit,
    onNavigateToCombat: () -> Unit,
    onNavigateToCampaigns: () -> Unit,
    onCharacterImported: (Any) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Nfc, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("D&D NFC Master", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(48.dp))

        // 1. BIBLIOTECA
        Button(
            onClick = onNavigateToCharacters,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.List, null)
            Spacer(Modifier.width(16.dp))
            Text("Biblioteca de Figuras")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. MESA DE COMBATE
        Button(
            onClick = onNavigateToCombat,
            modifier = Modifier.fillMaxWidth().height(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            // CORRECCIÓN AQUÍ: Usamos FlashOn en lugar de Swords
            Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("MESA DE COMBATE", style = MaterialTheme.typography.titleMedium)
                Text("Iniciativa y Turnos", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. NUEVA FIGURA
        OutlinedButton(onClick = onNavigateToNewCharacter, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(16.dp))
            Text("Crear Figura Nueva")
        }
    }
}