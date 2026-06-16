package com.example.indianbloom

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.indianbloom.data.database.FocusState
import com.example.indianbloom.data.nfc.NfcCrypto
import com.example.indianbloom.data.nfc.NfcHelper
import com.example.indianbloom.service.BloomForegroundService
import com.example.indianbloom.theme.IndianBloomTheme
import com.example.indianbloom.ui.main.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    
    // UI state to know if we are currently waiting to pair a card
    private var isWaitingToPairCard by mutableStateOf(false)

    // UI state to know if we are waiting to disable strict mode
    private var isWaitingToDisableStrictMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        val intentFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val nfcIntent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, intentFlags)

        enableEdgeToEdge()
        setContent {
            IndianBloomTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isWaitingToPairCard = isWaitingToPairCard,
                        onSetWaitingToPair = { isWaitingToPairCard = it },
                        isWaitingToDisableStrictMode = isWaitingToDisableStrictMode,
                        onSetWaitingToDisableStrictMode = { isWaitingToDisableStrictMode = it }
                    )
                }
            }
        }
        
        // Process intent if started via cold boot from NFC event
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action || 
            NfcAdapter.ACTION_TECH_DISCOVERED == action || 
            NfcAdapter.ACTION_TAG_DISCOVERED == action) {
            val uid = NfcHelper.extractUid(intent)
            if (uid == null) {
                Toast.makeText(this, "NFC read error: UID missing", Toast.LENGTH_SHORT).show()
                return
            }

            val app = application as BloomApplication
            val repository = app.repository

            lifecycleScope.launch(Dispatchers.IO) {
                val user = repository.getOrCreateUser(
                    Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
                )
                val card = repository.getCard(uid)

                 if (isWaitingToPairCard) {
                    // --- CARD PAIRING MODE ---
                    if (card != null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Card already paired!", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Cryptographically sign UID
                    val signature = NfcCrypto.generateSignature(uid)
                    
                    // Write NDEF text payload back to tag
                    val success = NfcHelper.writeNdefPayload(intent, signature)
                    if (success) {
                        repository.registerCard(uid, user.id, signature, "Auren Focus Card")
                        isWaitingToPairCard = false
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Auren Card Registered!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Fallback: If card is read-only (like contactless ATM, transit cards)
                        repository.registerCard(uid, user.id, "READ_ONLY_FALLBACK", "Read-Only Key")
                        isWaitingToPairCard = false
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Auren Card Registered (UID Fallback)!", Toast.LENGTH_LONG).show()
                        }
                    }
                } else if (isWaitingToDisableStrictMode) {
                    // --- DISABLE STRICT MODE ---
                    if (card == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Card not recognized. Use your paired Auren card.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val payload = NfcHelper.extractNdefPayload(intent)
                    val isValid = if (card.signatureHash == "READ_ONLY_FALLBACK") {
                        true
                    } else {
                        payload != null && NfcCrypto.verifySignature(uid, payload)
                    }

                    if (isValid) {
                        getSharedPreferences("auren_prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("strict_mode", false)
                            .apply()
                        isWaitingToDisableStrictMode = false
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Strict Mode Disabled!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Untrusted signature verification.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // --- TOGGLE FOCUS SESSION ---
                    if (card == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Card not recognized. Pair it first.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val payload = NfcHelper.extractNdefPayload(intent)
                    val isValid = if (card.signatureHash == "READ_ONLY_FALLBACK") {
                        true // Bypass NDEF reading for ATM/Transit cards
                    } else {
                        payload != null && NfcCrypto.verifySignature(uid, payload)
                    }

                    if (isValid) {
                        val activeSession = repository.getActiveSession()
                        if (activeSession != null && activeSession.state == FocusState.ACTIVE) {
                            // Stop focus session
                            repository.endFocusSession(activeSession.id)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Auren Deactivated!", Toast.LENGTH_LONG).show()
                                BloomForegroundService.stopService(this@MainActivity)
                            }
                        } else {
                            // Start focus session
                            repository.startFocusSession(uid)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Auren Activated! Focus mode on.", Toast.LENGTH_LONG).show()
                                BloomForegroundService.startService(this@MainActivity)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Untrusted signature verification.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}
