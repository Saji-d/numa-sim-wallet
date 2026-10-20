package com.sajid.numa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier

/** Routes. Tabs are roots (stack reset); everything else is pushed on a back stack. */
sealed interface Screen {
    data object Welcome : Screen
    data object SignIn : Screen
    data object Home : Screen
    data object Numbers : Screen
    data object Esim : Screen
    data object Messages : Screen
    data object Calls : Screen
    data object Profile : Screen
    data object GetNumber : Screen
    data class PickNumber(val country: Country) : Screen
    data class NumberDetail(val id: Int) : Screen
    data class Thread(val id: Int) : Screen
    data object Dialer : Screen
    data class InCall(val peer: String) : Screen
    data class EsimCountry(val country: Country) : Screen
    data class EsimPlanDetail(val planId: String) : Screen
    data object MyEsims : Screen
    data object Wallet : Screen
    data object Settings : Screen
}

class Nav(val stack: SnapshotStateList<Screen>) {
    val current get() = stack.last()
    fun push(s: Screen) { stack.add(s) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun tab(s: Screen) { stack.clear(); stack.add(s) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NumaTheme(store.themeMode) { App() } }
    }
}

private val TABS = listOf(
    Triple(Screen.Home, "Home", Icons.Default.Home),
    Triple(Screen.Esim, "eSIM", Icons.Default.Phone),
    Triple(Screen.Numbers, "Numbers", Icons.Default.Email),
    Triple(Screen.Calls, "Calls", Icons.Default.Call),
    Triple(Screen.Profile, "Profile", Icons.Default.Person),
)

@Composable
fun App() {
    val nav = remember { Nav(mutableStateListOf<Screen>(Screen.Welcome)) }
    LaunchedEffect(store.signedIn) {
        if (!store.signedIn) nav.tab(Screen.Welcome)
        else if (nav.current == Screen.Welcome || nav.current == Screen.SignIn) nav.tab(Screen.Home)
    }
    BackHandler(enabled = nav.stack.size > 1) { nav.back() }

    val cur = nav.current
    Scaffold(
        bottomBar = {
            if (TABS.any { it.first == cur }) NavigationBar {
                TABS.forEach { (s, label, icon) ->
                    NavigationBarItem(
                        selected = cur == s, onClick = { nav.tab(s) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (cur) {
                Screen.Welcome -> WelcomeScreen(nav)
                Screen.SignIn -> SignInScreen(nav)
                Screen.Home -> HomeScreen(nav)
                Screen.Profile -> ProfileScreen(nav)
                Screen.Settings -> SettingsScreen(nav)
                Screen.Numbers -> NumbersScreen(nav)
                Screen.Messages -> MessagesScreen(nav)
                Screen.GetNumber -> GetNumberScreen(nav)
                is Screen.PickNumber -> PickNumberScreen(nav, cur.country)
                is Screen.NumberDetail -> NumberDetailScreen(nav, cur.id)
                is Screen.Thread -> ThreadScreen(nav, cur.id)
                Screen.Calls -> CallsScreen(nav)
                Screen.Dialer -> DialerScreen(nav)
                is Screen.InCall -> InCallScreen(nav, cur.peer)
                Screen.Esim -> EsimScreen(nav)
                is Screen.EsimCountry -> EsimCountryScreen(nav, cur.country)
                is Screen.EsimPlanDetail -> EsimPlanDetailScreen(nav, cur.planId)
                Screen.MyEsims -> MyEsimsScreen(nav)
                Screen.Wallet -> WalletScreen(nav)
            }
        }
    }
}
