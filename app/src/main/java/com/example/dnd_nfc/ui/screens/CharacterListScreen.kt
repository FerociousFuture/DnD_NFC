package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.local.CharacterManager
import com.example.dnd_nfc.data.model.PlayerCharacter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onCharacterClick: (PlayerCharacter) -> Unit,
    onNewCharacterClick: () -> Unit
) {
    val context = LocalContext.current
    var characterList by remember { mutableStateOf(listOf<PlayerCharacter>()) }

    LaunchedEffect(Unit) {
        characterList = CharacterManager.getCharacters(context)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewCharacterClick) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Figura")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Biblioteca de Figuras", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (characterList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay figuras guardadas.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(characterList) { char ->
                        CharacterCard(
                            character = char,
                            onClick = { onCharacterClick(char) },
                            onDelete = {
                                CharacterManager.deleteCharacter(context, char.id)
                                characterList = CharacterManager.getCharacters(context)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterCard(character: PlayerCharacter, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = character.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${character.race} ${character.charClass}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                // Muestra AC y HP en vez de Nivel
                Row {
                    BadgeInfo("AC: ${character.ac}")
                    Spacer(Modifier.width(8.dp))
                    BadgeInfo("HP: ${character.hpCurrent}/${character.hpMax}")
                    if (character.status.isNotEmpty() && character.status != "Normal") {
                        Spacer(Modifier.width(8.dp))
                        BadgeInfo(character.status, Color(0xFFFFE0E0), Color.Red)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun BadgeInfo(text: String, containerColor: Color = Color.LightGray.copy(alpha = 0.3f), textColor: Color = Color.Black) {
    Surface(shape = RoundedCornerShape(4.dp), color = containerColor, modifier = Modifier.padding(top = 2.dp)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textColor)
    }
}