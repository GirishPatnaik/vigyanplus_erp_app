package com.vigyan.juniorcollege.data.local

import androidx.room.TypeConverter
import com.vigyan.juniorcollege.data.local.entity.AttendanceStatus
import com.vigyan.juniorcollege.data.local.entity.Gender
import com.vigyan.juniorcollege.data.local.entity.StudentStatus
import com.vigyan.juniorcollege.data.local.entity.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name
    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromGender(value: Gender): String = value.name
    @TypeConverter
    fun toGender(value: String): Gender = Gender.valueOf(value)

    @TypeConverter
    fun fromStudentStatus(value: StudentStatus): String = value.name
    @TypeConverter
    fun toStudentStatus(value: String): StudentStatus = StudentStatus.valueOf(value)

    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus): String = value.name
    @TypeConverter
    fun toAttendanceStatus(value: String): AttendanceStatus = AttendanceStatus.valueOf(value)
}
