package com.sajid.numa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------- Onboarding

private data class IntroPage(val icon: ImageVector, val title: String, val body: String)

private val INTRO = listOf(
    IntroPage(Icons.Default.Email, "A number for every purpose",
        "Choose a virtual number from eight countries and keep a separate line for sign-ups, work and travel."),
    IntroPage(Icons.Default.Phone, "Travel eSIM plans",
        "Compare data plans by destination, see the price and validity up front, and follow the install steps."),
    IntroPage(Icons.Default.ShoppingCart, "One wallet",
        "Numbers, renewals and eSIM plans all draw from a single credit balance with a full transaction history."),
)

@Composable
fun WelcomeScreen(nav: Nav) {
    var page by remember { mutableIntStateOf(0) }
    val p = INTRO[page]
    val last = page == INTRO.lastIndex
    Column(Modifier.fillMaxSize().padding(Dim.screen + 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (!last) TextButton(onClick = { nav.push(Screen.SignIn) }) { Text("Skip") }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(132.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(p.icon, null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(32.dp))
        Text("Numa", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(p.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            p.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.semantics { contentDescription = "Page ${page + 1} of ${INTRO.size}" },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            INTRO.indices.forEach { i ->
                Box(
                    Modifier.height(8.dp).width(if (i == page) 24.dp else 8.dp).clip(CircleShape)
                        .background(if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(if (last) "Get started" else "Next") {
            if (last) nav.push(Screen.SignIn) else page++
        }
    }
}

@Composable
fun SignInScreen(nav: Nav) {
    var name by remember { mutableStateOf("") }
    var mail by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val submit = { error = store.signIn(name, mail) }

    ScreenFrame("Create your profile", onBack = { nav.back() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(Dim.gap))
            Text(
                "Set up the profile used across your numbers, messages and plans.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Dim.screen + 4.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it; error = null },
                label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            )
            Spacer(Modifier.height(Dim.gap))
            OutlinedTextField(
                value = mail, onValueChange = { mail = it; error = null },
                label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); submit() }),
            )
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(Dim.screen + 4.dp))
            PrimaryButton("Continue") { focus.clearFocus(); submit() }
            Spacer(Modifier.height(Dim.gap))
            ServiceNotice("Your profile is stored on this device only.")
        }
    }
}

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(nav: Nav) {
    val first = store.displayName.trim().substringBefore(' ').ifEmpty { "there" }
    val expiring = store.numbers.filter { store.isExpired(it) || daysLeft(it.expiresAt) <= 5 }
    val readyEsims = store.esims.filter { !it.installed }
    val installedEsims = store.esims.filter { it.installed }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dim.screen)) {
        Text("Hi, $first", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Your numbers and travel plans",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dim.screen))

        // Wallet
        Card(
            onClick = { nav.push(Screen.Wallet) },
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Wallet balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    "${creditsText(store.credits)}", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary,
                )
                Text("Add credits or view history", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        // Quick actions
        Spacer(Modifier.height(Dim.screen))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction(Icons.Default.Add, "Get number", Modifier.weight(1f)) { nav.push(Screen.GetNumber) }
            QuickAction(Icons.Default.Phone, "Buy eSIM", Modifier.weight(1f)) { nav.tab(Screen.Esim) }
            QuickAction(Icons.Default.Call, "Dial", Modifier.weight(1f)) { nav.push(Screen.Dialer) }
            QuickAction(Icons.Default.Star, "My eSIMs", Modifier.weight(1f)) { nav.push(Screen.MyEsims) }
        }

        // Attention: expiring numbers
        if (expiring.isNotEmpty()) {
            SectionTitle("Needs attention")
            expiring.forEach { n ->
                val expired = store.isExpired(n)
                AppCard(Modifier.padding(bottom = 8.dp), onClick = { nav.push(Screen.NumberDetail(n.id)) }) {
                    Text(
                        if (expired) "${n.e164} has expired" else "${n.e164} expires in ${daysLeft(n.expiresAt)} days",
                        fontWeight = FontWeight.SemiBold,
                        color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Text("Open to renew for ${creditsText(n.country.pricePerMonth)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Messages
        if (store.unreadCount > 0) {
            SectionTitle("Messages")
            AppCard(onClick = { nav.push(Screen.Messages) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Dim.gap))
                    Text(
                        if (store.unreadCount == 1) "1 unread conversation" else "${store.unreadCount} unread conversations",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Numbers
        SectionTitle("Your numbers")
        if (store.numbers.isEmpty()) {
            AppCard {
                Text("No numbers yet", fontWeight = FontWeight.SemiBold)
                Text(
                    "Pick a country and add a number to your account.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dim.gap))
                Button(onClick = { nav.push(Screen.GetNumber) }) { Text("Get a number") }
            }
        } else {
            store.numbers.take(3).forEach { n ->
                AppCard(Modifier.padding(bottom = 8.dp), onClick = { nav.push(Screen.NumberDetail(n.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(n.country.flag, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(Dim.gap))
                        Column(Modifier.weight(1f)) {
                            Text(n.e164, fontWeight = FontWeight.SemiBold)
                            Text(n.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (store.numbers.size > 3) TextButton(onClick = { nav.tab(Screen.Numbers) }) { Text("View all ${store.numbers.size} numbers") }
        }

        // eSIMs
        SectionTitle("Travel eSIMs")
        if (store.esims.isEmpty()) {
            AppCard(onClick = { nav.tab(Screen.Esim) }) {
                Text("No plans yet", fontWeight = FontWeight.SemiBold)
                Text(
                    "Browse destinations and compare data plans.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            AppCard(onClick = { nav.push(Screen.MyEsims) }) {
                Text(
                    buildString {
                        append("${store.esims.size} ${if (store.esims.size == 1) "plan" else "plans"}")
                        if (readyEsims.isNotEmpty()) append(" · ${readyEsims.size} ready to install")
                        if (installedEsims.isNotEmpty()) append(" · ${installedEsims.size} installed")
                    },
                    fontWeight = FontWeight.SemiBold,
                )
                val latest = store.esims.first()
                Text(
                    "${latest.plan.country.flag} ${latest.plan.country.name} · ${latest.plan.dataGb} GB · ${latest.plan.days} days",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Recent activity
        SectionTitle("Recent activity")
        if (store.txns.isEmpty()) {
            Text("No activity yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            store.txns.take(3).forEach { t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.bodyMedium)
                        Text(formatRelative(t.at), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        (if (t.amount > 0) "+" else "") + t.amount,
                        fontWeight = FontWeight.SemiBold,
                        color = if (t.amount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Spacer(Modifier.height(Dim.screen))
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick, modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------- Profile

@Composable
fun ProfileScreen(nav: Nav) {
    var editing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dim.screen)) {
        Spacer(Modifier.height(Dim.gap))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    store.displayName.trim().take(1).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(Dim.screen))
            Column(Modifier.weight(1f)) {
                Text(store.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(store.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, "Edit profile") }
        }

        Spacer(Modifier.height(Dim.screen))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Numbers", store.numbers.size.toString(), Modifier.weight(1f))
            StatTile("eSIMs", store.esims.size.toString(), Modifier.weight(1f))
            StatTile("Credits", store.credits.toString(), Modifier.weight(1f))
        }

        SectionTitle("Account")
        ProfileLink(Icons.Default.ShoppingCart, "Wallet", "Balance and transactions") { nav.push(Screen.Wallet) }
        ProfileLink(Icons.Default.Email, "Messages", if (store.unreadCount > 0) "${store.unreadCount} unread" else "All caught up") { nav.push(Screen.Messages) }
        ProfileLink(Icons.Default.Phone, "My eSIMs", "${store.esims.size} purchased") { nav.push(Screen.MyEsims) }
        ProfileLink(Icons.Default.Settings, "Settings", "Appearance, notifications, data") { nav.push(Screen.Settings) }
    }

    if (editing) EditProfileDialog(onDismiss = { editing = false })
}

@Composable
private fun EditProfileDialog(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(store.displayName) }
    var mail by remember { mutableStateOf(store.email) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column {
                OutlinedTextField(
                    name, { name = it; error = null }, label = { Text("Full name") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    mail, { mail = it; error = null }, label = { Text("Email") }, singleLine = true, isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val e = store.updateProfile(name, mail)
                if (e == null) onDismiss() else error = e
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    AppCard(Modifier.padding(bottom = 8.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Dim.gap))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------- Settings

private val THEME_LABELS = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: Nav) {
    var confirmDelete by remember { mutableStateOf(false) }

    ScreenFrame("Settings", onBack = { nav.back() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionTitle("Appearance")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                THEME_LABELS.forEach { (mode, label) ->
                    FilterChip(
                        selected = store.themeMode == mode,
                        onClick = { store.setThemeMode(mode) },
                        label = { Text(label) },
                    )
                }
            }

            SectionTitle("Notifications")
            SettingSwitch("Notifications", "Reminders for expiring numbers and plans", store.notifications) { store.setNotifications(it) }
            SettingSwitch("Message alerts", "Alert when a new message arrives", store.messageAlerts) { store.setMessageAlerts(it) }

            SectionTitle("Integrations")
            ServiceNotice("These services need a provider account and are not connected in this build.")
            Spacer(Modifier.height(8.dp))
            listOf("Voice calling", "SMS delivery", "eSIM provisioning", "Payments").forEach { IntegrationRow(it) }

            SectionTitle("About")
            AppCard {
                Text("Numa", fontWeight = FontWeight.SemiBold)
                Text("Version 0.2.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Data is stored locally on this device.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionTitle("Account")
            OutlinedButton(onClick = { store.signOut() }, Modifier.fillMaxWidth().height(48.dp)) { Text("Sign out") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmDelete = true }, Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete all data") }
            Spacer(Modifier.height(Dim.screen))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all data?") },
            text = { Text("This removes your profile, numbers, messages, call history, plans and wallet from this device. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; store.deleteAllData() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun IntegrationRow(name: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("Not connected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
