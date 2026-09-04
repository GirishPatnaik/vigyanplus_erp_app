package com.vigyan.juniorcollege.ui.screens.students

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vigyan.juniorcollege.VigyanApp
import com.vigyan.juniorcollege.data.local.entity.Gender
import com.vigyan.juniorcollege.data.local.entity.StudentStatus
import com.vigyan.juniorcollege.ui.components.LabeledDropdown
import com.vigyan.juniorcollege.util.simpleFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAddEditScreen(navController: NavController, studentId: Long?) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: StudentAddEditViewModel = viewModel(
        factory = simpleFactory { StudentAddEditViewModel(app.studentRepository, app.masterRepository, studentId) }
    )
    val state by viewModel.state.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val streams by viewModel.allStreams.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val batches by viewModel.batches.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val academicYears by viewModel.academicYears.collectAsState()

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.update { s -> s.copy(photoUri = it.toString()) } }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Student" else "Add Student") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { photoPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (state.photoUri != null) {
                    AsyncImage(
                        model = state.photoUri,
                        contentDescription = "Student Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Change Photo", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Student Profile")

            LabeledField("Admission No *", state.admissionNo) { viewModel.update { s -> s.copy(admissionNo = it) } }
            LabeledField("Roll No", state.rollNo) { viewModel.update { s -> s.copy(rollNo = it) } }
            LabeledField("Student Name *", state.studentName) { viewModel.update { s -> s.copy(studentName = it) } }
            LabeledField("Father Name", state.fatherName) { viewModel.update { s -> s.copy(fatherName = it) } }
            LabeledField("Mother Name", state.motherName) { viewModel.update { s -> s.copy(motherName = it) } }
            LabeledField("DOB (yyyy-MM-dd)", state.dob) { viewModel.update { s -> s.copy(dob = it) } }

            Spacer(Modifier.height(8.dp))
            Text("Gender", style = MaterialTheme.typography.labelLarge)
            Row {
                Gender.entries.forEach { g ->
                    FilterChip(
                        selected = state.gender == g,
                        onClick = { viewModel.update { s -> s.copy(gender = g) } },
                        label = { Text(g.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            LabeledField("Address", state.address) { viewModel.update { s -> s.copy(address = it) } }
            LabeledField("Mobile No 1", state.mobileNo1) { viewModel.update { s -> s.copy(mobileNo1 = it) } }
            LabeledField("Mobile No 2", state.mobileNo2) { viewModel.update { s -> s.copy(mobileNo2 = it) } }
            LabeledField("WhatsApp No", state.whatsappNo) { viewModel.update { s -> s.copy(whatsappNo = it) } }
            LabeledField("Board", state.boardName) { viewModel.update { s -> s.copy(boardName = it) } }
            LabeledField("Aadhaar No (12 digits, optional)", state.aadhaarNo) { input ->
                viewModel.update { s -> s.copy(aadhaarNo = input.filter { c -> c.isDigit() }.take(12)) }
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel("Academic Structure")

            LabeledDropdown(
                label = "Academic Year", options = academicYears, selectedId = state.academicYearId,
                idOf = { it.id }, labelOf = { it.label },
                onSelected = { id -> viewModel.update { s -> s.copy(academicYearId = id) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
            LabeledDropdown(
                label = "Course", options = courses, selectedId = state.courseId,
                idOf = { it.id }, labelOf = { it.name },
                onSelected = { id -> viewModel.update { s -> s.copy(courseId = id) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
            LabeledDropdown(
                label = "Stream", options = streams.filter { it.courseId == state.courseId }, selectedId = state.streamId,
                idOf = { it.id }, labelOf = { it.name },
                onSelected = { id -> viewModel.update { s -> s.copy(streamId = id) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
            LabeledDropdown(
                label = "Class", options = classes, selectedId = state.classId,
                idOf = { it.id }, labelOf = { it.name },
                onSelected = { id -> viewModel.update { s -> s.copy(classId = id) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
            LabeledDropdown(
                label = "Batch", options = batches, selectedId = state.batchId,
                idOf = { it.id }, labelOf = { it.name },
                onSelected = { id -> viewModel.update { s -> s.copy(batchId = id) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
            LabeledDropdown(
                label = "Section", options = sections, selectedId = state.sectionId,
                idOf = { it.id }, labelOf = { it.name },
                onSelected = { id -> viewModel.update { s -> s.copy(sectionId = id) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            Spacer(Modifier.height(8.dp))
            Text("Student Status", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth()) {
                StudentStatus.entries.forEach { st ->
                    FilterChip(
                        selected = state.status == st,
                        onClick = { viewModel.update { s -> s.copy(status = st) } },
                        label = { Text(st.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(if (state.isEditMode) "Update Student" else "Save Student")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}
