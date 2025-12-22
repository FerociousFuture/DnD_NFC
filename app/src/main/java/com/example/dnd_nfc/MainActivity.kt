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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dnd_nfc.data.local.CharacterManager
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.ScanEvent
import com.example.dnd_nfc.nfc.NfcCombatManager
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.*
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // --- ESTADOS GLOBALES ---
    // 1. Para Compartir Ficha Completa (Modo Gestión)
    private var pendingCharacterToWrite: PlayerCharacter? = null
    private var lastScanEvent: ScanEvent? = null

    // 2. Para el Combate en Tiempo Real (Modo Sala/Juego)
    private var currentAttackRequest: NfcCombatManager.AttackRequest? = null
    private var lastAttackResult by mutableStateOf<NfcCombatManager.AttackResult?>(null)

    // Callback auxiliar para notificar lectura básica
    private var onNfcScanned: ((ScanEvent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            DnD_NFCTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    // Detectamos en qué pantalla estamos para saber qué hacer con el NFC
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Configuración del callback de escaneo básico (para menús)
                    if (currentRoute == "main_menu" || currentRoute == "character_list") {
                        onNfcScanned = { event ->
                            lastScanEvent = event
                            navController.navigate("nfc_read")
                        }
                    } else {
                        onNfcScanned = null
                    }

                    NavHost(navController = navController, startDestination = "main_menu") {

                        // 0. MENÚ PRINCIPAL
                        composable("main_menu") {
                            MainMenuScreen(
                                onNavigateToCharacters = { navController.navigate("character_list") },
                                onNavigateToCampaigns = { navController.navigate("campaign_list") },
                                onHostGame = { navController.navigate("host_game") },
                                onJoinGame = { navController.navigate("join_game") }
                            )
                        }

                        // --- GESTIÓN DE PERSONAJES (OFFLINE) ---

                        // 1. Lista
                        composable("character_list") {
                            CharacterListScreen(
                                onCharacterClick = { char -> navController.navigate("character_sheet/${char.id}") },
                                onNewCharacterClick = { navController.navigate("character_sheet/new") }
                            )
                        }

                        // 2. Ficha
                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")
                            val character = if (charId != null && charId != "new") {
                                CharacterManager.getCharacterById(context, charId)
                            } else { null }

                            CharacterSheetScreen(
                                existingCharacter = character,
                                onBack = { navController.popBackStack() },
                                onWriteNfc = { charToLink ->
                                    // Preparamos escritura de ficha completa
                                    pendingCharacterToWrite = charToLink
                                    Toast.makeText(context, "¡Modo Escritura! Acerca tarjeta para grabar.", Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        // 3. Lectura de Ficha
                        composable("nfc_read") {
                            NfcReadScreen(
                                scanEvent = lastScanEvent,
                                onFullCharacterLoaded = { fullChar ->
                                    CharacterManager.saveCharacter(context, fullChar)
                                    navController.navigate("character_sheet/${fullChar.id}") {
                                        popUpTo("character_list")
                                    }
                                },
                                onScanAgainClick = { lastScanEvent = null }
                            )
                        }

                        // --- MULTIJUGADOR LOCAL (SALA WIFI) ---

                        // 4. Crear Sala (DM)
                        composable("host_game") {
                            HostGameScreen(
                                onBack = { navController.navigate("action_screen") } // El DM también juega/gestiona
                            )
                        }

                        // 5. Unirse a Sala (QR)
                        composable("join_game") {
                            JoinGameScreen(
                                onConnected = {
                                    // Al conectar, vamos directo a la pantalla de acción
                                    navController.navigate("action_screen")
                                }
                            )
                        }

                        // 6. PANTALLA DE ACCIÓN (Combate en Tiempo Real)
                        composable("action_screen") {
                            // Esta pantalla actualiza la variable global 'currentAttackRequest'
                            ActionScreen(
                                lastResult = lastAttackResult,
                                onSetupAttack = { request ->
                                    currentAttackRequest = request
                                }
                            )
                        }

                        // Placeholder
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

    // --- CEREBRO CENTRAL DEL NFC ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            // LÓGICA DE DECISIÓN: ¿Qué hacemos con la tarjeta?

            // CASO A: Estamos en PANTALLA DE ACCIÓN -> COMBATE
            if (currentAttackRequest != null) {
                // Nota: Idealmente verificaríamos también que la ruta actual sea "action_screen"
                // Pero si currentAttackRequest no es null, asumimos intención de combate.

                val result = NfcCombatManager.performAttack(tag, currentAttackRequest!!)
                if (result != null) {
                    lastAttackResult = result
                    if (result.hit) {
                        Toast.makeText(this, "⚔️ ¡Impacto! ${result.damageDealt} daño", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "🛡️ Fallo...", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error: No es una figura de combate válida.", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // CASO B: Usuario pidió "Guardar Personaje" en la ficha -> ESCRITURA COMPLETA
            if (pendingCharacterToWrite != null) {
                val success = NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)
                if (success) {
                    Toast.makeText(this, "¡Personaje guardado en tarjeta!", Toast.LENGTH_LONG).show()
                    pendingCharacterToWrite = null
                } else {
                    Toast.makeText(this, "Error: Ficha demasiado grande.", Toast.LENGTH_LONG).show()
                }
                return
            }

            // CASO C: Comportamiento por defecto -> LEER FICHA COMPLETA (Importar/Ver)
            val character = NfcManager.readCharacterFromIntent(intent)
            if (character != null) {
                // Notificamos a la navegación para ir a NfcReadScreen
                // Convertimos el PlayerCharacter a un CharacterSheet ligero para el evento
                val lightSheet = com.example.dnd_nfc.data.model.CharacterSheet(
                    id = character.id,
                    n = character.name,
                    s = "Nivel ${character.level} ${character.charClass}"
                )
                onNfcScanned?.invoke(ScanEvent(lightSheet))

                // Hack: Si estamos en el menú principal y no hay callback activo, guardamos y navegamos manualmente
                if (onNfcScanned == null) {
                    CharacterManager.saveCharacter(this, character)
                    Toast.makeText(this, "Personaje ${character.name} importado.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Tarjeta desconocida o vacía.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}