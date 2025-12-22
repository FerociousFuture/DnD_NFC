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

    // 1. GESTIÓN (Importar/Exportar Fichas Completas)
    private var pendingCharacterToWrite: PlayerCharacter? = null

    // 2. CREACIÓN DE FIGURA (Nuevo estado para inicializar miniaturas)
    private var pendingBattleStateToWrite: BattleState? = null

    // 3. COMBATE (Ataques en tiempo real)
    private var currentAttackRequest: NfcCombatManager.AttackRequest? = null
    private var lastAttackResult by mutableStateOf<NfcCombatManager.AttackResult?>(null)

    // 4. LECTURA SIMPLE (Menús)
    private var lastScanEvent: ScanEvent? = null
    private var onNfcScanned: ((ScanEvent) -> Unit)? = null

    // 5. MULTIJUGADOR (Lista de sala)
    private var battleStateList by mutableStateOf<List<BattleState>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Sincronizar UI con el socket
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

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Lógica de escaneo por defecto según pantalla
                    if (currentRoute == "main_menu" || currentRoute == "character_list") {
                        onNfcScanned = { event ->
                            lastScanEvent = event
                            navController.navigate("nfc_read")
                        }
                    } else {
                        onNfcScanned = null
                    }

                    NavHost(navController = navController, startDestination = "main_menu") {

                        // 0. MENÚ
                        composable("main_menu") {
                            MainMenuScreen(
                                onNavigateToCharacters = { navController.navigate("character_list") },
                                onNavigateToCampaigns = { navController.navigate("campaign_list") },
                                onHostGame = { navController.navigate("host_game") },
                                onJoinGame = { navController.navigate("join_game") }
                            )
                        }

                        // 1. LISTA
                        composable("character_list") {
                            CharacterListScreen(
                                onCharacterClick = { char -> navController.navigate("character_sheet/${char.id}") },
                                onNewCharacterClick = { navController.navigate("character_sheet/new") }
                            )
                        }

                        // 2. FICHA (Con las dos opciones de escritura NFC)
                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")
                            val character = if (charId != null && charId != "new") {
                                CharacterManager.getCharacterById(context, charId)
                            } else { null }

                            CharacterSheetScreen(
                                existingCharacter = character,
                                onBack = { navController.popBackStack() },

                                // Opción A: Guardar Backup Completo
                                onWriteNfc = { charToLink ->
                                    pendingCharacterToWrite = charToLink
                                    pendingBattleStateToWrite = null // Limpiar el otro modo
                                    Toast.makeText(context, "Modo BACKUP: Acerca tarjeta.", Toast.LENGTH_LONG).show()
                                },

                                // Opción B: Crear Figura de Combate
                                onWriteCombatNfc = { battleState ->
                                    pendingBattleStateToWrite = battleState
                                    pendingCharacterToWrite = null // Limpiar el otro modo
                                    Toast.makeText(context, "Modo FIGURA: Acerca miniatura.", Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        // 3. LECTURA
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

                        // 4. HOST (DM)
                        composable("host_game") {
                            HostGameScreen(
                                onBack = { navController.navigate("action_screen") }
                            )
                        }

                        // 5. JOIN (Jugador)
                        composable("join_game") {
                            JoinGameScreen(
                                onConnected = { navController.navigate("action_screen") }
                            )
                        }

                        // 6. ACCIÓN / COMBATE
                        composable("action_screen") {
                            if (currentAttackRequest == null) {
                                currentAttackRequest = NfcCombatManager.AttackRequest(0, "1d8", 0)
                            }

                            ActionScreen(
                                lastResult = lastAttackResult,
                                battleList = battleStateList,
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

            // --- PRIORIDAD 1: ESCRITURA INICIAL (Inicializar Miniatura) ---
            if (pendingBattleStateToWrite != null) {
                // Usamos el método writeTag que hicimos público en NfcCombatManager
                val success = NfcCombatManager.writeTag(tag, pendingBattleStateToWrite!!)
                if (success) {
                    Toast.makeText(this, "¡Figura de Combate creada!", Toast.LENGTH_LONG).show()
                    pendingBattleStateToWrite = null
                } else {
                    Toast.makeText(this, "Error al escribir figura.", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // --- PRIORIDAD 2: COMBATE (Atacar/Interactuar en Sala) ---
            if (currentAttackRequest != null) {
                val result = NfcCombatManager.performAttack(tag, currentAttackRequest!!)
                if (result != null) {
                    lastAttackResult = result
                    val icono = if (result.hit) "⚔️" else "🛡️"
                    Toast.makeText(this, "$icono ${result.message}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Figura no válida para combate.", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // --- PRIORIDAD 3: BACKUP (Guardar Ficha Completa) ---
            if (pendingCharacterToWrite != null) {
                val success = NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)
                if (success) {
                    Toast.makeText(this, "¡Backup guardado!", Toast.LENGTH_LONG).show()
                    pendingCharacterToWrite = null
                } else {
                    Toast.makeText(this, "Error: Ficha demasiado grande.", Toast.LENGTH_LONG).show()
                }
                return
            }

            // --- PRIORIDAD 4: LECTURA DEFAULT (Importar) ---
            val character = NfcManager.readCharacterFromIntent(intent)
            if (character != null) {
                val lightSheet = com.example.dnd_nfc.data.model.CharacterSheet(
                    id = character.id,
                    n = character.name,
                    s = "Nivel ${character.level} ${character.charClass}"
                )

                if (onNfcScanned != null) {
                    onNfcScanned?.invoke(ScanEvent(lightSheet))
                } else {
                    CharacterManager.saveCharacter(this, character)
                    Toast.makeText(this, "Personaje importado: ${character.name}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Tarjeta desconocida.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}