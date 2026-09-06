package com.vigyan.juniorcollege.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vigyan.juniorcollege.R
import com.vigyan.juniorcollege.ui.nav.Routes
import kotlinx.coroutines.launch

data class DrawerItem(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

val drawerItems = listOf(
    DrawerItem("Dashboard", Routes.DASHBOARD, Icons.Default.Home),
    DrawerItem("Student Management", Routes.studentList(), Icons.Default.Group),
    DrawerItem("Attendance", Routes.ATTENDANCE_MARK, Icons.Default.EventAvailable),
    DrawerItem("CSV Import", Routes.CSV_IMPORT, Icons.Default.UploadFile),
    DrawerItem("Reports", Routes.REPORTS, Icons.Default.BarChart),
    DrawerItem("Recycle Bin", Routes.RECYCLE_BIN, Icons.Default.Delete),
    DrawerItem("Master Settings", Routes.MASTER_SETTINGS, Icons.Default.Settings),
    DrawerItem("Database & Backup", Routes.BACKUP, Icons.Default.Backup),
    DrawerItem("Change Password", Routes.CHANGE_PASSWORD, Icons.Default.Lock),
    DrawerItem("Cloud Sync", Routes.CLOUD_SYNC, Icons.Default.Sync),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavController,
    title: String,
    currentRoute: String,
    userName: String,
    onLogout: () -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_vigyan),
                        contentDescription = "Vigyan International",
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("VIGYAN INTERNATIONAL", style = MaterialTheme.typography.titleMedium)
                    Text("Junior College", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(userName, style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute.substringBefore("?") == item.route.substringBefore("?"),
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute.substringBefore("?") != item.route.substringBefore("?")) {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.DASHBOARD) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Logout") },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = onLogout,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            floatingActionButton = floatingActionButton
        ) { padding -> content(padding) }
    }
}
