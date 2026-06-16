package com.example.indianbloom.data.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

// --- Entities (Replicated from Entities.kt for seamless compilation) ---

data class UserEntity(
    val id: String,
    val deviceId: String,
    val registeredAt: Long,
    val isPremium: Boolean = false
)

data class NfcCardEntity(
    val uid: String,
    val userId: String,
    val signatureHash: String,
    val pairedAt: Long,
    val aliasName: String?
)

enum class FocusState {
    ACTIVE,
    COMPLETED,
    EMERGENCY_COUNTDOWN,
    BYPASSED
}

data class FocusSessionEntity(
    val id: Long = 0,
    val cardUid: String,
    val startTime: Long,
    val endTime: Long?,
    val state: FocusState,
    val totalBlocksTriggered: Int = 0
)

data class BlockedAppEntity(
    val packageName: String,
    val appName: String,
    val isCustom: Boolean = false
)

// --- Simulated Database Interface ---

class AppDatabase private constructor(context: Context) {

    private val dbHelper = BloomDbHelper(context)

    // Flow triggers to notify subscribers of database updates (replaces Room's invalidation tracker)
    private val usersTrigger = MutableStateFlow(System.currentTimeMillis())
    private val cardsTrigger = MutableStateFlow(System.currentTimeMillis())
    private val sessionsTrigger = MutableStateFlow(System.currentTimeMillis())
    private val blockedAppsTrigger = MutableStateFlow(System.currentTimeMillis())

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Support Direct Boot Mode where files are locked. Store in device protected context.
                val protectedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.createDeviceProtectedStorageContext()
                } else {
                    context
                }
                
                val instance = AppDatabase(protectedContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // --- DAO Accessors ---

    fun userDao() = object : UserDao {
        override fun getUserFlow(): Flow<UserEntity?> = usersTrigger.flatMapLatest {
            flow { emit(getUser()) }
        }

        override suspend fun getUser(): UserEntity? {
            val db = dbHelper.readableDatabase
            var user: UserEntity? = null
            val cursor = db.rawQuery("SELECT * FROM users LIMIT 1", null)
            if (cursor.moveToFirst()) {
                user = UserEntity(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    deviceId = cursor.getString(cursor.getColumnIndexOrThrow("deviceId")),
                    registeredAt = cursor.getLong(cursor.getColumnIndexOrThrow("registeredAt")),
                    isPremium = cursor.getInt(cursor.getColumnIndexOrThrow("isPremium")) == 1
                )
            }
            cursor.close()
            return user
        }

        override suspend fun insertUser(user: UserEntity) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", user.id)
                put("deviceId", user.deviceId)
                put("registeredAt", user.registeredAt)
                put("isPremium", if (user.isPremium) 1 else 0)
            }
            db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            usersTrigger.value = System.currentTimeMillis()
        }

        override suspend fun updateUser(user: UserEntity) {
            insertUser(user)
        }
    }

    fun nfcCardDao() = object : NfcCardDao {
        override fun getAllCardsFlow(): Flow<List<NfcCardEntity>> = cardsTrigger.flatMapLatest {
            flow { emit(getAllCards()) }
        }

        private fun getAllCards(): List<NfcCardEntity> {
            val db = dbHelper.readableDatabase
            val list = mutableListOf<NfcCardEntity>()
            val cursor = db.rawQuery("SELECT * FROM nfc_cards", null)
            while (cursor.moveToNext()) {
                list.add(
                    NfcCardEntity(
                        uid = cursor.getString(cursor.getColumnIndexOrThrow("uid")),
                        userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                        signatureHash = cursor.getString(cursor.getColumnIndexOrThrow("signatureHash")),
                        pairedAt = cursor.getLong(cursor.getColumnIndexOrThrow("pairedAt")),
                        aliasName = cursor.getString(cursor.getColumnIndexOrThrow("aliasName"))
                    )
                )
            }
            cursor.close()
            return list
        }

        override suspend fun getCardByUid(uid: String): NfcCardEntity? {
            val db = dbHelper.readableDatabase
            var card: NfcCardEntity? = null
            val cursor = db.rawQuery("SELECT * FROM nfc_cards WHERE uid = ? LIMIT 1", arrayOf(uid))
            if (cursor.moveToFirst()) {
                card = NfcCardEntity(
                    uid = cursor.getString(cursor.getColumnIndexOrThrow("uid")),
                    userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                    signatureHash = cursor.getString(cursor.getColumnIndexOrThrow("signatureHash")),
                    pairedAt = cursor.getLong(cursor.getColumnIndexOrThrow("pairedAt")),
                    aliasName = cursor.getString(cursor.getColumnIndexOrThrow("aliasName"))
                )
            }
            cursor.close()
            return card
        }

        override suspend fun insertCard(card: NfcCardEntity) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("uid", card.uid)
                put("userId", card.userId)
                put("signatureHash", card.signatureHash)
                put("pairedAt", card.pairedAt)
                put("aliasName", card.aliasName)
            }
            db.insertWithOnConflict("nfc_cards", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            cardsTrigger.value = System.currentTimeMillis()
        }

        override suspend fun deleteCard(card: NfcCardEntity) {
            val db = dbHelper.writableDatabase
            db.delete("nfc_cards", "uid = ?", arrayOf(card.uid))
            cardsTrigger.value = System.currentTimeMillis()
        }
    }

    fun focusSessionDao() = object : FocusSessionDao {
        override suspend fun getActiveSession(): FocusSessionEntity? {
            val db = dbHelper.readableDatabase
            var session: FocusSessionEntity? = null
            val cursor = db.rawQuery(
                "SELECT * FROM focus_sessions WHERE state = 'ACTIVE' OR state = 'EMERGENCY_COUNTDOWN' LIMIT 1",
                null
            )
            if (cursor.moveToFirst()) {
                val endTimeVal = cursor.getLong(cursor.getColumnIndexOrThrow("endTime"))
                session = FocusSessionEntity(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cardUid = cursor.getString(cursor.getColumnIndexOrThrow("cardUid")),
                    startTime = cursor.getLong(cursor.getColumnIndexOrThrow("startTime")),
                    endTime = if (cursor.isNull(cursor.getColumnIndexOrThrow("endTime"))) null else endTimeVal,
                    state = FocusState.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("state"))),
                    totalBlocksTriggered = cursor.getInt(cursor.getColumnIndexOrThrow("totalBlocksTriggered"))
                )
            }
            cursor.close()
            return session
        }

        override fun getActiveSessionFlow(): Flow<FocusSessionEntity?> = sessionsTrigger.flatMapLatest {
            flow { emit(getActiveSession()) }
        }

        override fun getAllSessionsFlow(): Flow<List<FocusSessionEntity>> = sessionsTrigger.flatMapLatest {
            flow {
                val db = dbHelper.readableDatabase
                val list = mutableListOf<FocusSessionEntity>()
                val cursor = db.rawQuery("SELECT * FROM focus_sessions ORDER BY startTime DESC", null)
                while (cursor.moveToNext()) {
                    val endTimeVal = cursor.getLong(cursor.getColumnIndexOrThrow("endTime"))
                    list.add(
                        FocusSessionEntity(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            cardUid = cursor.getString(cursor.getColumnIndexOrThrow("cardUid")),
                            startTime = cursor.getLong(cursor.getColumnIndexOrThrow("startTime")),
                            endTime = if (cursor.isNull(cursor.getColumnIndexOrThrow("endTime"))) null else endTimeVal,
                            state = FocusState.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("state"))),
                            totalBlocksTriggered = cursor.getInt(cursor.getColumnIndexOrThrow("totalBlocksTriggered"))
                        )
                    )
                }
                cursor.close()
                emit(list)
            }
        }

        override suspend fun insertSession(session: FocusSessionEntity): Long {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("cardUid", session.cardUid)
                put("startTime", session.startTime)
                if (session.endTime == null) putNull("endTime") else put("endTime", session.endTime)
                put("state", session.state.name)
                put("totalBlocksTriggered", session.totalBlocksTriggered)
            }
            val id = db.insert("focus_sessions", null, values)
            sessionsTrigger.value = System.currentTimeMillis()
            return id
        }

        override suspend fun updateSession(session: FocusSessionEntity) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("cardUid", session.cardUid)
                put("startTime", session.startTime)
                if (session.endTime == null) putNull("endTime") else put("endTime", session.endTime)
                put("state", session.state.name)
                put("totalBlocksTriggered", session.totalBlocksTriggered)
            }
            db.update("focus_sessions", values, "id = ?", arrayOf(session.id.toString()))
            sessionsTrigger.value = System.currentTimeMillis()
        }

        override suspend fun incrementBlocksTriggered(sessionId: Long) {
            val db = dbHelper.writableDatabase
            db.execSQL(
                "UPDATE focus_sessions SET totalBlocksTriggered = totalBlocksTriggered + 1 WHERE id = ?",
                arrayOf(sessionId.toString())
            )
            sessionsTrigger.value = System.currentTimeMillis()
        }
    }

    fun blockedAppDao() = object : BlockedAppDao {
        override fun getAllBlockedAppsFlow(): Flow<List<BlockedAppEntity>> = blockedAppsTrigger.flatMapLatest {
            flow { emit(getAllBlockedApps()) }
        }

        override suspend fun getAllBlockedApps(): List<BlockedAppEntity> {
            val db = dbHelper.readableDatabase
            val list = mutableListOf<BlockedAppEntity>()
            val cursor = db.rawQuery("SELECT * FROM blocked_apps", null)
            while (cursor.moveToNext()) {
                list.add(
                    BlockedAppEntity(
                        packageName = cursor.getString(cursor.getColumnIndexOrThrow("packageName")),
                        appName = cursor.getString(cursor.getColumnIndexOrThrow("appName")),
                        isCustom = cursor.getInt(cursor.getColumnIndexOrThrow("isCustom")) == 1
                    )
                )
            }
            cursor.close()
            return list
        }

        override suspend fun isAppBlocked(packageName: String): Boolean {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT 1 FROM blocked_apps WHERE packageName = ? LIMIT 1", arrayOf(packageName))
            val exists = cursor.count > 0
            cursor.close()
            return exists
        }

        override suspend fun insertBlockedApp(app: BlockedAppEntity) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("packageName", app.packageName)
                put("appName", app.appName)
                put("isCustom", if (app.isCustom) 1 else 0)
            }
            db.insertWithOnConflict("blocked_apps", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            blockedAppsTrigger.value = System.currentTimeMillis()
        }

        override suspend fun insertBlockedApps(apps: List<BlockedAppEntity>) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                for (app in apps) {
                    val values = ContentValues().apply {
                        put("packageName", app.packageName)
                        put("appName", app.appName)
                        put("isCustom", if (app.isCustom) 1 else 0)
                    }
                    db.insertWithOnConflict("blocked_apps", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            blockedAppsTrigger.value = System.currentTimeMillis()
        }

        override suspend fun deleteBlockedApp(app: BlockedAppEntity) {
            deleteBlockedAppByPackage(app.packageName)
        }

        override suspend fun deleteBlockedAppByPackage(packageName: String) {
            val db = dbHelper.writableDatabase
            db.delete("blocked_apps", "packageName = ?", arrayOf(packageName))
            blockedAppsTrigger.value = System.currentTimeMillis()
        }
    }

    // --- Inner SQLiteOpenHelper ---

    private class BloomDbHelper(context: Context) : SQLiteOpenHelper(context, "bloom_sqlite.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE users (
                    id TEXT PRIMARY KEY,
                    deviceId TEXT NOT NULL,
                    registeredAt INTEGER NOT NULL,
                    isPremium INTEGER DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE nfc_cards (
                    uid TEXT PRIMARY KEY,
                    userId TEXT NOT NULL,
                    signatureHash TEXT NOT NULL,
                    pairedAt INTEGER NOT NULL,
                    aliasName TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE focus_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cardUid TEXT NOT NULL,
                    startTime INTEGER NOT NULL,
                    endTime INTEGER,
                    state TEXT NOT NULL,
                    totalBlocksTriggered INTEGER DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE blocked_apps (
                    packageName TEXT PRIMARY KEY,
                    appName TEXT NOT NULL,
                    isCustom INTEGER DEFAULT 0
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS users")
            db.execSQL("DROP TABLE IF EXISTS nfc_cards")
            db.execSQL("DROP TABLE IF EXISTS focus_sessions")
            db.execSQL("DROP TABLE IF EXISTS blocked_apps")
            onCreate(db)
        }
    }
}

// --- Interface Mimicry for Repository Compatibility ---

interface UserDao {
    fun getUserFlow(): Flow<UserEntity?>
    suspend fun getUser(): UserEntity?
    suspend fun insertUser(user: UserEntity)
    suspend fun updateUser(user: UserEntity)
}

interface NfcCardDao {
    fun getAllCardsFlow(): Flow<List<NfcCardEntity>>
    suspend fun getCardByUid(uid: String): NfcCardEntity?
    suspend fun insertCard(card: NfcCardEntity)
    suspend fun deleteCard(card: NfcCardEntity)
}

interface FocusSessionDao {
    suspend fun getActiveSession(): FocusSessionEntity?
    fun getActiveSessionFlow(): Flow<FocusSessionEntity?>
    fun getAllSessionsFlow(): Flow<List<FocusSessionEntity>>
    suspend fun insertSession(session: FocusSessionEntity): Long
    suspend fun updateSession(session: FocusSessionEntity)
    suspend fun incrementBlocksTriggered(sessionId: Long)
}

interface BlockedAppDao {
    fun getAllBlockedAppsFlow(): Flow<List<BlockedAppEntity>>
    suspend fun getAllBlockedApps(): List<BlockedAppEntity>
    suspend fun isAppBlocked(packageName: String): Boolean
    suspend fun insertBlockedApp(app: BlockedAppEntity)
    suspend fun insertBlockedApps(apps: List<BlockedAppEntity>)
    suspend fun deleteBlockedApp(app: BlockedAppEntity)
    suspend fun deleteBlockedAppByPackage(packageName: String)
}
