package com.example.dnd_nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dnd_nfc.data.local.CharacterManager
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.ScanEvent
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.*
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // VARIABLE CLAVE: Almacena el personaje COMPLETO que queremos pasar al siguiente móvil
    private var pendingCharacterToWrite: PlayerCharacter? = null

    // Estado para comunicar el escaneo a la UI (Pantalla de Lectura)
    private var lastScanEvent: ScanEvent? = null

    // Callback de navegación (se configura desde cada pantalla)
    private var onNfcScanned: ((ScanEvent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            DnD_NFCTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    // NAVEGACIÓN PRINCIPAL
                    NavHost(navController = navController, startDestination = "main_menu") {

                        // 0. MENÚ PRINCIPAL
                        composable("main_menu") {
                            // Si escaneamos aquí, es modo LECTURA (recibir personaje)
                            onNfcScanned = { event ->
                                lastScanEvent = event
                                navController.navigate("nfc_read")
                            }

                            MainMenuScreen(
                                onNavigateToCharacters = { navController.navigate("character_list") },
                                onNavigateToCampaigns = { navController.navigate("campaign_list") }
                            )
                        }

                        // 1. LISTA DE PERSONAJES
                        composable("character_list") {
                            onNfcScanned = { event ->
                                lastScanEvent = event
                                navController.navigate("nfc_read")
                            }

                            CharacterListScreen(
                                onCharacterClick = { char ->
                                    navController.navigate("character_sheet/${char.id}")
                                },
                                onNewCharacterClick = {
                                    navController.navigate("character_sheet/new")
                                }
                            )
                        }

                        // 2. FICHA DE PERSONAJE
                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")

                            val character = if (charId != null && charId != "new") {
                                CharacterManager.getCharacterById(context, charId)
                            } else {
                                null
                            }

                            // En la ficha no navegamos automáticamente al escanear para no perder cambios
                            onNfcScanned = {}

                            CharacterSheetScreen(
                                existingCharacter = character,
                                onBack = { navController.popBackStack() },
                                onWriteNfc = { charToLink ->
                                    // MODO COMPARTIR: Preparamos el OBJETO ENTERO para escribir
                                    pendingCharacterToWrite = charToLink
                                    Toast.makeText(context, "¡Listo! Acerca el otro móvil/tarjeta para transferir.", Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        // 3. PANTALLA DE LECTURA (Resultado)
                        composable("nfc_read") {
                            NfcReadScreen(
                                scanEvent = lastScanEvent,
                                onFullCharacterLoaded = { fullChar ->
                                    // Guardar copia local si es nuevo (Importar)
                                    CharacterManager.saveCharacter(context, fullChar)
                                    // Ir a la ficha
                                    navController.navigate("character_sheet/${fullChar.id}") {
                                        popUpTo("character_list")
                                    }
                                },
                                onScanAgainClick = {
                                    lastScanEvent = null
                                }
                            )
                        }

                        // 4. LISTA DE CAMPAÑAS (Placeholder)
                        composable("campaign_list") {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Gestor de Campañas (Próximamente)")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CICLO DE VIDA NFC ---

    override fun onResume() {
        super.onResume()
        enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun enableForegroundDispatch() {
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    // --- DETECCIÓN DE ETIQUETA ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            if (tag != null) {
                if (pendingCharacterToWrite != null) {
                    // --- MODO ESCRITURA (COMPARTIR) ---
                    // Usamos la nueva función que comprime todo el personaje
                    val success = NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)
                    if (success) {
                        Toast.makeText(this, "¡Personaje transferido con éxito!", Toast.LENGTH_LONG).show()
                        pendingCharacterToWrite = null
                    } else {
                        Toast.makeText(this, "Error: Ficha demasiado grande o conexión fallida.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // --- MODO LECTURA (RECIBIR) ---
                    // Intentamos leer un personaje completo comprimido
                    val character = NfcManager.readCharacterFromIntent(intent)

                    if (character != null) {
                        // Importamos automáticamente al recibirlo
                        CharacterManager.saveCharacter(this, character)
                        Toast.makeText(this, "¡Personaje recibido: ${character.name}!", Toast.LENGTH_SHORT).show()

                        // Navegamos para mostrarlo (usamos un CharacterSheet ligero para el evento, o adaptamos ScanEvent)
                        // Para simplificar, creamos un ScanEvent asumiendo que lo actualizaste para soportar PlayerCharacter
                        // O mapeamos a la versión ligera visual:
                        val lightSheet = com.example.dnd_nfc.data.model.CharacterSheet(
                            id = character.id,
                            n = character.name,
                            s = "Nivel ${character.level} ${character.charClass}"
                        )
                        val event = ScanEvent(lightSheet)
                        onNfcScanned?.invoke(event)
                    } else {
                        Toast.makeText(this, "Tarjeta vacía o formato desconocido.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}