package com.santhomach.estateexpense.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.santhomach.estateexpense.data.export.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class DailyBackupManager(
    private val context: Context,
    private val exportManager: ExportManager,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val LAST_BACKUP_DATE_KEY = stringPreferencesKey("last_auto_backup_date")
        private const val BACKUP_DIR = "auto_backups"
        private const val MAX_BACKUPS = 7
    }

    suspend fun maybeBackup() {
        try {
            val todayStr = LocalDate.now().toString()
            val prefs = dataStore.data.first()
            if (prefs[LAST_BACKUP_DATE_KEY] == todayStr) return

            val jsonData = exportManager.exportToJson()

            withContext(Dispatchers.IO) {
                val backupDir = File(context.filesDir, BACKUP_DIR).apply { mkdirs() }
                File(backupDir, "backup_$todayStr.json").writeText(jsonData)

                backupDir.listFiles()
                    ?.sortedByDescending { it.name }
                    ?.drop(MAX_BACKUPS)
                    ?.forEach { it.delete() }
            }

            dataStore.edit { it[LAST_BACKUP_DATE_KEY] = todayStr }
        } catch (_: Exception) {
            // Backup is best-effort; never surface errors to the user
        }
    }
}
