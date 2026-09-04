package com.vigyan.juniorcollege

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vigyan.juniorcollege.ui.nav.Routes
import com.vigyan.juniorcollege.ui.screens.account.ChangePasswordScreen
import com.vigyan.juniorcollege.ui.screens.attendance.AttendanceScreen
import com.vigyan.juniorcollege.ui.screens.backup.BackupScreen
import com.vigyan.juniorcollege.ui.screens.csv.CsvImportScreen
import com.vigyan.juniorcollege.ui.screens.dashboard.DashboardScreen
import com.vigyan.juniorcollege.ui.screens.login.LoginScreen
import com.vigyan.juniorcollege.ui.screens.master.MasterSettingsScreen
import com.vigyan.juniorcollege.ui.screens.reports.ReportsScreen
import com.vigyan.juniorcollege.ui.screens.students.RecycleBinScreen
import com.vigyan.juniorcollege.ui.screens.students.StudentAddEditScreen
import com.vigyan.juniorcollege.ui.screens.students.StudentListScreen
import com.vigyan.juniorcollege.ui.screens.students.StudentProfileScreen
import com.vigyan.juniorcollege.ui.theme.VigyanErpTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VigyanErpTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VigyanApp()
                }
            }
        }
    }
}

@Composable
private fun VigyanApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.vigyan.juniorcollege.VigyanApp
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val userId by app.sessionManager.userId.collectAsStateWithLifecycle(initialValue = null)
    val userNameFlowState by app.sessionManager.userName.collectAsStateWithLifecycle(initialValue = null)
    var sessionChecked by remember { mutableStateOf(false) }

    LaunchedEffect(userId) { sessionChecked = true }

    if (!sessionChecked) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (userId != null) Routes.DASHBOARD else Routes.LOGIN
    val userName = userNameFlowState ?: "User"

    fun logout() {
        scope.launch {
            app.sessionManager.logout()
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(navController, userName, onLogout = { logout() })
        }
        composable(Routes.STUDENT_LIST) {
            StudentListScreen(navController, userName, onLogout = { logout() })
        }
        composable(Routes.STUDENT_ADD) {
            StudentAddEditScreen(navController, studentId = null)
        }
        composable(
            Routes.STUDENT_EDIT,
            arguments = listOf(navArgument("studentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("studentId") ?: 0L
            StudentAddEditScreen(navController, studentId = id)
        }
        composable(
            Routes.STUDENT_PROFILE,
            arguments = listOf(navArgument("studentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("studentId") ?: 0L
            StudentProfileScreen(navController, studentId = id)
        }
        composable(Routes.RECYCLE_BIN) {
            RecycleBinScreen(navController, userName, onLogout = { logout() })
        }
        composable(Routes.ATTENDANCE_MARK) {
            AttendanceScreen(navController, userName, userId, onLogout = { logout() })
        }
        composable(Routes.REPORTS) {
            ReportsScreen(navController, userName, onLogout = { logout() })
        }
        composable(Routes.MASTER_SETTINGS) {
            MasterSettingsScreen(navController, userName, onLogout = { logout() })
        }
        composable(Routes.BACKUP) {
            BackupScreen(navController, userName, onLogout = { logout() })
        }
        composable(Routes.CSV_IMPORT) {
            CsvImportScreen(navController, userName, onLogout = { logout() })
        }
        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(navController, userId)
        }
    }
}
