package com.example.dnd_nfc.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dnd_nfc.data.local.CharacterManager
import com.example.dnd_nfc.data.model.BattleState
import com.example.dnd_nfc.data.model.PlayerCharacter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    characterId: String? = null,
    existingCharacter: PlayerCharacter? = null,
    onBack: () -> Unit,
    onWriteNfc: (PlayerCharacter) -> Unit,
    onWriteCombatNfc: (BattleState) -> Unit
) {
    val context = LocalContext.current

    // Estado del personaje (Nuevo o Existente)
    var figureData by remember {
        mutableStateOf(existingCharacter ?: PlayerCharacter(id = UUID.randomUUID().toString()))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (figureData.name.isEmpty()) "Nueva Figura" else figureData.name) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") } },
                actions = {
                    // BOTÓN GRABAR
                    Button(
                        onClick = { onWriteNfc(figureData) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Nfc, null)
                        Spacer(Modifier.width(4.dp))
                        Text("GRABAR MINI")
                    }
                }
            )
        },
        floatingActionButton = {
            // BOTÓN GUARDAR BIBLIOTECA
            FloatingActionButton(
                onClick = {
                    CharacterManager.saveCharacter(context, figureData)
                    Toast.makeText(context, "Guardado en Biblioteca", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.Save, "Guardar")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. IDENTIDAD
            Card(elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Identidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    // Nombre
                    OutlinedTextField(
                        value = figureData.name,
                        onValueChange = { figureData = figureData.copy(name = it) },
                        label = { Text("Nombre") },
                        placeholder = { Text("Ej: Goblin Jefe", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    // Descripción Corta (Reemplaza Clase/Especie)
                    OutlinedTextField(
                        value = figureData.shortDescription,
                        onValueChange = { if (it.length <= 50) figureData = figureData.copy(shortDescription = it) },
                        label = { Text("Descripción Corta (${figureData.shortDescription.length}/50)") },
                        placeholder = { Text("Ej: Guerrero Orco Nvl 3", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // 2. ESTADÍSTICAS (Botones +/-)
            Card(elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Estadísticas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatCounter("FUE", figureData.str) { figureData = figureData.copy(str = it) }
                        StatCounter("DES", figureData.dex) { figureData = figureData.copy(dex = it) }
                        StatCounter("CON", figureData.con) { figureData = figureData.copy(con = it) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatCounter("INT", figureData.int) { figureData = figureData.copy(int = it) }
                        StatCounter("SAB", figureData.wis) { figureData = figureData.copy(wis = it) }
                        StatCounter("CAR", figureData.cha) { figureData = figureData.copy(cha = it) }
                    }
                }
            }

            // 3. COMBATE
            Card(elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Combate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // AC
                        OutlinedTextField(
                            value = if(figureData.ac == 0) "" else "${figureData.ac}",
                            onValueChange = { figureData = figureData.copy(ac = it.toIntOrNull() ?: 0) },
                            label = { Text("AC") },
                            placeholder = { Text("10", color = Color.LightGray) },
                            modifier = Modifier.weight(0.8f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
                        )

                        // HP Actual
                        OutlinedTextField(
                            value = if(figureData.hpCurrent == 0) "" else "${figureData.hpCurrent}",
                            onValueChange = { figureData = figureData.copy(hpCurrent = it.toIntOrNull() ?: 0) },
                            label = { Text("HP Actual") },
                            placeholder = { Text("Max", color = Color.LightGray) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        )
                        Text("/", style = MaterialTheme.typography.headlineMedium)
                        // HP Max
                        OutlinedTextField(
                            value = if(figureData.hpMax == 0) "" else "${figureData.hpMax}",
                            onValueChange = { figureData = figureData.copy(hpMax = it.toIntOrNull() ?: 0) },
                            label = { Text("HP Max") },
                            placeholder = { Text("Max", color = Color.LightGray) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
                        )
                    }

                    // Estado
                    OutlinedTextField(
                        value = figureData.status,
                        onValueChange = { figureData = figureData.copy(status = it) },
                        label = { Text("Estado Inicial") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Normal", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.Info, null) }
                    )
                }
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

// Componente visual para control numérico (+/-)
@Composable
fun StatCounter(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = { onValueChange((value - 1).coerceAtLeast(1)) },
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Text(
                "$value",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            FilledIconButton(
                onClick = { onValueChange((value + 1).coerceAtMost(30)) },
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        val mod = (value - 10) / 2
        Text(
            if(mod >= 0) "+$mod" else "$mod",
            style = MaterialTheme.typography.bodySmall,
            color = if(mod >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}