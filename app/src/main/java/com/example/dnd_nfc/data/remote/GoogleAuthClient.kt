package com.example.dnd_nfc.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class GoogleAuthClient(private val context: Context) {
    private val auth = Firebase.auth
    private val credentialManager = CredentialManager.create(context)

    // REEMPLAZA ESTO CON TU WEB CLIENT ID COPIADO DE FIREBASE CONSOLE
    private val WEB_CLIENT_ID = "TU_WEB_CLIENT_ID_AQUI.apps.googleusercontent.com"

    suspend fun signIn(): Boolean {
        try {
            // 1. Configuramos la petición a Google
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(true) // Autologin si ya dio permiso antes
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 2. Mostramos el diálogo al usuario
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // 3. Extraemos el token del resultado
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                // 4. Intercambiamos el token de Google por uno de Firebase
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                auth.signInWithCredential(authCredential).await()
                return true
            }
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuth", "Error en credenciales: ${e.message}")
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Error general: ${e.message}")
        }
        return false
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser() = auth.currentUser
}