package com.example.dnd_nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ActivityInfo
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dnd_nfc.data.local.CharacterManager
import com.example.dnd_nfc.data.model.CharacterSheet
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.ScanEvent
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.*
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // VARIABLE ÚNICA DE ESCRITURA
    private var pendingCharacterToWrite: PlayerCharacter? = null

    // ESTADO DE LECTURA
    private var lastScanEvent by mutableStateOf<ScanEvent?>(null)

    // Callback para navegación automática al escanear
    private var onNfcScanned: ((ScanEvent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Bloqueo de orientación vertical
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            DnD_NFCTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    // Configuración de navegación global para el escáner
                    DisposableEffect(Unit) {
                        onNfcScanned = { event ->
                            lastScanEvent = event
                            navController.navigate("nfc_read") {
                                launchSingleTop = true
                            }
                        }
                        onDispose { onNfcScanned = null }
                    }

                    NavHost(navController = navController, startDestination = "main_menu") {

                        // --- PANTALLA 1: MENÚ PRINCIPAL ---
                        composable("main_menu") {
                            DisposableEffect(Unit) {
                                pendingCharacterToWrite = null
                                onDispose { }
                            }

                            MainMenuScreen(
                                // Botón "Biblioteca": Va a la lista
                                onNavigateToCharacters = { navController.navigate("character_list") },
                                // Botón "Nueva Figura": Va al editor vacío
                                onNavigateToNewCharacter = { navController.navigate("character_sheet/new") },
                                // Ignorados
                                onNavigateToCampaigns = { },
                                onNavigateToCombat = { },
                                onCharacterImported = { }
                            )
                        }

                        // --- PANTALLA 1.5: BIBLIOTECA (¡ESTO FALTABA!) ---
                        composable("character_list") {
                            DisposableEffect(Unit) {
                                pendingCharacterToWrite = null
                                onDispose { }
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

                        // --- PANTALLA 2: EDITOR DE FIGURA (CREAR/EDITAR) ---
                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")
                            val character = if (charId != null && charId != "new") {
                                CharacterManager.getCharacterById(context, charId)
                            } else { null }

                            CharacterSheetScreen(
                                existingCharacter = character,
                                onBack = { navController.popBackStack() },

                                // Acción: Botón "GRABAR"
                                onWriteNfc = { charData ->
                                    pendingCharacterToWrite = charData
                                    Toast.makeText(context, "Modo GRABACIÓN: Acerca la figura NFC.", Toast.LENGTH_LONG).show()
                                },
                                onWriteCombatNfc = { }
                            )
                        }

                        // --- PANTALLA 3: LECTOR DE FIGURA (VER/ACTUALIZAR) ---
                        composable("nfc_read") {
                            NfcReadScreen(
                                scanEvent = lastScanEvent,
                                // Acción: Botón "ACTUALIZAR"
                                onFullCharacterLoaded = { updatedChar ->
                                    pendingCharacterToWrite = updatedChar
                                    Toast.makeText(context, "Modo ACTUALIZACIÓN: Acerca la misma figura.", Toast.LENGTH_LONG).show()
                                },
                                onScanAgainClick = {
                                    lastScanEvent = null
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

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

    // --- CEREBRO NFC ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            // 1. MODO ESCRITURA
            if (pendingCharacterToWrite != null) {
                if (NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)) {
                    feedback("¡Figura guardada correctamente!")
                    pendingCharacterToWrite = null
                } else {
                    feedback("Error: No se pudo escribir")
                }
                return
            }

            // 2. MODO LECTURA
            val char = NfcManager.readCharacterFromIntent(intent)
            if (char != null) {
                val event = ScanEvent(
                    character = CharacterSheet(char.id, char.name, "${char.race} ${char.charClass}"),
                    fullCharacter = char
                )

                if (onNfcScanned != null) {
                    onNfcScanned?.invoke(event)
                } else {
                    lastScanEvent = event
                }
                feedback("Figura detectada: ${char.name}")
            } else {
                feedback("Figura vacía o formato desconocido")
            }
        }
    }

    private fun feedback(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}