package com.cloneapp.numa

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val Brand = Color(0xFF5B3DF5)
val BrandSoft = Color(0xFFE9E4FF)

private val Light = lightColorScheme(primary = Brand, secondary = Color(0xFF00B88A), primaryContainer = BrandSoft)
private val Dark = darkColorScheme(primary = Color(0xFFB4A6FF), secondary = Color(0xFF4ADBB4))

@Composable
fun NumaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

// ---- Shared components (use these in every screen) ----
object Dim { val screen = 16.dp; val gap = 12.dp }

/** Standard screen scaffold with title and optional back arrow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenFrame(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.SemiBold) },
            navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            actions = actions,
        )
        Column(Modifier.fillMaxSize().padding(horizontal = Dim.screen), content = content)
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val inner: @Composable ColumnScope.() -> Unit = { Column(Modifier.padding(Dim.screen), content = content) }
    if (onClick != null) Card(onClick, modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), content = inner)
    else Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), content = inner)
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier.fillMaxWidth().height(52.dp), enabled = enabled, shape = RoundedCornerShape(14.dp)) { Text(text) }
}

@Composable
fun EmptyState(title: String, body: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null) { Spacer(Modifier.height(16.dp)); Button(onAction) { Text(actionLabel) } }
    }
}

/** Small "Demo" chip: put it on every screen whose result is simulated. */
@Composable
fun DemoBadge() {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text("DEMO", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, Modifier.padding(top = 16.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}
