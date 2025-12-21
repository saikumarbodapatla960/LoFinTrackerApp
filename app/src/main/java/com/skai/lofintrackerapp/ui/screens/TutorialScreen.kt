package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

data class TutorialPage(val title: String, val description: String, val icon: ImageVector, val color: Color)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(viewModel: MainViewModel, onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()

    // --- UPDATED: Instructional Steps ---
    val pages = listOf(
        TutorialPage(
            "Add Your Accounts",
            "Go to the 'Balance' page and click the '+' button to add your Bank Accounts or Cash. This is where your money lives.",
            Icons.Default.AccountBalance,
            Color(0xFF2196F3)
        ),
        TutorialPage(
            "Track Transactions",
            "Use the '+' button on the Dashboard, Income, or Expense pages to log money coming in or going out. Select the correct account to keep balances updated.",
            Icons.Default.AddCircle,
            Color(0xFF4CAF50)
        ),
        TutorialPage(
            "Manage Loans & Cards",
            "Visit the 'Loans' or 'Credit Cards' pages to track what you owe. You can add payments later to reduce your debt automatically.",
            Icons.Default.CreditCard,
            Color(0xFFF44336)
        ),
        TutorialPage(
            "Set Reminders",
            "Have a monthly bill? Go to 'Recurring Payments' and set a schedule. We will notify you 8 hours before it's due.",
            Icons.Default.Alarm,
            Color(0xFFFF9800)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { scope.launch { viewModel.saveHasSeenTutorial(); onFinish() } }) { Text("Skip") }
        }
        Spacer(modifier = Modifier.weight(1f))
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { pageIndex ->
            val page = pages[pageIndex]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(page.color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = page.icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = page.color)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = page.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = page.description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).size(if (pagerState.currentPage == iteration) 12.dp else 8.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                scope.launch {
                    if (pagerState.currentPage < pages.size - 1) pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    else { viewModel.saveHasSeenTutorial(); onFinish() }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (pagerState.currentPage == pages.size - 1) "Start Using App" else "Next")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}