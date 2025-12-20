package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcWriteScreen(
    onReadyToWrite: (String) -> Unit
) {
    // --- DATOS ---
    val classList = listOf(
        "Bárbaro", "Bardo", "Brujo", "Clérigo", "Druida",
        "Explorador", "Guerrero", "Hechicero",
        "Mago", "Monje", "Paladín", "Pícaro", "Artífice"
    )
    val raceList = listOf(
        "Dracónido", "Enano", "Elfo", "Gnomo", "Humano", "Mediano",
        "Semielfo", "SemiOrco", "Tiflin", "Aasimar", "Goliat", "Orco"
    )

    // --- ESTADO ---
    var name by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(classList[0]) }
    var selectedRace by remember { mutableStateOf(raceList[4]) }

    // Estadísticas Base (Point Buy empieza en 8)
    var str by remember { mutableIntStateOf(8) }
    var dex by remember { mutableIntStateOf(8) }
    var con by remember { mutableIntStateOf(8) }
    var int by remember { mutableIntStateOf(8) }
    var wis by remember { mutableIntStateOf(8) }
    var cha by remember { mutableIntStateOf(8) }

    // --- LÓGICA DE POINT BUY ---
    // Coste acumulado para llegar a X valor desde 8
    fun getCost(score: Int): Int {
        return when (score) {
            8 -> 0
            9 -> 1
            10 -> 2
            11 -> 3
            12 -> 4
            13 -> 5
            14 -> 7 // Salto de coste
            15 -> 9 // Salto de coste
            else -> 0
        }
    }

    val totalPointsSpent = getCost(str) + getCost(dex) + getCost(con) + getCost(int) + getCost(wis) + getCost(cha)
    val maxPoints = 27
    val remainingPoints = maxPoints - totalPointsSpent

    // Función para intentar cambiar un stat
    fun tryUpdateStat(currentVal: Int, delta: Int, setter: (Int) -> Unit) {
        val newVal = currentVal + delta
        if (newVal !in 8..15) return // Límites hardcap de 5e

        val costDiff = getCost(newVal) - getCost(currentVal)

        // Si subimos, checamos si hay puntos. Si bajamos, siempre se puede.
        if (delta > 0) {
            if (remainingPoints >= costDiff) setter(newVal)
        } else {
            setter(newVal)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forja de Héroes") },
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
                leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
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

            // --- POINT BUY HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Estadísticas (Point Buy)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)

                // Indicador de Puntos
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (remainingPoints < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Icon(Icons.Default.AutoGraph, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Puntos: $remainingPoints / $maxPoints",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (remainingPoints == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // --- LISTA DE STATS CON +/- ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatRow("Fuerza", "FUE", Icons.Default.FitnessCenter, str, remainingPoints) { tryUpdateStat(str, it) { v -> str = v } }
                StatRow("Destreza", "DES", Icons.Default.DirectionsRun, dex, remainingPoints) { tryUpdateStat(dex, it) { v -> dex = v } }
                StatRow("Constitución", "CON", Icons.Default.Shield, con, remainingPoints) { tryUpdateStat(con, it) { v -> con = v } }
                StatRow("Inteligencia", "INT", Icons.Default.AutoStories, int, remainingPoints) { tryUpdateStat(int, it) { v -> int = v } }
                StatRow("Sabiduría", "SAB", Icons.Default.Visibility, wis, remainingPoints) { tryUpdateStat(wis, it) { v -> wis = v } }
                StatRow("Carisma", "CAR", Icons.Default.Favorite, cha, remainingPoints) { tryUpdateStat(cha, it) { v -> cha = v } }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTÓN DE GRABAR ---
            Button(
                onClick = {
                    val safeName = name.replace(",", " ").trim().ifEmpty { "Sin Nombre" }
                    // Guardamos los valores finales
                    val csvData = "$safeName,$selectedClass,$selectedRace,$str,$dex,$con,$int,$wis,$cha"
                    onReadyToWrite(csvData)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Nfc, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("GRABAR EN NFC")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- COMPONENTE DE FILA DE ESTADÍSTICA ---
@Composable
fun StatRow(
    fullName: String,
    abbr: String,
    icon: ImageVector,
    value: Int,
    poolPoints: Int,
    onValueChange: (Int) -> Unit
) {
    // Calculamos si se puede subir (coste siguiente nivel)
    val nextCost = when (value) {
        in 8..12 -> 1
        in 13..14 -> 2
        else -> 999
    }
    val canIncrease = value < 15 && poolPoints >= nextCost
    val canDecrease = value > 8

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icono y Nombre
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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

            // Controles +/-
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón Menos
                IconButton(
                    onClick = { onValueChange(-1) },
                    enabled = canDecrease,
                    modifier = Modifier.background(Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = "Bajar",
                        tint = if (canDecrease) MaterialTheme.colorScheme.onSurface else Color.DarkGray
                    )
                }

                // Valor Central
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center,
                    color = if (value == 15) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                )

                // Botón Más
                IconButton(
                    onClick = { onValueChange(1) },
                    enabled = canIncrease
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Subir",
                        tint = if (canIncrease) MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                }
            }
        }
    }
}

// --- DROPDOWN (Reutilizado del anterior) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnDDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelectionChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectionChanged(option)
                        expanded = false
                    }
                )
            }
        }
    }
}