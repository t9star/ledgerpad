package jp.tpp.t9s.ledgerpad

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import jp.tpp.t9s.ledgerpad.data.CashType
import jp.tpp.t9s.ledgerpad.data.TxnType
import jp.tpp.t9s.ledgerpad.ui.screens.AddCustomerScreen
import jp.tpp.t9s.ledgerpad.ui.screens.AmountEntryScreen
import jp.tpp.t9s.ledgerpad.ui.screens.CustomerDetailScreen
import jp.tpp.t9s.ledgerpad.ui.screens.EntryMode
import jp.tpp.t9s.ledgerpad.ui.screens.HomeScreen
import jp.tpp.t9s.ledgerpad.ui.screens.LockScreen
import jp.tpp.t9s.ledgerpad.ui.screens.SettingsScreen
import jp.tpp.t9s.ledgerpad.ui.theme.LedgerPadTheme

sealed class Screen {
    data object Home : Screen()
    data class CustomerDetail(val customerId: Long) : Screen()
    data object AddCustomer : Screen()
    data class AmountEntry(val mode: EntryMode) : Screen()
    data object Settings : Screen()
}

class MainActivity : FragmentActivity() {

    private val container by lazy { (application as LedgerApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Ads and UMP consent
        container.adsManager.initializeConsentAndAds(this)

        setContent {
            LedgerPadTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LedgerAppNavigation(
                        container = container,
                        onAuthenticate = { onAuthenticated ->
                            showBiometricPrompt(onAuthenticated)
                        }
                    )
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_ledgerpad))
            .setSubtitle(getString(R.string.authenticate_to_access_records))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    override fun onDestroy() {
        super.onDestroy()
        container.billingManager.destroy()
    }
}

@Composable
fun LedgerAppNavigation(
    container: AppContainer,
    onAuthenticate: (onAuthenticated: () -> Unit) -> Unit
) {
    val lockEnabled by container.prefs.lockEnabled.collectAsState()
    var isUnlocked by remember { mutableStateOf(!lockEnabled) }

    LaunchedEffect(lockEnabled) {
        if (!lockEnabled) isUnlocked = true
    }

    if (lockEnabled && !isUnlocked) {
        LockScreen(
            onUnlockClick = {
                onAuthenticate { isUnlocked = true }
            }
        )
        return
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Crossfade(targetState = currentScreen, label = "screen_navigation") { screen ->
        when (screen) {
            is Screen.Home -> {
                HomeScreen(
                    container = container,
                    onOpenCustomer = { id -> currentScreen = Screen.CustomerDetail(id) },
                    onAddCustomer = { currentScreen = Screen.AddCustomer },
                    onAddCash = { cashType ->
                        currentScreen = Screen.AmountEntry(EntryMode.Cashbook(cashType))
                    },
                    onOpenSettings = { currentScreen = Screen.Settings }
                )
            }
            is Screen.CustomerDetail -> {
                CustomerDetailScreen(
                    customerId = screen.customerId,
                    container = container,
                    onBack = { currentScreen = Screen.Home },
                    onAddTxn = { txnType ->
                        currentScreen = Screen.AmountEntry(EntryMode.CustomerTxn(screen.customerId, txnType))
                    },
                    onEditTxn = { /* Edit can be added */ }
                )
            }
            is Screen.AddCustomer -> {
                AddCustomerScreen(
                    container = container,
                    onCustomerCreated = { newId ->
                        currentScreen = Screen.CustomerDetail(newId)
                    },
                    onBack = { currentScreen = Screen.Home }
                )
            }
            is Screen.AmountEntry -> {
                AmountEntryScreen(
                    mode = screen.mode,
                    container = container,
                    onDone = {
                        when (screen.mode) {
                            is EntryMode.CustomerTxn -> currentScreen = Screen.CustomerDetail(screen.mode.customerId)
                            is EntryMode.Cashbook -> currentScreen = Screen.Home
                        }
                    },
                    onBack = {
                        when (screen.mode) {
                            is EntryMode.CustomerTxn -> currentScreen = Screen.CustomerDetail(screen.mode.customerId)
                            is EntryMode.Cashbook -> currentScreen = Screen.Home
                        }
                    }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    container = container,
                    onBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}
