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
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // Estados
    private var pendingCharacterToWrite: PlayerCharacter? = null
    private val combatList = mutableStateListOf<BattleState>()
    private var isCombatModeActive by mutableStateOf(false)
    private var pendingCombatAction: CombatAction? = null // ACCIÓN PENDIENTE (Nuevo)
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
                            if (!isCombatModeActive) navController.navigate("nfc_read") { launchSingleTop = true }
                        }
                        onDispose { onNfcScanned = null }
                    }

                    NavHost(navController = navController, startDestination = "main_menu") {
                        composable("main_menu") {
                            DisposableEffect(Unit) { pendingCharacterToWrite = null; pendingCombatAction = null; onDispose {} }
                            MainMenuScreen(
                                onNavigateToCharacters = { navController.navigate("character_list") },
                                onNavigateToNewCharacter = { navController.navigate("character_sheet/new") },
                                onNavigateToCombat = { navController.navigate("action_screen") },
                                onNavigateToCampaigns = {},
                                onCharacterImported = { if (it is PlayerCharacter) { CharacterManager.saveCharacter(context, it); navController.navigate("character_sheet/${it.id}") } }
                            )
                        }
                        composable("character_list") {
                            DisposableEffect(Unit) { pendingCharacterToWrite = null; onDispose {} }
                            CharacterListScreen(onCharacterClick = { navController.navigate("character_sheet/${it.id}") }, onNewCharacterClick = { navController.navigate("character_sheet/new") })
                        }
                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")
                            val character = if (charId != null && charId != "new") CharacterManager.getCharacterById(context, charId) else null
                            CharacterSheetScreen(existingCharacter = character, onBack = { navController.popBackStack() }, onWriteNfc = { pendingCharacterToWrite = it; Toast.makeText(context, "GRABAR: Acerca NFC", Toast.LENGTH_LONG).show() }, onWriteCombatNfc = {})
                        }
                        composable("nfc_read") {
                            NfcReadScreen(scanEvent = lastScanEvent, onFullCharacterLoaded = { pendingCharacterToWrite = it; Toast.makeText(context, "ACTUALIZAR: Acerca NFC", Toast.LENGTH_LONG).show() }, onScanAgainClick = { lastScanEvent = null; if (navController.previousBackStackEntry != null) navController.popBackStack() })
                        }
                        composable("action_screen") {
                            DisposableEffect(Unit) { onDispose { pendingCombatAction = null } }
                            ActionScreen(
                                combatList = combatList,
                                onUpdateList = { combatList.clear(); combatList.addAll(it) },
                                onResetCombat = { combatList.clear() },
                                onTriggerAction = { action ->
                                    pendingCombatAction = action
                                    val msg = if (action is CombatAction.SkillCheck) "Acerca HÉROE" else "Acerca ENEMIGO"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); enableForegroundDispatch() }
    override fun onPause() { super.onPause(); nfcAdapter?.disableForegroundDispatch(this) }
    private fun enableForegroundDispatch() {
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            // 1. ESCRITURA
            if (pendingCharacterToWrite != null) {
                if (NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)) { feedback("¡Guardado!"); pendingCharacterToWrite = null }
                else feedback("Error escritura")
                return
            }

            // 2. ACCIÓN DE COMBATE (Ataque/Prueba)
            if (pendingCombatAction != null) {
                handleCombatAction(intent, tag)
                return
            }

            // 3. AÑADIR A COMBATE
            if (isCombatModeActive) { addToCombat(intent, tag); return }

            // 4. LECTURA
            readAndNavigate(intent, tag)
        }
    }

    private fun handleCombatAction(intent: Intent, tag: Tag) {
        val action = pendingCombatAction ?: return
        val char = NfcManager.readCharacterFromIntent(intent) ?: run { feedback("Error leer figura"); return }

        when (action) {
            is CombatAction.SkillCheck -> {
                val statVal = when(action.attribute) { "FUE"->char.str; "DES"->char.dex; "CON"->char.con; "INT"->char.int; "SAB"->char.wis; "CAR"->char.cha; else->10 }
                val mod = (statVal - 10) / 2
                val roll = Random.nextInt(1, 21)
                val total = roll + mod + action.bonus
                feedback("Prueba ${action.attribute}: $total (Dado $roll)")
            }
            is CombatAction.Attack -> {
                var dmg = 0
                repeat(action.diceCount) { dmg += Random.nextInt(1, action.diceFaces + 1) }
                dmg += action.bonus
                val newHp = (char.hpCurrent - dmg).coerceAtLeast(0)
                if (NfcManager.writeCharacterToTag(tag, char.copy(hpCurrent = newHp))) {
                    val idx = combatList.indexOfFirst { it.id == char.id }
                    if (idx >= 0) combatList[idx] = combatList[idx].copy(hp = newHp)
                    feedback("¡Daño aplicado! -$dmg HP")
                } else feedback("Error al actualizar etiqueta")
            }
        }
        pendingCombatAction = null
    }

    private fun addToCombat(intent: Intent, tag: Tag) {
        val fullChar = NfcManager.readCharacterFromIntent(intent)
        if (fullChar != null) {
            val dexMod = (fullChar.dex - 10) / 2
            val newFighter = BattleState(fullChar.id, fullChar.name, fullChar.hpCurrent, fullChar.hpMax, fullChar.ac, fullChar.status, dexMod, str=fullChar.str, dex=fullChar.dex, con=fullChar.con, int=fullChar.int, wis=fullChar.wis, cha=fullChar.cha)
            val idx = combatList.indexOfFirst { it.id == newFighter.id }
            if (idx >= 0) { combatList[idx] = newFighter.copy(initiativeTotal = combatList[idx].initiativeTotal, isSelected = combatList[idx].isSelected); feedback("Actualizado") }
            else { combatList.add(newFighter); feedback("Añadido") }
        } else {
            val mini = NfcCombatManager.readTag(tag)
            if (mini != null) {
                val idx = combatList.indexOfFirst { it.id == mini.id }
                if (idx >= 0) combatList[idx] = mini else combatList.add(mini)
                feedback("Miniatura Legacy añadida")
            } else feedback("No se pudo leer")
        }
    }

    private fun readAndNavigate(intent: Intent, tag: Tag) {
        val char = NfcManager.readCharacterFromIntent(intent)
        if (char != null) {
            val event = ScanEvent(CharacterSheet(char.id, char.name, "${char.race} ${char.charClass}"), char)
            if (onNfcScanned != null) onNfcScanned?.invoke(event) else lastScanEvent = event
            feedback("Leído: ${char.name}")
        } else feedback("Etiqueta vacía")
    }

    private fun feedback(msg: String) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
}