package com.cloneapp.numa

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val MissedRed = Color(0xFFD32F2F)
private val HangUpRed = Color(0xFFE53935)

private fun fmtDuration(s: Int): String = "%d:%02d".format(s / 60, s % 60)

// ---------------------------------------------------------------- Call history (tab root)

@Composable
fun CallsScreen(nav: Nav) {
    Box(Modifier.fillMaxSize()) {
        ScreenFrame("Calls", actions = { DemoBadge(); Spacer(Modifier.width(Dim.screen)) }) {
            if (Store.calls.isEmpty()) {
                EmptyState(
                    "No calls yet",
                    "Your demo call history will show up here.",
                    "Open dialer",
                ) { nav.push(Screen.Dialer) }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(Store.calls) { c -> CallRow(c) { nav.push(Screen.InCall(c.peer)) } }
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
}

@Composable
private fun CallRow(c: CallEntry, onClick: () -> Unit) {
    val (icon, tint, label) = when (c.dir) {
        CallDir.IN -> Triple(Icons.Default.ArrowBack, MaterialTheme.colorScheme.secondary, "Incoming")
        CallDir.OUT -> Triple(Icons.Default.ArrowForward, MaterialTheme.colorScheme.primary, "Outgoing")
        CallDir.MISSED -> Triple(Icons.Default.Close, MissedRed, "Missed")
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Dim.gap),
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
            Text(c.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (c.dir != CallDir.MISSED) {
                Text(fmtDuration(c.seconds), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val callerId = Store.numbers.getOrNull(callerIdx) ?: Store.numbers.firstOrNull()

    ScreenFrame("Dialer", onBack = { nav.back() }, actions = { DemoBadge(); Spacer(Modifier.width(Dim.screen)) }) {
        // Caller ID
        if (Store.numbers.isEmpty()) {
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
                Store.numbers.forEachIndexed { i, n ->
                    FilterChip(
                        selected = callerId?.id == n.id,
                        onClick = { callerIdx = i },
                        label = { Text("${n.country.flag} ${n.e164}") },
                    )
                }
            }
        }

        // Number display + backspace
        Row(Modifier.fillMaxWidth().padding(vertical = Dim.gap), verticalAlignment = Alignment.CenterVertically) {
            Text(
                number.ifEmpty { "Enter number" },
                Modifier.weight(1f),
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = if (number.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = { number = number.dropLast(1) }, enabled = number.isNotEmpty()) {
                Icon(Icons.Default.Clear, "Delete digit")
            }
        }

        // Keypad
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            KEYS.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { k -> KeyButton(k) { if (number.length < 20) number += k } }
                }
            }
        }

        PrimaryButton("Call", enabled = number.isNotEmpty()) { nav.push(Screen.InCall(number)) }
        Spacer(Modifier.height(4.dp))
        Text(
            "Demo only: no real call is placed.",
            Modifier.fillMaxWidth().padding(bottom = Dim.gap),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(68.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 26.sp, fontWeight = FontWeight.Medium) }
}

// ---------------------------------------------------------------- In-call (demo)

@Composable
fun InCallScreen(nav: Nav, peer: String) {
    var elapsed by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    var keypad by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) { delay(1000); elapsed++ }
    }

    val connected = elapsed >= 2
    val talkSeconds = if (connected) elapsed - 2 else 0

    fun hangUp() {
        if (ended) return
        ended = true
        Store.logCall(peer, talkSeconds)
        nav.back()
    }
    BackHandler { hangUp() }

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
        Text(
            if (connected) "Connected · ${fmtDuration(talkSeconds)}" else "Calling...",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        DemoBadge()
        Text(
            "Demo call – no audio",
            Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ToggleChip("Mute", muted) { muted = !muted }
            ToggleChip("Speaker", speaker) { speaker = !speaker }
            ToggleChip("Keypad", keypad) { keypad = !keypad }
        }

        if (keypad) {
            Spacer(Modifier.height(Dim.screen))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KEYS.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { k -> KeyButton(k) {} }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(HangUpRed).clickable { hangUp() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Call, "End call", Modifier.rotate(135f), tint = Color.White) }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ToggleChip(label: String, on: Boolean, onClick: () -> Unit) {
    FilterChip(selected = on, onClick = onClick, label = { Text(label) })
}
