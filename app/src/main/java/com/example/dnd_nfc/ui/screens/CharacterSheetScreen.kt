package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dnd_nfc.data.model.Attack
import com.example.dnd_nfc.data.model.InventoryItem
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
    val icons = listOf(Icons.Default.Face, Icons.Default.Shield, Icons.Default.AutoStories, Icons.Default.Backpack, Icons.Default.Description)

    // Validación mínima
    val isFormValid = charData.name.isNotBlank() && charData.charClass.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (charData.name.isEmpty()) "Nuevo Personaje" else charData.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") }
                },
                actions = {
                    if (charData.id.isNotEmpty()) {
                        IconButton(onClick = { onWriteNfc(charData) }, enabled = isFormValid) {
                            Icon(Icons.Default.Nfc, "Vincular", tint = if (isFormValid) MaterialTheme.colorScheme.onSurface else Color.Gray)
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            if (FirebaseService.saveCharacter(charData)) onBack()
                        }
                    }, enabled = isFormValid) {
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
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
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

// ==========================================
// PESTAÑA 1: GENERAL (Datos, Stats, Skills)
// ==========================================
@Composable
fun GeneralTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        SectionTitle("Identidad")
        OutlinedTextField(
            value = char.name,
            onValueChange = { onUpdate(char.copy(name = it)) },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.charClass, onValueChange = { onUpdate(char.copy(charClass = it)) }, label = { Text("Clase") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = "${char.level}", onValueChange = { onUpdate(char.copy(level = it.toIntOrNull() ?: 1)) }, label = { Text("Nivel") }, modifier = Modifier.weight(0.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.race, onValueChange = { onUpdate(char.copy(race = it)) }, label = { Text("Especie") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = char.subclass, onValueChange = { onUpdate(char.copy(subclass = it)) }, label = { Text("Subclase") }, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.background, onValueChange = { onUpdate(char.copy(background = it)) }, label = { Text("Trasfondo") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = char.xp, onValueChange = { onUpdate(char.copy(xp = it)) }, label = { Text("XP") }, modifier = Modifier.weight(0.5f))
        }

        OutlinedTextField(value = char.size, onValueChange = { onUpdate(char.copy(size = it)) }, label = { Text("Tamaño") }, modifier = Modifier.fillMaxWidth())

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // --- ATRIBUTOS & HABILIDADES ---
        SectionTitle("Atributos & Habilidades")

        StatBlock("FUERZA", char.str, { onUpdate(char.copy(str = it)) }, char, onUpdate, listOf("Atletismo"))
        StatBlock("DESTREZA", char.dex, { onUpdate(char.copy(dex = it)) }, char, onUpdate, listOf("Acrobacias", "Juego de Manos", "Sigilo"))
        StatBlock("CONSTITUCIÓN", char.con, { onUpdate(char.copy(con = it)) }, char, onUpdate, listOf())
        StatBlock("INTELIGENCIA", char.int, { onUpdate(char.copy(int = it)) }, char, onUpdate, listOf("C. Arcano", "Historia", "Investigación", "Naturaleza", "Religión"))
        StatBlock("SABIDURÍA", char.wis, { onUpdate(char.copy(wis = it)) }, char, onUpdate, listOf("T. con Animales", "Medicina", "Percepción", "Perspicacia", "Supervivencia"))
        StatBlock("CARISMA", char.cha, { onUpdate(char.copy(cha = it)) }, char, onUpdate, listOf("Engaño", "Interpretación", "Intimidación", "Persuasión"))

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Percepción Pasiva: ", fontWeight = FontWeight.Bold)
            CompactInput("", char.passivePerception) { onUpdate(char.copy(passivePerception = it)) }
        }
    }
}

// ==========================================
// PESTAÑA 2: COMBATE (Defensas, HP, Ataques)
// ==========================================
@Composable
fun CombatTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    var showAddAttackDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        SectionTitle("Estado de Combate")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactInput("CA", char.ac) { onUpdate(char.copy(ac = it)) }
            CompactInput("Inic.", char.initiative) { onUpdate(char.copy(initiative = it)) }
            CompactInput("Vel.", char.speed) { onUpdate(char.copy(speed = it)) }
            CompactInput("Bono Comp.", char.proficiencyBonus) { onUpdate(char.copy(proficiencyBonus = it)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = char.inspiration, onCheckedChange = { onUpdate(char.copy(inspiration = it)) })
            Text("Inspiración Heroica")
        }

        Divider()

        // HP
        SectionTitle("Puntos de Golpe")
        Row {
            OutlinedTextField(value = "${char.hpCurrent}", onValueChange = { onUpdate(char.copy(hpCurrent = it.toIntOrNull() ?: 0)) }, label = { Text("Actuales") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = "${char.hpMax}", onValueChange = { onUpdate(char.copy(hpMax = it.toIntOrNull() ?: 0)) }, label = { Text("Máximo") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = "${char.tempHp}", onValueChange = { onUpdate(char.copy(tempHp = it.toIntOrNull() ?: 0)) }, label = { Text("Temp") }, modifier = Modifier.weight(0.7f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        // Dados de Golpe y Salvaciones
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = char.hitDiceTotal, onValueChange = { onUpdate(char.copy(hitDiceTotal = it)) }, label = { Text("Dados Golpe") }, modifier = Modifier.weight(0.8f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1.2f)) {
                Text("Salvaciones de Muerte", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Éxito: ", fontSize = 10.sp)
                    repeat(3) { i -> Checkbox(checked = i < char.deathSaveSuccess, onCheckedChange = { onUpdate(char.copy(deathSaveSuccess = if(it) i+1 else i)) }, modifier = Modifier.size(24.dp)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fallo:  ", fontSize = 10.sp)
                    repeat(3) { i -> Checkbox(checked = i < char.deathSaveFail, onCheckedChange = { onUpdate(char.copy(deathSaveFail = if(it) i+1 else i)) }, modifier = Modifier.size(24.dp)) }
                }
            }
        }

        Divider()

        // --- LISTA DE ATAQUES ---
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Ataques y Conjuros")
            IconButton(onClick = { showAddAttackDialog = true }) {
                Icon(Icons.Default.AddCircle, "Añadir", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }

        if (char.attacks.isEmpty()) {
            Text("No hay ataques registrados.", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        } else {
            char.attacks.forEach { attack ->
                AttackCard(attack) {
                    val newList = char.attacks.toMutableList().apply { remove(attack) }
                    onUpdate(char.copy(attacks = newList))
                }
            }
        }
    }

    if (showAddAttackDialog) {
        AddAttackDialog(
            onDismiss = { showAddAttackDialog = false },
            onAdd = { newAttack ->
                onUpdate(char.copy(attacks = char.attacks + newAttack))
                showAddAttackDialog = false
            }
        )
    }
}

// ==========================================
// PESTAÑA 3: MAGIA (D&D 2024)
// ==========================================
@Composable
fun MagicTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    var showAddSpellDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                SectionTitle("Estadísticas de Lanzamiento")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = char.spellAbility, onValueChange = { onUpdate(char.copy(spellAbility = it)) }, label = { Text("Habilidad") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = "${char.spellSaveDC}", onValueChange = { onUpdate(char.copy(spellSaveDC = it.toIntOrNull() ?: 10)) }, label = { Text("CD Salv.") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = if (char.spellAttackBonus >= 0) "+${char.spellAttackBonus}" else "${char.spellAttackBonus}", onValueChange = { onUpdate(char.copy(spellAttackBonus = it.toIntOrNull() ?: 0)) }, label = { Text("Ataque") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        }

        Divider()
        SectionTitle("Espacios de Conjuro")
        Text("Usa +/- para definir Total. Toca círculos para Gastar.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        for (level in 1..9) {
            SpellSlotRowVisual(level, char, onUpdate)
        }

        Divider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Conjuros Preparados")
            IconButton(onClick = { showAddSpellDialog = true }) {
                Icon(Icons.Default.AddCircle, "Añadir", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }

        if (char.spells.isEmpty()) {
            Text("No hay conjuros preparados.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        } else {
            char.spells.sortedBy { it.level }.forEach { spell ->
                SpellCardDetailed(spell) {
                    val newList = char.spells.toMutableList().apply { remove(spell) }
                    onUpdate(char.copy(spells = newList))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showAddSpellDialog) {
        AddSpellDialogDetailed(
            onDismiss = { showAddSpellDialog = false },
            onAdd = { newSpell ->
                onUpdate(char.copy(spells = char.spells + newSpell))
                showAddSpellDialog = false
            }
        )
    }
}

// ==========================================
// PESTAÑA 4: EQUIPO (Monedas, Objetos, Inventario)
// ==========================================
@Composable
fun EquipmentTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    var showAddItemDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        SectionTitle("Monedas")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CoinInput("PC", char.cp, Color(0xFFB87333)) { onUpdate(char.copy(cp = it)) }
            CoinInput("PP", char.sp, Color(0xFFC0C0C0)) { onUpdate(char.copy(sp = it)) }
            CoinInput("PE", char.ep, Color(0xFF50C878)) { onUpdate(char.copy(ep = it)) }
            CoinInput("PO", char.gp, Color(0xFFFFD700)) { onUpdate(char.copy(gp = it)) }
            CoinInput("PPT", char.pp, Color(0xFFE5E4E2)) { onUpdate(char.copy(pp = it)) }
        }

        Divider()

        // Objetos Sintonizados
        SectionTitle("Objetos Sintonizados (${char.attunedItems.size}/3)")
        char.attunedItems.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(item, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val newList = char.attunedItems.toMutableList().apply { removeAt(index) }
                    onUpdate(char.copy(attunedItems = newList))
                }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
            }
        }

        if (char.attunedItems.size < 3) {
            var tempAttuned by remember { mutableStateOf("") }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = tempAttuned,
                    onValueChange = { tempAttuned = it },
                    placeholder = { Text("Nuevo objeto sintonizado") },
                    modifier = Modifier.weight(1f).height(56.dp)
                )
                IconButton(onClick = {
                    if (tempAttuned.isNotBlank()) {
                        onUpdate(char.copy(attunedItems = char.attunedItems + tempAttuned))
                        tempAttuned = ""
                    }
                }) { Icon(Icons.Default.Add, "Añadir") }
            }
        }

        Divider()

        // Inventario
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Mochila e Inventario")
            IconButton(onClick = { showAddItemDialog = true }) {
                Icon(Icons.Default.AddCircle, "Añadir", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (char.inventoryItems.isEmpty()) {
            Text("Inventario vacío.", color = Color.Gray)
        } else {
            char.inventoryItems.forEach { item ->
                InventoryItemRow(item) {
                    val newList = char.inventoryItems.toMutableList().apply { remove(item) }
                    onUpdate(char.copy(inventoryItems = newList))
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddInventoryDialog(
            onDismiss = { showAddItemDialog = false },
            onAdd = { newItem ->
                onUpdate(char.copy(inventoryItems = char.inventoryItems + newItem))
                showAddItemDialog = false
            }
        )
    }
}

// ==========================================
// PESTAÑA 5: RASGOS
// ==========================================
@Composable
fun FeaturesTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        SectionTitle("Rasgos y Dotes")
        OutlinedTextField(
            value = char.speciesTraits, onValueChange = { onUpdate(char.copy(speciesTraits = it)) },
            label = { Text("Rasgos de Especie") }, modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        OutlinedTextField(
            value = char.classFeatures, onValueChange = { onUpdate(char.copy(classFeatures = it)) },
            label = { Text("Rasgos de Clase") }, modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        OutlinedTextField(
            value = char.feats, onValueChange = { onUpdate(char.copy(feats = it)) },
            label = { Text("Dotes (Feats)") }, modifier = Modifier.fillMaxWidth().height(100.dp)
        )

        SectionTitle("Competencias")
        OutlinedTextField(
            value = char.otherProficiencies, onValueChange = { onUpdate(char.copy(otherProficiencies = it)) },
            label = { Text("Idiomas y Herramientas") }, modifier = Modifier.fillMaxWidth()
        )

        SectionTitle("Biografía")
        OutlinedTextField(value = char.personality, onValueChange = { onUpdate(char.copy(personality = it)) }, label = { Text("Personalidad") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = char.ideals, onValueChange = { onUpdate(char.copy(ideals = it)) }, label = { Text("Ideales") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = char.bonds, onValueChange = { onUpdate(char.copy(bonds = it)) }, label = { Text("Vínculos") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = char.flaws, onValueChange = { onUpdate(char.copy(flaws = it)) }, label = { Text("Defectos") }, modifier = Modifier.fillMaxWidth())

        OutlinedTextField(
            value = char.backstory, onValueChange = { onUpdate(char.copy(backstory = it)) },
            label = { Text("Historia") }, modifier = Modifier.fillMaxWidth().height(200.dp)
        )
    }
}

// ==========================================
// COMPONENTES AUXILIARES
// ==========================================

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun CompactInput(label: String, value: Int, onValueChange: (Int) -> Unit) {
    OutlinedTextField(
        value = "$value",
        onValueChange = { onValueChange(it.toIntOrNull() ?: 0) },
        label = { Text(label, fontSize = 11.sp) },
        modifier = Modifier.width(90.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
fun CoinInput(label: String, value: Int, color: Color, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        OutlinedTextField(
            value = "$value",
            onValueChange = { onValueChange(it.toIntOrNull() ?: 0) },
            modifier = Modifier.width(60.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
    }
}

@Composable
fun StatBlock(statName: String, statValue: Int, onStatChange: (Int) -> Unit, char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit, skills: List<String>) {
    val mod = (statValue - 10) / 2
    val modStr = if (mod >= 0) "+$mod" else "$mod"
    val profBonus = char.proficiencyBonus

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(statName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("$statValue ($modStr)", fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = { if(statValue>1) onStatChange(statValue - 1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Remove, null) }
                    IconButton(onClick = { if(statValue<30) onStatChange(statValue + 1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, null) }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color.Gray.copy(alpha=0.3f))

            // Salvación
            val saveKey = "Salvación $statName"
            val isSaveProficient = char.savingThrowProficiencies[saveKey] == true
            val saveTotal = mod + (if (isSaveProficient) profBonus else 0)
            SkillRow("Salvación", saveTotal, isSaveProficient) { isProf ->
                val newMap = char.savingThrowProficiencies.toMutableMap()
                newMap[saveKey] = isProf
                onUpdate(char.copy(savingThrowProficiencies = newMap))
            }

            // Skills
            skills.forEach { skillName ->
                val isProficient = char.skillProficiencies[skillName] == true
                val total = mod + (if (isProficient) profBonus else 0)
                SkillRow(skillName, total, isProficient) { isProf ->
                    val newMap = char.skillProficiencies.toMutableMap()
                    newMap[skillName] = isProf
                    onUpdate(char.copy(skillProficiencies = newMap))
                }
            }
        }
    }
}

@Composable
fun SkillRow(name: String, total: Int, isProficient: Boolean, onProficiencyChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onProficiencyChange(!isProficient) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isProficient, onCheckedChange = { onProficiencyChange(it) }, modifier = Modifier.size(32.dp))
        Text(text = if (total >= 0) "+$total" else "$total", fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
        Spacer(Modifier.width(12.dp))
        Text(text = name, fontSize = 14.sp)
    }
}

// --- Cards & Dialogs ---

@Composable
fun AttackCard(attack: Attack, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(attack.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Bono/CD: ${attack.bonus}", fontWeight = FontWeight.Bold)
                    Text("Daño: ${attack.damage}", color = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) { Text("Tipo: ${attack.damageType}", style = MaterialTheme.typography.bodySmall) }
            }
            if (attack.notes.isNotEmpty()) Text("Notas: ${attack.notes}", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
    }
}

@Composable
fun InventoryItemRow(item: InventoryItem, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text("${item.quantity}x ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(item.name, fontWeight = FontWeight.Bold)
                }
                if (item.notes.isNotEmpty()) Text(item.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Gray) }
        }
    }
}

@Composable
fun SpellSlotRowVisual(level: Int, char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    val totalKey = "$level"; val usedKey = "${level}_used"
    val totalSlots = char.spellSlots[totalKey] ?: 0; val usedSlots = char.spellSlots[usedKey] ?: 0
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(60.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp)).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            Text("Nivel $level", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (totalSlots > 0) updateSlots(char, level, totalSlots - 1, minOf(usedSlots, totalSlots - 1), onUpdate) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Remove, null) }
            Text("$totalSlots", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
            IconButton(onClick = { if (totalSlots < 9) updateSlots(char, level, totalSlots + 1, usedSlots, onUpdate) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Add, null) }
        }
        Spacer(Modifier.width(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until totalSlots) {
                val isUsed = i < usedSlots
                Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).background(if (isUsed) MaterialTheme.colorScheme.primary else Color.Transparent).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).clickable {
                        val newUsed = if (isUsed) i else i + 1; updateSlots(char, level, totalSlots, newUsed, onUpdate)
                    }, contentAlignment = Alignment.Center
                ) { if (isUsed) Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

fun updateSlots(char: PlayerCharacter, level: Int, total: Int, used: Int, onUpdate: (PlayerCharacter) -> Unit) {
    val newSlots = char.spellSlots.toMutableMap(); newSlots["$level"] = total; newSlots["${level}_used"] = used
    onUpdate(char.copy(spellSlots = newSlots))
}

@Composable
fun SpellCardDetailed(spell: Spell, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(spell.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if(spell.level == 0) "Truco" else "Nivel ${spell.level}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Gray) }
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Tiempo: ${spell.castingTime}", style = MaterialTheme.typography.bodySmall)
                    Text("Duración: ${spell.duration}", style = MaterialTheme.typography.bodySmall)
                }
                Column(Modifier.weight(1f)) {
                    Text("Comp: ${spell.components}", style = MaterialTheme.typography.bodySmall)
                    Text("Alcance: ${spell.range}", style = MaterialTheme.typography.bodySmall)
                }
            }
            if(spell.material.isNotEmpty()) Text("Mat: ${spell.material}", style = MaterialTheme.typography.labelSmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (spell.concentration) SuggestionChip(onClick = {}, label = { Text("Concentración") })
                if (spell.ritual) SuggestionChip(onClick = {}, label = { Text("Ritual") })
            }
            if (spell.notes.isNotEmpty()) Text(spell.notes, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// --- DIALOGOS ---

@Composable
fun AddAttackDialog(onDismiss: () -> Unit, onAdd: (Attack) -> Unit) {
    var name by remember { mutableStateOf("") }; var bonus by remember { mutableStateOf("") }
    var damage by remember { mutableStateOf("") }; var type by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nuevo Ataque") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(bonus, { bonus = it }, label = { Text("Bono (+5)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(damage, { damage = it }, label = { Text("Daño (1d8)") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(type, { type = it }, label = { Text("Tipo") }); OutlinedTextField(notes, { notes = it }, label = { Text("Notas") })
        }
    }, confirmButton = { Button(onClick = { onAdd(Attack(name, bonus, damage, type, notes)) }) { Text("Añadir") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun AddInventoryDialog(onDismiss: () -> Unit, onAdd: (InventoryItem) -> Unit) {
    var name by remember { mutableStateOf("") }; var quantity by remember { mutableStateOf("1") }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nuevo Objeto") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") })
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Cantidad") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(notes, { notes = it }, label = { Text("Descripción") })
        }
    }, confirmButton = { Button(onClick = { onAdd(InventoryItem(name, quantity.toIntOrNull() ?: 1, notes)) }) { Text("Añadir") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun AddSpellDialogDetailed(onDismiss: () -> Unit, onAdd: (Spell) -> Unit) {
    var name by remember { mutableStateOf("") }; var level by remember { mutableStateOf("0") }
    var castingTime by remember { mutableStateOf("") }; var duration by remember { mutableStateOf("") }; var range by remember { mutableStateOf("") }
    var components by remember { mutableStateOf("") }; var material by remember { mutableStateOf("") }
    var concentration by remember { mutableStateOf(false) }; var ritual by remember { mutableStateOf(false) }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nuevo Conjuro") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(level, { level = it }, label = { Text("Nivel (0-9)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(castingTime, { castingTime = it }, label = { Text("Tirada") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(duration, { duration = it }, label = { Text("Duración") }, modifier = Modifier.weight(1f))
                OutlinedTextField(range, { range = it }, label = { Text("Alcance") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(components, { components = it }, label = { Text("Comp (V,S,M)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(material, { material = it }, label = { Text("Materiales") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = concentration, onCheckedChange = { concentration = it }); Text("Concentración", fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Checkbox(checked = ritual, onCheckedChange = { ritual = it }); Text("Ritual", fontSize = 12.sp)
            }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth().height(100.dp))
        }
    }, confirmButton = {
        Button(onClick = { onAdd(Spell(name, level.toIntOrNull()?:0, castingTime, duration, range, components, material, concentration, ritual, notes)) }) { Text("Añadir") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}