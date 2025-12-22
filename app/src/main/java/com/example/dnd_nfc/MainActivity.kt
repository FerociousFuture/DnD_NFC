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
import com.example.dnd_nfc.data.model.BattleState
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.ScanEvent
import com.example.dnd_nfc.network.GameClient
import com.example.dnd_nfc.nfc.NfcCombatManager
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.*
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // --- ESTADOS GLOBALES ---

    // 1. GESTIÓN (Importar/Exportar Fichas)
    private var pendingCharacterToWrite: PlayerCharacter? = null
    private var lastScanEvent: ScanEvent? = null

    // 2. COMBATE (Ataques y Batalla)
    private var currentAttackRequest: NfcCombatManager.AttackRequest? = null
    private var lastAttackResult by mutableStateOf<NfcCombatManager.AttackResult?>(null)

    // 3. MULTIJUGADOR (Lista de la sala)
    private var battleStateList by mutableStateOf<List<BattleState>>(emptyList())

    // Callback auxiliar para cuando estamos en menús y solo queremos leer
    private var onNfcScanned: ((ScanEvent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // CONECTAR EL CLIENTE DE RED CON LA INTERFAZ
        // Cuando llega una actualización de la sala, repintamos la UI
        GameClient.onGameStateReceived = { newList ->
            runOnUiThread {
                battleStateList = newList
            }
        }

        setContent {
            DnD_NFCTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    // Detectamos en qué pantalla estamos
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Configuración dinámica del comportamiento NFC según la pantalla
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

                        composable("character_list") {
                            CharacterListScreen(
                                onCharacterClick = { char -> navController.navigate("character_sheet/${char.id}") },
                                onNewCharacterClick = { navController.navigate("character_sheet/new") }
                            )
                        }

                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")
                            val character = if (charId != null && charId != "new") {
                                CharacterManager.getCharacterById(context, charId)
                            } else { null }

                            CharacterSheetScreen(
                                existingCharacter = character,
                                onBack = { navController.popBackStack() },
                                onWriteNfc = { charToLink ->
                                    // Activamos modo escritura de ficha completa
                                    pendingCharacterToWrite = charToLink
                                    Toast.makeText(context, "¡Listo! Acerca una tarjeta para guardar.", Toast.LENGTH_LONG).show()
                                }
                            )
                        }

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

                        // 1. Crear Sala (DM)
                        composable("host_game") {
                            HostGameScreen(
                                onBack = {
                                    // Al volver, el DM va a la pantalla de combate como un jugador más (pero con poderes)
                                    navController.navigate("action_screen")
                                }
                            )
                        }

                        // 2. Unirse a Sala (QR)
                        composable("join_game") {
                            JoinGameScreen(
                                onConnected = {
                                    // Al conectar, vamos directo al combate
                                    navController.navigate("action_screen")
                                }
                            )
                        }

                        // 3. PANTALLA DE ACCIÓN (Combate en Tiempo Real)
                        composable("action_screen") {
                            // Si salimos de aquí, limpiamos la intención de ataque
                            if (currentAttackRequest == null) {
                                // Default setup
                                currentAttackRequest = NfcCombatManager.AttackRequest(0, "1d8", 0)
                            }

                            ActionScreen(
                                lastResult = lastAttackResult,
                                battleList = battleStateList, // <--- LISTA EN TIEMPO REAL
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

            // DECISIÓN: ¿Qué hacemos con la tarjeta?

            // PRIORIDAD 1: COMBATE (Si estamos en ActionScreen y configuramos un ataque)
            if (currentAttackRequest != null) {
                // Intentamos procesar como Ataque
                val result = NfcCombatManager.performAttack(tag, currentAttackRequest!!)
                if (result != null) {
                    lastAttackResult = result
                    val icono = if (result.hit) "⚔️" else "🛡️"
                    Toast.makeText(this, "$icono ${result.message}", Toast.LENGTH_SHORT).show()
                } else {
                    // Si falló el ataque, tal vez intentó leer una tarjeta que no es de batalla
                    Toast.makeText(this, "Esta figura no tiene datos de combate.", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // PRIORIDAD 2: ESCRITURA (Si pulsamos "Guardar" en una ficha)
            if (pendingCharacterToWrite != null) {
                val success = NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)
                if (success) {
                    Toast.makeText(this, "¡Personaje guardado!", Toast.LENGTH_LONG).show()
                    pendingCharacterToWrite = null
                } else {
                    Toast.makeText(this, "Error: Ficha demasiado grande.", Toast.LENGTH_LONG).show()
                }
                return
            }

            // PRIORIDAD 3: LECTURA/IMPORTAR (Por defecto en menús)
            val character = NfcManager.readCharacterFromIntent(intent)
            if (character != null) {
                // Notificamos para ir a NfcReadScreen
                val lightSheet = com.example.dnd_nfc.data.model.CharacterSheet(
                    id = character.id,
                    n = character.name,
                    s = "Nivel ${character.level} ${character.charClass}"
                )

                if (onNfcScanned != null) {
                    onNfcScanned?.invoke(ScanEvent(lightSheet))
                } else {
                    // Si no hay callback (ej: estamos en una pantalla rara), guardamos y avisamos
                    CharacterManager.saveCharacter(this, character)
                    Toast.makeText(this, "Personaje importado: ${character.name}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Tarjeta desconocida.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}