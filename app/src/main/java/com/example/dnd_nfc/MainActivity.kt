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
    // Variable para guardar los datos pendientes de escribir en NFC (CSV)
    private var pendingWriteData: String? = null

    private lateinit var googleAuthClient: GoogleAuthClient

    // Estado del personaje escaneado por NFC (fuera de setContent para acceso en onNewIntent)
    private val _scannedCharacter = mutableStateOf<CharacterSheet?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de hardware y servicios
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        googleAuthClient = GoogleAuthClient(this)

        // Verificamos sesión para decidir pantalla inicial
        val startDestination = if (Firebase.auth.currentUser != null) "read" else "auth"

        setContent {
            DnD_NFCTheme {
                // --- ESTADOS DE NAVEGACIÓN ---
                var currentScreen by remember { mutableStateOf(startDestination) }

                // Estado del Lector NFC
                val scannedNfcChar by _scannedCharacter

                // Estado para la Biblioteca (Ficha Completa)
                var selectedFullCharacter by remember { mutableStateOf<PlayerCharacter?>(null) }

                when (currentScreen) {
                    // 1. LOGIN
                    "auth" -> {
                        AuthScreen(
                            onLoginSuccess = { currentScreen = "read" }
                        )
                    }

                    // 2. HOME / LECTOR NFC
                    "read" -> {
                        NfcReadScreen(
                            character = scannedNfcChar,
                            isWaiting = true,
                            onCreateClick = { currentScreen = "write" }, // Ir a Grabar NFC
                            onEditClick = {
                                if (scannedNfcChar != null) {
                                    currentScreen = "edit"
                                } else {
                                    Toast.makeText(this, "Primero escanea un personaje", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLibraryClick = { currentScreen = "library" }, // Ir a Biblioteca
                            onSignOutClick = {
                                googleAuthClient.signOut()
                                currentScreen = "auth"
                                _scannedCharacter.value = null
                            },
                            onScanAgainClick = { _scannedCharacter.value = null }
                        )
                    }

                    // 3. BIBLIOTECA DE PERSONAJES (CLOUD)
                    "library" -> {
                        BackHandler { currentScreen = "read" } // Atrás -> Home
                        CharacterListScreen(
                            onCharacterClick = { char ->
                                selectedFullCharacter = char
                                currentScreen = "full_sheet"
                            },
                            onNewCharacterClick = {
                                selectedFullCharacter = null // Null indica crear nuevo
                                currentScreen = "full_sheet"
                            }
                        )
                    }

                    // 4. HOJA DE PERSONAJE COMPLETA (TABS)
                    "full_sheet" -> {
                        BackHandler { currentScreen = "library" } // Atrás -> Lista
                        CharacterSheetScreen(
                            existingCharacter = selectedFullCharacter,
                            onBack = { currentScreen = "library" }
                        )
                    }

                    // 5. EDITOR RÁPIDO NFC
                    "edit" -> {
                        BackHandler { currentScreen = "read" }
                        if (scannedNfcChar != null) {
                            NfcEditScreen(
                                character = scannedNfcChar!!,
                                onReadyToWrite = { csvData ->
                                    pendingWriteData = csvData
                                    Toast.makeText(this, "Acerca la etiqueta para SOBREESCRIBIR", Toast.LENGTH_LONG).show()
                                },
                                onCancel = { currentScreen = "read" }
                            )
                        } else {
                            currentScreen = "read"
                        }
                    }

                    // 6. CREADOR NFC NUEVO
                    "write" -> {
                        BackHandler {
                            currentScreen = "read"
                            pendingWriteData = null
                        }
                        NfcWriteScreen(
                            onReadyToWrite = { csvData ->
                                pendingWriteData = csvData
                                Toast.makeText(this, "Acerca una etiqueta NFC vacía", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // --- CICLO DE VIDA NFC ---
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

    // --- DETECCIÓN DE ETIQUETA ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            // CASO A: ESCRITURA PENDIENTE
            if (pendingWriteData != null && tag != null) {
                val success = NfcManager.writeToTag(tag, pendingWriteData!!)
                if (success) {
                    Toast.makeText(this, "¡Escritura Exitosa!", Toast.LENGTH_LONG).show()
                    pendingWriteData = null

                    // Opcional: Leer lo que acabamos de escribir para confirmar visualmente
                    val updatedChar = NfcManager.readFromIntent(intent)
                    if (updatedChar != null) _scannedCharacter.value = updatedChar

                } else {
                    Toast.makeText(this, "Error al escribir. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                }
            }
            // CASO B: LECTURA NORMAL
            else {
                val character = NfcManager.readFromIntent(intent)
                if (character != null) {
                    _scannedCharacter.value = character
                    Toast.makeText(this, "¡Minis detectada: ${character.n}!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Etiqueta leída, pero formato desconocido.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}