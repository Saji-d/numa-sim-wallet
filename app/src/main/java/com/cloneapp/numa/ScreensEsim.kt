package com.cloneapp.numa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun usd(v: Double) = "$" + String.format("%.2f", v)

private fun fromPrice(c: Country): Double =
    ESIM_PLANS.filter { it.country.code == c.code }.minOfOrNull { it.priceUsd } ?: 0.0

/** Travel eSIM store (tab root). */
@Composable
fun EsimScreen(nav: Nav) {
    var query by remember { mutableStateOf("") }
    val shown = COUNTRIES.filter { it.name.contains(query.trim(), ignoreCase = true) }
    ScreenFrame("Travel eSIM", actions = { DemoBadge(); Spacer(Modifier.width(Dim.screen)) }) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search destination") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true, shape = RoundedCornerShape(14.dp),
        )
        Spacer(Modifier.height(Dim.gap))
        AppCard(onClick = { nav.push(Screen.MyEsims) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("My eSIMs", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (Store.esims.isEmpty()) "No eSIMs yet" else "${Store.esims.size} purchased (demo)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("View", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        SectionTitle("Popular destinations")
        if (shown.isEmpty()) {
            EmptyState("No matches", "No destination matches \"$query\".")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
                items(shown, key = { it.code }) { c ->
                    AppCard(onClick = { nav.push(Screen.EsimCountry(c)) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(c.flag, fontSize = 28.sp)
                            Spacer(Modifier.width(Dim.gap))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Data plans from ${usd(fromPrice(c))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Plans for one destination. */
@Composable
fun EsimCountryScreen(nav: Nav, country: Country) {
    val plans = ESIM_PLANS.filter { it.country.code == country.code }
    ScreenFrame("${country.flag} ${country.name}", onBack = { nav.back() }) {
        Spacer(Modifier.height(Dim.gap))
        if (plans.isEmpty()) {
            EmptyState("No plans", "There are no plans for ${country.name} yet.", "Back") { nav.back() }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
                items(plans, key = { it.id }) { p ->
                    AppCard(onClick = { nav.push(Screen.EsimPlanDetail(p.id)) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${p.dataGb} GB", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "${p.days} days validity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(usd(p.priceUsd), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

/** Plan details + simulated purchase. */
@Composable
fun EsimPlanDetailScreen(nav: Nav, planId: String) {
    val plan = ESIM_PLANS.firstOrNull { it.id == planId }
    var error by remember(planId) { mutableStateOf<String?>(null) }
    var bought by remember(planId) { mutableStateOf(false) }

    ScreenFrame("Plan details", onBack = { nav.back() }) {
        if (plan == null) {
            EmptyState("Plan not found", "This plan is no longer available.", "Back") { nav.back() }
            return@ScreenFrame
        }
        LazyColumn(Modifier.weight(1f)) {
            item {
                Spacer(Modifier.height(Dim.gap))
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.country.flag, fontSize = 32.sp)
                        Spacer(Modifier.width(Dim.gap))
                        Column(Modifier.weight(1f)) {
                            Text(plan.country.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${plan.dataGb} GB / ${plan.days} days")
                        }
                        DemoBadge()
                    }
                    Spacer(Modifier.height(Dim.gap))
                    DetailRow("Data", "${plan.dataGb} GB")
                    DetailRow("Validity", "${plan.days} days")
                    DetailRow("Coverage", plan.country.name)
                    DetailRow("Price", "${usd(plan.priceUsd)} (${Math.round(plan.priceUsd)} credits)")
                    DetailRow("Your balance", "${Store.credits} credits")
                }
                Spacer(Modifier.height(Dim.gap))
                Text(
                    "DEMO: sample plan data. No real eSIM profile is issued and no payment is taken. Network and speeds are illustrative only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dim.gap))

                if (!bought) {
                    PrimaryButton("Buy with credits (demo)") {
                        error = Store.buyEsim(plan)
                        bought = error == null
                    }
                    error?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(msg, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { nav.push(Screen.Wallet) }, Modifier.fillMaxWidth()) { Text("Top up credits") }
                    }
                } else {
                    AppCard {
                        Text("Purchase complete (demo)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            Modifier.fillMaxWidth().height(160.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Demo QR - not a real eSIM profile", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        SectionTitle("Install steps")
                        Text("1. Open Settings > Mobile network > Add eSIM.")
                        Text("2. Scan the QR code from your provider.")
                        Text("3. Turn on data roaming for this line.")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "These steps are illustrative; this demo cannot activate an eSIM.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(Dim.gap))
                    PrimaryButton("Go to My eSIMs") { nav.push(Screen.MyEsims) }
                }
                Spacer(Modifier.height(Dim.screen))
            }
        }
    }
}

/** Purchased eSIMs (demo). */
@Composable
fun MyEsimsScreen(nav: Nav) {
    ScreenFrame("My eSIMs", onBack = { nav.back() }) {
        if (Store.esims.isEmpty()) {
            EmptyState("No eSIMs yet", "Browse destinations and pick a data plan.", "Browse plans") { nav.push(Screen.Esim) }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
                item { Spacer(Modifier.height(4.dp)) }
                items(Store.esims, key = { it.id }) { e ->
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(e.plan.country.flag, fontSize = 28.sp)
                            Spacer(Modifier.width(Dim.gap))
                            Column(Modifier.weight(1f)) {
                                Text("${e.plan.country.name} ${e.plan.dataGb} GB", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${e.plan.days} days - ${e.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DemoBadge()
                        }
                        Spacer(Modifier.height(Dim.gap))
                        val frac = if (e.plan.dataGb > 0) (e.usedGb / e.plan.dataGb).toFloat().coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(progress = { frac }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${String.format("%.1f", e.usedGb)} / ${e.plan.dataGb} GB used",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

/** Credits wallet: balance, demo top-up, transactions. */
@Composable
fun WalletScreen(nav: Nav) {
    var confirmation by remember { mutableStateOf<String?>(null) }
    ScreenFrame("Wallet", onBack = { nav.back() }, actions = { DemoBadge(); Spacer(Modifier.width(Dim.screen)) }) {
        LazyColumn(Modifier.weight(1f)) {
            item {
                Spacer(Modifier.height(Dim.gap))
                AppCard {
                    Text("Balance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${Store.credits} credits", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                SectionTitle("Top up")
                Text(
                    "DEMO: no payment is taken. Credits are added instantly for testing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CREDIT_PACKS.forEach { (credits, price) ->
                        OutlinedButton(
                            onClick = { Store.addCredits(credits); confirmation = "Added $credits demo credits (no payment taken)." },
                            Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$credits", fontWeight = FontWeight.Bold)
                                Text(price, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                confirmation?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                }
                SectionTitle("Transactions")
                if (Store.txns.isEmpty()) {
                    EmptyState("No transactions", "Top-ups and purchases will appear here.")
                }
            }
            items(Store.txns.toList()) { t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.title)
                        Text(t.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        (if (t.amount > 0) "+" else "") + t.amount,
                        fontWeight = FontWeight.Bold,
                        color = if (t.amount >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
