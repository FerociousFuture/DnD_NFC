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

    // --- ESTADOS GLOBALES ---
    private var pendingCharacterToWrite: PlayerCharacter? = null
    private val combatList = mutableStateListOf<BattleState>()
    private var isCombatModeActive by mutableStateOf(false)
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

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    isCombatModeActive = (currentRoute == "action_screen")

                    DisposableEffect(Unit) {
                        onNfcScanned = { event ->
                            lastScanEvent = event
                            if (!isCombatModeActive) {
                                navController.navigate("nfc_read") { launchSingleTop = true }
                            }
                        }
                        onDispose { onNfcScanned = null }
                    }

                    NavHost(navController = navController, startDestination = "main_menu") {

                        // 1. MENÚ PRINCIPAL
                        composable("main_menu") {
                            DisposableEffect(Unit) {
                                pendingCharacterToWrite = null
                                onDispose { }
                            }
                            MainMenuScreen(
                                onNavigateToCharacters = { navController.navigate("character_list") },
                                onNavigateToNewCharacter = { navController.navigate("character_sheet/new") },
                                onNavigateToCombat = { navController.navigate("action_screen") },
                                onNavigateToCampaigns = { },
                                onCharacterImported = { importedChar ->
                                    if (importedChar is PlayerCharacter) {
                                        CharacterManager.saveCharacter(context, importedChar)
                                        navController.navigate("character_sheet/${importedChar.id}")
                                    }
                                }
                            )
                        }

                        // 2. LISTA DE PERSONAJES (BIBLIOTECA) - ¡ESTA ES LA QUE FALTABA!
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

                        // 3. EDITOR DE PERSONAJE
                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")
                            val character = if (charId != null && charId != "new") {
                                CharacterManager.getCharacterById(context, charId)
                            } else { null }

                            CharacterSheetScreen(
                                existingCharacter = character,
                                onBack = { navController.popBackStack() },
                                onWriteNfc = { charData ->
                                    pendingCharacterToWrite = charData
                                    Toast.makeText(context, "Modo GRABACIÓN: Acerca etiqueta NFC.", Toast.LENGTH_LONG).show()
                                },
                                onWriteCombatNfc = { }
                            )
                        }

                        // 4. LECTOR NFC
                        composable("nfc_read") {
                            NfcReadScreen(
                                scanEvent = lastScanEvent,
                                onFullCharacterLoaded = { updatedChar ->
                                    pendingCharacterToWrite = updatedChar
                                    Toast.makeText(context, "Modo ACTUALIZACIÓN: Acerca la figura.", Toast.LENGTH_LONG).show()
                                },
                                onScanAgainClick = {
                                    lastScanEvent = null
                                    if (navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        // 5. MESA DE COMBATE
                        composable("action_screen") {
                            ActionScreen(
                                combatList = combatList,
                                onUpdateList = { newList ->
                                    combatList.clear()
                                    combatList.addAll(newList)
                                },
                                onResetCombat = { combatList.clear() }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            if (pendingCharacterToWrite != null) {
                if (NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)) {
                    feedback("¡Guardado correctamente!")
                    pendingCharacterToWrite = null
                } else feedback("Error al escribir")
                return
            }

            if (isCombatModeActive) {
                addToCombat(intent, tag)
                return
            }

            readAndNavigate(intent, tag)
        }
    }

    private fun addToCombat(intent: Intent, tag: Tag) {
        val fullChar = NfcManager.readCharacterFromIntent(intent)
        if (fullChar != null) {
            val dexMod = (fullChar.dex - 10) / 2
            val newFighter = BattleState(
                id = fullChar.id,
                name = fullChar.name,
                hp = fullChar.hpCurrent, maxHp = fullChar.hpMax, ac = fullChar.ac,
                status = fullChar.status, initiativeBonus = dexMod,
                str = fullChar.str, dex = fullChar.dex, con = fullChar.con,
                int = fullChar.int, wis = fullChar.wis, cha = fullChar.cha
            )
            val idx = combatList.indexOfFirst { it.id == newFighter.id }
            if (idx >= 0) {
                val existing = combatList[idx]
                combatList[idx] = newFighter.copy(initiativeTotal = existing.initiativeTotal, isSelected = existing.isSelected)
                feedback("Actualizado: ${fullChar.name}")
            } else {
                combatList.add(newFighter)
                feedback("Añadido: ${fullChar.name}")
            }
        } else {
            val mini = NfcCombatManager.readTag(tag)
            if (mini != null) {
                val idx = combatList.indexOfFirst { it.id == mini.id }
                if (idx >= 0) combatList[idx] = mini
                else combatList.add(mini)
                feedback("Miniatura añadida: ${mini.name}")
            } else feedback("No se pudo leer la figura.")
        }
    }

    private fun readAndNavigate(intent: Intent, tag: Tag) {
        val char = NfcManager.readCharacterFromIntent(intent)
        if (char != null) {
            val event = ScanEvent(CharacterSheet(char.id, char.name, "${char.race} ${char.charClass}"), char)
            if (onNfcScanned != null) onNfcScanned?.invoke(event) else lastScanEvent = event
            feedback("Figura leída: ${char.name}")
        } else {
            val mini = NfcCombatManager.readTag(tag)
            if (mini != null) feedback("Miniatura simple: ${mini.name}") else feedback("Etiqueta vacía")
        }
    }

    private fun feedback(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}