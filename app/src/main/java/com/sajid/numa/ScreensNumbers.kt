package com.sajid.numa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------- Shared bits

@Composable
private fun UnreadDot() {
    Box(
        Modifier
            .size(10.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .semantics { contentDescription = "Unread" },
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

private fun numberStatus(n: VirtualNumber): String =
    if (store.isExpired(n)) "Expired" else daysLeft(n.expiresAt).let { "$it day${if (it == 1) "" else "s"} left" }

// ---------------------------------------------------------------- Numbers (tab root)

@Composable
fun NumbersScreen(nav: Nav) {
    val unread = store.unreadCount
    ScreenFrame("My numbers") {
        AppCard(onClick = { nav.push(Screen.Messages) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(Dim.gap))
                Column(Modifier.weight(1f)) {
                    Text("Messages", fontWeight = FontWeight.SemiBold)
                    Text(
                        "${store.threads.size} conversation${if (store.threads.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (unread > 0) Badge { Text("$unread") }
            }
        }
        Spacer(Modifier.height(Dim.gap))
        if (store.numbers.isEmpty()) {
            Box(Modifier.weight(1f)) {
                EmptyState(
                    "No numbers yet",
                    "Add a virtual number to keep a second line on your account.",
                    "Get a number",
                ) { nav.push(Screen.GetNumber) }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
                items(store.numbers, key = { it.id }) { n ->
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
                            Text(
                                numberStatus(n),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (store.isExpired(n)) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Dim.gap))
            PrimaryButton("Get a number") { nav.push(Screen.GetNumber) }
            Spacer(Modifier.height(Dim.gap))
        }
    }
}

// ---------------------------------------------------------------- Messages (conversation list)

@Composable
fun MessagesScreen(nav: Nav) {
    var deleteId by remember { mutableStateOf<Int?>(null) }
    ScreenFrame("Messages", onBack = { nav.back() }) {
        val threads = store.threads.sortedByDescending { it.last?.at ?: 0L }
        if (threads.isEmpty()) {
            EmptyState(
                "No conversations",
                "Conversations for your numbers will appear here once you add a number.",
                "Get a number",
            ) { nav.push(Screen.GetNumber) }
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
                items(threads, key = { it.id }) { t ->
                    AppCard(onClick = { nav.push(Screen.Thread(t.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (t.unread) { UnreadDot(); Spacer(Modifier.width(8.dp)) }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        t.peer,
                                        Modifier.weight(1f),
                                        fontWeight = if (t.unread) FontWeight.Bold else FontWeight.SemiBold,
                                    )
                                    t.last?.let {
                                        Text(
                                            formatRelative(it.at),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                store.number(t.numberId)?.let {
                                    Text(
                                        "To ${it.e164}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    t.last?.let { (if (it.fromMe) "You: " else "") + it.text } ?: "No messages",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { deleteId = t.id }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete conversation with ${t.peer}")
                            }
                        }
                    }
                }
            }
        }
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Delete conversation?") },
            text = { Text("All messages in this conversation will be removed from this device.") },
            confirmButton = { TextButton(onClick = { store.deleteThread(id); deleteId = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } },
        )
    }
}

// ---------------------------------------------------------------- Get number: country list

@Composable
fun GetNumberScreen(nav: Nav) {
    var query by remember { mutableStateOf("") }
    ScreenFrame("Choose a country", onBack = { nav.back() }) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search country or dial code") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )
        Spacer(Modifier.height(Dim.gap))
        val q = query.trim()
        val countries = COUNTRIES.filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) || it.dial.contains(q) }
        if (countries.isEmpty()) {
            Box(Modifier.weight(1f)) { EmptyState("No matches", "No country matches \"$q\". Try another name or dial code.") }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
                items(countries, key = { it.code }) { c ->
                    AppCard(onClick = { nav.push(Screen.PickNumber(c)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.flag, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(Dim.gap))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    c.dial,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("${creditsText(c.pricePerMonth)} / 30 days", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Pick a number + purchase

@Composable
fun PickNumberScreen(nav: Nav, country: Country) {
    var selected by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var addedNumber by remember { mutableStateOf<String?>(null) }
    val owned = store.numbers.map { it.e164 }
    val price = country.pricePerMonth
    val affordable = store.credits >= price

    ScreenFrame("${country.flag} ${country.name}", onBack = { nav.back() }) {
        Text(
            "Pick a number · ${creditsText(price)} for 30 days",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dim.gap))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(candidateNumbers(country), key = { it }) { num ->
                val isOwned = num in owned
                AppCard(onClick = if (isOwned) null else ({ selected = num; error = null })) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == num, onClick = null, enabled = !isOwned)
                        Spacer(Modifier.width(Dim.gap))
                        Text(
                            num,
                            Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOwned) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        if (isOwned) Text("Already yours", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(Dim.gap))
        Text(
            "Balance: ${creditsText(store.credits)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!affordable) {
            ErrorText("Not enough credits (need $price, have ${store.credits}).")
            OutlinedButton(onClick = { nav.push(Screen.Wallet) }) { Text("Add credits") }
        }
        error?.let { ErrorText(it) }
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = if (selected == null) "Select a number" else "Get $selected · ${creditsText(price)}",
            enabled = selected != null && affordable,
        ) {
            val num = selected ?: return@PrimaryButton
            val err = store.buyNumber(num, country)
            if (err == null) addedNumber = num else error = err
        }
        Spacer(Modifier.height(Dim.gap))
    }

    addedNumber?.let { num ->
        val finish = { addedNumber = null; nav.tab(Screen.Numbers) }
        AlertDialog(
            onDismissRequest = finish,
            title = { Text("Number added") },
            text = {
                Text("$num was added to your account on this device and is valid for $NUMBER_TERM_DAYS days. You can renew it from the number's page.")
            },
            confirmButton = { TextButton(onClick = finish) { Text("Done") } },
        )
    }
}

// ---------------------------------------------------------------- Number detail

@Composable
fun NumberDetailScreen(nav: Nav, id: Int) {
    val n = store.number(id)
    if (n == null) {
        ScreenFrame("Number", onBack = { nav.back() }) {
            EmptyState("Number not found", "This number is no longer on your account.", "Back") { nav.back() }
        }
        return
    }

    var showRename by remember { mutableStateOf(false) }
    var showRelease by remember { mutableStateOf(false) }
    var renewError by remember { mutableStateOf<String?>(null) }
    val expired = store.isExpired(n)
    val thread = store.threads.firstOrNull { it.numberId == n.id }

    ScreenFrame(n.label, onBack = { nav.back() }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.country.flag, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(Dim.gap))
                    Column(Modifier.weight(1f)) {
                        Text(n.e164, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            n.country.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { showRename = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename number")
                    }
                }
            }
            Spacer(Modifier.height(Dim.gap))
            AppCard {
                DetailRow("Status", numberStatus(n), error = expired)
                DetailRow("Added", formatDate(n.purchasedAt))
                DetailRow("${if (expired) "Expired" else "Expires"}", formatDate(n.expiresAt), error = expired)
                DetailRow("Renewal", "${creditsText(n.country.pricePerMonth)} / $NUMBER_TERM_DAYS days")
            }
            Spacer(Modifier.height(Dim.gap))
            PrimaryButton("Renew $NUMBER_TERM_DAYS days · ${creditsText(n.country.pricePerMonth)}") {
                renewError = store.renewNumber(n.id)
            }
            renewError?.let {
                Spacer(Modifier.height(4.dp))
                ErrorText(it)
                if (store.credits < n.country.pricePerMonth) {
                    TextButton(onClick = { nav.push(Screen.Wallet) }) { Text("Add credits") }
                }
            }
            Spacer(Modifier.height(Dim.gap))
            Row(horizontalArrangement = Arrangement.spacedBy(Dim.gap)) {
                OutlinedButton(
                    onClick = { nav.push(thread?.let { Screen.Thread(it.id) } ?: Screen.Messages) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Messages")
                }
                OutlinedButton(onClick = { nav.push(Screen.Dialer) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Call, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Call")
                }
            }
            Spacer(Modifier.height(Dim.gap))
            TextButton(onClick = { showRelease = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text("Release number", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(Dim.gap))
        }
    }

    if (showRename) {
        var label by remember { mutableStateOf(n.label) }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename number") },
            text = {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it; err = null },
                    singleLine = true,
                    isError = err != null,
                    supportingText = err?.let { { Text(it) } },
                    label = { Text("Name") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val e = store.renameNumber(n.id, label)
                    if (e == null) showRename = false else err = e
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }

    if (showRelease) {
        AlertDialog(
            onDismissRequest = { showRelease = false },
            title = { Text("Release ${n.e164}?") },
            text = { Text("The number and its conversations will be removed from your account. Credits are not refunded.") },
            confirmButton = {
                TextButton(onClick = {
                    showRelease = false
                    store.release(n.id)
                    nav.tab(Screen.Numbers)
                }) { Text("Release", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showRelease = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, error: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------------------------------------------------------- Conversation

@Composable
fun ThreadScreen(nav: Nav, id: Int) {
    val t = store.thread(id)
    if (t == null) {
        ScreenFrame("Conversation", onBack = { nav.back() }) {
            EmptyState("Conversation not found", "It may have been deleted.", "Back") { nav.back() }
        }
        return
    }

    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(id) { store.markRead(id) }
    LaunchedEffect(t.messages.size) {
        if (t.messages.isNotEmpty()) listState.animateScrollToItem(t.messages.lastIndex)
    }

    ScreenFrame(t.peer, onBack = { nav.back() }) {
        ServiceNotice("Messages are stored on this device. SMS delivery needs a messaging provider.")
        Spacer(Modifier.height(Dim.gap))
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(t.messages.size) { i ->
                val m = t.messages[i]
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.fromMe) Alignment.End else Alignment.Start) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (m.fromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 280.dp),
                    ) {
                        Text(
                            m.text,
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (m.fromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        formatRelative(m.at),
                        Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        error?.let { ErrorText(it) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it; error = null },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                maxLines = 4,
                isError = draft.length > MAX_MESSAGE_LENGTH,
                supportingText = if (draft.length > MAX_MESSAGE_LENGTH * 8 / 10) {
                    { Text("${draft.length}/$MAX_MESSAGE_LENGTH") }
                } else null,
                keyboardOptions = KeyboardOptions.Default,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                val e = store.send(id, draft)
                if (e == null) { draft = ""; error = null } else error = e
            }) {
                Icon(Icons.Default.Send, contentDescription = "Send message", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(Dim.gap))
    }
}
