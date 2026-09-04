package com.vigyan.juniorcollege.ui.nav

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val STUDENT_LIST = "student_list"
    const val STUDENT_ADD = "student_add"
    const val STUDENT_EDIT = "student_edit/{studentId}"
    const val STUDENT_PROFILE = "student_profile/{studentId}"
    const val ATTENDANCE_MARK = "attendance_mark"
    const val ATTENDANCE_HISTORY = "attendance_history/{studentId}"
    const val REPORTS = "reports"
    const val MASTER_SETTINGS = "master_settings"
    const val BACKUP = "backup"
    const val CSV_IMPORT = "csv_import"
    const val RECYCLE_BIN = "recycle_bin"
    const val CHANGE_PASSWORD = "change_password"

    fun studentEdit(id: Long) = "student_edit/$id"
    fun studentProfile(id: Long) = "student_profile/$id"
    fun attendanceHistory(id: Long) = "attendance_history/$id"
}
