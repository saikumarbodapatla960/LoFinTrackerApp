// In ...ui.screens/TutorialScreen.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(onGetStarted: () -> Unit) {
    val pages = listOf(
        TutorialPageData(
            title = "Dashboard",
            desc = "Your financial headquarters. See your total balance, loans, and a summary of income vs expenses. Tap cards to navigate quickly.",
            icon = Icons.Default.Dashboard
        ),
        TutorialPageData(
            title = "Income",
            desc = "Track every penny you earn. Use filters to see income by date or category. View helpful charts to understand your sources.",
            icon = Icons.Default.ArrowUpward
        ),
        TutorialPageData(
            title = "Expenses",
            desc = "Control your spending. Log daily expenses, categorize them, and use charts to see where your money goes.",
            icon = Icons.Default.ArrowDownward
        ),
        TutorialPageData(
            title = "Balance & Accounts",
            desc = "Manage all your bank accounts and cash wallets in one place. See individual balances and history.",
            icon = Icons.Default.AccountBalance
        ),
        TutorialPageData(
            title = "Loans",
            desc = "Never lose track of debts. Add loans, track payments, and see your outstanding balances instantly.",
            icon = Icons.Default.Savings
        ),
        TutorialPageData(
            title = "Credit Cards",
            desc = "Manage credit limits and bill payments. Track how much you owe and pay bills directly from your accounts.",
            icon = Icons.Default.CreditCard
        ),
        TutorialPageData(
            title = "Scheduled Payments",
            desc = "Set up recurring bills or income (like Salary or Rent). The app will remind you and pre-fill transactions for you.",
            icon = Icons.Default.EventRepeat
        ),
        TutorialPageData(
            title = "Income vs. Expense",
            desc = "The big picture. Compare your earnings against your spending with clear bar charts to stay profitable.",
            icon = Icons.Default.BarChart
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

            // --- PAGER ---
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = page.desc,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- NAVIGATION BUTTONS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dots Indicator
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        Surface(modifier = Modifier.size(8.dp), shape = MaterialTheme.shapes.small, color = color) {}
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onGetStarted()
                        }
                    }
                ) {
                    Text(if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started")
                }
            }
        }
    }
}

data class TutorialPageData(val title: String, val desc: String, val icon: ImageVector)