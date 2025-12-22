package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.nfc.NfcCombatManager

@Composable
fun ActionScreen(
    lastResult: NfcCombatManager.AttackResult?, // Resultado del último escaneo
    onSetupAttack: (NfcCombatManager.AttackRequest) -> Unit // Callback para avisar a MainActivity
) {
    var atkBonus by remember { mutableStateOf("5") }
    var dmgDice by remember { mutableStateOf("1d8") }
    var dmgBonus by remember { mutableStateOf("2") }
    var advantage by remember { mutableStateOf(false) }

    // Cada vez que cambia algo, actualizamos la configuración en MainActivity
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
        Text("PANEL DE COMBATE", style = MaterialTheme.typography.headlineMedium)
        Text("1. Configura tu Ataque", color = Color.Gray)
        Text("2. Toca la figura enemiga", color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = advantage, onCheckedChange = { advantage = it })
                    Text("Con Ventaja")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = atkBonus, onValueChange = { atkBonus = it }, label = { Text("Bono Atq") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dmgDice, onValueChange = { dmgDice = it }, label = { Text("Dados") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dmgBonus, onValueChange = { dmgBonus = it }, label = { Text("+ Daño") }, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        if (lastResult != null) {
            // ZONA DE RESULTADO
            val color = if (lastResult.hit) Color.Red else Color.Gray
            Card(colors = CardDefaults.cardColors(containerColor = color), modifier = Modifier.fillMaxWidth().height(150.dp)) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(lastResult.message, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    if (lastResult.hit) {
                        Text("-${lastResult.damageDealt} HP", style = MaterialTheme.typography.displayMedium, color = Color.Yellow)
                        Text("${lastResult.enemyState.name} HP: ${lastResult.enemyState.hp}/${lastResult.enemyState.maxHp}", color = Color.White)
                    }
                }
            }
        } else {
            // ZONA DE ESPERA
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("Esperando Escaneo NFC...", style = MaterialTheme.typography.titleLarge, color = Color.LightGray)
            }
        }
    }
}