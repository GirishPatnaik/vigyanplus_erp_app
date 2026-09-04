package com.vigyan.juniorcollege.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
    exportSchema = true
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

        fun getInstance(context: Context): VigyanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VigyanDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
        }
    }
}
