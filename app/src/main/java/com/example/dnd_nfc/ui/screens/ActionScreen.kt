package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.model.BattleState
import com.example.dnd_nfc.nfc.NfcCombatManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionScreen(
    lastResult: NfcCombatManager.AttackResult?,
    battleList: List<BattleState>,
    onSetupAttack: (NfcCombatManager.AttackRequest) -> Unit
) {
    // Estados para configurar la tirada
    var diceCount by remember { mutableStateOf("1") }
    var dieFaces by remember { mutableStateOf("8") } // Por defecto d8
    var bonus by remember { mutableStateOf("0") }

    // Opciones de dados comunes
    val diceOptions = listOf("4", "6", "8", "10", "12", "20", "100")
    var expandedDiceMenu by remember { mutableStateOf(false) }

    // Actualizamos la configuración para el MainActivity
    LaunchedEffect(diceCount, dieFaces, bonus) {
        onSetupAttack(
            NfcCombatManager.AttackRequest(
                diceCount = diceCount.toIntOrNull() ?: 1,
                dieFaces = dieFaces.toIntOrNull() ?: 8,
                bonus = bonus.toIntOrNull() ?: 0
            )
        )
    }

    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TIRADA DE DADOS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("(Acerca figura NFC para aplicar)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        // --- 1. CONFIGURACIÓN SIMPLE (Cantidad | Dado | Bono) ---
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CANTIDAD
                OutlinedTextField(
                    value = diceCount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) diceCount = it },
                    label = { Text("#") },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text("d", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                // TIPO DE DADO (Menu desplegable)
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = dieFaces,
                        onValueChange = {},
                        label = { Text("Cara") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expandedDiceMenu = true }) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDiceMenu)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expandedDiceMenu,
                        onDismissRequest = { expandedDiceMenu = false }
                    ) {
                        diceOptions.forEach { faces ->
                            DropdownMenuItem(
                                text = { Text("d$faces") },
                                onClick = {
                                    dieFaces = faces
                                    expandedDiceMenu = false
                                }
                            )
                        }
                    }
                }

                Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                // BONIFICACIÓN
                OutlinedTextField(
                    value = bonus,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '-' }) bonus = it },
                    label = { Text("Bono") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // --- 2. RESULTADO (FEEDBACK VISUAL) ---
        if (lastResult != null) {
            val color = if (lastResult.hit) Color(0xFFC62828) else Color.Gray
            Card(
                colors = CardDefaults.cardColors(containerColor = color),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(lastResult.message, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = "-${lastResult.damageDealt} HP",
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                Text("Configura los dados y escanea una miniatura", color = Color.Gray)
            }
        }

        Divider()

        // --- 3. LISTA DE FIGURAS EN MESA ---
        Text("Figuras en juego:", modifier = Modifier.align(Alignment.Start).padding(top = 8.dp))

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
    val barColor = when {
        hpPercent > 0.5f -> Color.Green
        hpPercent > 0.2f -> Color.Yellow
        else -> Color.Red
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entity.name, fontWeight = FontWeight.Bold)
                Text("AC: ${entity.ac}", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(120.dp)) {
                Text("${entity.hp} / ${entity.maxHp} HP", fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { hpPercent },
                    modifier = Modifier.height(8.dp).fillMaxWidth().padding(top = 4.dp),
                    color = barColor,
                    trackColor = Color.Gray
                )
            }
        }
    }
}