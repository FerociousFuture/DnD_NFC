package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
    onSignOutClick: () -> Unit,
    onScanAgainClick: () -> Unit,
    onEditClick: () -> Unit // <--- NUEVO: Callback para editar
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lector Arcano", color = MaterialTheme.colorScheme.onBackground) },
                actions = {
                    IconButton(onClick = onSignOutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (character == null) {
                // ESTADO: ESPERANDO LECTURA
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Si no tienes un icono custom, usa uno de sistema o texto
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_search),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Acerca una miniatura...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isWaiting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.height(48.dp))

                    OutlinedButton(onClick = onCreateClick) {
                        Text("O graba una nueva")
                    }
                }
            } else {
                // ESTADO: FICHA DE PERSONAJE MOSTRADA
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Encabezado
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(character.n, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.secondary)
                            Text("${character.r} - ${character.c}", style = MaterialTheme.typography.titleMedium, color = Color.LightGray)
                        }
                    }

                    // Estadísticas en Grid
                    Text("Atributos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)

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

                    // Botones inferiores
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = onScanAgainClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Text("Leer Otro", color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }

                        // BOTÓN MODIFICAR
                        Button(
                            onClick = onEditClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Modificar")
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