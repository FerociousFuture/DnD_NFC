package com.example.dnd_nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.dnd_nfc.data.model.CharacterSheet
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.AuthScreen
import com.example.dnd_nfc.ui.screens.NfcReadScreen
import com.example.dnd_nfc.ui.screens.NfcWriteScreen
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    // Variable para guardar los datos pendientes de escribir
    private var pendingWriteData: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Verificamos si ya hay usuario logueado al iniciar
        val startDestination = if (Firebase.auth.currentUser != null) "read" else "auth"

        setContent {
            DnD_NFCTheme {
                // Estado de navegación simple
                var currentScreen by remember { mutableStateOf(startDestination) }
                var scannedCharacter by remember { mutableStateOf<CharacterSheet?>(null) }

                // --- PANTALLAS ---
                when (currentScreen) {
                    "auth" -> AuthScreen(
                        onLoginSuccess = { currentScreen = "read" }
                    )
                    "read" -> NfcReadScreen(
                        character = scannedCharacter,
                        isWaiting = true,
                        onCreateClick = { currentScreen = "write" } // Botón para ir a escribir
                    )
                    "write" -> NfcWriteScreen(
                        onReadyToWrite = { csvData ->
                            pendingWriteData = csvData
                            Toast.makeText(this, "¡Listo! Acerca una etiqueta NFC vacía", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }

    // --- LÓGICA NFC (No cambia mucho) ---
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
                    Toast.makeText(this, "¡Personaje grabado con éxito!", Toast.LENGTH_LONG).show()
                    pendingWriteData = null // Reseteamos
                } else {
                    Toast.makeText(this, "Error al escribir. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // MODO LECTURA
                val character = NfcManager.readFromIntent(intent)
                if (character != null) {
                    Toast.makeText(this, "Leído: ${character.n}", Toast.LENGTH_SHORT).show()
                    // NOTA: En una app real usarías un ViewModel para actualizar la UI aquí
                }
            }
        }
    }
}