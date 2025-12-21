package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    onWriteNfc: (PlayerCharacter) -> Unit // <--- Callback para ir a grabar
) {
    val scope = rememberCoroutineScope()
    var charData by remember { mutableStateOf(existingCharacter ?: PlayerCharacter()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Combate", "Equipo", "Bio")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (charData.name.isEmpty()) "Nuevo" else charData.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") }
                },
                actions = {
                    // BOTÓN NFC: Solo aparece si el personaje ya tiene ID (ya fue guardado)
                    if (charData.id.isNotEmpty()) {
                        IconButton(onClick = { onWriteNfc(charData) }) {
                            Icon(Icons.Default.Nfc, contentDescription = "Vincular a Tarjeta")
                        }
                    }

                    IconButton(onClick = {
                        scope.launch {
                            val success = FirebaseService.saveCharacter(charData)
                            if (success) {
                                onBack()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Save, "Guardar")
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
            when (selectedTab) {
                0 -> GeneralTab(charData) { charData = it }
                1 -> CombatTab(charData) { charData = it }
                2 -> InventoryTab(charData) { charData = it }
                3 -> BioTab(charData) { charData = it }
            }
        }
    }
}

// --- TABS Y COMPONENTES AUXILIARES ---

@Composable
fun GeneralTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Datos Principales")
        OutlinedTextField(value = char.name, onValueChange = { onUpdate(char.copy(name = it)) }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.charClass, onValueChange = { onUpdate(char.copy(charClass = it)) }, label = { Text("Clase") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = "${char.level}", onValueChange = { onUpdate(char.copy(level = it.toIntOrNull() ?: 1)) }, label = { Text("Nivel") }, modifier = Modifier.weight(0.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.race, onValueChange = { onUpdate(char.copy(race = it)) }, label = { Text("Especie/Raza") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = char.background, onValueChange = { onUpdate(char.copy(background = it)) }, label = { Text("Trasfondo") }, modifier = Modifier.weight(1f))
        }

        SectionTitle("Atributos (Stats)")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatInputCompact("FUE", char.str) { onUpdate(char.copy(str = it)) }
            StatInputCompact("DES", char.dex) { onUpdate(char.copy(dex = it)) }
            StatInputCompact("CON", char.con) { onUpdate(char.copy(con = it)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatInputCompact("INT", char.int) { onUpdate(char.copy(int = it)) }
            StatInputCompact("SAB", char.wis) { onUpdate(char.copy(wis = it)) }
            StatInputCompact("CAR", char.cha) { onUpdate(char.copy(cha = it)) }
        }
    }
}

@Composable
fun CombatTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Estado Vital")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = "${char.ac}", onValueChange = { onUpdate(char.copy(ac = it.toIntOrNull() ?: 10)) }, label = { Text("CA (Armor)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = "${char.hpCurrent}", onValueChange = { onUpdate(char.copy(hpCurrent = it.toIntOrNull() ?: 0)) }, label = { Text("HP Actual") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = "${char.hpMax}", onValueChange = { onUpdate(char.copy(hpMax = it.toIntOrNull() ?: 0)) }, label = { Text("HP Max") }, modifier = Modifier.weight(1f))
        }

        SectionTitle("Ataques y Conjuros")
        OutlinedTextField(
            value = char.attacksAndSpells,
            onValueChange = { onUpdate(char.copy(attacksAndSpells = it)) },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            placeholder = { Text("Ej: Espada Corta +5 (1d6+3) \nBola de Fuego (8d6)") }
        )
    }
}

@Composable
fun InventoryTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Inventario y Equipo")
        OutlinedTextField(
            value = char.inventory,
            onValueChange = { onUpdate(char.copy(inventory = it)) },
            modifier = Modifier.fillMaxWidth().height(300.dp),
            placeholder = { Text("Mochila, Cuerda (50m), Raciones...\n\nMONEDAS:\nPO: 10\nPP: 0") }
        )
    }
}

@Composable
fun BioTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Rasgos e Historia")
        OutlinedTextField(value = char.personality, onValueChange = { onUpdate(char.copy(personality = it)) }, label = { Text("Personalidad") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = char.ideals, onValueChange = { onUpdate(char.copy(ideals = it)) }, label = { Text("Ideales") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = char.bonds, onValueChange = { onUpdate(char.copy(bonds = it)) }, label = { Text("Vínculos") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = char.flaws, onValueChange = { onUpdate(char.copy(flaws = it)) }, label = { Text("Defectos") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = char.backstory,
            onValueChange = { onUpdate(char.copy(backstory = it)) },
            label = { Text("Historia del Personaje") },
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun StatInputCompact(label: String, value: Int, onValueChange: (Int) -> Unit) {
    val mod = (value - 10) / 2
    val modStr = if(mod >= 0) "+$mod" else "$mod"

    OutlinedTextField(
        value = "$value",
        onValueChange = { onValueChange(it.toIntOrNull() ?: 10) },
        label = { Text(label) },
        supportingText = { Text("Mod: $modStr") },
        modifier = Modifier.width(100.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}