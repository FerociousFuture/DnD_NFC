package com.example.dnd_nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.dnd_nfc.data.model.CharacterSheet
import com.example.dnd_nfc.nfc.NfcManager
import com.example.dnd_nfc.ui.screens.NfcReadScreen
import com.example.dnd_nfc.ui.theme.DnD_NFCTheme

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null

    // Estado de la UI
    private var scannedCharacter by mutableStateOf<CharacterSheet?>(null)
    private var isWaiting by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            DnD_NFCTheme {
                NfcReadScreen(
                    character = scannedCharacter,
                    isWaiting = isWaiting
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Activa la escucha prioritaria del NFC cuando la app está abierta
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val result = NfcManager.readFromIntent(intent)
            if (result != null) {
                scannedCharacter = result
                isWaiting = false
            }
        }
    }
}