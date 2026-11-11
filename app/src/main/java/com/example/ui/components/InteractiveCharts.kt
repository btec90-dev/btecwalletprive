package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Currencies
import com.example.ui.theme.LocalArtisanTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CategorySpending(
    val category: String,
    val amount: Double,
    val color: Color
)

data class MonthlyTrend(
    val monthName: String,
    val income: Double,
    val expense: Double
)

val CHART_PALETTE = listOf(
    Color(0xFF5D4037),
    Color(0xFF8D6E63),
    Color(0xFFD4AF37),
    Color(0xFF1B4D3E),
    Color(0xFF0D2B45),
    Color(0xFF581845),
    Color(0xFFE65100),
    Color(0xFF00796B),
    Color(0xFF455A64),
    Color(0xFF6D4C41),
    Color(0xFFC2185B),
    Color(0xFF388E3C)
)

@Composable
fun CategoryDonutChart(
    items: List<CategorySpending>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalArtisanTheme.current
    val total = items.sumOf { it.amount }

    if (total <= 0.0 || items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses recorded yet",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val displayCategory = selectedIndex?.let { items.getOrNull(it)?.category } ?: "Total Expenses"
    val displayAmount = selectedIndex?.let { items.getOrNull(it)?.amount } ?: total
    val displayPercent = selectedIndex?.let {
        items.getOrNull(it)?.let { item -> (item.amount / total * 100.0) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            val radius = size.width / 2f

                            // Detect tap within donut ring (between 50% and 100% of radius)
                            if (dist in (radius * 0.45f)..(radius * 1.05f)) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                var cumulative = 0f
                                var tapped: Int? = null
                                for ((idx, item) in items.withIndex()) {
                                    val sweep = (item.amount / total * 360.0).toFloat()
                                    if (angle >= cumulative && angle < cumulative + sweep) {
                                        tapped = idx
                                        break
                                    }
                                    cumulative += sweep
                                }
                                selectedIndex = if (selectedIndex == tapped) null else tapped
                            } else {
                                selectedIndex = null
                            }
                        }
                    }
            ) {
                val strokeWidth = 32.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f
                for ((index, item) in items.withIndex()) {
                    val sweepAngle = (item.amount / total * 360.0).toFloat()
                    val isSelected = selectedIndex == index
                    val currentStroke = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth

                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 2f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            // Center metric display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayCategory,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = Currencies.format(displayAmount, currencyCode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.primaryColor,
                    textAlign = TextAlign.Center
                )
                if (displayPercent != null) {
                    Text(
                        text = "%.1f%%".format(displayPercent),
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.stitchingColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Legend - fits cleanly without horizontal scrolling
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val topItems = items.take(5)
            for ((idx, item) in topItems.withIndex()) {
                val isSelected = selectedIndex == idx
                val percent = (item.amount / total * 100.0)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) theme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent
                        )
                        .clickable {
                            selectedIndex = if (selectedIndex == idx) null else idx
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = "%.1f%%".format(percent),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = Currencies.format(item.amount, currencyCode),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = theme.primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendBarChart(
    trends: List<MonthlyTrend>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalArtisanTheme.current
    if (trends.isEmpty()) return

    val maxVal = trends.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(100.0)
    var selectedMonth by remember { mutableStateOf<MonthlyTrend?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selected Tooltip if tapped
        selectedMonth?.let { month ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(theme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = month.monthName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryColor
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "+${Currencies.format(month.income, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "-${Currencies.format(month.expense, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Zero horizontal scrolling: entire chart spans the width evenly
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(trends) {
                    detectTapGestures { offset ->
                        val colWidth = size.width / trends.size
                        val index = (offset.x / colWidth).toInt().coerceIn(0, trends.lastIndex)
                        selectedMonth = if (selectedMonth == trends[index]) null else trends[index]
                    }
                }
        ) {
            val chartWidth = size.width
            val chartHeight = size.height - 24.dp.toPx()
            val colWidth = chartWidth / trends.size
            val barWidth = (colWidth * 0.32f).coerceAtMost(20.dp.toPx())
            val cornerRadius = 4.dp.toPx()

            // Draw subtle horizontal gridlines
            val gridColor = Color.LightGray.copy(alpha = 0.4f)
            drawLine(
                color = gridColor,
                start = Offset(0f, chartHeight),
                end = Offset(chartWidth, chartHeight),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, chartHeight * 0.5f),
                end = Offset(chartWidth, chartHeight * 0.5f),
                strokeWidth = 1.dp.toPx()
            )

            for ((i, item) in trends.withIndex()) {
                val groupCenterX = (i + 0.5f) * colWidth
                val incomeHeight = ((item.income / maxVal) * (chartHeight - 10f)).toFloat()
                val expenseHeight = ((item.expense / maxVal) * (chartHeight - 10f)).toFloat()

                // Income bar (green / forest)
                val incomeLeft = groupCenterX - barWidth - 2.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF388E3C),
                    topLeft = Offset(incomeLeft, chartHeight - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )

                // Expense bar (leather / terracotta)
                val expenseLeft = groupCenterX + 2.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFD32F2F),
                    topLeft = Offset(expenseLeft, chartHeight - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }

        // Month Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            for (item in trends) {
                Text(
                    text = item.monthName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedMonth == item) theme.primaryColor else Color.Gray,
                    fontWeight = if (selectedMonth == item) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend: Income / Expense
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF388E3C))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Income", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFD32F2F))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Expenses", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
