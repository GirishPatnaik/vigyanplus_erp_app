package com.vigyan.juniorcollege.data.repository

import android.content.Context
import android.net.Uri
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import com.vigyan.juniorcollege.data.local.dao.ImportHistoryDao
import com.vigyan.juniorcollege.data.local.dao.MasterDao
import com.vigyan.juniorcollege.data.local.dao.StudentDao
import com.vigyan.juniorcollege.data.local.entity.Gender
import com.vigyan.juniorcollege.data.local.entity.ImportHistoryEntity
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import com.vigyan.juniorcollege.data.local.entity.StudentStatus
import kotlinx.coroutines.flow.first
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class CsvRowError(val rowNumber: Int, val reason: String)

data class CsvImportSummary(
    val totalRows: Int,
    val successful: Int,
    val failed: Int,
    val errors: List<CsvRowError>
)

// Column order — kept in sync with Master Settings' academic-structure categories
// so a bulk import can place a student into the exact same structure an admin
// would pick from dropdowns when adding one manually.
private val CSV_HEADERS = arrayOf(
    "AdmissionNo", "RollNo", "StudentName", "FatherName", "MotherName",
    "DOB(yyyy-MM-dd)", "Gender(MALE/FEMALE/OTHER)", "Address",
    "MobileNo1", "MobileNo2", "WhatsappNo", "Board",
    "AcademicYear", "Course", "Stream", "Class", "Batch", "Section"
)

class CsvRepository(
    private val context: Context,
    private val studentDao: StudentDao,
    private val masterDao: MasterDao,
    private val importHistoryDao: ImportHistoryDao
) {

    /**
     * Writes a CSV template to the given Uri (from a SAF file picker). The sample
     * row uses the FIRST entry of each academic-structure category currently
     * configured in Master Settings, so it reflects your actual setup rather
     * than generic placeholder values — delete that sample row before importing.
     */
    suspend fun writeTemplate(uri: Uri) {
        val academicYear = masterDao.observeAcademicYears().first().firstOrNull()?.label ?: ""
        val course = masterDao.observeCourses().first().firstOrNull()
        val stream = course?.let { c -> masterDao.observeStreamsForCourse(c.id).first().firstOrNull() }
        val clazz = masterDao.observeClasses().first().firstOrNull()?.name ?: ""
        val batch = masterDao.observeBatches().first().firstOrNull()?.name ?: ""
        val section = masterDao.observeSections().first().firstOrNull()?.name ?: ""

        context.contentResolver.openOutputStream(uri)?.use { out ->
            CSVWriter(OutputStreamWriter(out)).use { writer ->
                writer.writeNext(CSV_HEADERS)
                writer.writeNext(
                    arrayOf(
                        "2025001", "1", "Sample Student (delete this row)", "Father Name", "Mother Name",
                        "2008-05-14", "MALE", "Sample Address",
                        "9999999999", "", "9999999999", "CHSE Odisha",
                        academicYear, course?.name ?: "", stream?.name ?: "", clazz, batch, section
                    )
                )
            }
        }
    }

    /** Exports all active students as CSV, including their resolved academic structure names. */
    suspend fun exportStudents(uri: Uri, students: List<StudentEntity>) {
        val academicYearsById = masterDao.observeAcademicYears().first().associateBy { it.id }
        val coursesById = masterDao.observeCourses().first().associateBy { it.id }
        val streamsById = masterDao.observeAllStreams().first().associateBy { it.id }
        val classesById = masterDao.observeClasses().first().associateBy { it.id }
        val batchesById = masterDao.observeBatches().first().associateBy { it.id }
        val sectionsById = masterDao.observeSections().first().associateBy { it.id }

        context.contentResolver.openOutputStream(uri)?.use { out ->
            CSVWriter(OutputStreamWriter(out)).use { writer ->
                writer.writeNext(CSV_HEADERS)
                students.forEach { s ->
                    writer.writeNext(
                        arrayOf(
                            s.admissionNo, s.rollNo, s.studentName, s.fatherName, s.motherName,
                            s.dob, s.gender.name, s.address, s.mobileNo1, s.mobileNo2,
                            s.whatsappNo, s.boardName,
                            academicYearsById[s.academicYearId]?.label ?: "",
                            coursesById[s.courseId]?.name ?: "",
                            streamsById[s.streamId]?.name ?: "",
                            classesById[s.classId]?.name ?: "",
                            batchesById[s.batchId]?.name ?: "",
                            sectionsById[s.sectionId]?.name ?: ""
                        )
                    )
                }
            }
        }
    }

    /**
     * Reads and validates a CSV file, then imports valid rows. Duplicate admission
     * numbers, missing required fields, and academic-structure values that don't
     * match anything in Master Settings are rejected per-row (so a typo'd Class
     * name doesn't silently import against nothing).
     */
    suspend fun importFromCsv(uri: Uri, fileName: String): CsvImportSummary {
        val errors = mutableListOf<CsvRowError>()
        var successful = 0
        var totalRows = 0

        // Build case-insensitive name -> id lookup maps once, from current Master Settings.
        val academicYearByName = masterDao.observeAcademicYears().first().associateBy { it.label.trim().lowercase() }
        val courseByName = masterDao.observeCourses().first().associateBy { it.name.trim().lowercase() }
        val streamByName = masterDao.observeAllStreams().first().associateBy { it.name.trim().lowercase() }
        val classByName = masterDao.observeClasses().first().associateBy { it.name.trim().lowercase() }
        val batchByName = masterDao.observeBatches().first().associateBy { it.name.trim().lowercase() }
        val sectionByName = masterDao.observeSections().first().associateBy { it.name.trim().lowercase() }

        /** Resolves a name to its ID, or returns null with no error if the cell was left blank. */
        fun <T> resolve(cell: String, lookup: Map<String, T>, idOf: (T) -> Long, fieldLabel: String, rowErrors: MutableList<String>): Long? {
            if (cell.isBlank()) return null
            val match = lookup[cell.trim().lowercase()]
            if (match == null) {
                rowErrors.add("Unknown $fieldLabel: \"$cell\" (check Master Settings for the exact name)")
                return null
            }
            return idOf(match)
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            CSVReader(InputStreamReader(input)).use { reader ->
                val rows = reader.readAll()
                if (rows.isEmpty()) return CsvImportSummary(0, 0, 0, emptyList())

                for ((index, row) in rows.withIndex()) {
                    if (index == 0) continue // header
                    totalRows++
                    val rowNum = index + 1

                    if (row.size < 8 || row[0].isBlank() || row[2].isBlank()) {
                        errors.add(CsvRowError(rowNum, "Missing required fields (AdmissionNo/StudentName)"))
                        continue
                    }
                    val admissionNo = row[0].trim()
                    if (studentDao.findAdmissionNo(admissionNo) != null) {
                        errors.add(CsvRowError(rowNum, "Duplicate Admission No: $admissionNo"))
                        continue
                    }
                    val gender = try {
                        Gender.valueOf(row.getOrElse(6) { "MALE" }.trim().uppercase())
                    } catch (e: Exception) {
                        errors.add(CsvRowError(rowNum, "Invalid Gender value"))
                        continue
                    }

                    val rowErrors = mutableListOf<String>()
                    val academicYearId = resolve(row.getOrElse(12) { "" }, academicYearByName, { it.id }, "Academic Year", rowErrors)
                    val courseId = resolve(row.getOrElse(13) { "" }, courseByName, { it.id }, "Course", rowErrors)
                    val streamId = resolve(row.getOrElse(14) { "" }, streamByName, { it.id }, "Stream", rowErrors)
                    val classId = resolve(row.getOrElse(15) { "" }, classByName, { it.id }, "Class", rowErrors)
                    val batchId = resolve(row.getOrElse(16) { "" }, batchByName, { it.id }, "Batch", rowErrors)
                    val sectionId = resolve(row.getOrElse(17) { "" }, sectionByName, { it.id }, "Section", rowErrors)

                    if (rowErrors.isNotEmpty()) {
                        errors.add(CsvRowError(rowNum, rowErrors.joinToString("; ")))
                        continue
                    }

                    try {
                        studentDao.insert(
                            StudentEntity(
                                admissionNo = admissionNo,
                                rollNo = row.getOrElse(1) { "" },
                                studentName = row.getOrElse(2) { "" },
                                fatherName = row.getOrElse(3) { "" },
                                motherName = row.getOrElse(4) { "" },
                                dob = row.getOrElse(5) { "" },
                                gender = gender,
                                address = row.getOrElse(7) { "" },
                                mobileNo1 = row.getOrElse(8) { "" },
                                mobileNo2 = row.getOrElse(9) { "" },
                                whatsappNo = row.getOrElse(10) { "" },
                                boardName = row.getOrElse(11) { "CHSE Odisha" },
                                academicYearId = academicYearId,
                                courseId = courseId,
                                streamId = streamId,
                                classId = classId,
                                batchId = batchId,
                                sectionId = sectionId,
                                status = StudentStatus.ACTIVE
                            )
                        )
                        successful++
                    } catch (e: Exception) {
                        errors.add(CsvRowError(rowNum, "Insert failed: ${e.message}"))
                    }
                }
            }
        }

        val summary = CsvImportSummary(totalRows, successful, errors.size, errors)
        importHistoryDao.insert(
            ImportHistoryEntity(
                fileName = fileName,
                totalRecords = totalRows,
                successful = successful,
                failed = errors.size,
                errorDetails = errors.joinToString("; ") { "Row ${it.rowNumber}: ${it.reason}" }
            )
        )
        return summary
    }
}
