package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Currencies
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.WalletCategories
import com.example.ui.components.ArtisanCard
import com.example.ui.components.CHART_PALETTE
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CategorySpending
import com.example.ui.components.MonthlyTrend
import com.example.ui.components.MonthlyTrendBarChart
import com.example.ui.theme.LocalArtisanTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    transactions: List<TransactionEntity>,
    baseCurrency: String
) {
    val theme = LocalArtisanTheme.current

    // Categorical spending computation
    val expenseCategories = remember(transactions) {
        val expenseTx = transactions.filter { it.type == TransactionType.EXPENSE && !it.isDeleted }
        val grouped = expenseTx.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        grouped.mapIndexed { index, (cat, amount) ->
            CategorySpending(
                category = cat,
                amount = amount,
                color = CHART_PALETTE[index % CHART_PALETTE.size]
            )
        }
    }

    val totalIncome = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME && !it.isDeleted }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE && !it.isDeleted }.sumOf { it.amount }
    }
    val netCashFlow = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome * 100.0).coerceIn(0.0, 100.0) else 0.0

    // Monthly trends computation (last 6 calendar months fitted cleanly across width)
    val monthlyTrends = remember(transactions) {
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)

        val months = mutableListOf<Pair<String, String>>() // Key to Display
        for (i in 4 downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
            months.add(monthKeyFormat.format(c.time) to monthFormat.format(c.time))
        }

        months.map { (key, display) ->
            val inc = transactions.filter {
                it.type == TransactionType.INCOME &&
                !it.isDeleted &&
                monthKeyFormat.format(Date(it.timestamp)) == key
            }.sumOf { it.amount }

            val exp = transactions.filter {
                it.type == TransactionType.EXPENSE &&
                !it.isDeleted &&
                monthKeyFormat.format(Date(it.timestamp)) == key
            }.sumOf { it.amount }

            MonthlyTrend(
                monthName = display,
                income = inc,
                expense = exp
            )
        }
    }

    val highestExpense = expenseCategories.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_screen")
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cash Flow & Savings Rate Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cash Flow Card
                ArtisanCard(
                    modifier = Modifier.weight(1f),
                    containerColor = theme.surfaceColor,
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (netCashFlow >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (netCashFlow >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.cash_flow),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = Currencies.format(netCashFlow, baseCurrency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (netCashFlow >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }

                // Savings Rate Card
                ArtisanCard(
                    modifier = Modifier.weight(1f),
                    containerColor = theme.surfaceColor,
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = theme.stitchingColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.savings_rate),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "%.1f%%".format(savingsRate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }
                }
            }
        }

        // Highest Category Banner
        if (highestExpense != null) {
            item {
                ArtisanCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = theme.primaryContainer.copy(alpha = 0.5f),
                    elevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = WalletCategories.getIcon(highestExpense.category),
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.highest_expense),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = highestExpense.category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                        }
                        Text(
                            text = Currencies.format(highestExpense.amount, baseCurrency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }
                }
            }
        }

        // Interactive Categorical Spending Donut Card
        item {
            ArtisanCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = theme.surfaceColor,
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = null,
                            tint = theme.stitchingColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.spending_breakdown),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    CategoryDonutChart(
                        items = expenseCategories,
                        currencyCode = baseCurrency
                    )
                }
            }
        }

        // Monthly Income vs Expenses Trend Bar Chart Card (Zero horizontal scrolling)
        item {
            ArtisanCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = theme.surfaceColor,
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = theme.stitchingColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.monthly_trends),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    MonthlyTrendBarChart(
                        trends = monthlyTrends,
                        currencyCode = baseCurrency
                    )
                }
            }
        }
    }
}
