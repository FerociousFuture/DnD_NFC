package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dnd_nfc.data.model.CharacterSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcEditScreen(
    character: CharacterSheet, // Recibimos el personaje a editar
    onReadyToWrite: (String) -> Unit,
    onCancel: () -> Unit
) {
    // --- DATOS INICIALES ---
    // Parseamos las estadísticas que vienen en formato "10-12-14..."
    val initialStats = remember(character.s) {
        val parts = character.s.split("-")
        if (parts.size >= 6) parts.map { it.toIntOrNull() ?: 10 } else List(6) { 10 }
    }

    val classList = listOf(
        "Bárbaro", "Bardo", "Brujo", "Clérigo", "Druida",
        "Explorador", "Guerrero", "Hechicero",
        "Mago", "Monje", "Paladín", "Pícaro", "Artífice"
    )
    val raceList = listOf(
        "Dracónido", "Enano", "Elfo", "Gnomo", "Humano", "Mediano",
        "Semielfo", "SemiOrco", "Tiflin", "Aasimar", "Goliat", "Orco"
    )

    // --- ESTADO (Inicializado con los datos del personaje) ---
    var name by remember { mutableStateOf(character.n) }
    var selectedClass by remember { mutableStateOf(if (character.c in classList) character.c else classList[0]) }
    var selectedRace by remember { mutableStateOf(if (character.r in raceList) character.r else raceList[0]) }

    var str by remember { mutableIntStateOf(initialStats[0]) }
    var dex by remember { mutableIntStateOf(initialStats[1]) }
    var con by remember { mutableIntStateOf(initialStats[2]) }
    var int by remember { mutableIntStateOf(initialStats[3]) }
    var wis by remember { mutableIntStateOf(initialStats[4]) }
    var cha by remember { mutableIntStateOf(initialStats[5]) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Personaje") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- IDENTIDAD ---
            Text("Identidad", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    DnDDropdown(label = "Clase", options = classList, selected = selectedClass) { selectedClass = it }
                }
                Box(modifier = Modifier.weight(1f)) {
                    DnDDropdown(label = "Raza", options = raceList, selected = selectedRace) { selectedRace = it }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))

            // --- ESTADÍSTICAS (Modo Libre) ---
            Text(
                "Ajuste de Atributos (Nivel/Objetos)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Usamos una versión simplificada de StatRow que no verifica costes de puntos
                EditStatRow("Fuerza", "FUE", Icons.Default.FitnessCenter, str) { str = it }
                EditStatRow("Destreza", "DES", Icons.Default.DirectionsRun, dex) { dex = it }
                EditStatRow("Constitución", "CON", Icons.Default.Shield, con) { con = it }
                EditStatRow("Inteligencia", "INT", Icons.Default.AutoStories, int) { int = it }
                EditStatRow("Sabiduría", "SAB", Icons.Default.Visibility, wis) { wis = it }
                EditStatRow("Carisma", "CAR", Icons.Default.Favorite, cha) { cha = it }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTÓN DE ACTUALIZAR ---
            Button(
                onClick = {
                    val safeName = name.replace(",", " ").trim().ifEmpty { "Sin Nombre" }
                    val csvData = "$safeName,$selectedClass,$selectedRace,$str,$dex,$con,$int,$wis,$cha"
                    onReadyToWrite(csvData)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SOBREESCRIBIR NFC")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- Fila de Edición Libre (1-30) ---
@Composable
fun EditStatRow(
    fullName: String,
    abbr: String,
    icon: ImageVector,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(abbr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if(value > 1) onValueChange(value - 1) }, // Mínimo 1
                    modifier = Modifier.background(Color.Transparent)
                ) {
                    Icon(Icons.Default.Remove, "Bajar", tint = MaterialTheme.colorScheme.onSurface)
                }

                Text(
                    text = "$value",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { if(value < 30) onValueChange(value + 1) } // Máximo 30 (Regla D&D)
                ) {
                    Icon(Icons.Default.Add, "Subir", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}