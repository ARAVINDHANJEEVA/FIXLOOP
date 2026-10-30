package com.fixloop.mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import com.fixloop.mobile.data.remote.model.AuthUserDto
import com.fixloop.mobile.data.remote.model.StudentProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _isLoggedInFlow = MutableStateFlow(isLoggedIn())
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    fun saveAuth(token: String, user: AuthUserDto) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, user.id)
            .putString(KEY_NAME, user.name)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_ROLE, user.role)
            .putString(KEY_INITIALS, user.avatarInitials ?: "")
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
        _isLoggedInFlow.value = true
    }

    fun updateProfile(profile: StudentProfileDto) {
        val editor = prefs.edit()
        profile.name?.let { editor.putString(KEY_NAME, it) }
        profile.email?.let { editor.putString(KEY_EMAIL, it) }
        profile.rollNo?.let { editor.putString(KEY_ROLL_NO, it) }
        profile.departmentName?.let { editor.putString(KEY_DEPARTMENT, it) }
        profile.defaultLocationBuilding?.let { editor.putString(KEY_HOSTEL_BLOCK, it) }
        profile.defaultLocationRoom?.let { editor.putString(KEY_ROOM_NO, it) }
        profile.avatarInitials?.let { editor.putString(KEY_INITIALS, it) }
        editor.apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getName(): String = prefs.getString(KEY_NAME, "Alex Johnson") ?: "Alex Johnson"

    fun getEmail(): String = prefs.getString(KEY_EMAIL, "student@fixloop.edu") ?: "student@fixloop.edu"

    fun getRollNo(): String = prefs.getString(KEY_ROLL_NO, "22CS084") ?: "22CS084"

    fun getDepartment(): String = prefs.getString(KEY_DEPARTMENT, "Computer Science & Engineering") ?: "Computer Science & Engineering"

    fun getHostelBlock(): String = prefs.getString(KEY_HOSTEL_BLOCK, "Hostel Block C (Aryabhata)") ?: "Hostel Block C (Aryabhata)"

    fun getRoomNo(): String = prefs.getString(KEY_ROOM_NO, "214") ?: "214"

    fun getInitials(): String = prefs.getString(KEY_INITIALS, "AJ") ?: "AJ"

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false) && getToken() != null

    // -------------------------------------------------------------------------
    // Connection Mode — how the phone reaches the backend (running on laptop)
    // -------------------------------------------------------------------------

    /**
     * Which connection mode is currently active.
     *
     * Topology: Phone creates Wi-Fi Hotspot → Laptop connects as client.
     * Android assigns the laptop a DHCP IP in the 192.168.43.x range.
     */
    enum class ConnectionMode {
        /** Android emulator — 10.0.2.2 routes to the host machine loopback. */
        EMULATOR,

        /** Physical phone + USB cable with `adb reverse tcp:8080 tcp:8080`. Phone loopback → laptop port 8080. */
        USB,

        /**
         * **Phone is the hotspot, laptop is the client**.
         * The laptop gets a dynamic IP (192.168.43.x) via Android hotspot DHCP.
         * User enters the laptop's current IP once in Connection Settings.
         * Run `ipconfig` on the laptop to find its IP on the hotspot network.
         */
        HOTSPOT,

        /**
         * **IP-resilient mode** — survives DHCP IP changes with zero reconfiguration.
         * Laptop publishes itself as `fixloop.local` via mDNS (Zeroconf/Bonjour).
         * The phone resolves the hostname automatically on the hotspot network.
         * One-time setup: run `start-mdns.bat` on the laptop.
         */
        MDNS,

        /** Fully custom URL entered by the user. */
        CUSTOM
    }

    fun getConnectionMode(): ConnectionMode {
        val stored = prefs.getString(KEY_CONNECTION_MODE, null)
        return try {
            if (stored != null) ConnectionMode.valueOf(stored) else defaultConnectionMode()
        } catch (_: IllegalArgumentException) {
            defaultConnectionMode()
        }
    }

    fun setConnectionMode(mode: ConnectionMode) {
        prefs.edit().putString(KEY_CONNECTION_MODE, mode.name).apply()
        // Invalidate the cached Retrofit instance so the next call picks up the new URL
        com.fixloop.mobile.data.remote.NetworkClient.invalidate()
    }

    private fun defaultConnectionMode(): ConnectionMode =
        if (isEmulator()) ConnectionMode.EMULATOR else ConnectionMode.HOTSPOT

    /** Returns the effective base URL for the current connection mode. */
    fun getBaseUrl(): String {
        return when (getConnectionMode()) {
            ConnectionMode.EMULATOR -> EMULATOR_BASE_URL
            ConnectionMode.USB      -> USB_DEVICE_BASE_URL
            ConnectionMode.HOTSPOT  -> {
                val savedIp = prefs.getString(KEY_HOTSPOT_IP, null)
                if (!savedIp.isNullOrBlank()) "http://$savedIp:8080/" else HOTSPOT_DEFAULT_BASE_URL
            }
            ConnectionMode.MDNS     -> MDNS_BASE_URL
            ConnectionMode.CUSTOM   -> {
                val saved = prefs.getString(KEY_BASE_URL, null)
                if (!saved.isNullOrBlank()) saved else HOTSPOT_DEFAULT_BASE_URL
            }
        }
    }

    /**
     * Saves the laptop's IP address as seen on the phone's hotspot network.
     * Example: "192.168.43.100"
     * Find this by running `ipconfig` on the laptop and looking for the
     * "Wireless LAN adapter" or "Wi-Fi" section under your phone's hotspot SSID.
     */
    fun setHotspotIp(ip: String) {
        prefs.edit().putString(KEY_HOTSPOT_IP, ip.trim()).apply()
        com.fixloop.mobile.data.remote.NetworkClient.invalidate()
    }

    fun getHotspotIp(): String =
        prefs.getString(KEY_HOTSPOT_IP, "192.168.43.100") ?: "192.168.43.100"

    /** Sets a fully custom base URL (used by CUSTOM mode). */
    fun setBaseUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
        com.fixloop.mobile.data.remote.NetworkClient.invalidate()
    }

    fun getCustomUrl(): String =
        prefs.getString(KEY_BASE_URL, "") ?: ""

    private fun isEmulator(): Boolean {
        val fingerprint = android.os.Build.FINGERPRINT ?: ""
        val model = android.os.Build.MODEL ?: ""
        val manufacturer = android.os.Build.MANUFACTURER ?: ""
        return (fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion"))
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()
        _isLoggedInFlow.value = false
    }

    companion object {
        private const val PREF_NAME = "sentinel_session_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_ROLL_NO = "user_roll_no"
        private const val KEY_DEPARTMENT = "user_dept"
        private const val KEY_HOSTEL_BLOCK = "user_hostel_block"
        private const val KEY_ROOM_NO = "user_room_no"
        private const val KEY_INITIALS = "user_initials"
        private const val KEY_ROLE = "user_role"
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_BASE_URL = "custom_base_url"
        private const val KEY_CONNECTION_MODE = "connection_mode"
        private const val KEY_HOTSPOT_IP = "hotspot_laptop_ip"

        // ---- Base URL constants ----

        /** Android emulator → host machine loopback. */
        const val EMULATOR_BASE_URL = "http://10.0.2.2:8080/"

        /**
         * USB cable + `adb reverse tcp:8080 tcp:8080`.
         * Phone loopback (127.0.0.1) tunnels to the laptop's port 8080.
         */
        const val USB_DEVICE_BASE_URL = "http://127.0.0.1:8080/"

        /**
         * Phone is the hotspot, laptop connects to it.
         * Android hotspot DHCP assigns the laptop an IP in the 192.168.43.x range.
         * This is the default starting guess. User should enter the exact IP.
         * Find it on the laptop: open CMD → `ipconfig` → look for Wi-Fi / WLAN adapter.
         */
        const val HOTSPOT_DEFAULT_BASE_URL = "http://192.168.43.100:8080/"

        /**
         * mDNS / Bonjour — IP-resilient, zero-config after one-time setup.
         * Laptop runs `start-mdns.bat` to register itself as `fixloop.local`.
         * Android resolves this hostname on the hotspot network automatically.
         * The IP can change freely — the app always finds the laptop.
         */
        const val MDNS_BASE_URL = "http://fixloop.local:8080/"
    }
}
