package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.Spell
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
    var charData by remember { mutableStateOf(existingCharacter ?: PlayerCharacter()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Combate", "Magia", "Equipo", "Rasgos")
    val isFormValid = charData.name.isNotBlank() && charData.charClass.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (charData.name.isEmpty()) "Nuevo Personaje" else charData.name) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") } },
                actions = {
                    if (charData.id.isNotEmpty()) {
                        IconButton(onClick = { onWriteNfc(charData) }, enabled = isFormValid) {
                            Icon(Icons.Default.Nfc, "Vincular", tint = if (isFormValid) MaterialTheme.colorScheme.onSurface else Color.Gray)
                        }
                    }
                    IconButton(onClick = { scope.launch { if(FirebaseService.saveCharacter(charData)) onBack() } }, enabled = isFormValid) {
                        Icon(Icons.Default.Save, "Guardar", tint = if (isFormValid) MaterialTheme.colorScheme.primary else Color.Gray)
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
                            when(index) {
                                0 -> Icon(Icons.Default.Face, null)
                                1 -> Icon(Icons.Default.Shield, null)
                                2 -> Icon(Icons.Default.AutoStories, null)
                                3 -> Icon(Icons.Default.Backpack, null)
                                else -> Icon(Icons.Default.Description, null)
                            }
                        },
                        label = { Text(title, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            when (selectedTab) {
                0 -> GeneralTab(charData) { charData = it }
                1 -> CombatTab(charData) { charData = it }
                2 -> MagicTab(charData) { charData = it }
                3 -> EquipmentTab(charData) { charData = it }
                4 -> FeaturesTab(charData) { charData = it }
            }
        }
    }
}

// --- PESTAÑAS ---
@Composable
fun GeneralTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Identidad (5.5e)")
        OutlinedTextField(value = char.name, onValueChange = { onUpdate(char.copy(name = it)) }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.charClass, onValueChange = { onUpdate(char.copy(charClass = it)) }, label = { Text("Clase") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = "${char.level}", onValueChange = { onUpdate(char.copy(level = it.toIntOrNull()?:1)) }, label = { Text("Nivel") }, modifier = Modifier.weight(0.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.race, onValueChange = { onUpdate(char.copy(race = it)) }, label = { Text("Especie") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = char.subclass, onValueChange = { onUpdate(char.copy(subclass = it)) }, label = { Text("Subclase") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.background, onValueChange = { onUpdate(char.copy(background = it)) }, label = { Text("Trasfondo") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = char.size, onValueChange = { onUpdate(char.copy(size = it)) }, label = { Text("Tamaño") }, modifier = Modifier.weight(0.5f))
        }

        SectionTitle("Atributos")
        StatRow("FUERZA", char.str) { onUpdate(char.copy(str = it)) }
        StatRow("DESTREZA", char.dex) { onUpdate(char.copy(dex = it)) }
        StatRow("CONSTITUCIÓN", char.con) { onUpdate(char.copy(con = it)) }
        StatRow("INTELIGENCIA", char.int) { onUpdate(char.copy(int = it)) }
        StatRow("SABIDURÍA", char.wis) { onUpdate(char.copy(wis = it)) }
        StatRow("CARISMA", char.cha) { onUpdate(char.copy(cha = it)) }

        SectionTitle("Habilidades")
        OutlinedTextField(value = "${char.passivePerception}", onValueChange = { onUpdate(char.copy(passivePerception = it.toIntOrNull()?:10)) }, label = { Text("Percepción Pasiva") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
}

@Composable
fun CombatTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Estado de Combate")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactInput("CA", char.ac) { onUpdate(char.copy(ac = it)) }
            CompactInput("Iniciativa", char.initiative) { onUpdate(char.copy(initiative = it)) }
            CompactInput("Velocidad", char.speed) { onUpdate(char.copy(speed = it)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Inspiración Heroica")
            Checkbox(checked = char.inspiration, onCheckedChange = { onUpdate(char.copy(inspiration = it)) })
        }
        SectionTitle("Puntos de Golpe")
        Row {
            OutlinedTextField(value = "${char.hpCurrent}", onValueChange = { onUpdate(char.copy(hpCurrent = it.toIntOrNull()?:0)) }, label = { Text("Actual") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = "${char.hpMax}", onValueChange = { onUpdate(char.copy(hpMax = it.toIntOrNull()?:0)) }, label = { Text("Máximo") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        SectionTitle("Salvaciones de Muerte")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Éxito: ")
            repeat(3) { i -> Checkbox(checked = i < char.deathSaveSuccess, onCheckedChange = { onUpdate(char.copy(deathSaveSuccess = if(it) i+1 else i)) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fallo:  ")
            repeat(3) { i -> Checkbox(checked = i < char.deathSaveFail, onCheckedChange = { onUpdate(char.copy(deathSaveFail = if(it) i+1 else i)) }) }
        }
    }
}

@Composable
fun MagicTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Estadísticas Mágicas")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.spellAbility, onValueChange = { onUpdate(char.copy(spellAbility = it)) }, label = { Text("Hab. Clave") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = "${char.spellSaveDC}", onValueChange = { onUpdate(char.copy(spellSaveDC = it.toIntOrNull()?:10)) }, label = { Text("CD Salv.") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = "+${char.spellAttackBonus}", onValueChange = { onUpdate(char.copy(spellAttackBonus = it.toIntOrNull()?:0)) }, label = { Text("Ataque") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        SectionTitle("Espacios de Conjuro")
        for(i in 1..9 step 3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SlotCounter(i, char, onUpdate)
                if(i+1<=9) SlotCounter(i+1, char, onUpdate)
                if(i+2<=9) SlotCounter(i+2, char, onUpdate)
            }
        }

        SectionTitle("Libro de Conjuros")
        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Añadir Conjuro") }
        char.spells.sortedBy { it.level }.forEach { spell ->
            Card(Modifier.padding(vertical=4.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(8.dp)) {
                    Text("${spell.name} (Nivel ${spell.level})", fontWeight = FontWeight.Bold)
                    Text("Tiempo: ${spell.time} | Alcance: ${spell.range}", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { onUpdate(char.copy(spells = char.spells - spell)) }, modifier = Modifier.align(Alignment.End).size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.Gray)
                    }
                }
            }
        }
    }
    if(showDialog) {
        var n by remember { mutableStateOf("") }; var l by remember { mutableStateOf("0") }
        var t by remember { mutableStateOf("") }; var r by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nuevo Conjuro") },
            text = { Column {
                OutlinedTextField(n, {n=it}, label={Text("Nombre")})
                OutlinedTextField(l, {l=it}, label={Text("Nivel")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
                OutlinedTextField(t, {t=it}, label={Text("Tiempo")})
                OutlinedTextField(r, {r=it}, label={Text("Alcance")})
            }},
            confirmButton = { Button(onClick={
                onUpdate(char.copy(spells = char.spells + Spell(n, l.toIntOrNull()?:0, t, r)))
                showDialog = false
            }) { Text("Añadir") }}
        )
    }
}

@Composable
fun EquipmentTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Monedas")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CoinInput("PC", char.cp, Color(0xFFB87333)) { onUpdate(char.copy(cp = it)) }
            CoinInput("PP", char.sp, Color(0xFFC0C0C0)) { onUpdate(char.copy(sp = it)) }
            CoinInput("PE", char.ep, Color(0xFF50C878)) { onUpdate(char.copy(ep = it)) }
            CoinInput("PO", char.gp, Color(0xFFFFD700)) { onUpdate(char.copy(gp = it)) }
            CoinInput("PPT", char.pp, Color(0xFFE5E4E2)) { onUpdate(char.copy(pp = it)) }
        }
        SectionTitle("Inventario")
        OutlinedTextField(value = char.inventory, onValueChange = { onUpdate(char.copy(inventory = it)) }, modifier = Modifier.fillMaxWidth().height(300.dp))
    }
}

@Composable
fun FeaturesTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Rasgos y Dotes")
        OutlinedTextField(char.speciesTraits, { onUpdate(char.copy(speciesTraits = it)) }, label = { Text("Rasgos de Especie") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(char.classFeatures, { onUpdate(char.copy(classFeatures = it)) }, label = { Text("Rasgos de Clase") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(char.feats, { onUpdate(char.copy(feats = it)) }, label = { Text("Dotes (Feats)") }, modifier = Modifier.fillMaxWidth())
        SectionTitle("Competencias")
        OutlinedTextField(char.otherProficiencies, { onUpdate(char.copy(otherProficiencies = it)) }, label = { Text("Idiomas y Herramientas") }, modifier = Modifier.fillMaxWidth())
        SectionTitle("Biografía")
        OutlinedTextField(char.backstory, { onUpdate(char.copy(backstory = it)) }, label = { Text("Historia") }, modifier = Modifier.fillMaxWidth().height(150.dp))
    }
}

// Helpers UI
@Composable fun SectionTitle(t: String) = Text(t, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
@Composable fun CompactInput(l: String, v: Int, ch: (Int)->Unit) = OutlinedTextField("$v", { ch(it.toIntOrNull()?:0) }, label={Text(l)}, modifier=Modifier.width(90.dp), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
@Composable fun CoinInput(l: String, v: Int, c: Color, ch: (Int)->Unit) = Column(horizontalAlignment=Alignment.CenterHorizontally) { Text(l, color=c, fontWeight=FontWeight.Bold); OutlinedTextField("$v", { ch(it.toIntOrNull()?:0) }, modifier=Modifier.width(60.dp), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)) }
@Composable fun SlotCounter(lvl: Int, char: PlayerCharacter, onUpdate: (PlayerCharacter)->Unit) {
    val total = char.spellSlots["$lvl"] ?: 0
    val used = char.spellSlots["${lvl}_used"] ?: 0
    Card(Modifier.width(100.dp).padding(2.dp)) {
        Column(Modifier.padding(4.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Text("Nivel $lvl", fontSize=12.sp)
            Text("$used / $total", fontWeight=FontWeight.Bold)
            Row {
                IconButton(onClick={ onUpdate(char.copy(spellSlots = char.spellSlots + ("${lvl}_used" to (if(used<total) used+1 else 0)))) }, modifier=Modifier.size(24.dp)) { Icon(Icons.Default.Add, null) }
                IconButton(onClick={ onUpdate(char.copy(spellSlots = char.spellSlots + ("${lvl}_used" to 0))) }, modifier=Modifier.size(24.dp)) { Icon(Icons.Default.Refresh, null) }
            }
            OutlinedTextField("$total", { onUpdate(char.copy(spellSlots = char.spellSlots + ("$lvl" to (it.toIntOrNull()?:0)))) }, label={Text("Max")}, textStyle=LocalTextStyle.current.copy(fontSize=12.sp))
        }
    }
}
@Composable fun StatRow(label: String, value: Int, onChange: (Int) -> Unit) {
    val mod = (value - 10) / 2
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("$label (Mod: ${if(mod>=0)"+$mod" else "$mod"})", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(value - 1) }) { Icon(Icons.Default.Remove, null) }
            Text("$value", fontWeight = FontWeight.Bold)
            IconButton(onClick = { onChange(value + 1) }) { Icon(Icons.Default.Add, null) }
        }
    }
}