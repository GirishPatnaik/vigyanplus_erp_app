package com.vigyan.juniorcollege.ui.screens.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vigyan.juniorcollege.VigyanApp
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.util.simpleFactory

@Composable
fun StudentProfileScreen(navController: NavController, studentId: Long) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: StudentProfileViewModel = viewModel(
        factory = simpleFactory { StudentProfileViewModel(app.studentRepository, app.attendanceRepository, studentId) }
    )
    val student by viewModel.student.collectAsState()
    val attendancePercent by viewModel.attendancePercent.collectAsState()
    val maskedAadhaar by viewModel.maskedAadhaar.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.studentEdit(studentId)) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        val s = student
        if (s == null) {
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

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
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (s.photoUri != null) {
                    AsyncImage(
                        model = s.photoUri, contentDescription = "Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(56.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(s.studentName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("Admission No: ${s.admissionNo}", modifier = Modifier.align(Alignment.CenterHorizontally))

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Attendance", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (attendancePercent / 100.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("%.1f%% overall".format(attendancePercent))
                }
            }

            Spacer(Modifier.height(16.dp))
            ProfileSection("Personal Details") {
                ProfileRow("Roll No", s.rollNo)
                ProfileRow("Father Name", s.fatherName)
                ProfileRow("Mother Name", s.motherName)
                ProfileRow("Date of Birth", s.dob)
                ProfileRow("Gender", s.gender.name)
                ProfileRow("Address", s.address)
            }

            ProfileSection("Contact") {
                ProfileRow("Mobile No 1", s.mobileNo1)
                ProfileRow("Mobile No 2", s.mobileNo2)
                ProfileRow("WhatsApp No", s.whatsappNo)
            }

            ProfileSection("Academic") {
                ProfileRow("Board", s.boardName)
                ProfileRow("Status", s.status.name)
                ProfileRow("Aadhaar No", maskedAadhaar)
            }

            Spacer(Modifier.height(24.dp))
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Student") },
                text = { Text("Move ${s.studentName} to the Recycle Bin? This can be restored later.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteStudent(s.id)
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) { content() }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text(value.ifBlank { "-" }, modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Medium)
    }
}
