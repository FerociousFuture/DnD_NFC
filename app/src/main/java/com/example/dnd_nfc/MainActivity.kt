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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dnd_nfc.data.local.CharacterManager
import com.example.dnd_nfc.data.model.BattleState
import com.example.dnd_nfc.data.model.CharacterSheet
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.ScanEvent
import com.example.dnd_nfc.nfc.NfcCombatManager
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.*
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // MODOS DE OPERACIÓN
    private var pendingCharacterToWrite: PlayerCharacter? = null
    private var pendingBattleStateToWrite: BattleState? = null
    private var currentAttackRequest: NfcCombatManager.AttackRequest? = null

    // ESTADOS UI
    private var lastAttackResult by mutableStateOf<NfcCombatManager.AttackResult?>(null)
    private var lastScanEvent: ScanEvent? = null
    private var onNfcScanned: ((ScanEvent) -> Unit)? = null

    // Lista Local de Combate
    private var localBattleList by mutableStateOf<List<BattleState>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            DnD_NFCTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                    // Redirección de escaneo básico
                    if (currentRoute == "main_menu" || currentRoute == "character_list") {
                        onNfcScanned = { event ->
                            lastScanEvent = event
                            navController.navigate("nfc_read")
                        }
                    } else {
                        onNfcScanned = null
                    }

                    NavHost(navController = navController, startDestination = "main_menu") {

                        // MENÚ PRINCIPAL
                        composable("main_menu") {
                            MainMenuScreen(
                                onNavigateToCharacters = { navController.navigate("character_list") },
                                onNavigateToCampaigns = { navController.navigate("campaign_list") },
                                onNavigateToCombat = { navController.navigate("action_screen") }
                            )
                        }

                        // LISTAS Y FICHAS
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

                            DisposableEffect(Unit) {
                                onDispose {
                                    pendingCharacterToWrite = null
                                    pendingBattleStateToWrite = null
                                }
                            }

                            CharacterSheetScreen(
                                existingCharacter = character,
                                onBack = { navController.popBackStack() },
                                onWriteNfc = { charToLink ->
                                    pendingCharacterToWrite = charToLink
                                    pendingBattleStateToWrite = null
                                    Toast.makeText(context, "Modo BACKUP: Acerca tarjeta.", Toast.LENGTH_SHORT).show()
                                },
                                onWriteCombatNfc = { battleState ->
                                    pendingBattleStateToWrite = battleState
                                    pendingCharacterToWrite = null
                                    Toast.makeText(context, "Modo FIGURA: Acerca miniatura.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // PANTALLAS DE UTILIDAD
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

                        composable("campaign_list") {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Próximamente") }
                        }

                        // MESA DE COMBATE LOCAL
                        composable("action_screen") {
                            DisposableEffect(Unit) {
                                if (currentAttackRequest == null) {
                                    // Valores por defecto
                                    currentAttackRequest = NfcCombatManager.AttackRequest(1, 20, 0)
                                }
                                onDispose { currentAttackRequest = null }
                            }

                            ActionScreen(
                                lastResult = lastAttackResult,
                                battleList = localBattleList,
                                onSetupAttack = { request -> currentAttackRequest = request }
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

    // --- PROCESAMIENTO NFC OFFLINE ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            // 1. ESCRITURA: CREAR FIGURA
            if (pendingBattleStateToWrite != null) {
                if (NfcCombatManager.writeTag(tag, pendingBattleStateToWrite!!)) {
                    feedback("¡Miniatura creada!")
                    pendingBattleStateToWrite = null
                } else feedback("Error de escritura")
                return
            }

            // 2. ESCRITURA: BACKUP
            if (pendingCharacterToWrite != null) {
                if (NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)) {
                    feedback("¡Backup guardado!")
                    pendingCharacterToWrite = null
                } else feedback("Error: Ficha muy grande")
                return
            }

            // 3. COMBATE (Tirada manual aplicada a figura)
            if (currentAttackRequest != null) {
                val result = NfcCombatManager.performAttack(tag, currentAttackRequest!!)
                if (result != null) {
                    lastAttackResult = result
                    updateLocalList(result.enemyState)
                } else {
                    feedback("Error o Tag incompatible")
                }
                return
            }

            // 4. LECTURA SIMPLE (Backup o Figura)
            val char = NfcManager.readCharacterFromIntent(intent)
            if (char != null) {
                // LEÍDO CORRECTAMENTE
                if (onNfcScanned != null) {
                    // Pasamos el personaje completo al evento para mostrarlo en pantalla
                    onNfcScanned?.invoke(
                        ScanEvent(
                            character = CharacterSheet(char.id, char.name, "Nivel ${char.level}"),
                            fullCharacter = char // <--- DATOS COMPLETOS AQUI
                        )
                    )
                } else {
                    CharacterManager.saveCharacter(this, char)
                    feedback("Personaje importado")
                }
            } else {
                // Intentar leer como figura de combate
                val combatState = NfcCombatManager.readTag(tag)
                if (combatState != null) {
                    feedback("Figura: ${combatState.name} (${combatState.hp} HP)")
                    updateLocalList(combatState)
                } else {
                    feedback("Tarjeta desconocida o ilegible")
                }
            }
        }
    }

    private fun updateLocalList(newState: BattleState) {
        val list = localBattleList.toMutableList()
        val idx = list.indexOfFirst { it.id == newState.id }
        if (idx != -1) list[idx] = newState else list.add(newState)
        localBattleList = list
    }

    private fun feedback(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}