package com.santhomach.estateexpense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.santhomach.estateexpense.ui.screens.DailyExpenseScreen
import com.santhomach.estateexpense.ui.screens.CsvImportScreen
import com.santhomach.estateexpense.ui.screens.ExpenseTypeSummaryScreen
import com.santhomach.estateexpense.ui.screens.HomeScreen
import com.santhomach.estateexpense.ui.screens.ReportsScreen
import com.santhomach.estateexpense.ui.screens.SettingsScreen
import java.time.LocalDate

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Reports : Screen("reports?startDate={startDate}&endDate={endDate}") {
        fun createRoute(startDate: LocalDate? = null, endDate: LocalDate? = null): String {
            return if (startDate != null && endDate != null) {
                "reports?startDate=$startDate&endDate=$endDate"
            } else {
                "reports"
            }
        }
    }
    object Settings : Screen("settings")
    object Payments : Screen("payments")
    object WeeklyFunds : Screen("weekly_funds")
    object Search : Screen("search")
    object ExpenseTypeSummary : Screen("expense_type_summary")
    object CsvImport : Screen("csv_import")
    object DailyExpense : Screen("daily_expense/{date}?expenseId={expenseId}") {
        fun createRoute(date: LocalDate, expenseId: Int? = null): String {
            return "daily_expense/${date}?expenseId=${expenseId ?: 0}"
        }
    }
}

@Composable
fun EstateExpenseNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToExpenseEntry = { date, expenseId ->
                    navController.navigate(Screen.DailyExpense.createRoute(date, expenseId))
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToPayments = {
                    navController.navigate(Screen.Payments.route)
                },
                onNavigateToWeeklyFunds = {
                    navController.navigate(Screen.WeeklyFunds.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToExpenseTypeSummary = {
                    navController.navigate(Screen.ExpenseTypeSummary.route)
                }
            )
        }

        composable(Screen.Search.route) {
            com.santhomach.estateexpense.ui.screens.SearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToExpenseEntry = { date, expenseId ->
                    navController.navigate(Screen.DailyExpense.createRoute(date, expenseId))
                }
            )
        }

        composable(
            route = Screen.Reports.route,
            arguments = listOf(
                navArgument("startDate") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("endDate") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val startDateStr = backStackEntry.arguments?.getString("startDate")
            val endDateStr = backStackEntry.arguments?.getString("endDate")
            
            val startDate = startDateStr?.let { try { LocalDate.parse(it) } catch(e: Exception) { null } }
            val endDate = endDateStr?.let { try { LocalDate.parse(it) } catch(e: Exception) { null } }

            ReportsScreen(
                startDate = startDate,
                endDate = endDate,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToExpenseEntry = { date, expenseId ->
                    navController.navigate(Screen.DailyExpense.createRoute(date, expenseId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCsvImport = {
                    navController.navigate(Screen.CsvImport.route)
                }
            )
        }

        composable(Screen.CsvImport.route) {
            CsvImportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Payments.route) {
            com.santhomach.estateexpense.ui.screens.WorkerPaymentScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ExpenseTypeSummary.route) {
            ExpenseTypeSummaryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WeeklyFunds.route) {
            com.santhomach.estateexpense.ui.screens.WeeklyFundsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToReports = { startDate: LocalDate, endDate: LocalDate ->
                    navController.navigate(Screen.Reports.createRoute(startDate, endDate))
                }
            )
        }

        composable(
            route = Screen.DailyExpense.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("expenseId") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val dateString = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
            val expenseId = backStackEntry.arguments?.getInt("expenseId") ?: 0

            val date = try {
                LocalDate.parse(dateString)
            } catch (e: Exception) {
                LocalDate.now()
            }

            DailyExpenseScreen(
                date = date,
                expenseId = if (expenseId > 0) expenseId else null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
