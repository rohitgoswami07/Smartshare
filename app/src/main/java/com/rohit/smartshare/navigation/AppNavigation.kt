package com.rohit.smartshare.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rohit.smartshare.screens.BucketDetailScreen
import com.rohit.smartshare.screens.BucketScreen
import com.rohit.smartshare.screens.HomeScreen
import com.rohit.smartshare.screens.LoginScreen
import com.rohit.smartshare.screens.RegisterScreen
import com.rohit.smartshare.screens.ForgotPasswordScreen
import com.rohit.smartshare.screens.ShareScreen
import com.rohit.smartshare.utils.SessionManager
import com.rohit.smartshare.viewmodel.SharedInboxViewModel

@Composable
fun AppNavigation(sharedInboxViewModel: SharedInboxViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val startDestination = remember {
        if (SessionManager.getSession(context) != null) Routes.HOME else Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = "login?registered={registered}",
            arguments = listOf(navArgument("registered") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStackEntry ->
            val registered = backStackEntry.arguments?.getBoolean("registered") ?: false
            LoginScreen(navController, registered)
        }
        composable(Routes.REGISTER) {
            RegisterScreen(navController)
        }
        composable(Routes.HOME) {
            val pendingUris by sharedInboxViewModel.pendingUris.collectAsState()
            HomeScreen(navController, sharedUris = pendingUris, onSharedUrisConsumed = { sharedInboxViewModel.consume() })
        }
        composable(Routes.BUCKET) {
            BucketScreen(navController)
        }
        composable(
            route = Routes.BUCKET_DETAIL,
            arguments = listOf(navArgument("bucketId") { type = NavType.IntType })
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getInt("bucketId") ?: 0
            BucketDetailScreen(navController, bucketId)
        }
        composable(Routes.SHARE) {
            ShareScreen(navController)
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(navController)
        }
    }
}
