package com.cloneapp.numa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
private fun UnreadDot() {
    Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
}

@Composable
fun NumbersScreen(nav: Nav) {
    val unread = Store.threads.any { it.unread }
    ScreenFrame("My numbers", actions = { DemoBadge(); Spacer(Modifier.width(Dim.screen)) }) {
        Column(Modifier.fillMaxSize()) {
            AppCard(onClick = { nav.push(Screen.Messages) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SMS inbox", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (unread) { UnreadDot(); Spacer(Modifier.width(8.dp)) }
                    Text(
                        "${Store.threads.size} thread${if (Store.threads.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(Dim.gap))
            if (Store.numbers.isEmpty()) {
                Box(Modifier.weight(1f)) {
                    EmptyState(
                        "No numbers yet",
                        "Get a virtual number to receive demo SMS and place demo calls.",
                        "Get number",
                    ) { nav.push(Screen.GetNumber) }
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
                    items(Store.numbers, key = { it.id }) { n ->
                        AppCard(onClick = { nav.push(Screen.NumberDetail(n.id)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(n.country.flag, style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.width(Dim.gap))
                                Column(Modifier.weight(1f)) {
                                    Text(n.e164, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${n.label} · ${n.country.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text("${n.country.pricePerMonth} cr/mo", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Dim.gap))
                PrimaryButton("Get number") { nav.push(Screen.GetNumber) }
                Spacer(Modifier.height(Dim.gap))
            }
        }
    }
}

@Composable
fun MessagesScreen(nav: Nav) {
    ScreenFrame("Messages", onBack = { nav.back() }, actions = { DemoBadge(); Spacer(Modifier.width(Dim.screen)) }) {
        if (Store.threads.isEmpty()) {
            EmptyState("No messages", "Messages sent to your demo numbers will show up here.", "Get number") {
                nav.push(Screen.GetNumber)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Dim.gap), contentPadding = PaddingValues(vertical = Dim.gap)) {
                items(Store.threads, key = { it.id }) { t ->
                    val last = t.messages.lastOrNull()
                    val num = Store.numbers.firstOrNull { it.id == t.numberId }
                    AppCard(onClick = { nav.push(Screen.Thread(t.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t.peer,
                                    fontWeight = if (t.unread) FontWeight.Bold else FontWeight.SemiBold,
                                )
                                if (num != null) Text(
                                    "to ${num.e164}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (last != null) Text(
                                    last.text,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (t.unread) UnreadDot()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GetNumberScreen(nav: Nav) {
    var query by remember { mutableStateOf("") }
    val shown = COUNTRIES.filter {
        it.name.contains(query, ignoreCase = true) || it.dial.contains(query) || it.code.contains(query, ignoreCase = true)
    }
    ScreenFrame("Choose a country", onBack = { nav.back() }) {
        OutlinedTextField(
            query, { query = it },
            Modifier.fillMaxWidth().padding(top = Dim.gap),
            placeholder = { Text("Search country or code") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
        )
        Spacer(Modifier.height(Dim.gap))
        if (shown.isEmpty()) {
            EmptyState("No matches", "No country matches \"$query\".")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
                items(shown, key = { it.code }) { c ->
                    AppCard(onClick = { nav.push(Screen.PickNumber(c)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.flag, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(Dim.gap))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.SemiBold)
                                Text(c.dial, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${c.pricePerMonth} cr/mo", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PickNumberScreen(nav: Nav, country: Country) {
    val options = remember(country) { candidateNumbers(country) }
    var selected by remember(country) { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf<String?>(null) }
    val cost = country.pricePerMonth
    val affordable = Store.credits >= cost

    ScreenFrame("${country.flag} ${country.name}", onBack = { nav.back() }, actions = { DemoBadge(); Spacer(Modifier.width(Dim.screen)) }) {
        SectionTitle("Available numbers")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { num ->
                val owned = Store.numbers.any { it.e164 == num }
                AppCard(onClick = { if (!owned) { selected = num; error = null } }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected == num, onClick = null, enabled = !owned)
                        Spacer(Modifier.width(Dim.gap))
                        Text(num, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        if (owned) Text("Owned", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(Dim.gap))
        Text("Cost: $cost credits / month  ·  Balance: ${Store.credits}", style = MaterialTheme.typography.bodyMedium)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (!affordable) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton({ nav.push(Screen.Wallet) }, Modifier.fillMaxWidth()) { Text("Add credits") }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Confirm (demo purchase)", enabled = selected != null && affordable) {
            val s = selected ?: return@PrimaryButton
            val err = Store.buyNumber(s, country)
            if (err == null) done = s else error = err
        }
        Spacer(Modifier.height(Dim.gap))
    }

    done?.let { num ->
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Check, null) },
            title = { Text("Number added") },
            text = { Text("$num is now in your account. This is a demo: no real number was provisioned and no payment was taken.") },
            confirmButton = { TextButton({ done = null; nav.tab(Screen.Numbers) }) { Text("OK") } },
        )
    }
}

@Composable
fun NumberDetailScreen(nav: Nav, id: Int) {
    val n = Store.numbers.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    ScreenFrame("Number details", onBack = { nav.back() }) {
        if (n == null) {
            EmptyState("Number not found", "This number may have been released.", "Back to numbers") { nav.tab(Screen.Numbers) }
            return@ScreenFrame
        }
        val thread = Store.threads.firstOrNull { it.numberId == n.id }
        Spacer(Modifier.height(Dim.gap))
        AppCard {
            Text(n.country.flag, style = MaterialTheme.typography.displaySmall)
            Text(n.e164, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Label: ${n.label}")
            Text("Country: ${n.country.name} (${n.country.dial})")
            Text("Monthly cost: ${n.country.pricePerMonth} credits")
            Spacer(Modifier.height(8.dp))
            DemoBadge()
        }
        Spacer(Modifier.height(Dim.gap))
        Row(horizontalArrangement = Arrangement.spacedBy(Dim.gap)) {
            Button(
                { if (thread != null) nav.push(Screen.Thread(thread.id)) else nav.push(Screen.Messages) },
                Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp),
            ) { Text("Message") }
            OutlinedButton(
                { nav.push(Screen.Dialer) },
                Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp),
            ) { Text("Call") }
        }
        Spacer(Modifier.height(Dim.gap))
        TextButton({ confirm = true }, Modifier.fillMaxWidth()) {
            Text("Release number", color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirm && n != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Release ${n.e164}?") },
            text = { Text("Its messages will be removed. Credits are not refunded.") },
            confirmButton = {
                TextButton({ confirm = false; Store.release(n); nav.tab(Screen.Numbers) }) { Text("Release") }
            },
            dismissButton = { TextButton({ confirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun ThreadScreen(nav: Nav, id: Int) {
    val t = Store.threads.firstOrNull { it.id == id }
    LaunchedEffect(id, t?.unread) { Store.markRead(id) }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(t?.messages?.size) {
        val c = t?.messages?.size ?: 0
        if (c > 0) listState.animateScrollToItem(c - 1)
    }
    ScreenFrame(t?.peer ?: "Conversation", onBack = { nav.back() }, actions = { DemoBadge(); Spacer(Modifier.width(Dim.screen)) }) {
        if (t == null) {
            EmptyState("Conversation not found", "It may have been removed with its number.")
            return@ScreenFrame
        }
        Text(
            "SMS is simulated in this demo. Nothing is sent over a carrier.",
            Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(t.messages) { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromMe) Arrangement.End else Arrangement.Start) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (m.fromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (m.fromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = 280.dp),
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(m.text)
                            Text(m.time, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                draft, { draft = it }, Modifier.weight(1f),
                placeholder = { Text("Message") }, maxLines = 3,
            )
            IconButton(
                onClick = { Store.send(id, draft); draft = "" },
                enabled = draft.isNotBlank(),
            ) { Icon(Icons.Default.Send, "Send") }
        }
    }
}
