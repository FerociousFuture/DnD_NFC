package com.example.dnd_nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ActivityInfo
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // <--- IMPORT AÑADIDO
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dnd_nfc.data.local.CharacterManager
import com.example.dnd_nfc.data.model.BattleState
import com.example.dnd_nfc.data.model.CharacterSheet
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.model.ScanEvent
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.*
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // --- ESTADOS DE DATOS ---
    private var pendingCharacterToWrite: PlayerCharacter? = null
    private val combatList = mutableStateListOf<BattleState>()
    private var isCombatModeActive by mutableStateOf(false)
    private var pendingCombatAction: CombatAction? = null
    private var lastScanEvent by mutableStateOf<ScanEvent?>(null)

    // --- ESTADOS DE UI (Overlays y Logs) ---
    private var showLoading by mutableStateOf(false)
    private var loadingMessage by mutableStateOf("Leyendo etiqueta NFC...")

    private var showResult by mutableStateOf(false)
    private var resultSuccess by mutableStateOf(true)
    private var resultTitle by mutableStateOf("")
    private var resultMessage by mutableStateOf("")

    // Lista de LOG (Historial)
    private val combatLog = mutableStateListOf<LogEntry>()

    private var onNfcScanned: ((ScanEvent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            DnD_NFCTheme {
                // BOX PARA CAPAS: App al fondo, Alertas encima
                Box(modifier = Modifier.fillMaxSize()) {

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
                                CharacterSheetScreen(existingCharacter = character, onBack = { navController.popBackStack() }, onWriteNfc = { pendingCharacterToWrite = it; showResult(true, "Modo Grabación", "Acerca la etiqueta para grabar.") }, onWriteCombatNfc = {})
                            }
                            composable("nfc_read") {
                                NfcReadScreen(scanEvent = lastScanEvent, onFullCharacterLoaded = { pendingCharacterToWrite = it; showResult(true, "Modo Actualizar", "Acerca la etiqueta para guardar cambios.") }, onScanAgainClick = { lastScanEvent = null; if (navController.previousBackStackEntry != null) navController.popBackStack() })
                            }
                            composable("action_screen") {
                                DisposableEffect(Unit) { onDispose { pendingCombatAction = null } }
                                ActionScreen(
                                    combatList = combatList,
                                    combatLog = combatLog, // Pasamos el log
                                    onUpdateList = { combatList.clear(); combatList.addAll(it) },
                                    onResetCombat = { combatList.clear(); combatLog.clear(); addLog("Combate reiniciado", "INFO") },
                                    onTriggerAction = { action ->
                                        if (action is CombatAction.SkillCheck) {
                                            // 1. PRUEBA DE HABILIDAD (LOCAL)
                                            val actor = action.actor
                                            val statVal = when(action.attribute) {
                                                "FUE" -> actor.str; "DES" -> actor.dex; "CON" -> actor.con
                                                "INT" -> actor.int; "SAB" -> actor.wis; "CAR" -> actor.cha; else -> 10
                                            }
                                            val mod = (statVal - 10) / 2
                                            val roll = Random.nextInt(1, 21)
                                            val total = roll + mod + action.bonus

                                            val resultHeader = if (total >= action.dc) "¡ÉXITO!" else "FALLO"
                                            val detailMsg = "Tirada: $roll\nMod ($mod) + Bono (${action.bonus})\nTOTAL: $total vs CD ${action.dc}"

                                            showResult(total >= action.dc, "$resultHeader (${action.attribute})", detailMsg)
                                            addLog("${actor.name} ${action.attribute}: $resultHeader ($total)", if(total>=action.dc) "SUCCESS" else "ERROR")

                                        } else {
                                            // 2. ATAQUE (REQUIERE NFC)
                                            pendingCombatAction = action
                                            showResult(true, "Escaneo Requerido", "Acerca al OBJETIVO para aplicar daño y estado.")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // --- CAPA DE OVERLAYS (ENCIMA DE TODO) ---

                    if (showLoading) {
                        LoadingOverlay(loadingMessage)
                    }

                    if (showResult) {
                        ResultOverlay(
                            success = resultSuccess,
                            title = resultTitle,
                            message = resultMessage,
                            onDismiss = { showResult = false }
                        )
                    }
                }
            }
        }
    }

    // --- FUNCIONES DE ESTADO UI ---

    private fun showLoadingState(msg: String) {
        loadingMessage = msg
        showLoading = true
    }

    private fun hideLoadingState() {
        showLoading = false
    }

    private fun showResult(success: Boolean, title: String, msg: String) {
        resultSuccess = success
        resultTitle = title
        resultMessage = msg
        showResult = true
    }

    private fun addLog(msg: String, type: String = "INFO") {
        combatLog.add(LogEntry(msg, type))
    }

    // --- PROCESAMIENTO NFC ---

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            // UI: Mostrar carga
            showLoadingState("Procesando etiqueta...")

            // 1. ESCRITURA
            if (pendingCharacterToWrite != null) {
                if (NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)) {
                    hideLoadingState()
                    showResult(true, "¡Éxito!", "Datos guardados en la figura.")
                    addLog("Escritura exitosa: ${pendingCharacterToWrite!!.name}", "SUCCESS")
                    pendingCharacterToWrite = null
                } else {
                    hideLoadingState()
                    showResult(false, "Error", "No se pudo escribir. Intenta de nuevo.")
                    addLog("Fallo escritura", "ERROR")
                }
                return
            }

            // 2. ACCIÓN DE COMBATE
            if (pendingCombatAction != null) {
                handleCombatAttack(intent, tag)
                return
            }

            // 3. AÑADIR A COMBATE
            if (isCombatModeActive) {
                addToCombat(intent, tag)
                return
            }

            // 4. LECTURA NORMAL
            readAndNavigate(intent, tag)
        }
    }

    private fun handleCombatAttack(intent: Intent, tag: Tag) {
        val action = pendingCombatAction as? CombatAction.Attack ?: return
        val targetChar = NfcManager.readCharacterFromIntent(intent)

        hideLoadingState() // Ocultamos carga inicial

        if (targetChar == null) {
            showResult(false, "Error de Lectura", "No se reconocieron datos en la figura.")
            return
        }

        var dmg = 0
        repeat(action.diceCount) { dmg += Random.nextInt(1, action.diceFaces + 1) }

        val statBonus = if(action.statAttribute != "NINGUNO") {
            val s = when(action.statAttribute) {
                "FUE" -> targetChar.str; "DES" -> targetChar.dex; "CON" -> targetChar.con
                "INT" -> targetChar.int; "SAB" -> targetChar.wis; "CAR" -> targetChar.cha; else -> 10
            }
            (s - 10) / 2
        } else 0

        val totalDmg = dmg + action.bonus + statBonus
        val newHp = (targetChar.hpCurrent - totalDmg).coerceAtLeast(0)

        // Actualizar Estado si se indicó
        val newStatus = if (action.statusEffect.isNotBlank()) action.statusEffect else targetChar.status
        val updatedChar = targetChar.copy(hpCurrent = newHp, status = newStatus)

        // Intentar escribir el daño
        showLoadingState("Aplicando Daño...")

        if (NfcManager.writeCharacterToTag(tag, updatedChar)) {
            hideLoadingState()
            // Actualizar lista en pantalla
            val idx = combatList.indexOfFirst { it.id == targetChar.id }
            if (idx >= 0) combatList[idx] = combatList[idx].copy(hp = newHp, status = newStatus)

            val statusMsg = if(action.statusEffect.isNotBlank()) "\nEstado: ${action.statusEffect}" else ""
            showResult(true, "¡Impacto!", "${targetChar.name} recibe $totalDmg de daño.\nHP restante: $newHp$statusMsg")
            addLog("Ataque a ${targetChar.name}: -$totalDmg HP ($newHp left)$statusMsg", "COMBAT")
        } else {
            hideLoadingState()
            showResult(false, "Error", "No se pudo actualizar la vida en la etiqueta.")
            addLog("Fallo al aplicar daño a ${targetChar.name}", "ERROR")
        }
        pendingCombatAction = null
    }

    private fun addToCombat(intent: Intent, tag: Tag) {
        val fullChar = NfcManager.readCharacterFromIntent(intent)
        hideLoadingState()

        if (fullChar != null) {
            val dexMod = (fullChar.dex - 10) / 2
            val newFighter = BattleState(
                id = fullChar.id, name = fullChar.name, hp = fullChar.hpCurrent, maxHp = fullChar.hpMax,
                ac = fullChar.ac, status = fullChar.status, initiativeBonus = dexMod,
                str = fullChar.str, dex = fullChar.dex, con = fullChar.con, int = fullChar.int, wis = fullChar.wis, cha = fullChar.cha
            )

            val idx = combatList.indexOfFirst { it.id == newFighter.id }
            if (idx >= 0) {
                combatList[idx] = newFighter.copy(initiativeTotal = combatList[idx].initiativeTotal, isSelected = combatList[idx].isSelected)
                showResult(true, "Actualizado", "${fullChar.name} sincronizado.")
                addLog("Sincronizado: ${fullChar.name}", "INFO")
            } else {
                combatList.add(newFighter)
                showResult(true, "Añadido", "${fullChar.name} entró al combate.")
                addLog("Entra al combate: ${fullChar.name}", "INFO")
            }
        } else {
            showResult(false, "Error", "Formato desconocido o etiqueta vacía.")
        }
    }

    private fun readAndNavigate(intent: Intent, tag: Tag) {
        val char = NfcManager.readCharacterFromIntent(intent)
        hideLoadingState()

        if (char != null) {
            val event = ScanEvent(CharacterSheet(char.id, char.name, char.shortDescription), char)
            if (onNfcScanned != null) onNfcScanned?.invoke(event) else lastScanEvent = event
            addLog("Leído: ${char.name}", "INFO")
        } else {
            showResult(false, "Vacío", "No se encontraron datos de personaje.")
        }
    }

    // --- COMPONENTES OVERLAY ---

    @Composable
    fun LoadingOverlay(msg: String) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(32.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(text = msg, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    @Composable
    fun ResultOverlay(success: Boolean, title: String, message: String, onDismiss: () -> Unit) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(32.dp).fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (success) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    // AQUÍ ES DONDE FALTABA EL IMPORT DE TextAlign
                    Text(text = message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "OK")
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
}