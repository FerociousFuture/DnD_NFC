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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionScreen(
    combatList: List<BattleState>,
    onUpdateList: (List<BattleState>) -> Unit,
    onResetCombat: () -> Unit
) {
    var combatPhase by remember { mutableIntStateOf(0) }
    var currentTurnIndex by remember { mutableIntStateOf(0) }
    var showDiceDialog by remember { mutableStateOf(false) }
    var showEffectDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (combatPhase == 0) Text("Preparación")
                    else {
                        val activeName = if(combatList.isNotEmpty()) combatList[currentTurnIndex].name else "Nadie"
                        Column {
                            Text("En Turno:", style = MaterialTheme.typography.labelSmall)
                            Text(activeName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showDiceDialog = true }) {
                        Icon(Icons.Default.Casino, "Tirar Dados")
                    }
                    IconButton(onClick = {
                        combatPhase = 0
                        currentTurnIndex = 0
                        onResetCombat()
                    }) {
                        Icon(Icons.Default.Refresh, "Reiniciar")
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
                        modifier = Modifier.weight(1f).padding(8.dp)
                    ) {
                        Text("Modificar ($selectedCount)")
                    }
                    Button(
                        onClick = {
                            if (combatList.isNotEmpty()) {
                                currentTurnIndex = (currentTurnIndex + 1) % combatList.size
                            }
                        },
                        modifier = Modifier.weight(1f).padding(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Siguiente Turno")
                        Icon(Icons.Default.SkipNext, null)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (combatPhase == 0) {
                Text(
                    "1. Escanea Figuras NFC\n2. Ajusta bonos de iniciativa\n3. ¡Empieza!",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
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
                            val roll = Random.nextInt(1, 21)
                            it.copy(initiativeTotal = roll + it.initiativeBonus)
                        }.sortedByDescending { it.initiativeTotal }
                        onUpdateList(sortedList)
                        combatPhase = 1
                        currentTurnIndex = 0
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("TIRAR INICIATIVA Y EMPEZAR")
                }
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
                            }
                        )
                    }
                }
            }
        }
    }

    // --- NUEVO DIÁLOGO DE DADOS PERSONALIZADO ---
    if (showDiceDialog) {
        DiceDialog(onDismiss = { showDiceDialog = false })
    }

    if (showEffectDialog) {
        EffectDialog(
            onDismiss = { showEffectDialog = false },
            onApply = { hpChange, newStatus ->
                val updatedList = combatList.map { char ->
                    if (char.isSelected) {
                        val newHp = (char.hp + hpChange).coerceIn(0, char.maxHp)
                        val finalStatus = if(newStatus.isNotBlank()) newStatus else char.status
                        char.copy(hp = newHp, status = finalStatus, isSelected = false)
                    } else char
                }
                onUpdateList(updatedList)
                showEffectDialog = false
            }
        )
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun SetupRow(char: BattleState, onBonusChange: (Int) -> Unit) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(char.name, fontWeight = FontWeight.Bold)
                Text("HP: ${char.hp}/${char.maxHp}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Bono:", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = "${char.initiativeBonus}",
                onValueChange = { onBonusChange(it.toIntOrNull() ?: 0) },
                modifier = Modifier.width(60.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }
}

@Composable
fun CombatRow(char: BattleState, isTurn: Boolean, onToggleSelect: () -> Unit) {
    val borderColor = if (isTurn) MaterialTheme.colorScheme.primary else Color.Transparent
    val bgColor = if (char.isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onToggleSelect() },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(Color.LightGray, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Text("${char.initiativeTotal}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(char.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Estado: ${char.status}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${char.hp} / ${char.maxHp} HP", fontWeight = FontWeight.Bold, color = if(char.hp < char.maxHp/2) Color.Red else Color.Green)
                if (char.isSelected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// === DIÁLOGO DE DADOS CORREGIDO ===
@Composable
fun DiceDialog(onDismiss: () -> Unit) {
    var count by remember { mutableStateOf("1") }
    var faces by remember { mutableStateOf("20") }
    var bonus by remember { mutableStateOf("0") }
    var resultText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tirada Personalizada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Fila de Entradas
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // CANTIDAD
                    OutlinedTextField(
                        value = count,
                        onValueChange = { if(it.all { c -> c.isDigit() }) count = it },
                        label = { Text("#") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    // CARAS
                    OutlinedTextField(
                        value = faces,
                        onValueChange = { if(it.all { c -> c.isDigit() }) faces = it },
                        label = { Text("d") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    // BONO
                    OutlinedTextField(
                        value = bonus,
                        onValueChange = { if(it.all { c -> c.isDigit() || c == '-' }) bonus = it },
                        label = { Text("+") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Resultado en grande
                if (resultText.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(resultText, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val c = count.toIntOrNull() ?: 1
                val f = faces.toIntOrNull() ?: 20
                val b = bonus.toIntOrNull() ?: 0

                var total = 0
                repeat(c) {
                    total += Random.nextInt(1, f + 1)
                }
                val final = total + b
                resultText = "$final"
            }) { Text("TIRAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
fun EffectDialog(onDismiss: () -> Unit, onApply: (Int, String) -> Unit) {
    var damageInput by remember { mutableStateOf("") }
    var statusInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar Efecto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vida (+Cura / -Daño):")
                OutlinedTextField(
                    value = damageInput,
                    onValueChange = { damageInput = it },
                    placeholder = { Text("-5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text("Estado:")
                OutlinedTextField(value = statusInput, onValueChange = { statusInput = it }, placeholder = { Text("Derribado") })
            }
        },
        confirmButton = { Button(onClick = { onApply(damageInput.toIntOrNull() ?: 0, statusInput) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}