package com.vigyan.juniorcollege.data.repository

import android.content.Context
import android.net.Uri
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import com.vigyan.juniorcollege.data.local.dao.ImportHistoryDao
import com.vigyan.juniorcollege.data.local.dao.StudentDao
import com.vigyan.juniorcollege.data.local.entity.Gender
import com.vigyan.juniorcollege.data.local.entity.ImportHistoryEntity
import com.vigyan.juniorcollege.data.local.entity.StudentEntity
import com.vigyan.juniorcollege.data.local.entity.StudentStatus
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class CsvRowError(val rowNumber: Int, val reason: String)

data class CsvImportSummary(
    val totalRows: Int,
    val successful: Int,
    val failed: Int,
    val errors: List<CsvRowError>
)

private val CSV_HEADERS = arrayOf(
    "AdmissionNo", "RollNo", "StudentName", "FatherName", "MotherName",
    "DOB(yyyy-MM-dd)", "Gender(MALE/FEMALE/OTHER)", "Address",
    "MobileNo1", "MobileNo2", "WhatsappNo", "Board", "Course", "Stream", "Class", "Section"
)

class CsvRepository(
    private val context: Context,
    private val studentDao: StudentDao,
    private val importHistoryDao: ImportHistoryDao
) {

    /** Writes the blank CSV template to the given Uri (from a SAF file picker). */
    fun writeTemplate(uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            CSVWriter(OutputStreamWriter(out)).use { writer ->
                writer.writeNext(CSV_HEADERS)
                writer.writeNext(
                    arrayOf(
                        "2025001", "1", "Sample Student", "Father Name", "Mother Name",
                        "2008-05-14", "MALE", "Sample Address",
                        "9999999999", "", "9999999999", "CHSE Odisha", "Science", "PCM", "+2 First Year", "A"
                    )
                )
            }
        }
    }

    /** Exports all active students as CSV to the given Uri. */
    suspend fun exportStudents(uri: Uri, students: List<StudentEntity>) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            CSVWriter(OutputStreamWriter(out)).use { writer ->
                writer.writeNext(CSV_HEADERS)
                students.forEach { s ->
                    writer.writeNext(
                        arrayOf(
                            s.admissionNo, s.rollNo, s.studentName, s.fatherName, s.motherName,
                            s.dob, s.gender.name, s.address, s.mobileNo1, s.mobileNo2,
                            s.whatsappNo, s.boardName, "", "", "", ""
                        )
                    )
                }
            }
        }
    }

    /**
     * Reads and validates a CSV file, then imports valid rows.
     * Duplicate admission numbers and missing required fields are rejected per-row.
     */
    suspend fun importFromCsv(uri: Uri, fileName: String): CsvImportSummary {
        val errors = mutableListOf<CsvRowError>()
        var successful = 0
        var totalRows = 0

        context.contentResolver.openInputStream(uri)?.use { input ->
            CSVReader(InputStreamReader(input)).use { reader ->
                val rows = reader.readAll()
                if (rows.isEmpty()) return CsvImportSummary(0, 0, 0, emptyList())

                // Skip header row
                for ((index, row) in rows.withIndex()) {
                    if (index == 0) continue
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
