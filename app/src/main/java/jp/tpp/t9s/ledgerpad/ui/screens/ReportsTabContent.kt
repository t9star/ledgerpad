package jp.tpp.t9s.ledgerpad.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.tpp.t9s.ledgerpad.AppContainer
import jp.tpp.t9s.ledgerpad.R
import jp.tpp.t9s.ledgerpad.data.CashEntry
import jp.tpp.t9s.ledgerpad.data.CashType
import jp.tpp.t9s.ledgerpad.data.Txn
import jp.tpp.t9s.ledgerpad.data.TxnType
import jp.tpp.t9s.ledgerpad.ui.theme.GreenCredit
import jp.tpp.t9s.ledgerpad.ui.theme.RedDebit
import jp.tpp.t9s.ledgerpad.util.Dates
import jp.tpp.t9s.ledgerpad.util.Money
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class ChartBucket(
    val label: String,
    val startMs: Long,
    val endMs: Long,
    val inflowMinor: Long,
    val outflowMinor: Long
)

@Composable
fun ReportsTabContent(
    container: AppContainer,
    currency: String,
    onOpenCustomer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableIntStateOf(0) } // 0: 7 Days, 1: This Month, 2: 6 Months

    val timeRange = remember(selectedPeriod) {
        val cal = Calendar.getInstance()
        val endMs = Dates.endOfDay(System.currentTimeMillis())
        when (selectedPeriod) {
            0 -> {
                cal.timeInMillis = Dates.startOfToday()
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.timeInMillis to endMs
            }
            1 -> {
                cal.timeInMillis = Dates.startOfToday()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis to endMs
            }
            else -> {
                cal.timeInMillis = Dates.startOfToday()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -5)
                cal.timeInMillis to endMs
            }
        }
    }

    val cashEntries by container.repository.observeCash(timeRange.first, timeRange.second)
        .collectAsState(initial = emptyList())
    val txns by container.repository.observeAllTxns(timeRange.first, timeRange.second)
        .collectAsState(initial = emptyList())
    val customers by container.repository.observeCustomers()
        .collectAsState(initial = emptyList())

    val cashIn = cashEntries.filter { it.type == CashType.IN }.sumOf { it.amountMinor }
    val cashOut = cashEntries.filter { it.type == CashType.OUT }.sumOf { it.amountMinor }
    val netCash = cashIn - cashOut

    val creditGave = txns.filter { it.type == TxnType.GAVE }.sumOf { it.amountMinor }
    val creditGot = txns.filter { it.type == TxnType.GOT }.sumOf { it.amountMinor }
    val recoveryRate = if (creditGave > 0) {
        ((creditGot.toFloat() / creditGave.toFloat()) * 100f).coerceIn(0f, 100f)
    } else if (creditGot > 0) 100f else 0f

    val buckets = remember(selectedPeriod, cashEntries, txns) {
        buildBuckets(selectedPeriod, cashEntries, txns)
    }

    val nowMs = System.currentTimeMillis()
    val topDebtors = remember(customers) {
        customers.filter { it.balanceMinor > 0 }
            .sortedByDescending { it.balanceMinor }
            .take(5)
    }
    val overdueCount = remember(customers, nowMs) {
        customers.count { it.balanceMinor > 0 && it.customer.dueAt != null && it.customer.dueAt < nowMs }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedPeriod == 0,
                onClick = { selectedPeriod = 0 },
                label = { Text(stringResource(R.string.period_7_days)) }
            )
            FilterChip(
                selected = selectedPeriod == 1,
                onClick = { selectedPeriod = 1 },
                label = { Text(stringResource(R.string.period_this_month)) }
            )
            FilterChip(
                selected = selectedPeriod == 2,
                onClick = { selectedPeriod = 2 },
                label = { Text(stringResource(R.string.period_6_months)) }
            )
        }

        // 1. Cashbook Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.report_cashflow_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cash_in_sales),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = Money.format(cashIn, currency),
                            color = GreenCredit,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cash_out_expenses),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = Money.format(cashOut, currency),
                            color = RedDebit,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = stringResource(R.string.daily_net_balance),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = Money.format(netCash, currency),
                            color = if (netCash >= 0) GreenCredit else RedDebit,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. Credit & Collection Recovery Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.report_credit_recovery_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${recoveryRate.toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GreenCredit
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { recoveryRate / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GreenCredit,
                    trackColor = RedDebit.copy(alpha = 0.25f)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.gave_credit), style = MaterialTheme.typography.labelMedium)
                        Text(Money.format(creditGave, currency), color = RedDebit, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.received_payment), style = MaterialTheme.typography.labelMedium)
                        Text(Money.format(creditGot, currency), color = GreenCredit, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Visual Bar Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.report_chart_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(color = GreenCredit, label = stringResource(R.string.chart_inflow))
                        LegendDot(color = RedDebit, label = stringResource(R.string.chart_outflow))
                    }
                }

                Spacer(Modifier.height(16.dp))

                val maxVal = buckets.maxOfOrNull { maxOf(it.inflowMinor, it.outflowMinor) }?.coerceAtLeast(1L) ?: 1L

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val count = buckets.size.coerceAtLeast(1)
                    val groupWidth = size.width / count
                    val barWidth = (groupWidth * 0.30f).coerceAtMost(28.dp.toPx())
                    val chartHeight = size.height

                    buckets.forEachIndexed { idx, bucket ->
                        val centerX = idx * groupWidth + groupWidth / 2f
                        val inRatio = (bucket.inflowMinor.toFloat() / maxVal.toFloat()).coerceIn(0.03f, 1f)
                        val outRatio = (bucket.outflowMinor.toFloat() / maxVal.toFloat()).coerceIn(0.03f, 1f)

                        val inBarH = if (bucket.inflowMinor > 0) chartHeight * inRatio else 4.dp.toPx()
                        val outBarH = if (bucket.outflowMinor > 0) chartHeight * outRatio else 4.dp.toPx()

                        // Inflow Bar (Green)
                        drawRoundRect(
                            color = GreenCredit,
                            topLeft = Offset(centerX - barWidth - 2.dp.toPx(), chartHeight - inBarH),
                            size = Size(barWidth, inBarH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Outflow Bar (Red)
                        drawRoundRect(
                            color = RedDebit,
                            topLeft = Offset(centerX + 2.dp.toPx(), chartHeight - outBarH),
                            size = Size(barWidth, outBarH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    buckets.forEach { bucket ->
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Top Pending Balances & Overdue
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.report_top_debtors_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (overdueCount > 0) {
                        Text(
                            text = stringResource(R.string.overdue_count_badge, overdueCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = RedDebit,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (topDebtors.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_pending_dues),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    topDebtors.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCustomer(item.customer.id) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.customer.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val due = item.customer.dueAt
                                if (due != null) {
                                    val isOverdue = due < nowMs
                                    Text(
                                        text = stringResource(R.string.due_prefix, Dates.formatDate(due)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOverdue) RedDebit else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = Money.format(item.balanceMinor, currency),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = RedDebit
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildBuckets(
    period: Int,
    cashEntries: List<CashEntry>,
    txns: List<Txn>
): List<ChartBucket> {
    val list = mutableListOf<ChartBucket>()
    val cal = Calendar.getInstance()

    when (period) {
        0 -> {
            // Last 7 days (1 bucket per day)
            val fmt = SimpleDateFormat("M/d", Locale.getDefault())
            cal.timeInMillis = Dates.startOfToday()
            cal.add(Calendar.DAY_OF_YEAR, -6)
            for (i in 0 until 7) {
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val end = cal.timeInMillis
                val label = fmt.format(Date(start))
                list += computeBucket(label, start, end, cashEntries, txns)
            }
        }
        1 -> {
            // This month split into 4 weekly segments
            cal.timeInMillis = Dates.startOfToday()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = cal.timeInMillis
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val segments = listOf(1 to 7, 8 to 14, 15 to 21, 22 to daysInMonth)
            for ((dStart, dEnd) in segments) {
                cal.timeInMillis = monthStart
                cal.set(Calendar.DAY_OF_MONTH, dStart)
                val s = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, dEnd)
                val e = Dates.endOfDay(cal.timeInMillis)
                list += computeBucket("$dStart-$dEnd", s, e, cashEntries, txns)
            }
        }
        else -> {
            // Last 6 months (1 bucket per month)
            val fmt = SimpleDateFormat("MMM", Locale.getDefault())
            cal.timeInMillis = Dates.startOfToday()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MONTH, -5)
            for (i in 0 until 6) {
                val start = cal.timeInMillis
                val label = fmt.format(Date(start))
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis
                list += computeBucket(label, start, end, cashEntries, txns)
            }
        }
    }
    return list
}

private fun computeBucket(
    label: String,
    startMs: Long,
    endMs: Long,
    cashEntries: List<CashEntry>,
    txns: List<Txn>
): ChartBucket {
    val cIn = cashEntries.filter { it.occurredAt in startMs until endMs && it.type == CashType.IN }
        .sumOf { it.amountMinor }
    val tGot = txns.filter { it.occurredAt in startMs until endMs && it.type == TxnType.GOT }
        .sumOf { it.amountMinor }

    val cOut = cashEntries.filter { it.occurredAt in startMs until endMs && it.type == CashType.OUT }
        .sumOf { it.amountMinor }
    val tGave = txns.filter { it.occurredAt in startMs until endMs && it.type == TxnType.GAVE }
        .sumOf { it.amountMinor }

    return ChartBucket(
        label = label,
        startMs = startMs,
        endMs = endMs,
        inflowMinor = cIn + tGot,
        outflowMinor = cOut + tGave
    )
}
