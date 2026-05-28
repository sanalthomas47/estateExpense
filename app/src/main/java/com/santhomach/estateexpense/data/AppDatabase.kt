package com.santhomach.estateexpense.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase as RawSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.santhomach.estateexpense.data.dao.DailyExpenseDao
import com.santhomach.estateexpense.data.dao.ExpenseTypeDao
import com.santhomach.estateexpense.data.dao.ExpenseSubtypeDao
import com.santhomach.estateexpense.data.dao.IncomeTypeDao
import com.santhomach.estateexpense.data.dao.WorkerTypeDao
import com.santhomach.estateexpense.data.dao.PermanentWorkerDao
import com.santhomach.estateexpense.data.dao.WeeklySettlementDao
import com.santhomach.estateexpense.data.dao.ExcessBalanceDao
import com.santhomach.estateexpense.data.dao.WorkTaskDao
import com.santhomach.estateexpense.data.dao.WorkerPaymentDao
import com.santhomach.estateexpense.data.dao.WeeklyFundsDao
import com.santhomach.estateexpense.data.dao.VendorPaymentDao
import com.santhomach.estateexpense.data.model.DailyExpense
import com.santhomach.estateexpense.data.model.ExpenseType
import com.santhomach.estateexpense.data.model.ExpenseSubtype
import com.santhomach.estateexpense.data.model.IncomeType
import com.santhomach.estateexpense.data.model.WorkerType
import com.santhomach.estateexpense.data.model.PermanentWorker
import com.santhomach.estateexpense.data.model.WeeklySettlement
import com.santhomach.estateexpense.data.model.ExcessBalance
import com.santhomach.estateexpense.data.model.WorkTask
import com.santhomach.estateexpense.data.model.WorkerPayment
import com.santhomach.estateexpense.data.model.VendorPayment
import java.io.File

@Database(
    entities = [
        DailyExpense::class,
        ExpenseType::class,
        ExpenseSubtype::class,
        IncomeType::class,
        WorkerType::class,
        PermanentWorker::class,
        WeeklySettlement::class,
        ExcessBalance::class,
        WorkTask::class,
        WorkerPayment::class,
        com.santhomach.estateexpense.data.model.WeeklyFunds::class,
        VendorPayment::class
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyExpenseDao(): DailyExpenseDao
    abstract fun expenseTypeDao(): ExpenseTypeDao
    abstract fun expenseSubtypeDao(): ExpenseSubtypeDao
    abstract fun incomeTypeDao(): IncomeTypeDao
    abstract fun workerTypeDao(): WorkerTypeDao
    abstract fun permanentWorkerDao(): PermanentWorkerDao
    abstract fun weeklySettlementDao(): WeeklySettlementDao
    abstract fun excessBalanceDao(): ExcessBalanceDao
    abstract fun workTaskDao(): WorkTaskDao
    abstract fun workerPaymentDao(): WorkerPaymentDao
    abstract fun weeklyFundsDao(): WeeklyFundsDao
    abstract fun vendorPaymentDao(): VendorPaymentDao

    companion object {
        const val DATABASE_VERSION = 10
        private const val DATABASE_NAME = "estate_expense.db"
        private const val BACKUP_NAME = "pre_upgrade_backup.db"

        // Insert order matters: parent tables before child tables (FK constraints).
        private val ALL_TABLES = listOf(
            "expense_types",
            "expense_subtypes",
            "income_types",
            "worker_types",
            "permanent_workers",
            "work_tasks",
            "daily_expenses",
            "weekly_settlements",
            "excess_balances",
            "worker_payments",
            "weekly_funds",
            "vendor_payments"
        )

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val backupFile = File(context.filesDir, BACKUP_NAME)

            // Only attempt a pre-upgrade backup when the on-disk version differs AND
            // we don't already have a pending backup from a previous failed restore.
            if (dbFile.exists() && !backupFile.exists()) {
                val diskVersion = readDatabaseVersion(dbFile)
                if (diskVersion > 0 && diskVersion != DATABASE_VERSION) {
                    val copied = copyDatabaseFiles(dbFile, backupFile)
                    if (copied) {
                        // Backup is safe — remove the original so Room creates a fresh
                        // schema via onCreate instead of hitting a version mismatch.
                        // If deleteOriginal fails here the DB is untouched; Room will
                        // throw a migration error but no data is lost.
                        deleteFile(dbFile)
                    }
                    // If copy itself failed, backupFile does not exist and the original
                    // is intact. Room will throw "migration required" — no data loss.
                }
            }

            // No fallbackToDestructiveMigration — Room must never silently wipe tables.
            // Schema mismatches that aren't covered by an explicit migration will surface
            // as a clear crash rather than silent data loss.
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
                .addCallback(AutoRestoreCallback(backupFile))
                .build()
        }

        // ---------------------------------------------------------------------------
        // Raw-SQLite helpers (used before Room is involved)
        // ---------------------------------------------------------------------------

        private fun readDatabaseVersion(file: File): Int = try {
            RawSQLiteDatabase.openDatabase(
                file.absolutePath, null, RawSQLiteDatabase.OPEN_READONLY
            ).use { it.version }
        } catch (e: Exception) { -1 }

        /** Returns true only when all files were copied successfully. */
        private fun copyDatabaseFiles(source: File, dest: File): Boolean {
            return try {
                source.copyTo(dest, overwrite = true)
                // Copy WAL / SHM so the backup captures the full committed state.
                listOf("-wal", "-shm").forEach { suffix ->
                    val extra = File("${source.absolutePath}$suffix")
                    if (extra.exists()) {
                        extra.copyTo(File("${dest.absolutePath}$suffix"), overwrite = true)
                    }
                }
                true
            } catch (e: Exception) {
                // Remove any partial copy so we don't restore a corrupt file.
                dest.delete()
                File("${dest.absolutePath}-wal").delete()
                File("${dest.absolutePath}-shm").delete()
                false
            }
        }

        private fun deleteFile(file: File) {
            file.delete()
            File("${file.absolutePath}-wal").delete()
            File("${file.absolutePath}-shm").delete()
        }

        // ---------------------------------------------------------------------------
        // Auto-restore callback — runs after Room creates the new empty schema
        // ---------------------------------------------------------------------------

        private class AutoRestoreCallback(private val backupFile: File) : RoomDatabase.Callback() {

            override fun onOpen(db: SupportSQLiteDatabase) {
                if (!backupFile.exists()) return

                var restoreSucceeded = false
                val escapedPath = backupFile.absolutePath.replace("'", "''")
                try {
                    // Disable FK checks for the bulk insert — we restore in dependency
                    // order already, but this removes any ordering risk.
                    db.execSQL("PRAGMA foreign_keys = OFF")
                    db.execSQL("ATTACH DATABASE '$escapedPath' AS backup")

                    db.beginTransaction()
                    try {
                        ALL_TABLES.forEach { table -> restoreTable(db, table) }
                        db.setTransactionSuccessful()
                        restoreSucceeded = true
                    } finally {
                        db.endTransaction()
                    }

                    db.execSQL("DETACH DATABASE backup")
                } catch (e: Exception) {
                    try { db.execSQL("DETACH DATABASE backup") } catch (ex: Exception) { /* ignore */ }
                } finally {
                    db.execSQL("PRAGMA foreign_keys = ON")
                    if (restoreSucceeded) {
                        // Only remove the backup once we know the data is safely in
                        // the new database. If restore failed the backup stays so the
                        // next launch can retry.
                        backupFile.delete()
                        File("${backupFile.absolutePath}-wal").delete()
                        File("${backupFile.absolutePath}-shm").delete()
                    }
                }
            }

            private fun restoreTable(db: SupportSQLiteDatabase, table: String) {
                try {
                    val newCols = tableColumns(db, "main", table)
                    val oldCols = tableColumns(db, "backup", table)
                    val common = newCols.intersect(oldCols)
                    if (common.isEmpty()) return

                    val cols = common.joinToString(", ") { "\"$it\"" }
                    // INSERT OR IGNORE preserves rows that already exist (e.g. after a
                    // normal migration that left data in place).
                    db.execSQL(
                        "INSERT OR IGNORE INTO \"$table\" ($cols) " +
                        "SELECT $cols FROM backup.\"$table\""
                    )
                } catch (e: Exception) {
                    // Table doesn't exist in backup (new table added in this version) — skip.
                }
            }

            private fun tableColumns(
                db: SupportSQLiteDatabase,
                schema: String,
                table: String
            ): Set<String> {
                val cols = mutableSetOf<String>()
                try {
                    db.query("PRAGMA $schema.table_info($table)").use { cursor ->
                        val nameIdx = cursor.getColumnIndex("name")
                        if (nameIdx >= 0) {
                            while (cursor.moveToNext()) cols.add(cursor.getString(nameIdx))
                        }
                    }
                } catch (e: Exception) { /* ignore */ }
                return cols
            }
        }

        // ---------------------------------------------------------------------------
        // Explicit migrations (always the preferred path — write one per version bump)
        // ---------------------------------------------------------------------------

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE daily_expenses ADD COLUMN extraOvertimeAmount TEXT NOT NULL DEFAULT '0'"
                )
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `vendor_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `vendorName` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAt` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vendor_payments_date` ON `vendor_payments` (`date`)"
                )
            }
        }
    }
}
