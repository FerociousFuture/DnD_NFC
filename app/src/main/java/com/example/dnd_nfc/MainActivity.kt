package com.example.dnd_nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.dnd_nfc.data.model.ScanEvent // Asegúrate de haber creado este archivo
import com.example.dnd_nfc.data.model.CharacterSheet
import com.example.dnd_nfc.data.model.PlayerCharacter
import com.example.dnd_nfc.data.remote.GoogleAuthClient
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.*
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingWriteData: String? = null
    private lateinit var googleAuthClient: GoogleAuthClient

    // Estado del escaneo (Evento único)
    private val _scannedEvent = mutableStateOf<ScanEvent?>(null)
    private val _navigateToRead = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        googleAuthClient = GoogleAuthClient(this)

        val startDestination = if (Firebase.auth.currentUser != null) "menu" else "auth"

        setContent {
            DnD_NFCTheme {
                var currentScreen by remember { mutableStateOf(startDestination) }
                val scanEvent by _scannedEvent

                var selectedFullCharacter by remember { mutableStateOf<PlayerCharacter?>(null) }
                var charToWriteToNfc by remember { mutableStateOf<PlayerCharacter?>(null) }

                // Navegación automática si se escanea desde fuera
                val shouldNavigate by _navigateToRead
                LaunchedEffect(shouldNavigate) {
                    if (shouldNavigate && currentScreen != "read") {
                        currentScreen = "read"
                        _navigateToRead.value = false
                    }
                }

                when (currentScreen) {
                    "auth" -> AuthScreen(onLoginSuccess = { currentScreen = "menu" })

                    "menu" -> MainMenuScreen(
                        onNfcClick = {
                            _scannedEvent.value = null // <--- RESETEO 1: Limpiamos al entrar manual
                            currentScreen = "read"
                        },
                        onLibraryClick = { currentScreen = "library" },
                        onSignOutClick = {
                            googleAuthClient.signOut()
                            currentScreen = "auth"
                            _scannedEvent.value = null
                        }
                    )

                    "read" -> {
                        BackHandler {
                            _scannedEvent.value = null // Limpiamos al salir
                            currentScreen = "menu"
                        }

                        NfcReadScreen(
                            scanEvent = scanEvent,
                            onFullCharacterLoaded = { fullChar ->
                                // ¡ÉXITO!
                                selectedFullCharacter = fullChar

                                // <--- RESETEO 2 (CRÍTICO): Matamos el evento de escaneo AQUÍ
                                // Esto hace que la próxima vez que entres a "read", esté vacía.
                                _scannedEvent.value = null

                                currentScreen = "full_sheet"
                            },
                            onScanAgainClick = { _scannedEvent.value = null }
                        )
                    }

                    "library" -> {
                        BackHandler { currentScreen = "menu" }
                        CharacterListScreen(
                            onCharacterClick = { char ->
                                selectedFullCharacter = char
                                currentScreen = "full_sheet"
                            },
                            onNewCharacterClick = {
                                selectedFullCharacter = null
                                currentScreen = "full_sheet"
                            }
                        )
                    }

                    "full_sheet" -> {
                        BackHandler { currentScreen = "library" }
                        CharacterSheetScreen(
                            existingCharacter = selectedFullCharacter,
                            onBack = { currentScreen = "library" },
                            onWriteNfc = { char ->
                                charToWriteToNfc = char
                                currentScreen = "write"
                            }
                        )
                    }

                    "write" -> {
                        BackHandler {
                            currentScreen = "full_sheet"
                            pendingWriteData = null
                        }
                        NfcWriteScreen(
                            characterToWrite = charToWriteToNfc,
                            onReadyToWrite = { csvData ->
                                pendingWriteData = csvData
                                Toast.makeText(this, "Acerca la tarjeta para VINCULAR", Toast.LENGTH_LONG).show()
                            },
                            onBack = { currentScreen = "full_sheet" }
                        )
                    }

                    else -> { currentScreen = "menu" }
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
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            if (pendingWriteData != null && tag != null) {
                // ESCRITURA
                val success = NfcManager.writeToTag(tag, pendingWriteData!!)
                if (success) {
                    Toast.makeText(this, "¡Personaje vinculado exitosamente!", Toast.LENGTH_LONG).show()
                    pendingWriteData = null
                } else {
                    Toast.makeText(this, "Error al escribir.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // LECTURA
                val character = NfcManager.readFromIntent(intent)
                if (character != null) {
                    // Creamos un evento nuevo con timestamp actual
                    _scannedEvent.value = ScanEvent(character)
                    _navigateToRead.value = true
                } else {
                    Toast.makeText(this, "Formato NFC desconocido.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}