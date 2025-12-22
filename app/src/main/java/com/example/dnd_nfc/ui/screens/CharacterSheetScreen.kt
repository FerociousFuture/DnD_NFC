package com.example.dnd_nfc.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // Cargamos datos o creamos nuevos por defecto
    var figureData by remember {
        mutableStateOf(existingCharacter ?: PlayerCharacter(id = UUID.randomUUID().toString(), name = "Nuevo Monstruo"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(figureData.name.ifEmpty { "Editor" }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") } },
                actions = {
                    // BOTÓN GRABAR EN NFC
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
            // BOTÓN GUARDAR EN BIBLIOTECA (LOCAL)
            FloatingActionButton(
                onClick = {
                    CharacterManager.saveCharacter(context, figureData)
                    Toast.makeText(context, "${figureData.name} guardado en Biblioteca", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.Save, "Guardar Localmente")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. NOMBRE E IDENTIDAD
            OutlinedTextField(
                value = figureData.name,
                onValueChange = { figureData = figureData.copy(name = it) },
                label = { Text("Nombre de la Figura") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Divider()

            // 2. ESTADÍSTICAS PRINCIPALES (GRID 3x2)
            Text("Estadísticas Base", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatInput("FUE", figureData.str) { figureData = figureData.copy(str = it) }
                StatInput("DES", figureData.dex) { figureData = figureData.copy(dex = it) }
                StatInput("CON", figureData.con) { figureData = figureData.copy(con = it) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatInput("INT", figureData.int) { figureData = figureData.copy(int = it) }
                StatInput("SAB", figureData.wis) { figureData = figureData.copy(wis = it) }
                StatInput("CAR", figureData.cha) { figureData = figureData.copy(cha = it) }
            }

            Divider()

            // 3. COMBATE (AC y HP)
            Text("Combate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // ARMADURA (AC)
                OutlinedTextField(
                    value = "${figureData.ac}",
                    onValueChange = { figureData = figureData.copy(ac = it.toIntOrNull() ?: 10) },
                    label = { Text("Armadura (AC)") },
                    modifier = Modifier.weight(0.8f),
                    colors = OutlinedTextFieldDefaults.colors( // <--- CORREGIDO AQUÍ
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
                )

                // VIDA (HP)
                OutlinedTextField(
                    value = "${figureData.hpCurrent}",
                    onValueChange = { figureData = figureData.copy(hpCurrent = it.toIntOrNull() ?: 0) },
                    label = { Text("HP Actual") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                )
                Text("/", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = "${figureData.hpMax}",
                    onValueChange = { figureData = figureData.copy(hpMax = it.toIntOrNull() ?: 0) },
                    label = { Text("HP Max") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
                )
            }

            // 4. ESTADO
            OutlinedTextField(
                value = figureData.status,
                onValueChange = { figureData = figureData.copy(status = it) },
                label = { Text("Estado / Notas") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Normal, Envenenado, Líder...") },
                leadingIcon = { Icon(Icons.Default.Info, null) }
            )

            Spacer(Modifier.height(60.dp)) // Espacio para el FAB
        }
    }
}

// Componente auxiliar para las cajitas de stats
@Composable
fun RowScope.StatInput(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = "$value",
        onValueChange = { onChange(it.toIntOrNull() ?: 10) },
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        modifier = Modifier.weight(1f),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
    )
}