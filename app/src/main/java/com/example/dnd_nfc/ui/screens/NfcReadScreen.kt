package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.MenuBook // Icono de Libro
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dnd_nfc.data.model.CharacterSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcReadScreen(
    character: CharacterSheet?,
    isWaiting: Boolean,
    onCreateClick: () -> Unit,
    onEditClick: () -> Unit,
    onLibraryClick: () -> Unit, // <--- NUEVO PARÁMETRO
    onSignOutClick: () -> Unit,
    onScanAgainClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lector Arcano") },
                actions = {
                    // BOTÓN BIBLIOTECA (Nuevo)
                    IconButton(onClick = onLibraryClick) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Biblioteca", tint = MaterialTheme.colorScheme.primary)
                    }
                    // BOTÓN SALIR
                    IconButton(onClick = onSignOutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        // ... (El resto del contenido dentro de Box sigue igual que antes) ...
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (character == null) {
                // MODO ESPERA
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_search),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Acerca una miniatura...", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón principal grande para crear NFC
                    Button(onClick = onCreateClick) {
                        Text("Grabar Nueva en NFC")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón secundario para ir a la biblioteca sin NFC
                    OutlinedButton(onClick = onLibraryClick) {
                        Icon(Icons.Default.MenuBook, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ver Mis Fichas Guardadas")
                    }
                }
            } else {
                // MODO FICHA LEÍDA
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjeta de Cabecera
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(character.n, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.secondary)
                            Text("${character.r} - ${character.c}", style = MaterialTheme.typography.titleMedium, color = Color.LightGray)
                        }
                    }

                    // Stats Grid
                    Text("Atributos", style = MaterialTheme.typography.titleMedium)
                    val stats = character.s.split("-")
                    val labels = listOf("FUE", "DES", "CON", "INT", "SAB", "CAR")

                    if (stats.size == 6) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox(labels[0], stats[0], Modifier.weight(1f))
                            StatBox(labels[1], stats[1], Modifier.weight(1f))
                            StatBox(labels[2], stats[2], Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox(labels[3], stats[3], Modifier.weight(1f))
                            StatBox(labels[4], stats[4], Modifier.weight(1f))
                            StatBox(labels[5], stats[5], Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Botones de Acción
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = onScanAgainClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Text("Leer Otro", color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        Button(
                            onClick = onEditClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Modificar NFC")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}