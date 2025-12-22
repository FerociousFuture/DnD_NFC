package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onNavigateToCharacters: () -> Unit, // Ir a "Biblioteca" (Lista)
    onNavigateToNewCharacter: () -> Unit, // Ir a "Nueva Figura Vacía"
    onNavigateToCampaigns: () -> Unit,
    onNavigateToCombat: () -> Unit,
    onCharacterImported: (Any) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Nfc,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Gestor de Figuras",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(48.dp))

        // OPCIÓN 1: BIBLIOTECA (CARGAR PRE-GUARDADO)
        Button(
            onClick = onNavigateToCharacters,
            modifier = Modifier.fillMaxWidth().height(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.List, null)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Biblioteca de Figuras", style = MaterialTheme.typography.titleMedium)
                Text("Cargar Goblins, Lobos, Jugadores...", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // OPCIÓN 2: NUEVA FIGURA (DESDE CERO)
        OutlinedButton(
            onClick = onNavigateToNewCharacter,
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(16.dp))
            Text("Crear Figura Nueva")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "O acerca una figura para leerla",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}