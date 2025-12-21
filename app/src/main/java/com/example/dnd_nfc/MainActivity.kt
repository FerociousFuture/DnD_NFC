package com.example.dnd_nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dnd_nfc.data.local.CharacterManager
import com.example.dnd_nfc.data.model.CharacterSheet
import com.example.dnd_nfc.data.model.ScanEvent
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.CharacterListScreen
import com.example.dnd_nfc.ui.screens.CharacterSheetScreen
import com.example.dnd_nfc.ui.screens.NfcReadScreen
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // Almacena datos temporalmente si queremos escribir en la próxima tarjeta
    private var pendingWriteData: String? = null

    // Estado simple para comunicar el escaneo a la interfaz
    private var lastScanEvent: ScanEvent? = null

    // Callback para navegar cuando se escanea algo (se configura desde la UI)
    private var onNfcScanned: ((ScanEvent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos el adaptador NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            DnD_NFCTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    // NAVEGACIÓN SIMPLE (Sin Login)
                    NavHost(navController = navController, startDestination = "character_list") {

                        // 1. PANTALLA PRINCIPAL: LISTA DE PERSONAJES
                        composable("character_list") {
                            // Configurar el callback: Si escaneamos aquí, vamos a leer
                            onNfcScanned = { event ->
                                lastScanEvent = event
                                navController.navigate("nfc_read")
                            }

                            CharacterListScreen(
                                onCharacterClick = { char ->
                                    // Editar personaje existente
                                    navController.navigate("character_sheet/${char.id}")
                                },
                                onNewCharacterClick = {
                                    // Crear nuevo
                                    navController.navigate("character_sheet/new")
                                }
                            )
                        }

                        // 2. PANTALLA DE FICHA (DETALLE/EDICIÓN)
                        composable("character_sheet/{charId}") { backStackEntry ->
                            val charId = backStackEntry.arguments?.getString("charId")

                            // Cargamos el personaje desde la memoria del teléfono
                            val character = if (charId != null && charId != "new") {
                                CharacterManager.getCharacterById(context, charId)
                            } else {
                                null
                            }

                            // En la ficha, desactivamos la navegación por escaneo para evitar salidas accidentales
                            onNfcScanned = { }

                            CharacterSheetScreen(
                                existingCharacter = character,
                                onBack = { navController.popBackStack() },
                                onWriteNfc = { charToLink ->
                                    // MODO VINCULAR: Preparamos los datos para la próxima tarjeta que toque el móvil
                                    // Formato CSV simple: ID,Nombre,Fuerza,Destreza...
                                    val safeName = charToLink.name.replace(",", " ")
                                    val csvData = "${charToLink.id},$safeName,${charToLink.str},${charToLink.dex},${charToLink.con},${charToLink.int},${charToLink.wis},${charToLink.cha}"

                                    pendingWriteData = csvData
                                    Toast.makeText(context, "¡Listo! Acerca una tarjeta para grabar.", Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        // 3. PANTALLA DE LECTURA NFC (Resultado del escaneo)
                        composable("nfc_read") {
                            NfcReadScreen(
                                scanEvent = lastScanEvent,
                                onFullCharacterLoaded = { fullChar ->
                                    // Si encontramos el personaje en el móvil, abrimos su ficha
                                    navController.navigate("character_sheet/${fullChar.id}") {
                                        popUpTo("character_list") // Limpia la pila para que "atrás" vaya a la lista
                                    }
                                },
                                onScanAgainClick = {
                                    lastScanEvent = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- MÉTODOS DEL SISTEMA PARA NFC ---

    override fun onResume() {
        super.onResume()
        // Dar prioridad a nuestra app para recibir tarjetas NFC mientras está abierta
        enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        // Dejar de escuchar si la app se minimiza
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun enableForegroundDispatch() {
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    // ESTE MÉTODO SE EJECUTA AUTOMÁTICAMENTE AL TOCAR UNA TARJETA
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            if (tag != null) {
                if (pendingWriteData != null) {
                    // --- CASO 1: MODO ESCRITURA (Vincular) ---
                    // El usuario le dio al botón "Vincular" antes
                    val success = NfcManager.writeToTag(tag, pendingWriteData!!)
                    if (success) {
                        Toast.makeText(this, "¡Personaje vinculado con éxito!", Toast.LENGTH_LONG).show()
                        pendingWriteData = null // Ya terminamos de escribir
                    } else {
                        Toast.makeText(this, "Error al escribir. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // --- CASO 2: MODO LECTURA (Escanear) ---
                    // El usuario solo acercó una tarjeta para ver qué tiene
                    val sheet = NfcManager.readFromIntent(intent)
                    if (sheet != null) {
                        // Notificamos a la navegación para que cambie de pantalla
                        val event = ScanEvent(sheet)
                        onNfcScanned?.invoke(event)
                    } else {
                        Toast.makeText(this, "Tarjeta vacía o formato desconocido.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}