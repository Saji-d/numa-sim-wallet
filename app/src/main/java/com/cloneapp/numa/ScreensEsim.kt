package com.cloneapp.numa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun fromPrice(c: Country): Double =
    ESIM_PLANS.filter { it.country.code == c.code }.minOfOrNull { it.priceUsd } ?: 0.0

private fun esimStatus(e: MyEsim, now: Long = System.currentTimeMillis()): String {
    val exp = e.expiresAt ?: return "Not installed"
    return if (exp <= now) "Expired" else "Installed - ${daysLeft(exp, now)} days left"
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

/** Travel eSIM store (tab root). */
@Composable
fun EsimScreen(nav: Nav) {
    var query by remember { mutableStateOf("") }
    val shown = COUNTRIES.filter { it.name.contains(query.trim(), ignoreCase = true) }
    ScreenFrame("Travel eSIM") {
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
                        if (store.esims.isEmpty()) "No eSIMs yet" else "${store.esims.size} purchased",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("View", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        SectionTitle("Destinations")
        if (shown.isEmpty()) {
            EmptyState("No matches", "No destination matches \"${query.trim()}\".")
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
                                    "Data plans from ${formatUsd(fromPrice(c))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("View plans", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                item { Spacer(Modifier.height(Dim.gap)) }
            }
        }
    }
}

/** Plans for one destination. */
@Composable
fun EsimCountryScreen(nav: Nav, country: Country) {
    val plans = ESIM_PLANS.filter { it.country.code == country.code }
    ScreenFrame("${country.flag} ${country.name}", onBack = { nav.back() }) {
        if (plans.isEmpty()) {
            EmptyState("No plans yet", "There are no plans for ${country.name} right now.", "Back") { nav.back() }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
                item { SectionTitle("Choose a data plan") }
                items(plans, key = { it.id }) { p ->
                    AppCard(onClick = { nav.push(Screen.EsimPlanDetail(p.id)) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${p.dataGb} GB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${p.days} days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatUsd(p.priceUsd), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "${p.credits} credits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(Dim.gap)) }
            }
        }
    }
}

/** Plan details and purchase. */
@Composable
fun EsimPlanDetailScreen(nav: Nav, planId: String) {
    val plan = planById(planId)
    if (plan == null) {
        ScreenFrame("Plan", onBack = { nav.back() }) {
            EmptyState("Plan not found", "This plan is no longer available.", "Back") { nav.back() }
        }
        return
    }
    var error by remember { mutableStateOf<String?>(null) }
    var purchased by remember { mutableStateOf(false) }
    val affordable = store.credits >= plan.credits

    ScreenFrame("${plan.country.flag} ${plan.country.name}", onBack = { nav.back() }) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
            AppCard {
                Text("${plan.dataGb} GB", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Data only, valid ${plan.days} days after you install it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dim.gap))
                InfoRow("Destination", plan.country.name)
                InfoRow("Validity", "${plan.days} days")
                InfoRow("Price", "${formatUsd(plan.priceUsd)} / ${plan.credits} credits")
                InfoRow("Your balance", "${store.credits} credits")
                if (!purchased) {
                    val after = store.credits - plan.credits
                    InfoRow("Balance after purchase", if (affordable) "$after credits" else "Not enough credits")
                }
            }

            if (!purchased) {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                if (!affordable) {
                    OutlinedButton(
                        onClick = { nav.push(Screen.Wallet) },
                        Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Add credits") }
                }
                PrimaryButton("Buy for ${plan.credits} credits", enabled = affordable) {
                    val err = store.buyEsim(plan)
                    error = err
                    if (err == null) purchased = true
                }
            } else {
                AppCard {
                    Text("Purchase complete", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(Dim.gap))
                    Text("To install your eSIM", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    listOf(
                        "1. Open your phone's Settings and go to Mobile network (or SIM manager).",
                        "2. Choose Add eSIM.",
                        "3. Enter the activation details you receive from the eSIM provider.",
                        "4. Come back here and mark the eSIM as installed to start its validity period.",
                    ).forEach {
                        Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
                ServiceNotice("Activation details come from an eSIM provider, which is not connected in this build, so this purchase is recorded on this device only.")
                PrimaryButton("Go to My eSIMs") { nav.push(Screen.MyEsims) }
            }
            Spacer(Modifier.height(Dim.gap))
        }
    }
}

/** Purchased eSIMs. */
@Composable
fun MyEsimsScreen(nav: Nav) {
    var confirmInstall by remember { mutableStateOf<MyEsim?>(null) }
    var confirmRemove by remember { mutableStateOf<MyEsim?>(null) }

    ScreenFrame("My eSIMs", onBack = { nav.back() }) {
        if (store.esims.isEmpty()) {
            EmptyState("No eSIMs yet", "Plans you buy will be listed here.", "Browse plans") { nav.tab(Screen.Esim) }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
                item { Spacer(Modifier.height(4.dp)) }
                items(store.esims, key = { it.id }) { e ->
                    AppCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(e.plan.country.flag, fontSize = 28.sp)
                            Spacer(Modifier.width(Dim.gap))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${e.plan.country.name} - ${e.plan.dataGb} GB",
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${e.plan.days} days - bought ${formatDate(e.purchasedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            esimStatus(e),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (esimStatus(e) == "Expired") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (!e.installed) {
                                TextButton(onClick = { confirmInstall = e }) { Text("Mark as installed") }
                            }
                            TextButton(onClick = { confirmRemove = e }) { Text("Remove") }
                        }
                    }
                }
                item { Spacer(Modifier.height(Dim.gap)) }
            }
        }
    }

    confirmInstall?.let { e ->
        AlertDialog(
            onDismissRequest = { confirmInstall = null },
            title = { Text("Mark as installed?") },
            text = { Text("The ${e.plan.days}-day validity period starts now. Do this once the eSIM is installed on your phone.") },
            confirmButton = { TextButton(onClick = { store.markEsimInstalled(e.id); confirmInstall = null }) { Text("Mark installed") } },
            dismissButton = { TextButton(onClick = { confirmInstall = null }) { Text("Cancel") } },
        )
    }
    confirmRemove?.let { e ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove this eSIM?") },
            text = { Text("It will be removed from this list. Credits are not refunded.") },
            confirmButton = { TextButton(onClick = { store.removeEsim(e.id); confirmRemove = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text("Cancel") } },
        )
    }
}

/** Credits wallet: balance, top-up and transactions. */
@Composable
fun WalletScreen(nav: Nav) {
    var amountText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<String?>(null) }

    fun add(amount: Int) {
        val err = store.addCredits(amount)
        error = err
        confirmation = if (err == null) "Added $amount credits." else null
        if (err == null) amountText = ""
    }

    ScreenFrame("Wallet", onBack = { nav.back() }) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dim.gap)) {
            item {
                Spacer(Modifier.height(4.dp))
                AppCard {
                    Text("Balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${store.credits} credits", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            item {
                AppCard {
                    Text("Add credits", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CREDIT_PACKS.forEach { c ->
                            OutlinedButton(onClick = { add(c) }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("+$c") }
                        }
                    }
                    Spacer(Modifier.height(Dim.gap))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter(Char::isDigit).take(4); error = null; confirmation = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Custom amount") },
                        supportingText = { Text("1 to $MAX_TOP_UP credits") },
                        isError = error != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    confirmation?.let { Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Add credits") { add(amountText.toIntOrNull() ?: 0) }
                }
            }
            item { ServiceNotice("Credits are a balance kept on this device. No payment is processed.") }
            item { SectionTitle("Transactions") }
            if (store.txns.isEmpty()) {
                item { Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(store.txns) { t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatRelative(t.at),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            (if (t.amount > 0) "+" else "") + "${t.amount}",
                            fontWeight = FontWeight.SemiBold,
                            color = if (t.amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(Dim.gap)) }
        }
    }
}
