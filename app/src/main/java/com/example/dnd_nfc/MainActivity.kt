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

    // 1. Escritura NFC (Si no es nulo, el próximo toque escribe esto)
    private var pendingCharacterToWrite: PlayerCharacter? = null

    // 2. Lista de Combate (Centralizada aquí para que persista durante la navegación)
    private val combatList = mutableStateListOf<BattleState>()

    // 3. Control de Modo Combate (Para saber si el escaneo va a la mesa o al lector)
    private var isCombatModeActive by mutableStateOf(false)

    // 4. Último escaneo (Para mostrar en NfcReadScreen)
    private var lastScanEvent by mutableStateOf<ScanEvent?>(null)

    // Callback temporal para navegación
    private var onNfcScanned: ((ScanEvent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // FORZAR MODO VERTICAL
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            DnD_NFCTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    // Detectar en qué pantalla estamos para ajustar el comportamiento del NFC
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Si estamos en "action_screen", activamos modo combate para redirección directa
                    isCombatModeActive = (currentRoute == "action_screen")

                    // Configuración para redirección automática al leer (fuera de combate)
                    DisposableEffect(Unit) {
                        onNfcScanned = { event ->
                            lastScanEvent = event
                            // Solo navegamos si NO estamos en combate
                            if (!isCombatModeActive) {
                                navController.navigate("nfc_read") {
                                    launchSingleTop = true
                                }
                            }
                        }
                        onDispose { onNfcScanned = null }
                    }

                    NavHost(navController = navController, startDestination = "main_menu") {

                        // --- MENÚ PRINCIPAL ---
                        composable("main_menu") {
                            // Limpieza de estados al volver al menú
                            DisposableEffect(Unit) {
                                pendingCharacterToWrite = null
                                onDispose { }
                            }

                            MainMenuScreen(
                                onNavigateToCharacters = { navController.navigate("character_list") },
                                onNavigateToNewCharacter = { navController.navigate("character_sheet/new") },
                                onNavigateToCombat = { navController.navigate("action_screen") },
                                onNavigateToCampaigns = { }, // Sin uso por ahora
                                onCharacterImported = { importedChar ->
                                    // Callback para cuando leemos QR
                                    if (importedChar is PlayerCharacter) {
                                        CharacterManager.saveCharacter(context, importedChar)
                                        navController.navigate("character_sheet/${importedChar.id}")
                                    }
                                }
                            )
                        }

                        // --- BIBLIOTECA DE FIGURAS (LISTA) ---
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

                        // --- EDITOR DE FIGURA (CREAR/EDITAR) ---
                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")
                            val character = if (charId != null && charId != "new") {
                                CharacterManager.getCharacterById(context, charId)
                            } else { null }

                            CharacterSheetScreen(
                                existingCharacter = character,
                                onBack = { navController.popBackStack() },

                                // Acción: Botón "GRABAR MINI"
                                onWriteNfc = { charData ->
                                    pendingCharacterToWrite = charData
                                    Toast.makeText(context, "Modo GRABACIÓN ACTIVADO: Acerca una etiqueta NFC.", Toast.LENGTH_LONG).show()
                                },
                                onWriteCombatNfc = { } // No usado en este diseño simplificado
                            )
                        }

                        // --- LECTOR DE FIGURA (VER DETALLES) ---
                        composable("nfc_read") {
                            NfcReadScreen(
                                scanEvent = lastScanEvent,
                                // Acción: Botón "ACTUALIZAR" (Write-back)
                                onFullCharacterLoaded = { updatedChar ->
                                    pendingCharacterToWrite = updatedChar
                                    Toast.makeText(context, "Modo ACTUALIZACIÓN: Acerca la misma figura para guardar cambios.", Toast.LENGTH_LONG).show()
                                },
                                onScanAgainClick = {
                                    lastScanEvent = null
                                    if (navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        // --- MESA DE COMBATE (NUEVO SISTEMA) ---
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

    // --- LÓGICA CENTRAL DE NFC ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            // CASO 1: Estamos en MODO ESCRITURA (Grabando o Actualizando)
            if (pendingCharacterToWrite != null) {
                if (NfcManager.writeCharacterToTag(tag, pendingCharacterToWrite!!)) {
                    feedback("¡Datos guardados en la figura!")
                    pendingCharacterToWrite = null // Apagar modo escritura
                } else {
                    feedback("Error: No se pudo escribir en el tag.")
                }
                return
            }

            // CASO 2: Estamos en MESA DE COMBATE -> Añadir figura
            if (isCombatModeActive) {
                addToCombat(intent, tag)
                return
            }

            // CASO 3: Lectura Normal (Ir a pantalla de detalle)
            readAndNavigate(intent, tag)
        }
    }

    // Función auxiliar para leer y añadir a la lista de combate
    private fun addToCombat(intent: Intent, tag: Tag) {
        // Intento 1: Leer formato completo (PlayerCharacter)
        val fullChar = NfcManager.readCharacterFromIntent(intent)

        if (fullChar != null) {
            // Calcular bono de iniciativa (Dex mod)
            val dexMod = (fullChar.dex - 10) / 2

            // Crear estado de batalla (CORREGIDO: Incluye todos los parámetros)
            val newFighter = BattleState(
                id = fullChar.id,
                name = fullChar.name,
                hp = fullChar.hpCurrent,
                maxHp = fullChar.hpMax,
                ac = fullChar.ac,
                status = fullChar.status,
                initiativeBonus = dexMod,

                // Mapeo de estadísticas
                str = fullChar.str,
                dex = fullChar.dex,
                con = fullChar.con,
                int = fullChar.int,
                wis = fullChar.wis,
                cha = fullChar.cha
            )

            // Añadir o Actualizar en la lista
            val idx = combatList.indexOfFirst { it.id == newFighter.id }
            if (idx >= 0) {
                // Actualizar manteniendo el estado actual de la iniciativa si ya tiró
                val existing = combatList[idx]
                combatList[idx] = newFighter.copy(
                    initiativeTotal = existing.initiativeTotal,
                    isSelected = existing.isSelected
                )
                feedback("Actualizado: ${fullChar.name}")
            } else {
                combatList.add(newFighter)
                feedback("Añadido: ${fullChar.name}")
            }
        } else {
            // Intento 2: Leer formato antiguo (BattleState simple)
            val mini = NfcCombatManager.readTag(tag)
            if (mini != null) {
                val idx = combatList.indexOfFirst { it.id == mini.id }
                if (idx >= 0) combatList[idx] = mini // Actualizar
                else combatList.add(mini) // Añadir
                feedback("Miniatura añadida: ${mini.name}")
            } else {
                feedback("No se pudo leer la figura.")
            }
        }
    }

    // Función auxiliar para lectura normal
    private fun readAndNavigate(intent: Intent, tag: Tag) {
        val char = NfcManager.readCharacterFromIntent(intent)
        if (char != null) {
            val event = ScanEvent(
                character = CharacterSheet(char.id, char.name, "${char.race} ${char.charClass}"),
                fullCharacter = char
            )
            // Notificar a la UI
            if (onNfcScanned != null) {
                onNfcScanned?.invoke(event)
            } else {
                lastScanEvent = event
            }
            feedback("Figura leída: ${char.name}")
        } else {
            val mini = NfcCombatManager.readTag(tag)
            if (mini != null) {
                feedback("Miniatura simple: ${mini.name} (Solo lectura)")
            } else {
                feedback("Etiqueta vacía o formato desconocido")
            }
        }
    }

    private fun feedback(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}