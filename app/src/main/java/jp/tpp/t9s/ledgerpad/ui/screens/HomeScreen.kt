package jp.tpp.t9s.ledgerpad.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.tpp.t9s.ledgerpad.AppContainer
import jp.tpp.t9s.ledgerpad.R
import jp.tpp.t9s.ledgerpad.ads.BannerAdView
import jp.tpp.t9s.ledgerpad.data.CashEntry
import jp.tpp.t9s.ledgerpad.data.CashType
import jp.tpp.t9s.ledgerpad.data.CustomerSummary
import jp.tpp.t9s.ledgerpad.data.SortMode
import jp.tpp.t9s.ledgerpad.ui.theme.GreenCredit
import jp.tpp.t9s.ledgerpad.ui.theme.RedDebit
import jp.tpp.t9s.ledgerpad.util.Dates
import jp.tpp.t9s.ledgerpad.util.Money
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenCustomer: (Long) -> Unit,
    onAddCustomer: () -> Unit,
    onAddCash: (CashType) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Customers, 1: Cashbook
    val shopName by container.prefs.shopName.collectAsState()
    val currency by container.prefs.currency.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = shopName.ifBlank { stringResource(R.string.app_name) },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.offline_ledger_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BannerAdView(prefs = container.prefs)
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onAddCustomer,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_customer)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_customers), fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_cashbook), fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                CustomersTabContent(
                    container = container,
                    currency = currency,
                    onOpenCustomer = onOpenCustomer
                )
            } else {
                CashbookTabContent(
                    container = container,
                    currency = currency,
                    onAddCash = onAddCash
                )
            }
        }
    }
}

@Composable
private fun CustomersTabContent(
    container: AppContainer,
    currency: String,
    onOpenCustomer: (Long) -> Unit
) {
    val customers by container.repository.observeCustomers().collectAsState(initial = emptyList())
    val sortMode by container.prefs.sortMode.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Summary calculations
    val totalToGet = customers.filter { it.balanceMinor > 0 }.sumOf { it.balanceMinor }
    val totalToGive = customers.filter { it.balanceMinor < 0 }.sumOf { -it.balanceMinor }

    val filtered = remember(customers, searchQuery, sortMode) {
        val q = searchQuery.trim().lowercase()
        val list = if (q.isBlank()) customers else customers.filter {
            it.customer.name.lowercase().contains(q) || it.customer.phone.contains(q)
        }
        when (sortMode) {
            SortMode.RECENT -> list.sortedByDescending { it.lastActivityAt ?: it.customer.createdAt }
            SortMode.BALANCE -> list.sortedByDescending { it.balanceMinor }
            SortMode.NAME -> list.sortedBy { it.customer.name.lowercase() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.total_to_receive),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = Money.format(totalToGet, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RedDebit
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.total_to_give),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = Money.format(totalToGive, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenCredit
                    )
                }
            }
        }

        // Search Bar & Sort Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.search_customers)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.sort))
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_recent)) },
                        onClick = {
                            scope.launch { container.prefs.setSortMode(SortMode.RECENT) }
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_balance)) },
                        onClick = {
                            scope.launch { container.prefs.setSortMode(SortMode.BALANCE) }
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_name)) },
                        onClick = {
                            scope.launch { container.prefs.setSortMode(SortMode.NAME) }
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Customer List
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) stringResource(R.string.empty_customers_hint) else stringResource(R.string.no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
            ) {
                items(filtered, key = { it.customer.id }) { item ->
                    CustomerListItem(
                        item = item,
                        currency = currency,
                        onClick = { onOpenCustomer(item.customer.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerListItem(
    item: CustomerSummary,
    currency: String,
    onClick: () -> Unit
) {
    val initial = item.customer.name.take(1).uppercase()
    val balance = item.balanceMinor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.customer.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtext = when {
                item.customer.dueAt != null -> stringResource(R.string.due_prefix, Dates.formatDate(item.customer.dueAt))
                item.lastActivityAt != null -> Dates.formatDate(item.lastActivityAt)
                else -> stringResource(R.string.no_activity_yet)
            }
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Money.format(kotlin.math.abs(balance), currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    balance > 0 -> RedDebit
                    balance < 0 -> GreenCredit
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = when {
                    balance > 0 -> stringResource(R.string.badge_to_receive)
                    balance < 0 -> stringResource(R.string.badge_to_give)
                    else -> stringResource(R.string.badge_settled)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CashbookTabContent(
    container: AppContainer,
    currency: String,
    onAddCash: (CashType) -> Unit
) {
    val todayStart = remember { Dates.startOfToday() }
    val todayEnd = remember { Dates.endOfDay(todayStart) }
    val entries by container.repository.observeCash(todayStart, todayEnd).collectAsState(initial = emptyList())

    val totalIn = entries.filter { it.type == CashType.IN }.sumOf { it.amountMinor }
    val totalOut = entries.filter { it.type == CashType.OUT }.sumOf { it.amountMinor }
    val net = totalIn - totalOut

    Column(modifier = Modifier.fillMaxSize()) {
        // Daily Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.todays_cash_summary),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.cash_in_sales), style = MaterialTheme.typography.labelMedium)
                        Text(Money.format(totalIn, currency), color = GreenCredit, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(stringResource(R.string.cash_out_expenses), style = MaterialTheme.typography.labelMedium)
                        Text(Money.format(totalOut, currency), color = RedDebit, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(stringResource(R.string.daily_net_balance), style = MaterialTheme.typography.labelMedium)
                        Text(
                            Money.format(net, currency),
                            color = if (net >= 0) GreenCredit else RedDebit,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Action Buttons: + Cash In (Sale) / - Cash Out (Expense)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.Button(
                onClick = { onAddCash(CashType.IN) },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = GreenCredit)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.btn_cash_in))
            }

            androidx.compose.material3.Button(
                onClick = { onAddCash(CashType.OUT) },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RedDebit)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.btn_cash_out))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Entries list
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_cash_entries_today),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
            ) {
                items(entries, key = { it.id }) { item ->
                    CashEntryListItem(entry = item, currency = currency)
                }
            }
        }
    }
}

@Composable
private fun CashEntryListItem(entry: CashEntry, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (entry.type == CashType.IN) GreenCredit.copy(alpha = 0.15f) else RedDebit.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (entry.type == CashType.IN) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = if (entry.type == CashType.IN) GreenCredit else RedDebit
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.note.ifBlank {
                    if (entry.type == CashType.IN) stringResource(R.string.cash_in_sale) else stringResource(R.string.cash_out_expense)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = Dates.formatDateTime(entry.occurredAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = (if (entry.type == CashType.IN) "+ " else "- ") + Money.format(entry.amountMinor, currency),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (entry.type == CashType.IN) GreenCredit else RedDebit
        )
    }
}
