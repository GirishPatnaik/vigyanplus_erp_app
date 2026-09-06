package com.vigyan.juniorcollege.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.vigyan.juniorcollege.data.local.dao.AttendanceDao
import com.vigyan.juniorcollege.data.local.dao.MasterDao
import com.vigyan.juniorcollege.data.local.dao.StudentDao
import com.vigyan.juniorcollege.data.local.entity.*
import com.vigyan.juniorcollege.util.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SyncResult(
    val success: Boolean,
    val message: String,
    val studentsSynced: Int = 0,
    val attendanceSynced: Int = 0
)

/** Local ID <-> remote (Supabase) UUID lookup for one master-data category. */
private data class SyncMaps(val localToRemote: Map<Long, String>, val remoteToLocal: Map<String, Long>)

class SyncRepository(
    private val context: Context,
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao,
    private val masterDao: MasterDao
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json".toMediaType()

    suspend fun syncAll(): SyncResult = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext SyncResult(false, "Cloud sync isn't set up yet — add your Supabase project URL and key in SupabaseConfig.kt first.")
        }
        try {
            val courseMaps = syncSimpleCategory(
                table = "courses", nameColumn = "name",
                localRows = masterDao.observeCourses().first().map { it.id to (it.name to it.remoteId) },
                persistRemoteId = { id, remoteId -> masterDao.upsertCourse(masterDao.observeCourses().first().first { it.id == id }.copy(remoteId = remoteId)) },
                insertLocalFromRemote = { name, remoteId -> masterDao.upsertCourse(CourseEntity(name = name, remoteId = remoteId)) }
            )
            val freshCourseMaps = buildMaps { masterDao.observeCourses().first().map { it.id to it.remoteId } }
            syncStreams(freshCourseMaps)
            syncSimpleCategory(
                table = "college_classes", nameColumn = "name",
                localRows = masterDao.observeClasses().first().map { it.id to (it.name to it.remoteId) },
                persistRemoteId = { id, remoteId -> masterDao.upsertClass(masterDao.observeClasses().first().first { it.id == id }.copy(remoteId = remoteId)) },
                insertLocalFromRemote = { name, remoteId -> masterDao.upsertClass(ClassEntity(name = name, remoteId = remoteId)) }
            )
            syncSimpleCategory(
                table = "batches", nameColumn = "name",
                localRows = masterDao.observeBatches().first().map { it.id to (it.name to it.remoteId) },
                persistRemoteId = { id, remoteId -> masterDao.upsertBatch(masterDao.observeBatches().first().first { it.id == id }.copy(remoteId = remoteId)) },
                insertLocalFromRemote = { name, remoteId -> masterDao.upsertBatch(BatchEntity(name = name, remoteId = remoteId)) }
            )
            syncSimpleCategory(
                table = "sections", nameColumn = "name",
                localRows = masterDao.observeSections().first().map { it.id to (it.name to it.remoteId) },
                persistRemoteId = { id, remoteId -> masterDao.upsertSection(masterDao.observeSections().first().first { it.id == id }.copy(remoteId = remoteId)) },
                insertLocalFromRemote = { name, remoteId -> masterDao.upsertSection(SectionEntity(name = name, remoteId = remoteId)) }
            )
            syncSimpleCategory(
                table = "academic_years", nameColumn = "label",
                localRows = masterDao.observeAcademicYears().first().map { it.id to (it.label to it.remoteId) },
                persistRemoteId = { id, remoteId -> masterDao.upsertAcademicYear(masterDao.observeAcademicYears().first().first { it.id == id }.copy(remoteId = remoteId)) },
                insertLocalFromRemote = { label, remoteId -> masterDao.upsertAcademicYear(AcademicYearEntity(label = label, remoteId = remoteId)) }
            )
            syncCollegeDetails()

            val academicYearMaps = buildMaps { masterDao.observeAcademicYears().first().map { it.id to it.remoteId } }
            val courseMapsFinal = buildMaps { masterDao.observeCourses().first().map { it.id to it.remoteId } }
            val streamMaps = buildMaps { masterDao.observeAllStreams().first().map { it.id to it.remoteId } }
            val classMaps = buildMaps { masterDao.observeClasses().first().map { it.id to it.remoteId } }
            val batchMaps = buildMaps { masterDao.observeBatches().first().map { it.id to it.remoteId } }
            val sectionMaps = buildMaps { masterDao.observeSections().first().map { it.id to it.remoteId } }

            val studentsSynced = syncStudents(academicYearMaps, courseMapsFinal, streamMaps, classMaps, batchMaps, sectionMaps)
            val studentIdMaps = buildMaps { studentDao.getAllForSync().map { it.id to it.remoteId } }
            val attendanceSynced = syncAttendance(studentIdMaps, classMaps, batchMaps)

            SyncResult(true, "Sync complete", studentsSynced, attendanceSynced)
        } catch (e: IOException) {
            SyncResult(false, "No internet connection, or the server couldn't be reached. Try again when you have signal.")
        } catch (e: Exception) {
            SyncResult(false, "Sync failed: ${e.message}")
        }
    }

    private suspend fun buildMaps(localIdToRemoteId: suspend () -> List<Pair<Long, String?>>): SyncMaps {
        val pairs = localIdToRemoteId().filter { it.second != null }
        val l2r = pairs.associate { it.first to it.second!! }
        val r2l = pairs.associate { it.second!! to it.first }
        return SyncMaps(l2r, r2l)
    }

    // ---- Generic (id: Long, name: String, remoteId: String?) master-data categories ----

    private suspend fun syncSimpleCategory(
        table: String,
        nameColumn: String,
        localRows: List<Pair<Long, Pair<String, String?>>>, // localId to (name, existingRemoteId)
        persistRemoteId: suspend (Long, String) -> Unit,
        insertLocalFromRemote: suspend (name: String, remoteId: String) -> Unit
    ): SyncMaps {
        val remoteRows = restGet(table, "select=id,$nameColumn")
        val remoteByName = HashMap<String, String>()
        for (i in 0 until remoteRows.length()) {
            val row = remoteRows.getJSONObject(i)
            remoteByName[row.getString(nameColumn).trim().lowercase()] = row.getString("id")
        }

        val localToRemote = HashMap<Long, String>()
        val toPush = JSONArray()
        val toPushLocalIds = ArrayList<Long>()

        for ((localId, pair) in localRows) {
            val (name, existingRemoteId) = pair
            if (existingRemoteId != null) {
                localToRemote[localId] = existingRemoteId
                continue
            }
            val key = name.trim().lowercase()
            val foundRemote = remoteByName[key]
            if (foundRemote != null) {
                persistRemoteId(localId, foundRemote)
                localToRemote[localId] = foundRemote
            } else {
                val obj = JSONObject()
                obj.put(nameColumn, name)
                toPush.put(obj)
                toPushLocalIds.add(localId)
            }
        }

        if (toPush.length() > 0) {
            val pushed = restUpsert(table, toPush, nameColumn)
            val pushedByName = HashMap<String, String>()
            for (i in 0 until pushed.length()) {
                val row = pushed.getJSONObject(i)
                pushedByName[row.getString(nameColumn).trim().lowercase()] = row.getString("id")
            }
            for (localId in toPushLocalIds) {
                val name = localRows.first { it.first == localId }.second.first
                val remoteId = pushedByName[name.trim().lowercase()] ?: continue
                persistRemoteId(localId, remoteId)
                localToRemote[localId] = remoteId
                remoteByName[name.trim().lowercase()] = remoteId
            }
        }

        val localNamesLower = localRows.map { it.second.first.trim().lowercase() }.toHashSet()
        for (i in 0 until remoteRows.length()) {
            val row = remoteRows.getJSONObject(i)
            val name = row.getString(nameColumn)
            if (name.trim().lowercase() !in localNamesLower) {
                insertLocalFromRemote(name, row.getString("id"))
            }
        }

        val remoteToLocal = HashMap<String, Long>()
        for ((k, v) in localToRemote) remoteToLocal[v] = k
        return SyncMaps(localToRemote, remoteToLocal)
    }

    // ---- Streams (has a course_id foreign key, so handled separately) ----

    private suspend fun syncStreams(courseMaps: SyncMaps) {
        val remoteRows = restGet("streams", "select=id,name,course_id")
        val remoteByName = HashMap<String, JSONObject>()
        for (i in 0 until remoteRows.length()) {
            val row = remoteRows.getJSONObject(i)
            remoteByName[row.getString("name").trim().lowercase()] = row
        }

        val localStreams = masterDao.observeAllStreams().first()
        val toPush = JSONArray()
        val toPushLocal = ArrayList<StreamEntity>()

        for (s in localStreams) {
            if (s.remoteId != null) continue
            val key = s.name.trim().lowercase()
            val found = remoteByName[key]
            if (found != null) {
                masterDao.upsertStream(s.copy(remoteId = found.getString("id")))
            } else {
                val courseRemoteId = courseMaps.localToRemote[s.courseId]
                val obj = JSONObject()
                obj.put("name", s.name)
                if (courseRemoteId != null) obj.put("course_id", courseRemoteId) else obj.put("course_id", JSONObject.NULL)
                toPush.put(obj)
                toPushLocal.add(s)
            }
        }

        if (toPush.length() > 0) {
            val pushed = restUpsert("streams", toPush, "name")
            val pushedByName = HashMap<String, String>()
            for (i in 0 until pushed.length()) {
                val row = pushed.getJSONObject(i)
                pushedByName[row.getString("name").trim().lowercase()] = row.getString("id")
            }
            for (s in toPushLocal) {
                val remoteId = pushedByName[s.name.trim().lowercase()] ?: continue
                masterDao.upsertStream(s.copy(remoteId = remoteId))
            }
        }

        val localNamesLower = localStreams.map { it.name.trim().lowercase() }.toHashSet()
        for (i in 0 until remoteRows.length()) {
            val row = remoteRows.getJSONObject(i)
            val name = row.getString("name")
            if (name.trim().lowercase() !in localNamesLower) {
                val remoteCourseId = row.optString("course_id", "")
                val localCourseId = courseMaps.remoteToLocal[remoteCourseId] ?: continue
                masterDao.upsertStream(StreamEntity(name = name, courseId = localCourseId, remoteId = row.getString("id")))
            }
        }
    }

    // ---- College details (singleton, merged by updatedAt) ----

    private suspend fun syncCollegeDetails() {
        val local = masterDao.observeCollegeDetails().first() ?: return
        val remoteRows = restGet("college_details", "select=*")

        if (remoteRows.length() == 0) {
            val obj = collegeDetailsToJson(local)
            val pushed = restUpsert("college_details", JSONArray().put(obj), null)
            if (pushed.length() > 0) {
                masterDao.upsertCollegeDetails(local.copy(remoteId = pushed.getJSONObject(0).getString("id")))
            }
            return
        }

        val remote = remoteRows.getJSONObject(0)
        val remoteUpdatedAt = remote.optLong("updated_at", 0)
        val remoteId = remote.getString("id")

        if (local.updatedAt >= remoteUpdatedAt) {
            val obj = collegeDetailsToJson(local)
            obj.put("id", remoteId)
            restPatch("college_details", remoteId, obj)
            if (local.remoteId == null) masterDao.upsertCollegeDetails(local.copy(remoteId = remoteId))
        } else {
            masterDao.upsertCollegeDetails(
                local.copy(
                    name = remote.optString("name", local.name),
                    address = remote.optString("address", local.address),
                    phone = remote.optString("phone", local.phone),
                    email = remote.optString("email", local.email),
                    affiliationBoard = remote.optString("affiliation_board", local.affiliationBoard),
                    updatedAt = remoteUpdatedAt,
                    remoteId = remoteId
                )
            )
        }
    }

    private fun collegeDetailsToJson(c: CollegeDetailsEntity): JSONObject = JSONObject().apply {
        put("name", c.name); put("address", c.address); put("phone", c.phone)
        put("email", c.email); put("affiliation_board", c.affiliationBoard); put("updated_at", c.updatedAt)
    }

    // ---- Students ----

    private suspend fun syncStudents(
        academicYearMaps: SyncMaps, courseMaps: SyncMaps, streamMaps: SyncMaps,
        classMaps: SyncMaps, batchMaps: SyncMaps, sectionMaps: SyncMaps
    ): Int {
        var count = 0
        val remoteRows = restGet("students", "select=*")
        val remoteByAdmissionNo = HashMap<String, JSONObject>()
        for (i in 0 until remoteRows.length()) {
            val row = remoteRows.getJSONObject(i)
            remoteByAdmissionNo[row.getString("admission_no").trim().lowercase()] = row
        }

        val localStudents = studentDao.getAllForSync()
        val localByAdmissionNo = localStudents.associateBy { it.admissionNo.trim().lowercase() }

        for (s in localStudents) {
            val key = s.admissionNo.trim().lowercase()
            val remote = remoteByAdmissionNo[key]

            if (remote != null && remote.optLong("updated_at", 0) > s.updatedAt) {
                continue
            }

            var photoUrl = s.photoUri
            if (photoUrl != null && photoUrl.startsWith("content://")) {
                val uploaded = uploadPhoto(s.admissionNo, Uri.parse(photoUrl))
                if (uploaded != null) {
                    photoUrl = uploaded
                    studentDao.update(s.copy(photoUri = uploaded))
                }
            }

            val obj = JSONObject().apply {
                put("admission_no", s.admissionNo)
                put("roll_no", s.rollNo)
                put("student_name", s.studentName)
                put("father_name", s.fatherName)
                put("mother_name", s.motherName)
                put("dob", s.dob)
                put("gender", s.gender.name)
                put("address", s.address)
                put("mobile_no1", s.mobileNo1)
                put("mobile_no2", s.mobileNo2)
                put("whatsapp_no", s.whatsappNo)
                put("board_name", s.boardName)
                put("academic_year_id", s.academicYearId?.let { academicYearMaps.localToRemote[it] } ?: JSONObject.NULL)
                put("course_id", s.courseId?.let { courseMaps.localToRemote[it] } ?: JSONObject.NULL)
                put("stream_id", s.streamId?.let { streamMaps.localToRemote[it] } ?: JSONObject.NULL)
                put("class_id", s.classId?.let { classMaps.localToRemote[it] } ?: JSONObject.NULL)
                put("batch_id", s.batchId?.let { batchMaps.localToRemote[it] } ?: JSONObject.NULL)
                put("section_id", s.sectionId?.let { sectionMaps.localToRemote[it] } ?: JSONObject.NULL)
                put("aadhaar_encrypted", s.aadhaarEncrypted ?: JSONObject.NULL)
                put("status", s.status.name)
                put("photo_url", photoUrl ?: JSONObject.NULL)
                put("is_deleted", s.isDeleted)
                put("created_at", s.createdAt)
                put("updated_at", s.updatedAt)
            }

            val pushed = restUpsert("students", JSONArray().put(obj), "admission_no")
            if (pushed.length() > 0) {
                val newRemoteId = pushed.getJSONObject(0).getString("id")
                if (s.remoteId != newRemoteId) studentDao.setRemoteId(s.id, newRemoteId)
            }
            count++
        }

        for (i in 0 until remoteRows.length()) {
            val remote = remoteRows.getJSONObject(i)
            val admissionNo = remote.getString("admission_no")
            val key = admissionNo.trim().lowercase()
            val local = localByAdmissionNo[key]

            if (local != null && local.updatedAt >= remote.optLong("updated_at", 0)) continue

            val entity = StudentEntity(
                id = local?.id ?: 0,
                photoUri = remote.optStringOrNull("photo_url"),
                admissionNo = admissionNo,
                rollNo = remote.optString("roll_no", ""),
                academicYearId = remote.optStringOrNull("academic_year_id")?.let { academicYearMaps.remoteToLocal[it] },
                studentName = remote.optString("student_name", ""),
                fatherName = remote.optString("father_name", ""),
                motherName = remote.optString("mother_name", ""),
                dob = remote.optString("dob", ""),
                gender = try { Gender.valueOf(remote.optString("gender", "MALE")) } catch (e: Exception) { Gender.MALE },
                address = remote.optString("address", ""),
                mobileNo1 = remote.optString("mobile_no1", ""),
                mobileNo2 = remote.optString("mobile_no2", ""),
                whatsappNo = remote.optString("whatsapp_no", ""),
                boardName = remote.optString("board_name", "CHSE Odisha"),
                courseId = remote.optStringOrNull("course_id")?.let { courseMaps.remoteToLocal[it] },
                streamId = remote.optStringOrNull("stream_id")?.let { streamMaps.remoteToLocal[it] },
                classId = remote.optStringOrNull("class_id")?.let { classMaps.remoteToLocal[it] },
                batchId = remote.optStringOrNull("batch_id")?.let { batchMaps.remoteToLocal[it] },
                sectionId = remote.optStringOrNull("section_id")?.let { sectionMaps.remoteToLocal[it] },
                aadhaarEncrypted = remote.optStringOrNull("aadhaar_encrypted"),
                status = try { StudentStatus.valueOf(remote.optString("status", "ACTIVE")) } catch (e: Exception) { StudentStatus.ACTIVE },
                createdAt = remote.optLong("created_at", System.currentTimeMillis()),
                updatedAt = remote.optLong("updated_at", System.currentTimeMillis()),
                isDeleted = remote.optBoolean("is_deleted", false),
                remoteId = remote.getString("id")
            )

            if (local == null) studentDao.insert(entity) else studentDao.update(entity)
            count++
        }

        return count
    }

    // ---- Attendance ----

    private suspend fun syncAttendance(studentMaps: SyncMaps, classMaps: SyncMaps, batchMaps: SyncMaps): Int {
        var count = 0
        val remoteRows = restGet("attendance", "select=*")
        val remoteByKey = HashMap<String, JSONObject>()
        for (i in 0 until remoteRows.length()) {
            val row = remoteRows.getJSONObject(i)
            remoteByKey["${row.getString("student_id")}|${row.getString("date")}"] = row
        }

        val localRows = attendanceDao.getAllForSync()

        for (a in localRows) {
            val studentRemoteId = studentMaps.localToRemote[a.studentId] ?: continue
            val key = "$studentRemoteId|${a.date}"
            val remote = remoteByKey[key]
            if (remote != null && remote.optLong("marked_at", 0) > a.markedAt) continue

            val obj = JSONObject().apply {
                put("student_id", studentRemoteId)
                put("date", a.date)
                put("status", a.status.name)
                put("class_id", a.classId?.let { classMaps.localToRemote[it] } ?: JSONObject.NULL)
                put("batch_id", a.batchId?.let { batchMaps.localToRemote[it] } ?: JSONObject.NULL)
                put("marked_at", a.markedAt)
            }
            val pushed = restUpsert("attendance", JSONArray().put(obj), "student_id,date")
            if (pushed.length() > 0) {
                val newRemoteId = pushed.getJSONObject(0).getString("id")
                if (a.remoteId != newRemoteId) attendanceDao.setRemoteId(a.id, newRemoteId)
            }
            count++
        }

        for (i in 0 until remoteRows.length()) {
            val remote = remoteRows.getJSONObject(i)
            val studentRemoteId = remote.getString("student_id")
            val date = remote.getString("date")
            val localStudentId = studentMaps.remoteToLocal[studentRemoteId] ?: continue
            val existingLocal = attendanceDao.findByStudentAndDate(localStudentId, date)

            if (existingLocal != null && existingLocal.markedAt >= remote.optLong("marked_at", 0)) continue

            val entity = AttendanceEntity(
                id = existingLocal?.id ?: 0,
                studentId = localStudentId,
                date = date,
                status = try { AttendanceStatus.valueOf(remote.optString("status", "PRESENT")) } catch (e: Exception) { AttendanceStatus.PRESENT },
                classId = remote.optStringOrNull("class_id")?.let { classMaps.remoteToLocal[it] },
                batchId = remote.optStringOrNull("batch_id")?.let { batchMaps.remoteToLocal[it] },
                markedAt = remote.optLong("marked_at", System.currentTimeMillis()),
                remoteId = remote.getString("id")
            )
            attendanceDao.upsert(entity)
            count++
        }

        return count
    }

    // ---- Photo upload ----

    private fun uploadPhoto(admissionNo: String, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap == null) return null

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val bytes = stream.toByteArray()

            val safeName = admissionNo.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val path = "$safeName.jpg"
            val url = "${SupabaseConfig.SUPABASE_URL}/storage/v1/object/${SupabaseConfig.PHOTOS_BUCKET}/$path"

            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .header("Content-Type", "image/jpeg")
                .header("x-upsert", "true")
                .post(bytes.toRequestBody("image/jpeg".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
            }
            "${SupabaseConfig.SUPABASE_URL}/storage/v1/object/public/${SupabaseConfig.PHOTOS_BUCKET}/$path"
        } catch (e: Exception) {
            null
        }
    }

    // ---- Low-level REST helpers ----

    private fun restGet(table: String, query: String): JSONArray {
        val url = "${SupabaseConfig.SUPABASE_URL}/rest/v1/$table?$query"
        val request = Request.Builder()
            .url(url)
            .header("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: "[]"
            if (!resp.isSuccessful) throw IOException("GET $table failed (${resp.code}): $body")
            return JSONArray(body)
        }
    }

    /** Upserts rows via PostgREST's on_conflict + merge-duplicates, returning the resulting rows. */
    private fun restUpsert(table: String, rows: JSONArray, conflictCols: String?): JSONArray {
        val conflictParam = if (conflictCols != null) "?on_conflict=$conflictCols" else ""
        val url = "${SupabaseConfig.SUPABASE_URL}/rest/v1/$table$conflictParam"
        val request = Request.Builder()
            .url(url)
            .header("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates,return=representation")
            .post(rows.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: "[]"
            if (!resp.isSuccessful) throw IOException("Upsert $table failed (${resp.code}): $body")
            return JSONArray(body)
        }
    }

    private fun restPatch(table: String, id: String, fields: JSONObject) {
        val url = "${SupabaseConfig.SUPABASE_URL}/rest/v1/$table?id=eq.$id"
        val request = Request.Builder()
            .url(url)
            .header("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            .header("Content-Type", "application/json")
            .patch(fields.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Patch $table failed (${resp.code})")
        }
    }
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (isNull(key) || !has(key)) return null
    val v = optString(key, "")
    return v.ifBlank { null }
}
