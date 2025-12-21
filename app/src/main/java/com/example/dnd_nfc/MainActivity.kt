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
    private var pendingWriteData: String? = null
    private lateinit var googleAuthClient: GoogleAuthClient

    private val _scannedCharacter = mutableStateOf<CharacterSheet?>(null)

    // Variable para controlar la navegación desde fuera de la UI (ej. al escanear)
    private val _navigateToRead = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        googleAuthClient = GoogleAuthClient(this)

        // Si ya hay usuario, vamos al MENÚ, no directo al lector
        val startDestination = if (Firebase.auth.currentUser != null) "menu" else "auth"

        setContent {
            DnD_NFCTheme {
                var currentScreen by remember { mutableStateOf(startDestination) }
                val scannedNfcChar by _scannedCharacter
                var selectedFullCharacter by remember { mutableStateOf<PlayerCharacter?>(null) }

                // Efecto para navegar automáticamente si se escanea algo en el menú
                val shouldNavigate by _navigateToRead
                LaunchedEffect(shouldNavigate) {
                    if (shouldNavigate && currentScreen != "read") {
                        currentScreen = "read"
                        _navigateToRead.value = false
                    }
                }

                when (currentScreen) {
                    // 1. LOGIN
                    "auth" -> {
                        AuthScreen(onLoginSuccess = { currentScreen = "menu" })
                    }

                    // 2. MENÚ PRINCIPAL (NUEVO HUB)
                    "menu" -> {
                        MainMenuScreen(
                            onNfcClick = { currentScreen = "read" },
                            onLibraryClick = { currentScreen = "library" },
                            onSignOutClick = {
                                googleAuthClient.signOut()
                                currentScreen = "auth"
                                _scannedCharacter.value = null
                            }
                        )
                    }

                    // 3. LECTOR NFC
                    "read" -> {
                        BackHandler { currentScreen = "menu" } // Volver al menú

                        NfcReadScreen(
                            character = scannedNfcChar,
                            isWaiting = true,
                            onCreateClick = { currentScreen = "write" },
                            onEditClick = {
                                if (scannedNfcChar != null) currentScreen = "edit"
                                else Toast.makeText(this, "Primero escanea un personaje", Toast.LENGTH_SHORT).show()
                            },
                            // Como ya tenemos menú, estos botones son atajos opcionales
                            onLibraryClick = { currentScreen = "library" },
                            onSignOutClick = {
                                googleAuthClient.signOut()
                                currentScreen = "auth"
                            },
                            onScanAgainClick = { _scannedCharacter.value = null }
                        )
                    }

                    // 4. BIBLIOTECA
                    "library" -> {
                        BackHandler { currentScreen = "menu" } // Volver al menú

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

                    // 5. HOJA COMPLETA
                    "full_sheet" -> {
                        BackHandler { currentScreen = "library" }
                        CharacterSheetScreen(
                            existingCharacter = selectedFullCharacter,
                            onBack = { currentScreen = "library" }
                        )
                    }

                    // 6. NFC ESCRITURA/EDICIÓN
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
                        } else { currentScreen = "read" }
                    }
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

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            if (pendingWriteData != null && tag != null) {
                // Modo Escritura
                val success = NfcManager.writeToTag(tag, pendingWriteData!!)
                if (success) {
                    Toast.makeText(this, "¡Escritura Exitosa!", Toast.LENGTH_LONG).show()
                    pendingWriteData = null
                    val updatedChar = NfcManager.readFromIntent(intent)
                    if (updatedChar != null) _scannedCharacter.value = updatedChar
                } else {
                    Toast.makeText(this, "Error al escribir.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Modo Lectura
                val character = NfcManager.readFromIntent(intent)
                if (character != null) {
                    _scannedCharacter.value = character
                    Toast.makeText(this, "¡Minis detectada: ${character.n}!", Toast.LENGTH_SHORT).show()
                    // Si estamos en el menú, forzamos la navegación al lector
                    _navigateToRead.value = true
                } else {
                    Toast.makeText(this, "Formato desconocido.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}