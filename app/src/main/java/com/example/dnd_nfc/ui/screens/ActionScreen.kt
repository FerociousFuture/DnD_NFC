package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dnd_nfc.data.model.BattleState
import kotlin.random.Random

// Definimos acciones para MainActivity
sealed class CombatAction {
    data class SkillCheck(val attribute: String, val dc: Int, val bonus: Int) : CombatAction()
    data class Attack(val diceCount: Int, val diceFaces: Int, val bonus: Int) : CombatAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionScreen(
    combatList: List<BattleState>,
    onUpdateList: (List<BattleState>) -> Unit,
    onResetCombat: () -> Unit,
    onTriggerAction: (CombatAction) -> Unit
) {
    var combatPhase by remember { mutableIntStateOf(0) }
    var currentTurnIndex by remember { mutableIntStateOf(0) }
    var showDiceDialog by remember { mutableStateOf(false) }
    var showEffectDialog by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (combatPhase == 0) {
                        Text(text = "Preparación")
                    } else {
                        val activeName = if (combatList.isNotEmpty()) combatList[currentTurnIndex].name else "Nadie"
                        Column {
                            Text(text = "En Turno:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = activeName,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showDiceDialog = true }) {
                        Icon(Icons.Default.Casino, contentDescription = "Tirar Dados")
                    }
                    IconButton(onClick = {
                        combatPhase = 0
                        currentTurnIndex = 0
                        onResetCombat()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar")
                    }
                }
            )
        },
        bottomBar = {
            if (combatPhase == 1) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    val selectedCount = combatList.count { it.isSelected }
                    Button(
                        onClick = { showEffectDialog = true },
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    ) {
                        Text(text = "Modificar ($selectedCount)")
                    }
                    Button(
                        onClick = {
                            if (combatList.isNotEmpty()) {
                                currentTurnIndex = (currentTurnIndex + 1) % combatList.size
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = "Siguiente Turno")
                        Icon(Icons.Default.SkipNext, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (combatPhase == 0) {
                // FASE 0: PREPARACIÓN
                Text(
                    text = "1. Escanea Figuras NFC\n2. Ajusta bonos de iniciativa\n3. ¡Empieza!",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(combatList.size) { i ->
                        SetupRow(combatList[i]) { newBonus ->
                            val updatedList = combatList.toMutableList()
                            updatedList[i] = updatedList[i].copy(initiativeBonus = newBonus)
                            onUpdateList(updatedList)
                        }
                    }
                }
                Button(
                    onClick = {
                        val sortedList = combatList.map {
                            it.copy(initiativeTotal = Random.nextInt(1, 21) + it.initiativeBonus)
                        }.sortedByDescending { it.initiativeTotal }
                        onUpdateList(sortedList)
                        combatPhase = 1
                        currentTurnIndex = 0
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "TIRAR INICIATIVA")
                }
            } else {
                // FASE 1: COMBATE
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(combatList.size) { index ->
                        val char = combatList[index]
                        CombatRow(
                            char = char,
                            isTurn = index == currentTurnIndex,
                            onToggleSelect = {
                                val updatedList = combatList.toMutableList()
                                updatedList[index] = char.copy(isSelected = !char.isSelected)
                                onUpdateList(updatedList)
                            },
                            onActionClick = { showActionDialog = true }
                        )
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS ---
    if (showDiceDialog) {
        DiceDialog(onDismiss = { showDiceDialog = false })
    }

    if (showEffectDialog) {
        EffectDialog(
            onDismiss = { showEffectDialog = false },
            onApply = { hp, st ->
                val updatedList = combatList.map {
                    if (it.isSelected) it.copy(
                        hp = (it.hp + hp).coerceIn(0, it.maxHp),
                        status = if (st.isNotBlank()) st else it.status,
                        isSelected = false
                    ) else it
                }
                onUpdateList(updatedList)
                showEffectDialog = false
            }
        )
    }

    // NUEVO: DIÁLOGO DE ACCIÓN
    if (showActionDialog) {
        ActionSelectionDialog(
            onDismiss = { showActionDialog = false },
            onActionSelected = { action ->
                onTriggerAction(action)
                showActionDialog = false
            }
        )
    }
}

@Composable
fun CombatRow(
    char: BattleState,
    isTurn: Boolean,
    onToggleSelect: () -> Unit,
    onActionClick: () -> Unit
) {
    val borderColor = if (isTurn) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isTurn) 3.dp else 0.dp

    Card(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable { onToggleSelect() },
        elevation = CardDefaults.cardElevation(if (isTurn) 8.dp else 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.LightGray, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "${char.initiativeTotal}", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = char.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Estado: ${char.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    val hpColor = if (char.hp < char.maxHp / 2) Color.Red else Color.Green
                    Text(
                        text = "${char.hp} / ${char.maxHp} HP",
                        fontWeight = FontWeight.Bold,
                        color = hpColor
                    )
                    if (char.isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (isTurn) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text = "REALIZAR ACCIÓN")
                }
            }
        }
    }
}

@Composable
fun ActionSelectionDialog(onDismiss: () -> Unit, onActionSelected: (CombatAction) -> Unit) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var skillStat by remember { mutableStateOf("FUE") }
    var skillBonus by remember { mutableStateOf("0") }
    var attDiceCount by remember { mutableStateOf("1") }
    var attDiceFace by remember { mutableStateOf("8") }
    var attBonus by remember { mutableStateOf("3") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Elegir Acción") },
        text = {
            Column {
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text(text = "Prueba") }
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text(text = "Ataque") }
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (tabIndex == 0) {
                    Text(text = "Hacer prueba de habilidad:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val stats = listOf("FUE", "DES", "CON", "INT", "SAB", "CAR")
                        var expanded by remember { mutableStateOf(false) }
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = skillStat)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                stats.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(text = s) },
                                        onClick = {
                                            skillStat = s
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = skillBonus,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() || c == '-' }) skillBonus = it
                        },
                        label = { Text(text = "Bono Extra") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(
                        text = "Acerca la figura para tirar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    Text(text = "Atacar a un objetivo:")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = attDiceCount,
                            onValueChange = { attDiceCount = it },
                            label = { Text(text = "#") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Text(text = "d")
                        OutlinedTextField(
                            value = attDiceFace,
                            onValueChange = { attDiceFace = it },
                            label = { Text(text = "Caras") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Text(text = "+")
                        OutlinedTextField(
                            value = attBonus,
                            onValueChange = { attBonus = it },
                            label = { Text(text = "Bono") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Text(
                        text = "Acerca la figura ENEMIGA para aplicar daño.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (tabIndex == 0) {
                    onActionSelected(
                        CombatAction.SkillCheck(
                            skillStat,
                            15,
                            skillBonus.toIntOrNull() ?: 0
                        )
                    )
                } else {
                    onActionSelected(
                        CombatAction.Attack(
                            attDiceCount.toIntOrNull() ?: 1,
                            attDiceFace.toIntOrNull() ?: 8,
                            attBonus.toIntOrNull() ?: 0
                        )
                    )
                }
            }) {
                val buttonText = if (tabIndex == 0) "ESCANEAR HÉROE" else "ESCANEAR OBJETIVO"
                Text(text = buttonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}

@Composable
fun SetupRow(char: BattleState, onBonusChange: (Int) -> Unit) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = char.name, fontWeight = FontWeight.Bold)
                Text(
                    text = "HP: ${char.hp}/${char.maxHp}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedTextField(
                value = "${char.initiativeBonus}",
                onValueChange = { onBonusChange(it.toIntOrNull() ?: 0) },
                modifier = Modifier.width(60.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
fun DiceDialog(onDismiss: () -> Unit) {
    var c by remember { mutableStateOf("1") }
    var f by remember { mutableStateOf("20") }
    var b by remember { mutableStateOf("0") }
    var res by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Dados") },
        text = {
            Column {
                Row {
                    OutlinedTextField(
                        value = c,
                        onValueChange = { c = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = "#") }
                    )
                    Text(text = "d", modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(
                        value = f,
                        onValueChange = { f = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = "Caras") }
                    )
                    Text(text = "+", modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(
                        value = b,
                        onValueChange = { b = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = "Bono") }
                    )
                }
                if (res.isNotEmpty()) {
                    Text(
                        text = res,
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                var t = 0
                repeat(c.toIntOrNull() ?: 1) {
                    t += Random.nextInt(1, (f.toIntOrNull() ?: 20) + 1)
                }
                res = "${t + (b.toIntOrNull() ?: 0)}"
            }) {
                Text(text = "Tirar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cerrar")
            }
        }
    )
}

@Composable
fun EffectDialog(onDismiss: () -> Unit, onApply: (Int, String) -> Unit) {
    var hp by remember { mutableStateOf("") }
    var st by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Efecto") },
        text = {
            Column {
                OutlinedTextField(
                    value = hp,
                    onValueChange = { hp = it },
                    label = { Text(text = "HP (+/-)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = st,
                    onValueChange = { st = it },
                    label = { Text(text = "Estado") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onApply(hp.toIntOrNull() ?: 0, st) }) {
                Text(text = "Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}