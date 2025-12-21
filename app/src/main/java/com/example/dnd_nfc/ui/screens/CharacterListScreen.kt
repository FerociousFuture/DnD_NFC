package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.local.CharacterManager // <--- Nuevo Import
import com.example.dnd_nfc.data.model.PlayerCharacter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onCharacterClick: (PlayerCharacter) -> Unit,
    onNewCharacterClick: () -> Unit
) {
    val context = LocalContext.current // Necesario para leer archivos
    var characters by remember { mutableStateOf<List<PlayerCharacter>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialogo de borrado
    var showDeleteDialog by remember { mutableStateOf(false) }
    var charToDelete by remember { mutableStateOf<PlayerCharacter?>(null) }

    // CARGA DE DATOS (LOCAL)
    // Usamos LaunchedEffect para recargar la lista cada vez que volvemos a esta pantalla
    LaunchedEffect(Unit) {
        isLoading = true
        characters = CharacterManager.getCharacters(context)
        isLoading = false
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewCharacterClick,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Crear Héroe") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                "Mis Personajes (Local)",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (characters.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay personajes. ¡Crea uno!", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    items(characters) { char ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCharacterClick(char) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(char.name.ifEmpty { "Sin Nombre" }, style = MaterialTheme.typography.titleMedium)
                                    Text("Nivel ${char.level} | ${char.race} ${char.charClass}", style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { charToDelete = char; showDeleteDialog = true }) {
                                    Icon(Icons.Default.Delete, "Borrar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ALERTA DE BORRADO
        if (showDeleteDialog && charToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("¿Eliminar Personaje?") },
                text = { Text("Se borrará a ${charToDelete!!.name} de la memoria del teléfono.") },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = charToDelete!!.id
                            CharacterManager.deleteCharacter(context, id) // Borrado Local
                            // Recargamos la lista
                            characters = CharacterManager.getCharacters(context)
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Borrar") }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
            )
        }
    }
}