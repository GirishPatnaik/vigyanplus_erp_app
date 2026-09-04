package com.vigyan.juniorcollege.ui.screens.master

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vigyan.juniorcollege.VigyanApp
import com.vigyan.juniorcollege.data.local.entity.CollegeDetailsEntity
import com.vigyan.juniorcollege.ui.components.AppScaffold
import com.vigyan.juniorcollege.ui.components.EditableListCard
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.util.simpleFactory
import androidx.lifecycle.viewmodel.compose.viewModel

private val TABS = listOf("Academic Year", "Course", "Stream", "Class", "Batch", "Section", "College Details")

@Composable
fun MasterSettingsScreen(navController: NavController, userName: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: MasterSettingsViewModel = viewModel(
        factory = simpleFactory { MasterSettingsViewModel(app.masterRepository) }
    )
    var selectedTab by remember { mutableStateOf(0) }

    val academicYears by viewModel.academicYears.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val streams by viewModel.streams.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val batches by viewModel.batches.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val collegeDetails by viewModel.collegeDetails.collectAsState()

    AppScaffold(
        navController = navController,
        title = "Master Settings",
        currentRoute = Routes.MASTER_SETTINGS,
        userName = userName,
        onLogout = onLogout
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                TABS.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> EditableListCard(
                        "Academic Years", academicYears, { it.label },
                        onAdd = viewModel::addAcademicYear, onDelete = viewModel::deleteAcademicYear
                    )
                    1 -> EditableListCard(
                        "Courses", courses, { it.name },
                        onAdd = viewModel::addCourse, onDelete = viewModel::deleteCourse
                    )
                    2 -> Column {
                        Text(
                            "Streams are linked to a Course. New streams are added under the first available course; edit associations from the database if needed.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        EditableListCard(
                            "Streams", streams, { "${it.name} (Course #${it.courseId})" },
                            onAdd = { name -> courses.firstOrNull()?.let { c -> viewModel.addStream(name, c.id) } },
                            onDelete = viewModel::deleteStream
                        )
                    }
                    3 -> EditableListCard(
                        "Classes", classes, { it.name },
                        onAdd = viewModel::addClass, onDelete = viewModel::deleteClass
                    )
                    4 -> EditableListCard(
                        "Batches", batches, { it.name },
                        onAdd = viewModel::addBatch, onDelete = viewModel::deleteBatch
                    )
                    5 -> EditableListCard(
                        "Sections", sections, { it.name },
                        onAdd = viewModel::addSection, onDelete = viewModel::deleteSection
                    )
                    6 -> CollegeDetailsForm(collegeDetails, viewModel::saveCollegeDetails)
                }
            }
        }
    }
}

@Composable
private fun CollegeDetailsForm(details: CollegeDetailsEntity?, onSave: (CollegeDetailsEntity) -> Unit) {
    var name by remember(details) { mutableStateOf(details?.name ?: "VIGYAN INTERNATIONAL JUNIOR COLLEGE") }
    var address by remember(details) { mutableStateOf(details?.address ?: "Koraput, Odisha") }
    var phone by remember(details) { mutableStateOf(details?.phone ?: "") }
    var email by remember(details) { mutableStateOf(details?.email ?: "") }
    var board by remember(details) { mutableStateOf(details?.affiliationBoard ?: "CHSE Odisha") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("College Details", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("College Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = board, onValueChange = { board = it }, label = { Text("Affiliation Board") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSave(CollegeDetailsEntity(name = name, address = address, phone = phone, email = email, affiliationBoard = board)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save College Details") }
        }
    }
}
