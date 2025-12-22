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
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // MODOS DE ESCRITURA
    private var pendingCharacterToWrite: PlayerCharacter? = null
    private var pendingBattleStateToWrite: BattleState? = null

    // ESTADO DE COMBATE (Lista en vivo)
    private val combatParticipants = mutableStateListOf<BattleState>()
    private var isCombatModeActive = false

    // Eventos UI
    private var lastScanEvent by mutableStateOf<ScanEvent?>(null)
    private var onNfcScanned: ((ScanEvent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            DnD_NFCTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                    isCombatModeActive = (currentRoute == "action_screen")

                    LaunchedEffect(currentRoute) {
                        if (currentRoute == "main_menu" || currentRoute == "character_list") {
                            onNfcScanned = { event ->
                                lastScanEvent = event
                                navController.navigate("nfc_read") { launchSingleTop = true }
                            }
                        } else {
                            onNfcScanned = null
                        }
                    }

                    NavHost(navController = navController, startDestination = "main_menu") {

                        // --- CORRECCIÓN AQUÍ: Bloque único y limpio ---
                        composable("main_menu") {
                            DisposableEffect(Unit) {
                                ResetWriteModes()
                                onDispose { }
                            }

                            MainMenuScreen(
                                onNavigateToCharacters = { navController.navigate("character_list") },
                                onNavigateToCampaigns = { navController.navigate("campaign_list") },
                                onNavigateToCombat = { navController.navigate("action_screen") },
                                onCharacterImported = { importedChar ->
                                    CharacterManager.saveCharacter(context, importedChar)
                                    navController.navigate("character_sheet/${importedChar.id}")
                                }
                            )
                        }
                        // ---------------------------------------------

                        composable("character_list") {
                            DisposableEffect(Unit) { ResetWriteModes(); onDispose { } }
                            CharacterListScreen(
                                onCharacterClick = { char -> navController.navigate("character_sheet/${char.id}") },
                                onNewCharacterClick = { navController.navigate("character_sheet/new") }
                            )
                        }

                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")
                            val character = if (charId != null && charId != "new") CharacterManager.getCharacterById(context, charId) else null

                            DisposableEffect(Unit) { onDispose { ResetWriteModes() } }

                            CharacterSheetScreen(
                                existingCharacter = character,
                                onBack = { navController.popBackStack() },
                                onWriteNfc = { charToLink ->
                                    pendingCharacterToWrite = charToLink
                                    pendingBattleStateToWrite = null
                                    Toast.makeText(context, "Modo BACKUP: Acerca tarjeta.", Toast.LENGTH_SHORT).show()
                                },
                                onWriteCombatNfc = { battleState ->
                                    val dexMod = character?.let { (it.dex - 10) / 2 } ?: 0
                                    val stateWithMod = battleState.copy(initiativeMod = dexMod)
                                    pendingBattleStateToWrite = stateWithMod
                                    pendingCharacterToWrite = null
                                    Toast.makeText(context, "Modo MINIATURA: Acerca figura.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        composable("nfc_read") {
                            NfcReadScreen(
                                scanEvent = lastScanEvent,
                                onFullCharacterLoaded = { fullChar ->
                                    CharacterManager.saveCharacter(context, fullChar)
                                    navController.navigate("character_sheet/${fullChar.id}") { popUpTo("character_list") }
                                },
                                onScanAgainClick = { lastScanEvent = null }
                            )
                        }

                        composable("campaign_list") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Próximamente") } }

                        composable("action_screen") {
                            ActionScreen(
                                combatList = combatParticipants.sortedByDescending { it.currentInitiative ?: -99 },
                                onResetCombat = { combatParticipants.clear() },
                                onRemoveCombatant = { id -> combatParticipants.removeAll { it.id == id } }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun ResetWriteModes() {
        pendingCharacterToWrite = null
        pendingBattleStateToWrite = null
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            if (pendingBattleStateToWrite != null) {
                if (NfcCombatManager.writeTag(tag, pendingBattleStateToWrite!!)) {
                    feedback("¡Miniatura configurada!")
                    ResetWriteModes()
                } else feedback("Error al escribir")
                return
            }
            if (pendingCharacterToWrite != null) {
                if (NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)) {
                    feedback("¡Backup guardado!")
                    ResetWriteModes()
                } else feedback("Error de espacio/formato")
                return
            }

            if (isCombatModeActive) {
                addToCombat(intent, tag)
                return
            }

            val char = NfcManager.readCharacterFromIntent(intent)
            if (char != null) {
                val event = ScanEvent(CharacterSheet(char.id, char.name, "Nivel ${char.level}"), char)
                if (onNfcScanned != null) onNfcScanned?.invoke(event)
                else lastScanEvent = event
            } else {
                val mini = NfcCombatManager.readTag(tag)
                if (mini != null) feedback("Miniatura: ${mini.name}")
                else feedback("Tag vacío o desconocido")
            }
        }
    }

    private fun addToCombat(intent: Intent, tag: Tag) {
        val fullChar = NfcManager.readCharacterFromIntent(intent)
        if (fullChar != null) {
            val dexMod = (fullChar.dex - 10) / 2
            val roll = Random.nextInt(1, 21)
            val totalInit = roll + dexMod
            val combatant = BattleState(fullChar.id, fullChar.name, fullChar.hpMax, fullChar.hpMax, fullChar.ac, dexMod, totalInit)
            addOrUpdateCombatant(combatant)
            feedback("${fullChar.name} añadido (Inic: $totalInit)")
            return
        }
        val mini = NfcCombatManager.readTag(tag)
        if (mini != null) {
            val roll = Random.nextInt(1, 21)
            val totalInit = roll + mini.initiativeMod
            addOrUpdateCombatant(mini.copy(currentInitiative = totalInit))
            feedback("${mini.name} añadido (Inic: $totalInit)")
            return
        }
        feedback("Error de lectura en combate")
    }

    private fun addOrUpdateCombatant(new: BattleState) {
        val idx = combatParticipants.indexOfFirst { it.id == new.id }
        if (idx >= 0) combatParticipants[idx] = new else combatParticipants.add(new)
    }

    private fun feedback(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}