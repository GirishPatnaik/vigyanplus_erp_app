package com.vigyan.juniorcollege.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vigyan.juniorcollege.data.local.dao.*
import com.vigyan.juniorcollege.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        AuditLogEntity::class,
        StudentEntity::class,
        AttendanceEntity::class,
        AcademicYearEntity::class,
        CourseEntity::class,
        StreamEntity::class,
        ClassEntity::class,
        BatchEntity::class,
        SectionEntity::class,
        CollegeDetailsEntity::class,
        ImportHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VigyanDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun masterDao(): MasterDao
    abstract fun importHistoryDao(): ImportHistoryDao

    companion object {
        const val DB_NAME = "vigyan_erp.db"

        @Volatile
        private var INSTANCE: VigyanDatabase? = null

        /**
         * v1 -> v2: adds cloud-sync tracking columns (remoteId) to every entity that
         * can now sync via Supabase, plus updatedAt on college_details for merge
         * conflict resolution. A real migration (not destructive) so any test data
         * already on a device survives the app update.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE students ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE attendance ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE academic_years ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE courses ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE streams ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE college_classes ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE batches ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE sections ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE college_details ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE college_details ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): VigyanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VigyanDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
