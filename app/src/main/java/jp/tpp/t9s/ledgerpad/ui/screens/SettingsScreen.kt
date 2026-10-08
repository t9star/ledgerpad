package jp.tpp.t9s.ledgerpad.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.tpp.t9s.ledgerpad.AppContainer
import jp.tpp.t9s.ledgerpad.R
import jp.tpp.t9s.ledgerpad.backup.BackupCodec
import jp.tpp.t9s.ledgerpad.share.Sharing
import jp.tpp.t9s.ledgerpad.util.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val shopName by container.prefs.shopName.collectAsState()
    val currency by container.prefs.currency.collectAsState()
    val lockEnabled by container.prefs.lockEnabled.collectAsState()
    val isAdFree by container.prefs.adFree.collectAsState()
    val proPrice by container.billingManager.formattedPrice.collectAsState()

    var showShopDialog by remember { mutableStateOf(false) }
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf<String?>(null) }

    // Backup JSON Export launcher
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val backup = container.repository.snapshot(shopName, currency)
                val jsonStr = BackupCodec.encodeJson(backup)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(jsonStr.toByteArray())
                }
                snackbarHostState.showSnackbar(context.getString(R.string.backup_exported_success))

                if (context is Activity) {
                    container.adsManager.showInterstitialIfAllowed(context)
                }
            }
        }
    }

    // Backup JSON Import launcher
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val jsonStr = context.contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                if (jsonStr != null) {
                    showRestoreConfirm = jsonStr
                }
            }
        }
    }

    // Export All CSV launcher
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val customers = container.repository.observeCustomers().first()
                val csv = BackupCodec.exportAllCustomersCsv(currency, customers)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(csv.toByteArray())
                }
                snackbarHostState.showSnackbar(context.getString(R.string.csv_exported_success))
            }
        }
    }

    // QR Code management launcher
    val hasQrCode by container.prefs.hasQrCode.collectAsState()
    var showQrPreviewDialog by remember { mutableStateOf(false) }
    val pickQrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        container.prefs.qrCodeFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    container.prefs.notifyQrCodeUpdated()
                    snackbarHostState.showSnackbar(context.getString(R.string.qr_code_saved))
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to save QR code: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Remove Ads Banner
            if (!isAdFree) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.remove_ads_lifetime_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.remove_ads_lifetime_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (context is Activity) {
                                    container.billingManager.launchPurchaseFlow(context)
                                }
                            }
                        ) {
                            Text(stringResource(R.string.upgrade_pro_btn, proPrice))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // General Section
            Text(
                text = stringResource(R.string.section_general),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            // Shop Name Row
            SettingsRow(
                icon = Icons.Default.Store,
                title = stringResource(R.string.shop_business_name),
                subtitle = shopName.ifBlank { stringResource(R.string.not_set) },
                onClick = { showShopDialog = true }
            )

            // Currency Row
            SettingsRow(
                icon = Icons.Default.AttachMoney,
                title = stringResource(R.string.currency),
                subtitle = "${Money.getInfo(currency).symbol} ($currency)",
                onClick = { showCurrencyDropdown = true }
            )

            // App Lock Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_security_lock), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.biometric_pin_protect_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = lockEnabled,
                    onCheckedChange = { container.prefs.setLockEnabled(it) }
                )
            }

            // Shop Payment QR Row
            SettingsRow(
                icon = Icons.Default.QrCode,
                title = stringResource(R.string.payment_qr_code),
                subtitle = if (hasQrCode) stringResource(R.string.show_payment_qr) else stringResource(R.string.not_set),
                onClick = {
                    if (hasQrCode) {
                        showQrPreviewDialog = true
                    } else {
                        pickQrLauncher.launch("image/*")
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Data & Backup Section
            Text(
                text = stringResource(R.string.section_data_backup),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            SettingsRow(
                icon = Icons.Default.Backup,
                title = stringResource(R.string.export_full_backup_json),
                subtitle = stringResource(R.string.export_backup_desc),
                onClick = {
                    val safeDate = System.currentTimeMillis()
                    exportJsonLauncher.launch("ledgerpad_backup_$safeDate.json")
                }
            )

            SettingsRow(
                icon = Icons.Default.Backup,
                title = stringResource(R.string.restore_backup_json),
                subtitle = stringResource(R.string.restore_backup_desc),
                onClick = {
                    importJsonLauncher.launch("application/json")
                }
            )

            SettingsRow(
                icon = Icons.Default.TableChart,
                title = stringResource(R.string.export_all_customers_csv),
                subtitle = stringResource(R.string.export_all_csv_desc),
                onClick = {
                    exportCsvLauncher.launch("ledgerpad_all_customers_${System.currentTimeMillis()}.csv")
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About Section
            Text(
                text = stringResource(R.string.privacy_security_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.local_first_privacy_promise),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Currency selector dropdown
    if (showCurrencyDropdown) {
        AlertDialog(
            onDismissRequest = { showCurrencyDropdown = false },
            title = { Text(stringResource(R.string.select_currency)) },
            text = {
                Column {
                    Money.SUPPORTED.forEach { info ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    container.prefs.setCurrency(info.code)
                                    showCurrencyDropdown = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${info.symbol}  ${info.code}",
                                fontWeight = if (info.code == currency) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (info.code == currency) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Shop Name Dialog
    if (showShopDialog) {
        var tempName by remember { mutableStateOf(shopName) }
        AlertDialog(
            onDismissRequest = { showShopDialog = false },
            title = { Text(stringResource(R.string.shop_business_name)) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    placeholder = { Text(stringResource(R.string.my_general_store)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    container.prefs.setShopName(tempName)
                    showShopDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShopDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Restore Confirmation Dialog
    showRestoreConfirm?.let { jsonContent ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text(stringResource(R.string.restore_backup_title)) },
            text = { Text(stringResource(R.string.restore_backup_confirm_warning)) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val backup = BackupCodec.decodeJson(jsonContent)
                                container.repository.restore(backup)
                                if (backup.shopName.isNotBlank()) container.prefs.setShopName(backup.shopName)
                                if (backup.currency.isNotBlank()) container.prefs.setCurrency(backup.currency)
                                snackbarHostState.showSnackbar(context.getString(R.string.restore_success))
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(context.getString(R.string.restore_failed_corrupt))
                            }
                        }
                        showRestoreConfirm = null
                    }
                ) {
                    Text(stringResource(R.string.restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // QR Code Preview & Management Dialog
    if (showQrPreviewDialog) {
        val qrFile = container.prefs.qrCodeFile
        val bitmap = remember(hasQrCode) {
            if (qrFile.exists()) {
                android.graphics.BitmapFactory.decodeFile(qrFile.absolutePath)?.asImageBitmap()
            } else null
        }
        AlertDialog(
            onDismissRequest = { showQrPreviewDialog = false },
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
                                .size(220.dp)
                                .padding(8.dp)
                        )
                    } else {
                        Text(stringResource(R.string.payment_qr_not_set_hint))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pickQrLauncher.launch("image/*")
                        showQrPreviewDialog = false
                    }
                ) {
                    Text(stringResource(R.string.upload_qr_image))
                }
            },
            dismissButton = {
                Row {
                    if (hasQrCode) {
                        TextButton(
                            onClick = {
                                container.prefs.deleteQrCode()
                                showQrPreviewDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.qr_code_deleted))
                                }
                            }
                        ) {
                            Text(stringResource(R.string.delete_qr_code), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showQrPreviewDialog = false }) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
