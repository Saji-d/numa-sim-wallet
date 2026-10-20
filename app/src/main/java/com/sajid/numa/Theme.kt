package com.sajid.numa

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
fun NumaTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
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
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
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

/**
 * Quiet one-line note for a real integration boundary (for example "SMS delivery needs a messaging
 * provider"). Use it only where an action's effect stays on this device.
 */
@Composable
fun ServiceNotice(text: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, Modifier.padding(top = 16.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}
