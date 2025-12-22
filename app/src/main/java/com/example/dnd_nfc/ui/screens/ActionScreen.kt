package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import com.example.dnd_nfc.data.model.BattleState
import kotlin.random.Random

@Composable
fun ActionScreen(
    // Estados de Combate
    combatList: List<BattleState>,
    onResetCombat: () -> Unit,
    onRemoveCombatant: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Dados, 1: Combate

    Column(modifier = Modifier.fillMaxSize()) {
        // --- PESTAÑAS SUPERIORES ---
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("DADOS") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("COMBATE") })
        }

        when (selectedTab) {
            0 -> DiceRollerSection()
            1 -> CombatTrackerSection(combatList, onResetCombat, onRemoveCombatant)
        }
    }
}

// ---------------------------------------------------------
// SECCIÓN 1: LANZADOR DE DADOS (Independiente)
// ---------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceRollerSection() {
    var count by remember { mutableStateOf("1") }
    var faces by remember { mutableStateOf("20") }
    var bonus by remember { mutableStateOf("0") }
    var resultText by remember { mutableStateOf("") }
    var resultValue by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Lanzador Rápido", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        // Configuración
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = count,
                onValueChange = { if (it.all { c -> c.isDigit() }) count = it },
                label = { Text("#") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text("d", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = faces,
                onValueChange = { if (it.all { c -> c.isDigit() }) faces = it },
                label = { Text("Caras") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = bonus,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) bonus = it },
                label = { Text("Bono") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón Lanzar
        Button(
            onClick = {
                val c = count.toIntOrNull() ?: 1
                val f = faces.toIntOrNull() ?: 20
                val b = bonus.toIntOrNull() ?: 0

                var total = 0
                val rolls = mutableListOf<Int>()
                repeat(c) {
                    val r = Random.nextInt(1, f + 1)
                    rolls.add(r)
                    total += r
                }
                resultValue = total + b
                resultText = "Dados: $rolls ${if(b!=0) "+ ($b)" else ""}"
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("LANZAR", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Resultado
        if (resultText.isNotEmpty()) {
            Text(resultText, color = Color.Gray)
            Text(
                text = "$resultValue",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ---------------------------------------------------------
// SECCIÓN 2: TRACKER DE COMBATE (NFC)
// ---------------------------------------------------------
@Composable
fun CombatTrackerSection(
    combatList: List<BattleState>,
    onReset: () -> Unit,
    onRemove: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Cabecera de controles
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Modo Combate Activo", fontWeight = FontWeight.Bold)
                Text("Escanea NFC para añadir", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Reiniciar")
            }
        }

        // Lista de Iniciativa
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(combatList) { combatant ->
                CombatantRow(combatant, onRemove)
            }
        }

        if (combatList.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("La lista de iniciativa está vacía.", color = Color.Gray)
            }
        }
    }
}

@Composable
fun CombatantRow(combatant: BattleState, onRemove: (String) -> Unit) {
    Card(elevation = CardDefaults.cardElevation(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Círculo de Iniciativa
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${combatant.currentInitiative ?: 0}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Datos
            Column(modifier = Modifier.weight(1f)) {
                Text(combatant.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("HP: ${combatant.hp} / ${combatant.maxHp} | AC: ${combatant.ac}", style = MaterialTheme.typography.bodyMedium)
                Text("Bono Init: ${if(combatant.initiativeMod >=0) "+" else ""}${combatant.initiativeMod}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            // Botón Borrar
            IconButton(onClick = { onRemove(combatant.id) }) {
                Text("X", fontWeight = FontWeight.Bold, color = Color.Red)
            }
        }
    }
}