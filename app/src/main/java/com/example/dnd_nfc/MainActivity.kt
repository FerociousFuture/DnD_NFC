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
    // Usamos mutableStateListOf para que la UI reaccione a los cambios
    private val combatParticipants = mutableStateListOf<BattleState>()
    private var isCombatModeActive = false // Flag para saber si estamos en la pantalla de combate

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

                    // Flag de control: ¿Estamos en pantalla de Combate?
                    isCombatModeActive = (currentRoute == "action_screen")

                    // Navegación básica de lectura
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
                        // ... (Main Menu, Character List, Character Sheet, Nfc Read IGUAL QUE ANTES) ...
                        composable("main_menu") {
                            // Reset de modos previos
                            DisposableEffect(Unit) {
                                currentAttackRequest = null
                                pendingCharacterToWrite = null
                                pendingBattleStateToWrite = null
                                onDispose { }
                            }

                            composable("main_menu") {
                                ResetWriteModes()
                                MainMenuScreen(
                                    onNavigateToCharacters = { navController.navigate("character_list") },
                                    onNavigateToCampaigns = { navController.navigate("campaign_list") },
                                    onNavigateToCombat = { navController.navigate("action_screen") },
                                    // ESTA ES LA PARTE QUE FALTA:
                                    onCharacterImported = { importedChar ->
                                        CharacterManager.saveCharacter(context, importedChar)
                                        navController.navigate("character_sheet/${importedChar.id}")
                                    }
                                )
                            }

                        composable("character_list") {
                            ResetWriteModes()
                            CharacterListScreen(
                                onCharacterClick = { char -> navController.navigate("character_sheet/${char.id}") },
                                onNewCharacterClick = { navController.navigate("character_sheet/new") }
                            )
                        }

                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")
                            val character = if (charId != null && charId != "new") CharacterManager.getCharacterById(context, charId) else null

                            // Al salir de la ficha, limpiamos modos de escritura
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
                                    // AQUI ES IMPORTANTE: Escribir el battleState con el modificador correcto
                                    // Si venimos de la ficha, podemos calcular el Dex mod
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

                        // --- PANTALLA DE ACCIÓN (DADOS Y COMBATE) ---
                        composable("action_screen") {
                            // Pasamos la lista mutable al composable
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

    // --- PROCESAMIENTO NFC ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            // 1. ESCRITURA (Prioridad Alta)
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

            // 2. MODO COMBATE (Si estamos en la pantalla ActionScreen)
            if (isCombatModeActive) {
                addToCombat(intent, tag)
                return
            }

            // 3. MODO LECTURA DEFAULT
            val char = NfcManager.readCharacterFromIntent(intent)
            if (char != null) {
                val event = ScanEvent(CharacterSheet(char.id, char.name, "Nivel ${char.level}"), char)
                if (onNfcScanned != null) onNfcScanned?.invoke(event)
                else lastScanEvent = event
            } else {
                // Intentar leer mini simple
                val mini = NfcCombatManager.readTag(tag)
                if (mini != null) feedback("Miniatura: ${mini.name}")
                else feedback("Tag vacío o desconocido")
            }
        }
    }

    // Lógica para añadir al combate y calcular iniciativa
    private fun addToCombat(intent: Intent, tag: Tag) {
        // Intento 1: Leer Personaje Completo (Ficha)
        val fullChar = NfcManager.readCharacterFromIntent(intent)
        if (fullChar != null) {
            // Calculamos iniciativa: d20 + (dex-10)/2
            val dexMod = (fullChar.dex - 10) / 2
            val roll = Random.nextInt(1, 21)
            val totalInit = roll + dexMod

            // Convertimos a BattleState para la lista
            val combatant = BattleState(
                id = fullChar.id,
                name = fullChar.name,
                hp = fullChar.hpMax, // Asumimos full HP al inicio
                maxHp = fullChar.hpMax,
                ac = fullChar.ac,
                initiativeMod = dexMod,
                currentInitiative = totalInit
            )

            addOrUpdateCombatant(combatant)
            feedback("${fullChar.name} añadido (Iniciativa: $totalInit)")
            return
        }

        // Intento 2: Leer Miniatura (BattleState)
        val mini = NfcCombatManager.readTag(tag)
        if (mini != null) {
            val roll = Random.nextInt(1, 21)
            val totalInit = roll + mini.initiativeMod // Usa el mod guardado en la etiqueta

            val combatant = mini.copy(currentInitiative = totalInit)
            addOrUpdateCombatant(combatant)
            feedback("${mini.name} añadido (Iniciativa: $totalInit)")
            return
        }

        feedback("No se pudo leer datos de combate válidos")
    }

    private fun addOrUpdateCombatant(new: BattleState) {
        // Si ya existe, lo reemplazamos (actualizamos iniciativa)
        val idx = combatParticipants.indexOfFirst { it.id == new.id }
        if (idx >= 0) {
            combatParticipants[idx] = new
        } else {
            combatParticipants.add(new)
        }
    }

    private fun feedback(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}