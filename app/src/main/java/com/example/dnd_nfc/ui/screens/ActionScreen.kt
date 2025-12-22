package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.model.BattleState
import com.example.dnd_nfc.nfc.NfcCombatManager

@Composable
fun ActionScreen(
    lastResult: NfcCombatManager.AttackResult?,
    battleList: List<BattleState>, // <--- NUEVO: Recibimos la lista de la sala
    onSetupAttack: (NfcCombatManager.AttackRequest) -> Unit
) {
    var atkBonus by remember { mutableStateOf("5") }
    var dmgDice by remember { mutableStateOf("1d8") }
    var dmgBonus by remember { mutableStateOf("2") }
    var advantage by remember { mutableStateOf(false) }

    // Actualizamos configuración
    LaunchedEffect(atkBonus, dmgDice, dmgBonus, advantage) {
        onSetupAttack(
            NfcCombatManager.AttackRequest(
                attackBonus = atkBonus.toIntOrNull() ?: 0,
                damageDice = dmgDice,
                damageBonus = dmgBonus.toIntOrNull() ?: 0,
                hasAdvantage = advantage
            )
        )
    }

    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SALA DE COMBATE", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // --- SECCIÓN 1: CONFIGURAR ATAQUE ---
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Tu Arma", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = advantage, onCheckedChange = { advantage = it })
                    Text("Ventaja")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = atkBonus, onValueChange = { atkBonus = it }, label = { Text("Atq") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dmgDice, onValueChange = { dmgDice = it }, label = { Text("Dados") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dmgBonus, onValueChange = { dmgBonus = it }, label = { Text("Dmg") }, modifier = Modifier.weight(1f))
                }
            }
        }

        // --- SECCIÓN 2: RESULTADO ÚLTIMA ACCIÓN ---
        if (lastResult != null) {
            val color = if (lastResult.hit) Color(0xFF8B0000) else Color.Gray
            Card(colors = CardDefaults.cardColors(containerColor = color), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(lastResult.message, color = Color.White, fontWeight = FontWeight.Bold)
                    if (lastResult.hit) {
                        Text("-${lastResult.damageDealt} HP a ${lastResult.enemyState.name}", color = Color.Yellow)
                    }
                }
            }
        } else {
            Text("Acerca una figura para atacar...", color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
        }

        Divider()

        // --- SECCIÓN 3: LISTA EN TIEMPO REAL (NUEVO) ---
        Text("Estado de la Mesa:", modifier = Modifier.align(Alignment.Start).padding(top = 8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(battleList) { entity ->
                BattleRow(entity)
            }
        }
    }
}

@Composable
fun BattleRow(entity: BattleState) {
    val hpPercent = if (entity.maxHp > 0) entity.hp.toFloat() / entity.maxHp.toFloat() else 0f
    val barColor = if (hpPercent > 0.5f) Color.Green else if (hpPercent > 0.2f) Color.Yellow else Color.Red

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entity.name, fontWeight = FontWeight.Bold)
                Text("AC: ${entity.ac}", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(100.dp)) {
                Text("${entity.hp}/${entity.maxHp} HP", fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { hpPercent },
                    modifier = Modifier.height(6.dp).fillMaxWidth(),
                    color = barColor,
                )
            }
        }
    }
}