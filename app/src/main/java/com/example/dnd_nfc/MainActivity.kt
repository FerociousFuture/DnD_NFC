package com.example.dnd_nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler // <--- IMPORTANTE: Importar esto
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.dnd_nfc.data.model.CharacterSheet
import com.example.dnd_nfc.data.remote.GoogleAuthClient
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.AuthScreen
import com.example.dnd_nfc.ui.screens.NfcReadScreen
import com.example.dnd_nfc.ui.screens.NfcWriteScreen
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingWriteData: String? = null
    private lateinit var googleAuthClient: GoogleAuthClient

    // Estado del personaje escaneado fuera del setContent para que onNewIntent lo vea
    private val _scannedCharacter = mutableStateOf<CharacterSheet?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        googleAuthClient = GoogleAuthClient(this)

        // Determinamos la pantalla inicial
        val startDestination = if (Firebase.auth.currentUser != null) "read" else "auth"

        setContent {
            DnD_NFCTheme {
                // Estado de la navegación
                var currentScreen by remember { mutableStateOf(startDestination) }

                // Observamos el personaje escaneado
                val characterState by _scannedCharacter

                when (currentScreen) {
                    "auth" -> {
                        AuthScreen(
                            onLoginSuccess = { currentScreen = "read" }
                        )
                    }
                    "read" -> {
                        // Si estás en el Home y das Atrás, sale de la app (comportamiento normal)
                        NfcReadScreen(
                            character = characterState,
                            isWaiting = true,
                            onCreateClick = { currentScreen = "write" },
                            onSignOutClick = {
                                googleAuthClient.signOut()
                                currentScreen = "auth"
                                _scannedCharacter.value = null
                                Toast.makeText(applicationContext, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                            },
                            onScanAgainClick = {
                                _scannedCharacter.value = null
                            }
                        )
                    }
                    "write" -> {
                        // --- AQUÍ ESTÁ LA MAGIA ---
                        // Interceptamos el botón "Atrás" físico del celular
                        BackHandler(enabled = true) {
                            // En lugar de cerrar la app, volvemos al lector
                            currentScreen = "read"
                            // Opcional: Cancelamos cualquier escritura pendiente
                            pendingWriteData = null
                        }

                        NfcWriteScreen(
                            onReadyToWrite = { csvData ->
                                pendingWriteData = csvData
                                Toast.makeText(this, "Acerca una etiqueta NFC para grabar", Toast.LENGTH_LONG).show()
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
                // MODO ESCRITURA
                val success = NfcManager.writeToTag(tag, pendingWriteData!!)
                if (success) {
                    Toast.makeText(this, "¡Personaje grabado correctamente!", Toast.LENGTH_LONG).show()
                    pendingWriteData = null
                    // Opcional: Podrías limpiar el formulario o volver a leer aquí
                } else {
                    Toast.makeText(this, "Error al escribir en la etiqueta", Toast.LENGTH_SHORT).show()
                }
            } else {
                // MODO LECTURA
                val character = NfcManager.readFromIntent(intent)
                if (character != null) {
                    _scannedCharacter.value = character
                    Toast.makeText(this, "Leído: ${character.n}", Toast.LENGTH_SHORT).show()
                } else {
                    // Si lee algo pero no es un personaje válido
                    Toast.makeText(this, "Etiqueta leída, pero no contiene un personaje válido", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}