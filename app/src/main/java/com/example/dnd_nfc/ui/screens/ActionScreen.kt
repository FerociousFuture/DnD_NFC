package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign // <--- IMPORTACIÓN AÑADIDA
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dnd_nfc.data.model.BattleState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

// Acciones actualizadas
sealed class CombatAction {
    data class SkillCheck(val actor: BattleState, val attribute: String, val dc: Int, val bonus: Int) : CombatAction()
    data class Attack(val diceCount: Int, val diceFaces: Int, val bonus: Int, val statAttribute: String = "NINGUNO", val statusEffect: String = "") : CombatAction()
}

// Modelo Log
data class LogEntry(val message: String, val type: String = "INFO", val timestamp: Long = System.currentTimeMillis())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionScreen(
    combatList: List<BattleState>,
    combatLog: List<LogEntry>,
    onUpdateList: (List<BattleState>) -> Unit,
    onResetCombat: () -> Unit,
    onTriggerAction: (CombatAction) -> Unit
) {
    var combatPhase by remember { mutableIntStateOf(0) }
    var currentTurnIndex by remember { mutableIntStateOf(0) }

    // Diálogos
    var showDiceDialog by remember { mutableStateOf(false) }
    var showEffectDialog by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (combatPhase == 0) Text(text = "Preparación")
                    else {
                        val activeName = if(combatList.isNotEmpty()) combatList[currentTurnIndex].name else "Nadie"
                        Column {
                            Text(text = "En Turno:", style = MaterialTheme.typography.labelSmall)
                            Text(text = activeName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showLogDialog = true }) { Icon(Icons.Default.History, contentDescription = "Historial") }
                    IconButton(onClick = { showDiceDialog = true }) { Icon(Icons.Default.Casino, contentDescription = "Dados") }
                    IconButton(onClick = { combatPhase = 0; currentTurnIndex = 0; onResetCombat() }) { Icon(Icons.Default.Refresh, contentDescription = "Reiniciar") }
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
                        modifier = Modifier.weight(1f).padding(8.dp)
                    ) { Text(text = "Modificar ($selectedCount)") }
                    Button(
                        onClick = { if (combatList.isNotEmpty()) currentTurnIndex = (currentTurnIndex + 1) % combatList.size },
                        modifier = Modifier.weight(1f).padding(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text(text = "Pasar Turno"); Icon(Icons.Default.SkipNext, contentDescription = null) }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (combatPhase == 0) {
                Text(
                    text = "1. Escanea Figuras NFC\n2. Ajusta bonos de iniciativa\n3. ¡Empieza!",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray,
                    textAlign = TextAlign.Center // AQUI SE USA TextAlign
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
                        val sortedList = combatList.map { it.copy(initiativeTotal = Random.nextInt(1, 21) + it.initiativeBonus) }.sortedByDescending { it.initiativeTotal }
                        onUpdateList(sortedList)
                        combatPhase = 1; currentTurnIndex = 0
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)
                ) { Icon(Icons.Default.PlayArrow, null); Text(text = "TIRAR INICIATIVA") }
            } else {
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

    if (showDiceDialog) DiceDialog { showDiceDialog = false }

    if (showEffectDialog) EffectDialog({ showEffectDialog = false }) { hp, st ->
        val updatedList = combatList.map { if(it.isSelected) it.copy(hp = (it.hp + hp).coerceIn(0, it.maxHp), status = if(st.isNotBlank()) st else it.status, isSelected = false) else it }
        onUpdateList(updatedList); showEffectDialog = false
    }

    if (showActionDialog && combatList.isNotEmpty()) {
        ActionSelectionDialog(
            activeActor = combatList[currentTurnIndex],
            onDismiss = { showActionDialog = false },
            onActionSelected = { action ->
                onTriggerAction(action)
                showActionDialog = false
            }
        )
    }

    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text(text = "Historial de Combate") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                    items(combatLog.reversed()) { entry ->
                        Column(Modifier.padding(vertical = 4.dp)) {
                            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
                            val color = when(entry.type) {
                                "SUCCESS" -> Color(0xFF2E7D32)
                                "ERROR" -> Color(0xFFC62828)
                                "COMBAT" -> Color(0xFF1565C0)
                                else -> Color.Black
                            }
                            Text(text = "[$time] ${entry.message}", fontSize = 12.sp, color = color, lineHeight = 14.sp)
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLogDialog = false }) { Text(text = "Cerrar") } }
        )
    }
}

@Composable
fun ActionSelectionDialog(activeActor: BattleState, onDismiss: () -> Unit, onActionSelected: (CombatAction) -> Unit) {
    var tabIndex by remember { mutableIntStateOf(0) }

    // Skill
    var skillStat by remember { mutableStateOf("FUE") }
    var skillDC by remember { mutableStateOf("15") }
    var skillBonus by remember { mutableStateOf("0") }

    // Ataque
    var attDiceCount by remember { mutableStateOf("1") }
    var attDiceFace by remember { mutableStateOf("8") }
    var attBonus by remember { mutableStateOf("0") }
    var attStat by remember { mutableStateOf("FUE") }
    var attStatus by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Acción de ${activeActor.name}")
                Text(text = "Elige el tipo de acción", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        },
        text = {
            Column {
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text(text = "Prueba") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text(text = "Ataque") })
                }
                Spacer(Modifier.height(16.dp))

                val stats = listOf("FUE", "DES", "CON", "INT", "SAB", "CAR", "NINGUNO")
                @Composable fun StatSelector(current: String, onSelect: (String)->Unit) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(text = "Atributo: $current") }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            stats.forEach { s -> DropdownMenuItem(text = { Text(text = s) }, onClick = { onSelect(s); expanded = false }) }
                        }
                    }
                }

                if (tabIndex == 0) {
                    // PRUEBA
                    Text(text = "Usar estadísticas de ${activeActor.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    StatSelector(skillStat) { skillStat = it }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = skillDC, onValueChange = { if(it.all{c->c.isDigit()}) skillDC = it },
                            label = { Text(text = "CD") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = skillBonus, onValueChange = { if(it.all{c->c.isDigit() || c=='-'}) skillBonus = it },
                            label = { Text(text = "Bono") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                } else {
                    // ATAQUE
                    Text(text = "Configurar Daño:", fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(value = attDiceCount, onValueChange = { attDiceCount = it }, label = { Text(text = "#") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Text(text = "d", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = attDiceFace, onValueChange = { attDiceFace = it }, label = { Text(text = "Caras") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Text(text = "+", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = attBonus, onValueChange = { attBonus = it }, label = { Text(text = "Extra") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(text = "Sumar estadística:", fontSize = 12.sp)
                    StatSelector(attStat) { attStat = it }

                    Spacer(Modifier.height(8.dp))
                    // NUEVO CAMPO ESTADO
                    OutlinedTextField(
                        value = attStatus,
                        onValueChange = { attStatus = it },
                        label = { Text(text = "Aplicar Estado (Opcional)") },
                        placeholder = { Text(text = "Ej: Derribado") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(text = "Se escaneará al OBJETIVO para aplicar daño.", modifier = Modifier.padding(8.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tabIndex == 0) onActionSelected(CombatAction.SkillCheck(activeActor, skillStat, skillDC.toIntOrNull()?:10, skillBonus.toIntOrNull()?:0))
                    else onActionSelected(CombatAction.Attack(attDiceCount.toIntOrNull()?:1, attDiceFace.toIntOrNull()?:8, attBonus.toIntOrNull()?:0, attStat, attStatus))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if(tabIndex==0) "TIRAR AHORA (LOCAL)" else "ESCANEAR VÍCTIMA")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Cancelar") } }
    )
}

// Helpers
@Composable fun CombatRow(char: BattleState, isTurn: Boolean, onToggleSelect: () -> Unit, onActionClick: () -> Unit) {
    val borderColor = if (isTurn) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isTurn) 3.dp else 0.dp
    Card(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth().border(borderWidth, borderColor, RoundedCornerShape(12.dp)).clickable { onToggleSelect() }, elevation = CardDefaults.cardElevation(if(isTurn) 8.dp else 2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(Color.LightGray, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text(text = "${char.initiativeTotal}", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(text = char.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium); Text(text = "Estado: ${char.status}", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                Column(horizontalAlignment = Alignment.End) { Text(text = "${char.hp} / ${char.maxHp} HP", fontWeight = FontWeight.Bold, color = if(char.hp < char.maxHp/2) Color.Red else Color.Green); if (char.isSelected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
            }
            if (isTurn) { Spacer(Modifier.height(8.dp)); Button(onClick = onActionClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Icon(Icons.Default.FlashOn, null); Spacer(Modifier.width(8.dp)); Text(text = "REALIZAR ACCIÓN") } }
        }
    }
}
@Composable fun SetupRow(char: BattleState, onBonusChange: (Int) -> Unit) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(text = char.name, fontWeight = FontWeight.Bold); Text(text = "HP: ${char.hp}/${char.maxHp}", style = MaterialTheme.typography.bodySmall) }
            OutlinedTextField(value = "${char.initiativeBonus}", onValueChange = { onBonusChange(it.toIntOrNull()?:0) }, modifier = Modifier.width(60.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
    }
}
@Composable fun DiceDialog(onDismiss: () -> Unit) {
    var c by remember { mutableStateOf("1") }; var f by remember { mutableStateOf("20") }; var b by remember { mutableStateOf("0") }; var res by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(text = "Dados") }, text = { Column {
        Row { OutlinedTextField(c, {c=it}, Modifier.weight(1f)); Text(text = "d"); OutlinedTextField(f, {f=it}, Modifier.weight(1f)); Text(text = "+"); OutlinedTextField(b, {b=it}, Modifier.weight(1f)) }
        if(res.isNotEmpty()) Text(text = res, style = MaterialTheme.typography.displayMedium, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top=16.dp))
    }}, confirmButton = { Button({var t=0; repeat(c.toIntOrNull()?:1){t+=Random.nextInt(1,(f.toIntOrNull()?:20)+1)}; res="${t+(b.toIntOrNull()?:0)}" }){Text(text = "Tirar")} }, dismissButton = { TextButton(onDismiss){Text(text = "Cerrar")} })
}
@Composable fun EffectDialog(onDismiss: () -> Unit, onApply: (Int, String) -> Unit) {
    var hp by remember { mutableStateOf("") }; var st by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(text = "Efecto") }, text = { Column { OutlinedTextField(hp, {hp=it}, label={Text(text = "HP (+/-)")}); OutlinedTextField(st, {st=it}, label={Text(text = "Estado")}) } }, confirmButton = { Button({onApply(hp.toIntOrNull()?:0, st)}){Text(text = "Aplicar")} }, dismissButton = {TextButton(onDismiss){Text(text = "Cancelar")}})
}