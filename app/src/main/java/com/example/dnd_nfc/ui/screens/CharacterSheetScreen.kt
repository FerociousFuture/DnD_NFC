package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dnd_nfc.data.local.CharacterManager
import com.example.dnd_nfc.data.model.Attack
import com.example.dnd_nfc.data.model.InventoryItem
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.Spell
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    characterId: String? = null,
    existingCharacter: PlayerCharacter? = null,
    onBack: () -> Unit,
    onWriteNfc: (PlayerCharacter) -> Unit
) {
    val context = LocalContext.current
    // Si no existe, creamos uno nuevo. Si existe, usamos el pasado por parámetro.
    var charData by remember { mutableStateOf(existingCharacter ?: PlayerCharacter(id = UUID.randomUUID().toString())) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Estados para control de flujo
    var showSaveDialog by remember { mutableStateOf(false) }

    val tabs = listOf("General", "Combate", "Magia", "Equipo", "Rasgos")
    val icons = listOf(
        Icons.Default.Face,
        Icons.Default.Shield,
        Icons.Default.AutoStories,
        Icons.Default.Backpack,
        Icons.Default.Description
    )

    // Validación mínima para permitir guardar/vincular
    val isFormValid = charData.name.isNotBlank() && charData.charClass.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (charData.name.isEmpty()) "Nuevo Personaje" else charData.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") }
                },
                actions = {
                    // Botón NFC
                    IconButton(onClick = { onWriteNfc(charData) }, enabled = isFormValid) {
                        Icon(Icons.Default.Nfc, "Vincular", tint = if (isFormValid) MaterialTheme.colorScheme.onSurface else Color.Gray)
                    }

                    // Botón Guardar (LOCAL)
                    IconButton(onClick = { showSaveDialog = true }, enabled = isFormValid) {
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

            if (!isFormValid) {
                Text(
                    text = "* Faltan datos obligatorios: Nombre y Clase",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            when (selectedTab) {
                0 -> GeneralTab(charData) { charData = it }
                1 -> CombatTab(charData) { charData = it }
                2 -> MagicTab(charData) { charData = it }
                3 -> EquipmentTab(charData) { charData = it }
                4 -> FeaturesTab(charData) { charData = it }
            }
        }
    }

    // --- DIÁLOGO DE CONFIRMACIÓN DE GUARDADO (LOCAL) ---
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            icon = { Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("¿Guardar Personaje?") },
            text = {
                Text("Los datos se guardarán en la memoria de este dispositivo.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        // Guardado Local usando CharacterManager
                        val success = CharacterManager.saveCharacter(context, charData)
                        if (success) {
                            onBack() // Volvemos a la lista si se guardó bien
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ==========================================
// PESTAÑA 1: GENERAL
// ==========================================
@Composable
fun GeneralTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        SectionTitle("Identidad")
        OutlinedTextField(
            value = char.name,
            onValueChange = { onUpdate(char.copy(name = it)) },
            label = { Text("Nombre*") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = char.name.isBlank(),
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = char.charClass,
                onValueChange = { onUpdate(char.copy(charClass = it)) },
                label = { Text("Clase*") },
                isError = char.charClass.isBlank(),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(value = "${char.level}", onValueChange = { onUpdate(char.copy(level = it.toIntOrNull() ?: 1)) }, label = { Text("Nivel") }, modifier = Modifier.weight(0.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.race, onValueChange = { onUpdate(char.copy(race = it)) }, label = { Text("Especie") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = char.subclass, onValueChange = { onUpdate(char.copy(subclass = it)) }, label = { Text("Subclase") }, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = char.background, onValueChange = { onUpdate(char.copy(background = it)) }, label = { Text("Trasfondo") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = char.size, onValueChange = { onUpdate(char.copy(size = it)) }, label = { Text("Tamaño") }, modifier = Modifier.weight(0.5f))
            OutlinedTextField(value = char.xp, onValueChange = { onUpdate(char.copy(xp = it)) }, label = { Text("XP") }, modifier = Modifier.weight(0.5f))
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // --- ESTADÍSTICAS VITALES ---
        SectionTitle("Estado Vital")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactInput("CA", char.ac) { onUpdate(char.copy(ac = it)) }
            CompactInput("Inic.", char.initiative) { onUpdate(char.copy(initiative = it)) }
            CompactInput("Vel.", char.speed) { onUpdate(char.copy(speed = it)) }
            CompactInput("Perc.Pas", char.passivePerception) { onUpdate(char.copy(passivePerception = it)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = "${char.hpCurrent}", onValueChange = { onUpdate(char.copy(hpCurrent = it.toIntOrNull() ?: 0)) }, label = { Text("HP Actual") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = "${char.hpMax}", onValueChange = { onUpdate(char.copy(hpMax = it.toIntOrNull() ?: 0)) }, label = { Text("HP Max") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = "${char.tempHp}", onValueChange = { onUpdate(char.copy(tempHp = it.toIntOrNull() ?: 0)) }, label = { Text("Temp") }, modifier = Modifier.weight(0.7f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

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

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // --- ATRIBUTOS ---
        SectionTitle("Atributos & Habilidades")

        StatBlock("FUERZA", char.str, { onUpdate(char.copy(str = it)) }, char, onUpdate, listOf("Atletismo"))
        StatBlock("DESTREZA", char.dex, { onUpdate(char.copy(dex = it)) }, char, onUpdate, listOf("Acrobacias", "Juego de Manos", "Sigilo"))
        StatBlock("CONSTITUCIÓN", char.con, { onUpdate(char.copy(con = it)) }, char, onUpdate, listOf())
        StatBlock("INTELIGENCIA", char.int, { onUpdate(char.copy(int = it)) }, char, onUpdate, listOf("C. Arcano", "Historia", "Investigación", "Naturaleza", "Religión"))
        StatBlock("SABIDURÍA", char.wis, { onUpdate(char.copy(wis = it)) }, char, onUpdate, listOf("T. con Animales", "Medicina", "Percepción", "Perspicacia", "Supervivencia"))
        StatBlock("CARISMA", char.cha, { onUpdate(char.copy(cha = it)) }, char, onUpdate, listOf("Engaño", "Interpretación", "Intimidación", "Persuasión"))

        Spacer(Modifier.height(24.dp))
    }
}

// ==========================================
// PESTAÑA 2: COMBATE
// ==========================================
@Composable
fun CombatTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    var showAddAttackDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        SectionTitle("Resumen de Combate")
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
// PESTAÑA 3: MAGIA (DISEÑO DASHBOARD)
// ==========================================
@Composable
fun MagicTab(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    var showAddSpellDialog by remember { mutableStateOf(false) }
    val spellsByLevel = char.spells.groupBy { it.level }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. CABECERA VISUAL
        MagicStatsHeader(char, onUpdate)

        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

        // 2. ACORDEÓN POR NIVELES
        // Nivel 0 (Trucos)
        SpellLevelSection(0, char, spellsByLevel[0] ?: emptyList(), onUpdate)

        // Niveles 1 al 9
        for (lvl in 1..9) {
            SpellLevelSection(lvl, char, spellsByLevel[lvl] ?: emptyList(), onUpdate)
        }

        // Botón Final
        Button(
            onClick = { showAddSpellDialog = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.AutoAwesome, null)
            Spacer(Modifier.width(8.dp))
            Text("Aprender Nuevo Conjuro")
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
// PESTAÑA 4: EQUIPO
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Inventario")
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
// COMPONENTES AUXILIARES (UI)
// ==========================================

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun CompactInput(label: String, value: Int, onValueChange: (Int) -> Unit) {
    OutlinedTextField(
        value = "$value", onValueChange = { onValueChange(it.toIntOrNull() ?: 0) },
        label = { Text(label, fontSize = 11.sp) }, modifier = Modifier.width(90.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
    )
}

@Composable
fun CoinInput(label: String, value: Int, color: Color, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        OutlinedTextField(
            value = "$value", onValueChange = { onValueChange(it.toIntOrNull() ?: 0) },
            modifier = Modifier.width(60.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
    }
}

@Composable
fun StatBlock(statName: String, statValue: Int, onStatChange: (Int) -> Unit, char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit, skills: List<String>) {
    val mod = (statValue - 10) / 2
    val modStr = if (mod >= 0) "+$mod" else "$mod"
    val profBonus = char.proficiencyBonus

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
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

// --- VISUAL MAGIC COMPONENTS ---

@Composable
fun MagicStatsHeader(char: PlayerCharacter, onUpdate: (PlayerCharacter) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MagicStatCard("Habilidad", char.spellAbility.ifEmpty { "INT" }, Modifier.weight(1f), color = MaterialTheme.colorScheme.primaryContainer) {
            OutlinedTextField(value = char.spellAbility, onValueChange = { onUpdate(char.copy(spellAbility = it)) }, textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center))
        }
        MagicStatCard("CD Salv.", "${char.spellSaveDC}", Modifier.weight(1f), isBig = true, color = MaterialTheme.colorScheme.tertiaryContainer) {
            OutlinedTextField(value = "${char.spellSaveDC}", onValueChange = { onUpdate(char.copy(spellSaveDC = it.toIntOrNull()?:10)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center))
        }
        MagicStatCard("Ataque", if(char.spellAttackBonus >= 0) "+${char.spellAttackBonus}" else "${char.spellAttackBonus}", Modifier.weight(1f), color = MaterialTheme.colorScheme.secondaryContainer) {
            OutlinedTextField(value = "${char.spellAttackBonus}", onValueChange = { onUpdate(char.copy(spellAttackBonus = it.toIntOrNull()?:0)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center))
        }
    }
}

@Composable
fun MagicStatCard(label: String, value: String, modifier: Modifier = Modifier, isBig: Boolean = false, color: Color, editContent: @Composable () -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    Card(modifier = modifier.clickable { isEditing = !isEditing }.height(if (isEditing) 90.dp else 80.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (isEditing) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Box(Modifier.padding(4.dp)) { editContent() }
            } else {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(text = value, style = if(isBig) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SpellLevelSection(level: Int, char: PlayerCharacter, spells: List<Spell>, onUpdate: (PlayerCharacter) -> Unit) {
    var isExpanded by remember { mutableStateOf(true) }
    val totalKey = "$level"; val usedKey = "${level}_used"
    val totalSlots = char.spellSlots[totalKey] ?: 0; val usedSlots = char.spellSlots[usedKey] ?: 0
    val title = if (level == 0) "Trucos" else "Nivel $level"

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (level > 0 && totalSlots > 0) Text("${totalSlots - usedSlots} libres", style = MaterialTheme.typography.labelSmall)
            }

            if (isExpanded) {
                Spacer(Modifier.height(12.dp))
                if (level > 0) {
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Espacios:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (totalSlots > 0) updateSlots(char, level, totalSlots - 1, minOf(usedSlots, totalSlots - 1), onUpdate) }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Remove, null) }
                                Text("Max: $totalSlots", fontSize = 12.sp)
                                IconButton(onClick = { if (totalSlots < 9) updateSlots(char, level, totalSlots + 1, usedSlots, onUpdate) }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Add, null) }
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 0 until totalSlots) {
                                val isUsed = i < usedSlots
                                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(if (isUsed) Color.Gray.copy(alpha=0.2f) else MaterialTheme.colorScheme.primary).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape).clickable {
                                    updateSlots(char, level, totalSlots, if (isUsed) i else i + 1, onUpdate)
                                }, contentAlignment = Alignment.Center) {
                                    if (isUsed) Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (spells.isEmpty()) Text("Sin conjuros.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                else spells.forEach { spell ->
                    SpellCardCompact(spell) { onUpdate(char.copy(spells = char.spells - spell)) }
                }
            }
        }
    }
}

@Composable
fun SpellCardCompact(spell: Spell, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(spell.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row {
                    if (spell.concentration) Text("C", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp, modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.secondary, CircleShape).padding(horizontal=4.dp))
                    Spacer(Modifier.width(4.dp))
                    if (spell.ritual) Text("R", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp, modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.tertiary, CircleShape).padding(horizontal=4.dp))
                }
            }
            if (expanded) {
                Divider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) { Text("Tiempo: ${spell.castingTime}", fontSize = 11.sp); Text("Duración: ${spell.duration}", fontSize = 11.sp) }
                    Column(Modifier.weight(1f)) { Text("Alcance: ${spell.range}", fontSize = 11.sp); Text("Comp: ${spell.components}", fontSize = 11.sp) }
                }
                if (spell.notes.isNotEmpty()) Text(spell.notes, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(top = 4.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = onDelete) { Text("Olvidar", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                }
            }
        }
    }
}

fun updateSlots(char: PlayerCharacter, level: Int, total: Int, used: Int, onUpdate: (PlayerCharacter) -> Unit) {
    val newSlots = char.spellSlots.toMutableMap(); newSlots["$level"] = total; newSlots["${level}_used"] = used
    onUpdate(char.copy(spellSlots = newSlots))
}

// --- CARDS & DIALOGOS ---

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
                Column(Modifier.weight(1f)) { Text("Bono/CD: ${attack.bonus}", fontWeight = FontWeight.Bold); Text("Daño: ${attack.damage}", color = MaterialTheme.colorScheme.primary) }
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
                Row { Text("${item.quantity}x ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(item.name, fontWeight = FontWeight.Bold) }
                if (item.notes.isNotEmpty()) Text(item.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Gray) }
        }
    }
}

@Composable
fun AddAttackDialog(onDismiss: () -> Unit, onAdd: (Attack) -> Unit) {
    var name by remember { mutableStateOf("") }; var bonus by remember { mutableStateOf("") }
    var damage by remember { mutableStateOf("") }; var type by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nuevo Ataque") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(bonus, { bonus = it }, label = { Text("Bono") }, modifier = Modifier.weight(1f)); OutlinedTextField(damage, { damage = it }, label = { Text("Daño") }, modifier = Modifier.weight(1f)) }
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
            Row { OutlinedTextField(level, { level = it }, label = { Text("Nivel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); OutlinedTextField(castingTime, { castingTime = it }, label = { Text("Tirada") }, modifier = Modifier.weight(1f)) }
            Row { OutlinedTextField(duration, { duration = it }, label = { Text("Duración") }, modifier = Modifier.weight(1f)); OutlinedTextField(range, { range = it }, label = { Text("Alcance") }, modifier = Modifier.weight(1f)) }
            OutlinedTextField(components, { components = it }, label = { Text("Comp (V,S,M)") }); OutlinedTextField(material, { material = it }, label = { Text("Materiales") })
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(concentration, { concentration = it }); Text("Conc."); Spacer(Modifier.width(8.dp)); Checkbox(ritual, { ritual = it }); Text("Ritual") }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.height(100.dp))
        }
    }, confirmButton = { Button(onClick = { onAdd(Spell(name, level.toIntOrNull()?:0, castingTime, duration, range, components, material, concentration, ritual, notes)) }) { Text("Añadir") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}