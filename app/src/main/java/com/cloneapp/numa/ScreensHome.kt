package com.cloneapp.numa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ---------------- Onboarding ----------------

private data class IntroPage(val icon: ImageVector, val title: String, val body: String)

private val INTRO = listOf(
    IntroPage(Icons.Default.Email, "Numbers for anything", "Browse virtual numbers from several countries and keep a private line for sign-ups and messages."),
    IntroPage(Icons.Default.Phone, "Travel eSIM plans", "Pick a country, compare data plans and see how an eSIM would be set up before you fly."),
    IntroPage(Icons.Default.Call, "Calls & credits", "One wallet for numbers, plans and calls. This build is a demo: nothing is really connected."),
)

@Composable
fun WelcomeScreen(nav: Nav) {
    var page by remember { mutableIntStateOf(0) }
    val p = INTRO[page]
    val last = page == INTRO.lastIndex
    Column(Modifier.fillMaxSize().padding(Dim.screen + 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!last) TextButton(onClick = { page = INTRO.lastIndex }) { Text("Skip") }
            else Spacer(Modifier.height(48.dp))
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(120.dp).clip(CircleShape).background(BrandSoft), contentAlignment = Alignment.Center) {
            Icon(p.icon, null, Modifier.size(56.dp), tint = Brand)
        }
        Spacer(Modifier.height(32.dp))
        Text(p.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(p.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            INTRO.indices.forEach { i ->
                Box(Modifier.size(if (i == page) 24.dp else 8.dp, 8.dp).clip(CircleShape)
                    .background(if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(if (last) "Get started" else "Next") {
            if (last) nav.push(Screen.SignIn) else page++
        }
        Spacer(Modifier.height(8.dp))
        DemoBadge()
    }
}

@Composable
fun SignInScreen(nav: Nav) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val nameOk = name.isNotBlank()
    val emailOk = email.contains("@") && email.substringAfter("@").contains(".")
    ScreenFrame("Sign in", onBack = { nav.back() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create your account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DemoBadge()
            }
            Text(
                "Demo sign-in: no account is created, nothing is sent and no password is stored.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true,
                isError = tried && !nameOk, supportingText = { if (tried && !nameOk) Text("Enter your name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
                isError = tried && !emailOk, supportingText = { if (tried && !emailOk) Text("Enter a valid email") },
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("Continue") {
                tried = true
                if (nameOk && emailOk) Store.signIn(name.trim(), email.trim())
            }
        }
    }
}

// ---------------- Home ----------------

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier, onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun HomeScreen(nav: Nav) {
    val unread = Store.threads.count { it.unread }
    val esim = Store.esims.firstOrNull()
    ScreenFrame("Hello, ${Store.displayName}") {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
            Card(
                onClick = { nav.push(Screen.Wallet) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Credits balance", color = MaterialTheme.colorScheme.onPrimary)
                    Text("${Store.credits}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    Text("Tap to top up (demo)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dim.gap)) {
                QuickAction("Get number", Icons.Default.Add, Modifier.weight(1f)) { nav.push(Screen.GetNumber) }
                QuickAction("Buy eSIM", Icons.Default.Phone, Modifier.weight(1f)) { nav.tab(Screen.Esim) }
                QuickAction("Dial", Icons.Default.Call, Modifier.weight(1f)) { nav.push(Screen.Dialer) }
                QuickAction("My eSIMs", Icons.Default.Star, Modifier.weight(1f)) { nav.push(Screen.MyEsims) }
            }
            if (unread > 0) {
                AppCard(onClick = { nav.push(Screen.Messages) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("$unread unread message${if (unread > 1) "s" else ""}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            SectionTitle("Your numbers")
            if (Store.numbers.isEmpty()) {
                AppCard {
                    Text("No numbers yet", fontWeight = FontWeight.SemiBold)
                    Text("Pick a virtual number to start receiving demo messages.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton("Get a number") { nav.push(Screen.GetNumber) }
                }
            } else {
                Store.numbers.forEach { n ->
                    AppCard(onClick = { nav.push(Screen.NumberDetail(n.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(n.country.flag, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(n.e164, fontWeight = FontWeight.SemiBold)
                                Text("${n.label} · ${n.country.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            SectionTitle("Travel eSIM")
            if (esim == null) {
                AppCard(onClick = { nav.tab(Screen.Esim) }) {
                    Text("No eSIM yet", fontWeight = FontWeight.SemiBold)
                    Text("Browse plans for your next trip.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                AppCard(onClick = { nav.push(Screen.MyEsims) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(esim.plan.country.flag, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("${esim.plan.country.name} · ${esim.plan.dataGb} GB / ${esim.plan.days} days", fontWeight = FontWeight.SemiBold)
                            Text(esim.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Dim.screen))
        }
    }
}

// ---------------- Profile & Settings ----------------

@Composable
private fun LinkRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProfileScreen(nav: Nav) {
    ScreenFrame("Profile") {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
            Column(Modifier.fillMaxWidth().padding(vertical = Dim.screen), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text(
                        Store.displayName.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(Store.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (Store.email.isNotBlank()) Text(Store.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinkRow("Wallet & transactions", Icons.Default.ShoppingCart) { nav.push(Screen.Wallet) }
            LinkRow("Messages", Icons.Default.Email) { nav.push(Screen.Messages) }
            LinkRow("My eSIMs", Icons.Default.Phone) { nav.push(Screen.MyEsims) }
            LinkRow("Settings", Icons.Default.Settings) { nav.push(Screen.Settings) }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(nav: Nav) {
    var notifications by remember { mutableStateOf(true) }
    var lowCredit by remember { mutableStateOf(true) }
    var biometric by remember { mutableStateOf(false) }
    ScreenFrame("Settings", onBack = { nav.back() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
            SectionTitle("Preferences")
            AppCard {
                ToggleRow("Message notifications", notifications) { notifications = it }
                ToggleRow("Low credit alerts", lowCredit) { lowCredit = it }
                ToggleRow("App lock", biometric) { biometric = it }
                Text("Toggles are local to this screen and not saved.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionTitle("About")
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Numa 0.1", fontWeight = FontWeight.SemiBold)
                    DemoBadge()
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Demo build. Numbers, SMS, calls, payments and eSIM activation are simulated with in-memory sample data. " +
                        "No carrier, payment provider or server is connected.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { Store.signOut() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Sign out")
            }
            Spacer(Modifier.height(Dim.screen))
        }
    }
}
