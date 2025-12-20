package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcWriteScreen(
    onReadyToWrite: (String) -> Unit
) {
    // --- LISTAS PREDEFINIDAS ---
    val classList = listOf(
        "Bárbaro", "Bardo", "Brujo (Warlock)", "Clérigo", "Druida",
        "Explorador (Ranger)", "Guerrero (Fighter)", "Hechicero (Sorcerer)",
        "Mago (Wizard)", "Monje", "Paladín", "Pícaro (Rogue)", "Artífice (Artificer)"
    )
    val raceList = listOf(
        "Dracónido", "Enano", "Elfo", "Gnomo", "Humano", "Mediano",
        "Semielfo", "SemiOrco", "Tiflin", "Aasimar", "Goliat", "Orco"
    )
    // Lista de números para stats (1 al 20)
    val statValues = (1..20).map { it.toString() }

    // --- ESTADO ---
    var name by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(classList[0]) }
    var selectedRace by remember { mutableStateOf(raceList[4]) } // Default: Humano

    // Stats (Por defecto 10)
    var str by remember { mutableStateOf("10") }
    var dex by remember { mutableStateOf("10") }
    var con by remember { mutableStateOf("10") }
    var int by remember { mutableStateOf("10") }
    var wis by remember { mutableStateOf("10") }
    var cha by remember { mutableStateOf("10") }

    // Cálculo dinámico de la suma (Solo visual)
    val totalStats = (str.toIntOrNull() ?: 0) +
            (dex.toIntOrNull() ?: 0) +
            (con.toIntOrNull() ?: 0) +
            (int.toIntOrNull() ?: 0) +
            (wis.toIntOrNull() ?: 0) +
            (cha.toIntOrNull() ?: 0)

    // Color del indicador de suma según el poder (Visual feedback)
    val totalColor = when {
        totalStats < 60 -> Color.Gray       // Débil
        totalStats in 60..75 -> Color.White // Promedio
        totalStats in 76..85 -> Color(0xFFFFD700) // Heroico (Dorado)
        else -> Color(0xFFB00020)           // Legendario/Roto (Rojo)
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

            // 1. NOMBRE (Texto Libre)
            Text("Identidad", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nombre del Personaje") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done)
            )

            // 2. SELECTORES DE RAZA Y CLASE
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Dropdown Clase
                Box(modifier = Modifier.weight(1f)) {
                    DnDDropdown(label = "Clase", options = classList, selected = selectedClass) { selectedClass = it }
                }
                // Dropdown Raza
                Box(modifier = Modifier.weight(1f)) {
                    DnDDropdown(label = "Raza", options = raceList, selected = selectedRace) { selectedRace = it }
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))

            // 3. ESTADÍSTICAS (Dropdowns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Atributos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)

                // INDICADOR DE SUMA TOTAL (Visual)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("Total: ", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "$totalStats",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = totalColor
                        )
                    }
                }
            }

            // Grid de Stats (2 filas x 3 columnas)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatDropdown("FUE", Icons.Default.FitnessCenter, statValues, str) { str = it }
                StatDropdown("DES", Icons.Default.DirectionsRun, statValues, dex) { dex = it }
                StatDropdown("CON", Icons.Default.Shield, statValues, con) { con = it }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatDropdown("INT", Icons.Default.AutoStories, statValues, int) { int = it }
                StatDropdown("SAB", Icons.Default.Visibility, statValues, wis) { wis = it }
                StatDropdown("CAR", Icons.Default.Favorite, statValues, cha) { cha = it }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. BOTÓN DE GRABAR
            Button(
                onClick = {
                    val safeName = name.replace(",", " ").trim().ifEmpty { "Sin Nombre" }
                    // Construimos CSV: Nombre,Clase,Raza,FUE,DES,CON,INT,SAB,CAR
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

// --- COMPONENTES REUTILIZABLES ---

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
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.StatDropdown(
    label: String,
    icon: ImageVector,
    options: List<String>,
    selected: String,
    onSelectionChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.weight(1f)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                label = { Text(label, fontSize = 11.sp) },
                leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = null, // Sin flecha para ahorrar espacio
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.height(300.dp) // Limitamos altura para scroll
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelectionChanged(option)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    )
                }
            }
        }
    }
}