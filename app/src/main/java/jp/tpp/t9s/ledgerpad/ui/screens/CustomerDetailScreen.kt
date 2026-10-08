package jp.tpp.t9s.ledgerpad.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.tpp.t9s.ledgerpad.AppContainer
import jp.tpp.t9s.ledgerpad.R
import jp.tpp.t9s.ledgerpad.ads.BannerAdView
import jp.tpp.t9s.ledgerpad.backup.BackupCodec
import jp.tpp.t9s.ledgerpad.data.TxnRow
import jp.tpp.t9s.ledgerpad.data.TxnType
import jp.tpp.t9s.ledgerpad.pdf.StatementPdf
import jp.tpp.t9s.ledgerpad.share.Sharing
import jp.tpp.t9s.ledgerpad.ui.theme.GreenCredit
import jp.tpp.t9s.ledgerpad.ui.theme.RedDebit
import jp.tpp.t9s.ledgerpad.util.Dates
import jp.tpp.t9s.ledgerpad.util.Money
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    container: AppContainer,
    onBack: () -> Unit,
    onAddTxn: (TxnType) -> Unit,
    onEditTxn: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val customer by container.repository.observeCustomer(customerId).collectAsState(initial = null)
    val rows by container.repository.observeTxnRows(customerId).collectAsState(initial = emptyList())
    val currency by container.prefs.currency.collectAsState()
    val shopName by container.prefs.shopName.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    val hasQrCode by container.prefs.hasQrCode.collectAsState()

    val currentCustomer = customer ?: return

    val balance = rows.firstOrNull()?.runningBalanceMinor ?: 0L

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentCustomer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentCustomer.phone.isNotBlank()) {
                            Text(
                                text = currentCustomer.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    // Shop QR code button
                    IconButton(onClick = { showQrDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = stringResource(R.string.payment_qr_code),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    if (currentCustomer.phone.isNotBlank()) {
                        IconButton(onClick = { Sharing.dialNumber(context, currentCustomer.phone) }) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = stringResource(R.string.call),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    IconButton(onClick = {
                        val dueStr = currentCustomer.dueAt?.let { Dates.formatDate(it) }
                        Sharing.sendReminder(
                            context = context,
                            phoneNumber = currentCustomer.phone,
                            customerName = currentCustomer.name,
                            shopName = shopName,
                            balanceFormatted = Money.format(balance, currency),
                            dueFormatted = dueStr
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.send_reminder),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.download_pdf_report)) },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                val pdfFile = StatementPdf.createCustomerStatement(
                                    context = context,
                                    shopName = shopName,
                                    customer = currentCustomer,
                                    currencyCode = currency,
                                    balanceMinor = balance,
                                    rows = rows
                                )
                                // Show interstitial gracefully after generation
                                if (context is Activity) {
                                    container.adsManager.showInterstitialIfAllowed(context) {
                                        Sharing.shareFile(context, pdfFile, "application/pdf", "Account Statement")
                                    }
                                } else {
                                    Sharing.shareFile(context, pdfFile, "application/pdf", "Account Statement")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_csv)) },
                            onClick = {
                                showMenu = false
                                val csv = BackupCodec.exportCustomerCsv(currentCustomer.name, currency, rows)
                                val csvFile = File(context.cacheDir, "statement_${currentCustomer.id}.csv").apply {
                                    writeText(csv)
                                }
                                Sharing.shareFile(context, csvFile, "text/csv", "Customer Statement CSV")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.set_due_date)) },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showDatePicker = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_customer)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_customer), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
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
            Column {
                // Large GAVE / GOT buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onAddTxn(TxnType.GAVE) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedDebit),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_gave_credit),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Button(
                        onClick = { onAddTxn(TxnType.GOT) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenCredit),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_got_payment),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                BannerAdView(prefs = container.prefs)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.net_balance),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (currentCustomer.dueAt != null) {
                            Text(
                                text = stringResource(R.string.due_date_tag, Dates.formatDate(currentCustomer.dueAt!!)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = Money.format(kotlin.math.abs(balance), currency),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                balance > 0 -> RedDebit
                                balance < 0 -> GreenCredit
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = when {
                                balance > 0 -> stringResource(R.string.customer_owes_you)
                                balance < 0 -> stringResource(R.string.you_owe_customer)
                                else -> stringResource(R.string.badge_settled)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Timeline entries
            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_entries_yet_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(rows, key = { it.txn.id }) { row ->
                        TxnTimelineItem(
                            row = row,
                            currency = currency,
                            onClick = { onEditTxn(row.txn.id) },
                            onDelete = {
                                scope.launch {
                                    container.repository.deleteTxn(row.txn.id)
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.entry_deleted),
                                        actionLabel = context.getString(R.string.undo)
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        container.repository.restoreTxn(row.txn)
                                    }
                                }
                            },
                            onShareReceipt = {
                                Sharing.sendPaymentReceipt(
                                    context = context,
                                    phoneNumber = currentCustomer.phone,
                                    customerName = currentCustomer.name,
                                    shopName = shopName,
                                    paidAmountFormatted = Money.format(row.txn.amountMinor, currency),
                                    remainingBalanceFormatted = Money.format(row.runningBalanceMinor, currency)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Due Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentCustomer.dueAt ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        container.repository.setDueDate(currentCustomer.id, datePickerState.selectedDateMillis)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch { container.repository.setDueDate(currentCustomer.id, null) }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.clear))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Edit Customer Dialog
    if (showEditDialog) {
        var editName by remember { mutableStateOf(currentCustomer.name) }
        var editPhone by remember { mutableStateOf(currentCustomer.phone) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_customer)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.customer_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text(stringResource(R.string.phone_number)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editName.isNotBlank()) {
                        scope.launch {
                            container.repository.updateCustomer(currentCustomer.id, editName, editPhone)
                        }
                        showEditDialog = false
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete Customer Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_customer)) },
            text = { Text(stringResource(R.string.delete_customer_confirm_message, currentCustomer.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            container.repository.deleteCustomer(currentCustomer.id)
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Payment QR Code Dialog
    if (showQrDialog) {
        val qrFile = container.prefs.qrCodeFile
        val bitmap = remember(hasQrCode) {
            if (qrFile.exists()) {
                android.graphics.BitmapFactory.decodeFile(qrFile.absolutePath)?.asImageBitmap()
            } else null
        }
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text(stringResource(R.string.payment_qr_code)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = stringResource(R.string.payment_qr_code),
                            modifier = Modifier
                                .size(240.dp)
                                .padding(8.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.payment_qr_not_set_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun TxnTimelineItem(
    row: TxnRow,
    currency: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShareReceipt: (() -> Unit)? = null
) {
    val txn = row.txn
    val isGave = txn.type == TxnType.GAVE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isGave) RedDebit.copy(alpha = 0.15f) else GreenCredit.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isGave) "−" else "+",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGave) RedDebit else GreenCredit
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.note.ifBlank {
                    if (isGave) stringResource(R.string.gave_credit) else stringResource(R.string.received_payment)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                Dates.formatDateTime(txn.occurredAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (isGave) "+ " else "- ") + Money.format(txn.amountMinor, currency),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isGave) RedDebit else GreenCredit
            )
            Text(
                text = stringResource(R.string.bal_prefix, Money.format(row.runningBalanceMinor, currency)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isGave && onShareReceipt != null) {
            IconButton(onClick = onShareReceipt) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = stringResource(R.string.send_receipt),
                    tint = GreenCredit
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
