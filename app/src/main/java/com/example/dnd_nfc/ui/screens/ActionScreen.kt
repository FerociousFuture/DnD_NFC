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
    onUpdateList: (List<BattleState>) -> Unit, // Callback para actualizar la lista principal
    onResetCombat: () -> Unit
) {
    // FASES: 0 = Preparación (Escanear/Bonos), 1 = Combate (Turnos)
    var combatPhase by remember { mutableIntStateOf(0) }
    var currentTurnIndex by remember { mutableIntStateOf(0) }

    // DIÁLOGOS Y ESTADOS
    var showDiceDialog by remember { mutableStateOf(false) }
    var showEffectDialog by remember { mutableStateOf(false) } // Para dañar/curar a seleccionados

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
                        Icon(Icons.Default.Casino, "Dados")
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
                // BARRA DE CONTROL DE TURNO
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    // Botón Efectos (Dañar/Curar seleccionados)
                    val selectedCount = combatList.count { it.isSelected }
                    Button(
                        onClick = { showEffectDialog = true },
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).padding(8.dp)
                    ) {
                        Text("Modificar ($selectedCount)")
                    }

                    // Botón Pasar Turno
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
                // --- VISTA DE PREPARACIÓN ---
                Text(
                    "1. Escanea Figuras NFC\n2. Ajusta bonos de iniciativa\n3. ¡Empieza!",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(combatList) { char ->
                        SetupRow(char) { newBonus ->
                            val updatedList = combatList.map { if (it.id == char.id) it.copy(initiativeBonus = newBonus) else it }
                            onUpdateList(updatedList)
                        }
                    }
                }

                Button(
                    onClick = {
                        // CALCULAR INICIATIVA
                        val sortedList = combatList.map {
                            val roll = Random.nextInt(1, 21)
                            it.copy(initiativeTotal = roll + it.initiativeBonus)
                        }.sortedByDescending { it.initiativeTotal }

                        onUpdateList(sortedList)
                        combatPhase = 1 // Iniciar combate
                        currentTurnIndex = 0
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("TIRAR INICIATIVA Y EMPEZAR")
                }
            } else {
                // --- VISTA DE COMBATE ---
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(combatList.size) { index ->
                        val char = combatList[index]
                        val isTurn = index == currentTurnIndex

                        CombatRow(
                            char = char,
                            isTurn = isTurn,
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

    // --- DIÁLOGOS ---

    // 1. DADOS
    if (showDiceDialog) {
        DiceDialog(onDismiss = { showDiceDialog = false })
    }

    // 2. EFECTOS (DAÑO/CURA)
    if (showEffectDialog) {
        EffectDialog(
            onDismiss = { showEffectDialog = false },
            onApply = { hpChange, newStatus ->
                val updatedList = combatList.map { char ->
                    if (char.isSelected) {
                        val newHp = (char.hp + hpChange).coerceIn(0, char.maxHp)
                        val finalStatus = if(newStatus.isNotBlank()) newStatus else char.status
                        char.copy(hp = newHp, status = finalStatus, isSelected = false) // Deseleccionar al aplicar
                    } else char
                }
                onUpdateList(updatedList)
                showEffectDialog = false
            }
        )
    }
}

// ================= COMPONENTES VISUALES =================

@Composable
fun SetupRow(char: BattleState, onBonusChange: (Int) -> Unit) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(char.name, fontWeight = FontWeight.Bold)
                Text("HP: ${char.hp}/${char.maxHp}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Bono Inic:", style = MaterialTheme.typography.bodySmall)
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
            // Iniciativa
            Box(Modifier.size(32.dp).background(Color.LightGray, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Text("${char.initiativeTotal}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))

            // Datos
            Column(Modifier.weight(1f)) {
                Text(char.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Estado: ${char.status}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            // Vida
            Column(horizontalAlignment = Alignment.End) {
                Text("${char.hp} / ${char.maxHp} HP", fontWeight = FontWeight.Bold, color = if(char.hp < char.maxHp/2) Color.Red else Color.Green)
                if (char.isSelected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun DiceDialog(onDismiss: () -> Unit) {
    var result by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lanzador de Dados") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(result, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { result = "${Random.nextInt(1, 21)}" }) { Text("d20") }
                    Button(onClick = { result = "${Random.nextInt(1, 9) + Random.nextInt(1, 9)}" }) { Text("2d8") } // Ejemplo
                    Button(onClick = { result = "${Random.nextInt(1, 7)}" }) { Text("d6") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
fun EffectDialog(onDismiss: () -> Unit, onApply: (Int, String) -> Unit) {
    var damageInput by remember { mutableStateOf("") }
    var statusInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar Efecto a Seleccionados") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cambio de Vida (Usa - para daño, + para cura):")
                OutlinedTextField(
                    value = damageInput,
                    onValueChange = { damageInput = it },
                    placeholder = { Text("-5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text("Nuevo Estado (dejar vacío para no cambiar):")
                OutlinedTextField(value = statusInput, onValueChange = { statusInput = it }, placeholder = { Text("Derribado") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(damageInput.toIntOrNull() ?: 0, statusInput)
            }) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}