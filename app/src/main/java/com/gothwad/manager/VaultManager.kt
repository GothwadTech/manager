package com.gothwad.manager

import android.content.Context
import android.os.Environment
import java.io.File
import java.security.MessageDigest

object VaultManager {

    private const val PREF_VAULT = "gothwad_vault_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val VAULT_DIR_NAME = ".gothwad_vault"

    fun getVaultDir(context: Context): File {
        val root = Environment.getExternalStorageDirectory() ?: context.filesDir
        val dir = File(root, VAULT_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_VAULT, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    fun setPin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREF_VAULT, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN_HASH, hashPin(pin)).apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_VAULT, Context.MODE_PRIVATE)
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return savedHash == hashPin(pin)
    }

    fun moveToVault(context: Context, file: File): Boolean {
        if (!file.exists()) return false
        val vaultDir = getVaultDir(context)
        val dest = File(vaultDir, file.name)
        return if (file.renameTo(dest)) {
            true
        } else {
            try {
                FileUtils.copy(file, dest)
                FileUtils.deleteRecursively(file)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getVaultFiles(context: Context): List<File> {
        val dir = getVaultDir(context)
        return dir.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()
    }

    fun restoreFromVault(context: Context, file: File): Boolean {
        val destDir = File(Environment.getExternalStorageDirectory(), "Download")
        if (!destDir.exists()) destDir.mkdirs()
        val dest = File(destDir, file.name)
        return if (file.renameTo(dest)) {
            true
        } else {
            try {
                FileUtils.copy(file, dest)
                FileUtils.deleteRecursively(file)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
