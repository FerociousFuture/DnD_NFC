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
import androidx.compose.ui.graphics.Color
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var charToDelete by remember { mutableStateOf<PlayerCharacter?>(null) }
    val scope = rememberCoroutineScope()

    fun load() { scope.launch { isLoading = true; characters = FirebaseService.getUserCharacters(); isLoading = false } }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewCharacterClick, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Crear Héroe") })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Mis Personajes", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            if (isLoading) CircularProgressIndicator()
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(characters) { char ->
                    Card(Modifier.fillMaxWidth().clickable { onCharacterClick(char) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
        if (showDeleteDialog && charToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("¿Eliminar Personaje?") },
                text = { Text("Se borrará a ${charToDelete!!.name} permanentemente.") },
                confirmButton = {
                    Button(onClick = {
                        val id = charToDelete!!.id
                        // Actualización optimista: lo quitamos de la lista visualmente
                        characters = characters.filter { it.id != id }
                        showDeleteDialog = false
                        scope.launch { FirebaseService.deleteCharacter(id) }
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Borrar") }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
            )
        }
    }
}