package com.example.dnd_nfc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dnd_nfc.data.model.CharacterSheet
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.remote.FirebaseService

@Composable
fun NfcReadScreen(
    nfcCharacter: CharacterSheet?, // Datos ligeros leídos del NFC
    onFullCharacterLoaded: (PlayerCharacter) -> Unit, // Callback al terminar de cargar la nube
    onScanAgainClick: () -> Unit // Para limpiar y leer otro
) {
    // Estados para la carga asíncrona
    var isLoadingCloud by remember { mutableStateOf(false) }
    var cloudError by remember { mutableStateOf<String?>(null) }

    // EFECTO: Cuando llega un personaje NFC con ID, buscamos en la nube
    LaunchedEffect(nfcCharacter) {
        if (nfcCharacter != null && nfcCharacter.id.isNotEmpty()) {
            isLoadingCloud = true
            cloudError = null

            // Llamada a Firebase
            val fullChar = FirebaseService.getCharacterById(nfcCharacter.id)

            if (fullChar != null) {
                // ¡Éxito! Tenemos la ficha completa, navegamos
                onFullCharacterLoaded(fullChar)
            } else {
                cloudError = "Error: El personaje no existe en la nube o no tienes permisos."
            }
            isLoadingCloud = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (nfcCharacter == null) {
            // ESTADO 1: ESPERANDO LECTURA NFC
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_search),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Acerca una miniatura...",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            // ESTADO 2: LECTURA EXITOSA - PROCESANDO ID
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "¡Etiqueta Detectada!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Tarjeta con la información PRELIMINAR (Lo que venía en el NFC)
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = nfcCharacter.n, // Nombre
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "ID: ${nfcCharacter.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Mostramos los stats que venían en el NFC como preview
                        Text(
                            text = "Stats (Copia Física):",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = nfcCharacter.s, // String tipo "10-12-14..."
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Estado de la descarga de Firebase
                if (isLoadingCloud) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Sincronizando ficha completa...")
                    }
                } else if (cloudError != null) {
                    // Error al buscar en la nube
                    Text(
                        text = cloudError!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onScanAgainClick) {
                        Text("Intentar otra etiqueta")
                    }
                } else {
                    // Caso raro: ID vacío en el NFC (versión vieja del formato)
                    if (nfcCharacter.id.isEmpty()) {
                        Text(
                            "Esta etiqueta tiene un formato antiguo sin ID.",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onScanAgainClick) {
                            Text("Volver")
                        }
                    }
                }
            }
        }
    }
}