package com.cloneapp.numa

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MissedRed = Color(0xFFD32F2F)
private val EndCallRed = Color(0xFFE53935)
private const val MAX_DIGITS = 20

// ---------------------------------------------------------------- Call history (tab root)

@Composable
fun CallsScreen(nav: Nav) {
    var confirmClear by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        ScreenFrame(
            "Calls",
            actions = {
                if (store.calls.isNotEmpty()) {
                    IconButton(onClick = { confirmClear = true }) { Icon(Icons.Default.Delete, "Clear call history") }
                }
            },
        ) {
            if (store.calls.isEmpty()) {
                EmptyState(
                    "No calls yet",
                    "Calls you place from the dialer are listed here.",
                    "Open dialer",
                ) { nav.push(Screen.Dialer) }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(store.calls) { c -> CallRow(c) { nav.push(Screen.InCall(c.peer)) } }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { nav.push(Screen.Dialer) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(Dim.screen),
            icon = { Icon(Icons.Default.Call, null) },
            text = { Text("Dial") },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear call history?") },
            text = { Text("All entries will be removed from this device.") },
            confirmButton = { TextButton(onClick = { store.clearCalls(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CallRow(c: CallEntry, onClick: () -> Unit) {
    val (icon, tint, label) = when (c.dir) {
        CallDir.IN -> Triple(Icons.Default.ArrowBack, MaterialTheme.colorScheme.secondary, "Incoming")
        CallDir.OUT -> Triple(Icons.Default.ArrowForward, MaterialTheme.colorScheme.primary, "Outgoing")
        CallDir.MISSED -> Triple(Icons.Default.Close, MissedRed, "Missed")
    }
    val detail = when {
        c.seconds > 0 -> formatDuration(c.seconds)
        c.dir == CallDir.OUT -> "Not connected"
        else -> null
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "Call ${c.peer}", onClick = onClick).padding(vertical = Dim.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = tint) }
        Spacer(Modifier.width(Dim.gap))
        Column(Modifier.weight(1f)) {
            Text(
                c.peer, fontWeight = FontWeight.SemiBold,
                color = if (c.dir == CallDir.MISSED) MissedRed else MaterialTheme.colorScheme.onSurface,
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatRelative(c.at), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    HorizontalDivider()
}

// ---------------------------------------------------------------- Dialer

private val KEYS = listOf(
    listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("*", "0", "#"),
)

@Composable
fun DialerScreen(nav: Nav) {
    var number by remember { mutableStateOf("") }
    var callerIdx by remember { mutableIntStateOf(0) }
    val numbers = store.numbers
    val callerId = numbers.getOrNull(callerIdx) ?: numbers.firstOrNull()
    val valid = isValidDialString(number)

    ScreenFrame("Dialer", onBack = { nav.back() }) {
        // Caller ID
        if (numbers.isEmpty()) {
            AppCard {
                Text("No number to call from", fontWeight = FontWeight.SemiBold)
                Text(
                    "Get a virtual number to use as your caller ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { nav.push(Screen.GetNumber) }) { Text("Get a number") }
            }
        } else {
            Text("Caller ID", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                numbers.forEachIndexed { i, n ->
                    FilterChip(
                        selected = callerId?.id == n.id,
                        onClick = { callerIdx = i },
                        label = { Text("${n.country.flag} ${n.e164}") },
                    )
                }
            }
        }

        // Number display + backspace
        Row(Modifier.fillMaxWidth().padding(top = Dim.gap), verticalAlignment = Alignment.CenterVertically) {
            Text(
                number.ifEmpty { "Enter number" },
                Modifier.weight(1f),
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = if (number.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (number.isNotEmpty()) {
                BackspaceButton(onTap = { number = number.dropLast(1) }, onHold = { number = "" })
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
        Text(
            if (number.isNotEmpty() && !valid) "Enter 3 to 15 digits, with an optional leading +." else " ",
            Modifier.fillMaxWidth().padding(bottom = Dim.gap),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        // Keypad
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            KEYS.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { k ->
                        KeyButton(
                            k,
                            onLongClick = if (k == "0") ({ if (number.length < MAX_DIGITS) number += "+" }) else null,
                        ) { if (number.length < MAX_DIGITS) number += k }
                    }
                }
            }
        }

        Spacer(Modifier.height(Dim.gap))
        PrimaryButton("Call", enabled = valid, modifier = Modifier.padding(bottom = Dim.gap)) {
            if (isValidDialString(number)) nav.push(Screen.InCall(number.trim()))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackspaceButton(onTap: () -> Unit, onHold: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).combinedClickable(
            role = Role.Button,
            onClickLabel = "Delete last digit",
            onLongClickLabel = "Clear number",
            onLongClick = onHold,
            onClick = onTap,
        ),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Default.Clear, "Delete digit") }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyButton(label: String, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Box(
        Modifier.size(68.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).combinedClickable(
            role = Role.Button,
            onLongClick = onLongClick,
            onLongClickLabel = if (onLongClick != null) "Enter plus sign" else null,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 26.sp, fontWeight = FontWeight.Medium) }
}

// ---------------------------------------------------------------- Call attempt

@Composable
fun InCallScreen(nav: Nav, peer: String) {
    // Record exactly one attempt per visit, even across recomposition or rotation.
    var logged by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!logged) { logged = true; store.logCall(peer, 0) }
    }

    BackHandler { nav.back() }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(Dim.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            Modifier.size(112.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Person, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(Dim.screen))
        Text(peer, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("Call not connected", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dim.screen))
        ServiceNotice("Voice calling needs a calling provider, which is not connected in this build.")

        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(EndCallRed)
                .clickable(role = Role.Button, onClickLabel = "End call") { nav.back() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Call, "End call", Modifier.rotate(135f), tint = Color.White) }
        Spacer(Modifier.height(8.dp))
        Text("End", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
    }
}
