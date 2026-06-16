package com.example.indianbloom.data.repository

import com.example.indianbloom.data.database.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BloomRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val nfcCardDao = database.nfcCardDao()
    private val focusSessionDao = database.focusSessionDao()
    private val blockedAppDao = database.blockedAppDao()

    // --- User Profile Handling ---
    fun getUserFlow(): Flow<UserEntity?> = userDao.getUserFlow()

    suspend fun getOrCreateUser(deviceId: String): UserEntity {
        val existing = userDao.getUser()
        if (existing != null) return existing

        val newUser = UserEntity(
            id = UUID.randomUUID().toString(),
            deviceId = deviceId,
            registeredAt = System.currentTimeMillis()
        )
        userDao.insertUser(newUser)
        return newUser
    }

    // --- NFC Card Logic ---
    fun getCardsFlow(): Flow<List<NfcCardEntity>> = nfcCardDao.getAllCardsFlow()

    suspend fun getCard(uid: String): NfcCardEntity? = nfcCardDao.getCardByUid(uid)

    suspend fun registerCard(uid: String, userId: String, signatureHash: String, alias: String?) {
        val card = NfcCardEntity(
            uid = uid,
            userId = userId,
            signatureHash = signatureHash,
            pairedAt = System.currentTimeMillis(),
            aliasName = alias
        )
        nfcCardDao.insertCard(card)
    }

    suspend fun unregisterCard(uid: String) {
        val card = nfcCardDao.getCardByUid(uid)
        if (card != null) {
            nfcCardDao.deleteCard(card)
        }
    }

    // --- Focus Sessions ---
    fun getActiveSessionFlow(): Flow<FocusSessionEntity?> = focusSessionDao.getActiveSessionFlow()

    suspend fun getActiveSession(): FocusSessionEntity? = focusSessionDao.getActiveSession()

    fun getAllSessionsFlow(): Flow<List<FocusSessionEntity>> = focusSessionDao.getAllSessionsFlow()

    suspend fun startFocusSession(cardUid: String): Long {
        // Ensure any orphan active sessions are marked COMPLETED first
        val active = focusSessionDao.getActiveSession()
        if (active != null) {
            focusSessionDao.updateSession(active.copy(endTime = System.currentTimeMillis(), state = FocusState.COMPLETED))
        }

        val session = FocusSessionEntity(
            cardUid = cardUid,
            startTime = System.currentTimeMillis(),
            endTime = null,
            state = FocusState.ACTIVE
        )
        return focusSessionDao.insertSession(session)
    }

    suspend fun endFocusSession(sessionId: Long) {
        val active = focusSessionDao.getActiveSession()
        if (active != null && active.id == sessionId) {
            focusSessionDao.updateSession(
                active.copy(
                    endTime = System.currentTimeMillis(),
                    state = FocusState.COMPLETED
                )
            )
        }
    }

    suspend fun enterEmergencyCountdown(sessionId: Long) {
        val active = focusSessionDao.getActiveSession()
        if (active != null && active.id == sessionId) {
            focusSessionDao.updateSession(
                active.copy(
                    state = FocusState.EMERGENCY_COUNTDOWN
                )
            )
        }
    }

    suspend fun bypassFocusSession(sessionId: Long) {
        val active = focusSessionDao.getActiveSession()
        if (active != null && active.id == sessionId) {
            focusSessionDao.updateSession(
                active.copy(
                    endTime = System.currentTimeMillis(),
                    state = FocusState.BYPASSED
                )
            )
        }
    }

    suspend fun incrementBlockCount(sessionId: Long) {
        focusSessionDao.incrementBlocksTriggered(sessionId)
    }

    // --- Blocklist App Control ---
    fun getBlockedAppsFlow(): Flow<List<BlockedAppEntity>> = blockedAppDao.getAllBlockedAppsFlow()

    suspend fun getBlockedApps(): List<BlockedAppEntity> = blockedAppDao.getAllBlockedApps()

    suspend fun isAppBlocked(packageName: String): Boolean = blockedAppDao.isAppBlocked(packageName)

    suspend fun addAppToBlocklist(packageName: String, appName: String, isCustom: Boolean = false) {
        blockedAppDao.insertBlockedApp(BlockedAppEntity(packageName, appName, isCustom))
    }

    suspend fun removeAppFromBlocklist(packageName: String) {
        blockedAppDao.deleteBlockedAppByPackage(packageName)
    }

    suspend fun setupDefaultBlocklist() {
        val defaults = listOf(
            BlockedAppEntity("com.instagram.android", "Instagram"),
            BlockedAppEntity("com.google.android.youtube", "YouTube"),
            BlockedAppEntity("com.snapchat.android", "Snapchat"),
            BlockedAppEntity("com.twitter.android", "X / Twitter"),
            BlockedAppEntity("com.reddit.frontpage", "Reddit"),
            BlockedAppEntity("com.android.chrome", "Google Chrome") // Browser redirect example
        )
        blockedAppDao.insertBlockedApps(defaults)
    }
}
