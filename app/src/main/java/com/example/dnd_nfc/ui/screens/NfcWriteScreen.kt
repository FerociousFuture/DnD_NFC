package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun NfcWriteScreen(
    onReadyToWrite: (String) -> Unit // Callback para avisar a la Activity
) {
    var name by remember { mutableStateOf("") }
    var charClass by remember { mutableStateOf("") }
    var race by remember { mutableStateOf("") }

    // Estadísticas
    var str by remember { mutableStateOf("10") }
    var dex by remember { mutableStateOf("10") }
    var con by remember { mutableStateOf("10") }
    var int by remember { mutableStateOf("10") }
    var wis by remember { mutableStateOf("10") }
    var cha by remember { mutableStateOf("10") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Crear Personaje NFC", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = charClass, onValueChange = { charClass = it }, label = { Text("Clase") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = race, onValueChange = { race = it }, label = { Text("Raza") }, modifier = Modifier.fillMaxWidth())

        Text("Estadísticas", style = MaterialTheme.typography.titleMedium)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatInput("FUE", str) { str = it }
            StatInput("DES", dex) { dex = it }
            StatInput("CON", con) { con = it }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatInput("INT", int) { int = it }
            StatInput("SAB", wis) { wis = it }
            StatInput("CAR", cha) { cha = it }
        }

        Button(
            onClick = {
                // Formateamos como CSV: Nombre,Clase,Raza,FUE,DES,CON,INT,SAB,CAR
                val csvData = "$name,$charClass,$race,$str,$dex,$con,$int,$wis,$cha"
                onReadyToWrite(csvData)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("GRABAR EN NFC")
        }
    }
}

@Composable
fun RowScope.StatInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f).padding(4.dp)
    )
}