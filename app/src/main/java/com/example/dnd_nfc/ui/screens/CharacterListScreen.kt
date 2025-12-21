package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.remote.FirebaseService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onCharacterClick: (PlayerCharacter) -> Unit,
    onNewCharacterClick: () -> Unit
) {
    var characters by remember { mutableStateOf<List<PlayerCharacter>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Función para recargar la lista
    fun loadCharacters() {
        scope.launch {
            isLoading = true
            characters = FirebaseService.getUserCharacters()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadCharacters()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewCharacterClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Personaje")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                "Mis Personajes",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (characters.isEmpty()) {
                Text("No tienes personajes guardados. ¡Crea uno!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(characters) { char ->
                        CharacterCard(
                            character = char,
                            onClick = { onCharacterClick(char) },
                            onDelete = {
                                scope.launch {
                                    val success = FirebaseService.deleteCharacter(char.id)
                                    if (success) {
                                        loadCharacters() // Recargamos la lista
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterCard(
    character: PlayerCharacter,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(character.name.ifEmpty { "Sin Nombre" }, style = MaterialTheme.typography.titleMedium)
                    Text("Nivel ${character.level} - ${character.race} ${character.charClass}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Botón de Borrar
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}