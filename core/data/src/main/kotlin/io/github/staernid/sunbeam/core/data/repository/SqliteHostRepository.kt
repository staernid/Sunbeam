@file:Suppress(
    "TooGenericExceptionCaught",
    "SwallowedException",
    "TooManyFunctions",
    "MagicNumber",
)

package io.github.staernid.sunbeam.core.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.data.model.SunshineServerCapabilities
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Host repository backed by the legacy "computers4.db" SQLite database to ensure 100% data preservation
 * and zero-risk host pairing continuity.
 */
class SqliteHostRepository(
    private val context: Context,
    private val dbName: String = COMPUTER_DB_NAME,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : HostRepository {
    private val db: SQLiteDatabase by lazy {
        val database = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        initDatabase(database)
        database
    }

    private val dynamicHostStates = ConcurrentHashMap<String, Host>()
    private val _hostsFlow = MutableStateFlow<List<Host>>(emptyList())
    override val hostsFlow: Flow<List<Host>> = _hostsFlow.asStateFlow()

    init {
        // Eagerly populate initial hosts from database so hostsFlow is immediately populated on launch
        try {
            refreshStateFlow()
        } catch (e: Exception) {
            // Will retry in initialize()
        }
    }

    private fun initDatabase(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_UUID TEXT PRIMARY KEY,
                $COL_NAME TEXT NOT NULL,
                $COL_ADDRESSES TEXT NOT NULL,
                $COL_MAC TEXT,
                $COL_SERVER_CERT BLOB
            )
            """.trimIndent(),
        )
    }

    suspend fun initialize() =
        withContext(ioDispatcher) {
            refreshStateFlow()
        }

    override suspend fun getAllHosts(): List<Host> =
        withContext(ioDispatcher) {
            val list = mutableListOf<Host>()
            val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
            cursor.use {
                while (it.moveToNext()) {
                    list.add(readHostFromCursor(it))
                }
            }
            list
        }

    override suspend fun getHostByUuid(uuid: String): Host? =
        withContext(ioDispatcher) {
            val cursor =
                db.query(
                    TABLE_NAME,
                    null,
                    "$COL_UUID = ?",
                    arrayOf(uuid),
                    null,
                    null,
                    null,
                )
            cursor.use {
                if (it.moveToFirst()) {
                    readHostFromCursor(it)
                } else {
                    null
                }
            }
        }

    override suspend fun saveHost(host: Host): Unit =
        withContext(ioDispatcher) {
            val values =
                ContentValues().apply {
                    put(COL_UUID, host.uuid)
                    put(COL_NAME, host.name)
                    put(COL_ADDRESSES, addressesToJson(host))
                    put(COL_MAC, host.macAddress)
                    put(COL_SERVER_CERT, host.serverCertDer)
                }
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            dynamicHostStates[host.uuid] = host
            refreshStateFlow()
        }

    override suspend fun deleteHost(uuid: String): Unit =
        withContext(ioDispatcher) {
            db.delete(TABLE_NAME, "$COL_UUID = ?", arrayOf(uuid))
            dynamicHostStates.remove(uuid)
            refreshStateFlow()
        }

    override fun notifyHostUpdated(host: Host) {
        dynamicHostStates[host.uuid] = host
        val current = _hostsFlow.value
        val existingIndex = current.indexOfFirst { it.uuid == host.uuid }
        if (existingIndex >= 0) {
            val updated = current.toMutableList()
            updated[existingIndex] = host
            _hostsFlow.value = updated
        } else {
            refreshStateFlow()
        }
    }

    private fun refreshStateFlow() {
        val list = mutableListOf<Host>()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(readHostFromCursor(it))
            }
        }
        _hostsFlow.value = list
    }

    private fun readHostFromCursor(cursor: Cursor): Host {
        val uuid = cursor.getString(0)
        val name = cursor.getString(1)
        val addressesJson = cursor.getString(2)
        val mac = cursor.getString(3)
        val certBlob = cursor.getBlob(4)

        var localAddr: HostAddress? = null
        var remoteAddr: HostAddress? = null
        var manualAddr: HostAddress? = null
        var ipv6Addr: HostAddress? = null

        try {
            val json = JSONObject(addressesJson)
            localAddr = addressFromJson(json.optJSONObject(FIELD_LOCAL))
            remoteAddr = addressFromJson(json.optJSONObject(FIELD_REMOTE))
            manualAddr = addressFromJson(json.optJSONObject(FIELD_MANUAL))
            ipv6Addr = addressFromJson(json.optJSONObject(FIELD_IPV6))
        } catch (e: JSONException) {
            // Ignore corrupted address JSON
        }

        val isPaired = certBlob != null && certBlob.isNotEmpty()
        val cached = dynamicHostStates[uuid]

        return Host(
            uuid = uuid,
            name = name,
            localAddress = localAddr,
            remoteAddress = remoteAddr,
            manualAddress = manualAddr,
            ipv6Address = ipv6Addr,
            macAddress = mac,
            serverCertDer = certBlob,
            state = cached?.state ?: HostState.UNKNOWN,
            pairState = cached?.pairState ?: if (isPaired) PairState.PAIRED else PairState.NOT_PAIRED,
            currentGameId = cached?.currentGameId ?: 0,
            currentGameUuid = cached?.currentGameUuid,
            capabilities = cached?.capabilities ?: SunshineServerCapabilities.DEFAULT,
        )
    }

    private fun addressesToJson(host: Host): String {
        val root = JSONObject()
        root.put(FIELD_LOCAL, addressToJson(host.localAddress))
        root.put(FIELD_REMOTE, addressToJson(host.remoteAddress))
        root.put(FIELD_MANUAL, addressToJson(host.manualAddress))
        root.put(FIELD_IPV6, addressToJson(host.ipv6Address))
        return root.toString()
    }

    private fun addressToJson(address: HostAddress?): JSONObject? {
        if (address == null) return null
        return JSONObject().apply {
            put(FIELD_ADDRESS, address.address)
            put(FIELD_PORT, address.port)
        }
    }

    private fun addressFromJson(json: JSONObject?): HostAddress? {
        if (json == null) return null
        val addr = json.optString(FIELD_ADDRESS, "")
        val port = json.optInt(FIELD_PORT, 0)
        return if (addr.isNotEmpty() && port > 0) HostAddress(addr, port) else null
    }

    companion object {
        const val COMPUTER_DB_NAME = "computers4.db"
        private const val TABLE_NAME = "Computers"
        private const val COL_UUID = "UUID"
        private const val COL_NAME = "ComputerName"
        private const val COL_ADDRESSES = "Addresses"
        private const val COL_MAC = "MacAddress"
        private const val COL_SERVER_CERT = "ServerCert"

        private const val FIELD_LOCAL = "local"
        private const val FIELD_REMOTE = "remote"
        private const val FIELD_MANUAL = "manual"
        private const val FIELD_IPV6 = "ipv6"

        private const val FIELD_ADDRESS = "address"
        private const val FIELD_PORT = "port"
    }
}
