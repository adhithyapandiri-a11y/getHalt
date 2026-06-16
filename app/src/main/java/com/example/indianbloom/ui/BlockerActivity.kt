package com.example.indianbloom.ui

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.indianbloom.BloomApplication
import com.example.indianbloom.data.database.FocusSessionEntity
import com.example.indianbloom.data.database.FocusState
import com.example.indianbloom.data.nfc.NfcCrypto
import com.example.indianbloom.data.nfc.NfcHelper
import com.example.indianbloom.service.BloomForegroundService
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockerActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    private var sessionId by mutableStateOf<Long>(-1)
    private var blockedPackage by mutableStateOf("")
    private var isAntiUninstall by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionId = intent.getLongExtra("session_id", -1)
        blockedPackage = intent.getStringExtra("blocked_package") ?: ""
        isAntiUninstall = intent.getBooleanExtra("anti_uninstall", false)

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

        setContent {
            BlockerScreen(
                blockedPackage = blockedPackage,
                sessionId = sessionId,
                isAntiUninstall = isAntiUninstall,
                onEnableNfc = { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                onTriggerEmergency = { triggerEmergencyBypass() }
            )
        }
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
        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            // Handle NFC card tap to unlock Focus Mode / Strict Mode
            handleNfcUnlock(intent)
        } else {
            // Update blocked package details from the accessibility intercept launch
            sessionId = intent.getLongExtra("session_id", -1)
            blockedPackage = intent.getStringExtra("blocked_package") ?: ""
            isAntiUninstall = intent.getBooleanExtra("anti_uninstall", false)
        }
    }

    private fun handleNfcUnlock(intent: Intent) {
        val uid = NfcHelper.extractUid(intent)
        if (uid == null) {
            Toast.makeText(this, "Invalid card scan. Try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = NfcHelper.extractNdefPayload(intent)
        val app = application as BloomApplication
        val repository = app.repository

        lifecycleScope.launch(Dispatchers.IO) {
            val card = repository.getCard(uid)
            val isValid = if (card != null) {
                if (card.signatureHash == "READ_ONLY_FALLBACK") {
                    true
                } else {
                    payload != null && NfcCrypto.verifySignature(uid, payload)
                }
            } else {
                false
            }

            if (isValid) {
                // Cryptographic validation passes!
                if (isAntiUninstall) {
                    getSharedPreferences("auren_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("strict_mode", false)
                        .apply()
                }

                if (sessionId != -1L) {
                    repository.endFocusSession(sessionId)
                } else if (isAntiUninstall) {
                    // Turn off any active focus session too
                    val activeSession = repository.getActiveSession()
                    if (activeSession != null) {
                        repository.endFocusSession(activeSession.id)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (isAntiUninstall) {
                        Toast.makeText(this@BlockerActivity, "Strict Mode Disabled!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@BlockerActivity, "Auren Deactivated!", Toast.LENGTH_LONG).show()
                    }
                    BloomForegroundService.stopService(this@BlockerActivity)
                    finish()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BlockerActivity, "Untrusted card signature.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun triggerEmergencyBypass() {
        val app = application as BloomApplication
        val repository = app.repository
        lifecycleScope.launch(Dispatchers.IO) {
            repository.enterEmergencyCountdown(sessionId)
        }
    }

    // Intercept Back button press
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent dismissal by back button
        Toast.makeText(this, "Locked! Tap Auren Card to exit.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun BlockerScreen(
    blockedPackage: String,
    sessionId: Long,
    isAntiUninstall: Boolean,
    onEnableNfc: () -> Unit,
    onTriggerEmergency: () -> Unit
) {
    val app = LocalContext.current.applicationContext as BloomApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()

    var activeSession by remember { mutableStateOf<FocusSessionEntity?>(null) }
    var nfcEnabled by remember { mutableStateOf(true) }

    // Read active session state
    LaunchedEffect(sessionId) {
        repository.getActiveSessionFlow().collect {
            activeSession = it
        }
    }

    // Monitor NFC status
    LaunchedEffect(Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(app)
        while (true) {
            nfcEnabled = adapter?.isEnabled == true
            delay(1000)
        }
    }

    // Compute active timer delta
    var elapsedTimeStr by remember { mutableStateOf("00:00") }
    LaunchedEffect(activeSession) {
        while (activeSession?.state == FocusState.ACTIVE) {
            val delta = System.currentTimeMillis() - (activeSession?.startTime ?: System.currentTimeMillis())
            val secs = (delta / 1000) % 60
            val mins = (delta / (1000 * 60)) % 60
            val hrs = (delta / (1000 * 60 * 60))
            elapsedTimeStr = String.format("%02d:%02d:%02d", hrs, mins, secs)
            delay(1000)
        }
    }

    // Modern Minimalist Dark Obsidian background matching MainScreen
    val backgroundColor = Color(0xFF09090A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            // Minimalist dark container with a subtle metal border (Obsidian Slate Style)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131316)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF26262D)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Lock/Shield Indicator Circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF26262D))
                    ) {
                        Text(
                            text = if (isAntiUninstall) "🛡️" else "🔒",
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isAntiUninstall) "UNINSTALL BLOCKED" else "APP BLOCKED",
                        color = Color.White,
                        fontSize = if (isAntiUninstall) 20.sp else 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = if (isAntiUninstall) 2.sp else 4.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isAntiUninstall) "STRICT MODE ACTIVE" else "FOCUS ACTIVATED",
                        color = Color(0xFF8A8A93),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAntiUninstall) {
                        Text(
                            text = "Auren is locked to prevent uninstallation or bypassing.",
                            color = Color(0xFF8E8E93),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val appLabel = blockedPackage.substringAfterLast(".")
                        Text(
                            text = "Access to $appLabel is locked.",
                            color = Color(0xFF8E8E93),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    if (!isAntiUninstall) {
                        // Minimalist Elapsed Timer Panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF09090A))
                                .border(BorderStroke(1.dp, Color(0xFF26262D)), RoundedCornerShape(16.dp))
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "FOCUS ELAPSED",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = elapsedTimeStr,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        // Anti-Uninstall Info Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF2C0F12))
                                .border(BorderStroke(1.dp, Color(0xFF5E1A1E)), RoundedCornerShape(16.dp))
                                .padding(vertical = 16.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Strict Mode prevents uninstalling, force-stopping, or clearing data of Auren. Scan your paired Auren card to deactivate.",
                                color = Color(0xFFFF8B8B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Prompt message
                    Text(
                        text = if (isAntiUninstall) "Tap your physical Auren card to disable Strict Mode." else "Tap your physical Auren card to unlock your phone.",
                        color = Color(0xFF8E8E93),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // NFC disabled Warning UI
            if (!nfcEnabled) {
                Button(
                    onClick = onEnableNfc,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("⚠️ Turn NFC Back On", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Emergency trigger or countdown timer (only for focus sessions, not anti-uninstall)
            if (!isAntiUninstall) {
                val session = activeSession
                if (session != null) {
                    if (session.state == FocusState.EMERGENCY_COUNTDOWN) {
                        // Compute and render remaining emergency countdown
                        var timeLeftStr by remember { mutableStateOf("24:00:00") }
                        LaunchedEffect(Unit) {
                            while (true) {
                                val limit = session.startTime + (1000 * 60 * 60 * 24)
                                val remaining = limit - System.currentTimeMillis()
                                if (remaining <= 0) {
                                    repository.bypassFocusSession(session.id)
                                    BloomForegroundService.stopService(app)
                                    break
                                }
                                val secs = (remaining / 1000) % 60
                                val mins = (remaining / (1000 * 60)) % 60
                                val hrs = (remaining / (1000 * 60 * 60))
                                timeLeftStr = String.format("%02d:%02d:%02d", hrs, mins, secs)
                                delay(1000)
                            }
                        }
                        
                        Text(
                            text = "Emergency Bypass Countdown:\n$timeLeftStr",
                            color = Color(0xFFFF8B8B),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        TextButton(
                            onClick = onTriggerEmergency
                        ) {
                            Text(
                                text = "Lost card? Start Emergency 24H Countdown",
                                color = Color(0xFFD4AF37),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
