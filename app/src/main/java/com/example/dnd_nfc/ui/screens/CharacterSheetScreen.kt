package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.remote.FirebaseService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    characterId: String? = null,
    existingCharacter: PlayerCharacter? = null,
    onBack: () -> Unit,
    onWriteNfc: (PlayerCharacter) -> Unit
) {
    val scope = rememberCoroutineScope()
    // Estado del personaje (si es nuevo, empieza vacío)
    var charData by remember { mutableStateOf(existingCharacter ?: PlayerCharacter()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("General", "Combate", "Equipo", "Bio")

    // --- LÓGICA DE VALIDACIÓN ---
    // El botón guardar solo se activa si los campos obligatorios tienen texto
    val isFormValid = remember(charData) {
        charData.name.isNotBlank() &&
                charData.charClass.isNotBlank() &&
                charData.race.isNotBlank() &&
                charData.background.isNotBlank()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (charData.name.isEmpty()) "Nuevo Héroe" else charData.name)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    // BOTÓN VINCULAR NFC
                    // Solo visible si el personaje ya existe en la nube (tiene ID)
                    if (charData.id.isNotEmpty()) {
                        IconButton(
                            onClick = { onWriteNfc(charData) },
                            enabled = isFormValid // Bloqueado si faltan datos
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = "Vincular a Tarjeta",
                                tint = if (isFormValid) MaterialTheme.colorScheme.onSurface else Color.Gray
                            )
                        }
                    }

                    // BOTÓN GUARDAR (NUBE)
                    IconButton(
                        onClick = {
                            scope.launch {
                                // Guardamos en Firebase
                                val success = FirebaseService.saveCharacter(charData)
                                if (success) {
                                    onBack() // Volvemos a la lista si salió bien
                                }
                            }
                        },
                        enabled = isFormValid // Bloqueado si faltan datos
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Guardar",
                            tint = if (isFormValid) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Face, null)
                                1 -> Icon(Icons.Default.Shield, null)
                                2 -> Icon(Icons.Default.Backpack, null)
                                3 -> Icon(Icons.Default.MenuBook, null)
                            }
                        },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // MENSAJE DE ERROR (Si faltan datos)
            if (!isFormValid) {
                Text(
                    text = "* Faltan datos obligatorios (Nombre, Clase, Raza, Trasfondo)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // CONTENIDO DE LAS PESTAÑAS
            when (selectedTab) {
                0 -> GeneralTab(charData) { charData = it }
                1 -> CombatTab(charData) { charData = it }
                2 -> InventoryTab(charData) { charData = it }
                3 -> BioTab(charData) { charData = it }
            }
        }
    }
}

// ==========================================
//               PESTAÑA 1: GENERAL
// ==========================================
@Composable
fun GeneralTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    val scrollState = rememberScrollState()

    // Listas para los menús desplegables
    val classList = listOf("Bárbaro", "Bardo", "Brujo", "Clérigo", "Druida", "Explorador", "Guerrero", "Hechicero", "Mago", "Monje", "Paladín", "Pícaro", "Artífice")
    val raceList = listOf("Dracónido", "Enano", "Elfo", "Gnomo", "Humano", "Mediano", "Semielfo", "SemiOrco", "Tiflin", "Aasimar", "Goliat", "Orco")
    val levelList = (1..20).map { it.toString() }

    Column(
        modifier = Modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        SectionTitle("Identidad")

        // NOMBRE (Texto Libre)
        OutlinedTextField(
            value = char.name,
            onValueChange = { onUpdate(char.copy(name = it)) },
            label = { Text("Nombre del Personaje") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences)
        )

        // CLASE Y NIVEL (Dropdowns)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1.5f)) {
                DnDDropdown(
                    label = "Clase",
                    options = classList,
                    selected = char.charClass,
                    onSelectionChanged = { onUpdate(char.copy(charClass = it)) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                DnDDropdown(
                    label = "Nivel",
                    options = levelList,
                    selected = char.level.toString(),
                    onSelectionChanged = { onUpdate(char.copy(level = it.toInt())) }
                )
            }
        }

        // RAZA Y TRASFONDO
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                DnDDropdown(
                    label = "Raza",
                    options = raceList,
                    selected = char.race,
                    onSelectionChanged = { onUpdate(char.copy(race = it)) }
                )
            }
            // Trasfondo sigue siendo libre
            OutlinedTextField(
                value = char.background,
                onValueChange = { onUpdate(char.copy(background = it)) },
                label = { Text("Trasfondo") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionTitle("Estadísticas Base")

        // ESTADÍSTICAS CON STEPPERS (+/-)
        StatStepperRow("Fuerza (FUE)", char.str) { onUpdate(char.copy(str = it)) }
        StatStepperRow("Destreza (DES)", char.dex) { onUpdate(char.copy(dex = it)) }
        StatStepperRow("Constitución (CON)", char.con) { onUpdate(char.copy(con = it)) }
        StatStepperRow("Inteligencia (INT)", char.int) { onUpdate(char.copy(int = it)) }
        StatStepperRow("Sabiduría (SAB)", char.wis) { onUpdate(char.copy(wis = it)) }
        StatStepperRow("Carisma (CAR)", char.cha) { onUpdate(char.copy(cha = it)) }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ==========================================
//               COMPONENTES UI
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnDDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelectionChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Texto a mostrar: si está vacío mostramos el primero o placeholder
    val displayText = if (selected.isEmpty()) "Seleccionar" else selected

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
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

@Composable
fun StatStepperRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    // Calculamos modificador: (Valor - 10) / 2
    val mod = (value - 10) / 2
    val modStr = if (mod >= 0) "+$mod" else "$mod"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Label y Modificador
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold)
                Text(
                    "Modificador: $modStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mod >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            // Botones
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Menos
                IconButton(
                    onClick = { if (value > 1) onValueChange(value - 1) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Valor
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.Center
                )

                // Más
                IconButton(
                    onClick = { if (value < 30) onValueChange(value + 1) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)).size(32.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

// ==========================================
//           OTRAS PESTAÑAS (SIMPLES)
// ==========================================

@Composable
fun CombatTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Combate")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = "${char.ac}",
                onValueChange = { onUpdate(char.copy(ac = it.toIntOrNull() ?: 10)) },
                label = { Text("CA") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = "${char.hpCurrent}",
                onValueChange = { onUpdate(char.copy(hpCurrent = it.toIntOrNull() ?: 0)) },
                label = { Text("HP Actual") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = "${char.hpMax}",
                onValueChange = { onUpdate(char.copy(hpMax = it.toIntOrNull() ?: 0)) },
                label = { Text("HP Max") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        SectionTitle("Ataques y Conjuros")
        OutlinedTextField(
            value = char.attacksAndSpells,
            onValueChange = { onUpdate(char.copy(attacksAndSpells = it)) },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            placeholder = { Text("Ej: Espada Larga +5 (1d8+3)\nBola de Fuego (8d6)") }
        )
    }
}

@Composable
fun InventoryTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Inventario")
        OutlinedTextField(
            value = char.inventory,
            onValueChange = { onUpdate(char.copy(inventory = it)) },
            modifier = Modifier.fillMaxWidth().height(300.dp),
            placeholder = { Text("Equipo, oro, objetos mágicos...") }
        )
    }
}

@Composable
fun BioTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Historia y Rasgos")
        OutlinedTextField(value = char.personality, onValueChange = { onUpdate(char.copy(personality = it)) }, label = { Text("Personalidad") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = char.ideals, onValueChange = { onUpdate(char.copy(ideals = it)) }, label = { Text("Ideales") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = char.bonds, onValueChange = { onUpdate(char.copy(bonds = it)) }, label = { Text("Vínculos") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = char.flaws, onValueChange = { onUpdate(char.copy(flaws = it)) }, label = { Text("Defectos") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = char.backstory,
            onValueChange = { onUpdate(char.copy(backstory = it)) },
            label = { Text("Historia Completa") },
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )
    }
}