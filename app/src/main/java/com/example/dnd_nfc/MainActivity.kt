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
import com.example.dnd_nfc.data.remote.GoogleAuthClient // Asegúrate de importar esto
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

    // Instancia del cliente de autenticación
    private lateinit var googleAuthClient: GoogleAuthClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Inicializamos el cliente
        googleAuthClient = GoogleAuthClient(this)

        // LÓGICA DE PERSISTENCIA:
        // Si currentUser NO es null, el usuario sigue logueado y va directo a "read"
        val startDestination = if (Firebase.auth.currentUser != null) "read" else "auth"

        setContent {
            DnD_NFCTheme {
                var currentScreen by remember { mutableStateOf(startDestination) }
                var scannedCharacter by remember { mutableStateOf<CharacterSheet?>(null) }

                when (currentScreen) {
                    "auth" -> AuthScreen(
                        onLoginSuccess = { currentScreen = "read" }
                    )
                    "read" -> NfcReadScreen(
                        character = scannedCharacter,
                        isWaiting = true,
                        onCreateClick = { currentScreen = "write" },
                        onSignOutClick = {
                            // CERRAR SESIÓN
                            googleAuthClient.signOut()
                            currentScreen = "auth"
                            scannedCharacter = null // Limpiamos datos viejos
                            Toast.makeText(applicationContext, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                        }
                    )
                    "write" -> NfcWriteScreen(
                        onReadyToWrite = { csvData ->
                            pendingWriteData = csvData
                            Toast.makeText(this, "Acerca una etiqueta NFC para grabar", Toast.LENGTH_LONG).show()
                        }
                    )
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
                // Escritura
                val success = NfcManager.writeToTag(tag, pendingWriteData!!)
                if (success) {
                    Toast.makeText(this, "¡Personaje grabado!", Toast.LENGTH_LONG).show()
                    pendingWriteData = null
                } else {
                    Toast.makeText(this, "Error al escribir", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Lectura
                val character = NfcManager.readFromIntent(intent)
                if (character != null) {
                    // Aquí es donde en un futuro usaremos ViewModel, por ahora mostramos Toast
                    Toast.makeText(this, "Leído: ${character.n}", Toast.LENGTH_SHORT).show()

                    // TRUCO TEMPORAL: Forzamos recomposición (en una app real usa ViewModel)
                    // Como estamos dentro de onNewIntent, no tenemos acceso directo fácil a 'scannedCharacter' del setContent
                    // Por ahora, solo verás el Toast.
                    // Para ver la tarjeta en pantalla necesitaríamos mover 'scannedCharacter' a un ViewModel o variable global.
                }
            }
        }
    }
}