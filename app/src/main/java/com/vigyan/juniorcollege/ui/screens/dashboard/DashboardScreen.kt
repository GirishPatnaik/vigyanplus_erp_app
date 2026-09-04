package com.vigyan.juniorcollege.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.vigyan.juniorcollege.VigyanApp
import com.vigyan.juniorcollege.ui.components.AppScaffold
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.ui.theme.*
import com.vigyan.juniorcollege.util.simpleFactory

@Composable
fun DashboardScreen(navController: NavController, userName: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as VigyanApp
    val viewModel: DashboardViewModel = viewModel(
        factory = simpleFactory { DashboardViewModel(app.studentRepository, app.attendanceRepository) }
    )
    val state by viewModel.state.collectAsState()

    AppScaffold(
        navController = navController,
        title = "Dashboard",
        currentRoute = Routes.DASHBOARD,
        userName = userName,
        onLogout = onLogout
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Student Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    listOf(
                        Triple("Total Students", state.totalStudents.toString(), Icons.Default.Groups) to VigyanGreen,
                        Triple("Active Students", state.activeStudents.toString(), Icons.Default.CheckCircle) to VigyanCyan,
                        Triple("Inactive Students", state.inactiveStudents.toString(), Icons.Default.Cancel) to VigyanOrange,
                        Triple("Boys", state.boys.toString(), Icons.Default.Boy) to VigyanCyan,
                        Triple("Girls", state.girls.toString(), Icons.Default.Girl) to VigyanMagenta,
                    )
                ) { (triple, color) ->
                    StatCard(title = triple.first, value = triple.second, icon = triple.third, color = color)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Today's Attendance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttendanceMiniStat("Total", state.todayTotal.toString())
                    AttendanceMiniStat("Present", state.todayPresent.toString(), VigyanGreen)
                    AttendanceMiniStat("Absent", state.todayAbsent.toString(), Color(0xFFBA1A1A))
                    AttendanceMiniStat("%", "%.1f".format(state.todayAttendancePercent))
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    listOf(
                        Triple("Add Student", Icons.Default.PersonAdd, Routes.STUDENT_ADD),
                        Triple("Import CSV", Icons.Default.UploadFile, Routes.CSV_IMPORT),
                        Triple("Search Student", Icons.Default.Search, Routes.STUDENT_LIST),
                        Triple("Take Attendance", Icons.Default.EventAvailable, Routes.ATTENDANCE_MARK),
                        Triple("Reports", Icons.Default.BarChart, Routes.REPORTS),
                    )
                ) { (label, icon, route) ->
                    QuickActionCard(label, icon) { navController.navigate(route) }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, color: Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AttendanceMiniStat(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun QuickActionCard(label: String, icon: ImageVector, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
