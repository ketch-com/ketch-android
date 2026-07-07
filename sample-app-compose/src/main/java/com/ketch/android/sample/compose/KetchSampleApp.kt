package com.ketch.android.sample.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ketch.android.Ketch
import com.ketch.android.sample.compose.ui.theme.DarkDivider
import com.ketch.android.sample.compose.ui.theme.DarkLogBackground
import com.ketch.android.sample.compose.ui.theme.DarkLogText
import com.ketch.android.sample.compose.ui.theme.DarkToggleTrack
import com.ketch.android.sample.compose.ui.theme.KetchPurple
import com.ketch.android.sample.compose.ui.theme.KetchTheme
import com.ketch.android.sample.compose.ui.theme.LightDivider
import com.ketch.android.sample.compose.ui.theme.LightLogBackground
import com.ketch.android.sample.compose.ui.theme.LightLogText
import com.ketch.android.sample.compose.ui.theme.LightToggleTrack

@Composable
fun KetchSampleApp(
    orgCode: String,
    property: String,
    environment: String,
    language: String,
    jurisdiction: String,
    region: String,
    logEntries: List<String>,
    onReload: () -> Unit,
    onShowConsent: () -> Unit,
    onShowPreferences: (allowedTabs: List<Ketch.PreferencesTab>, initialTab: Ketch.PreferencesTab) -> Unit,
    onApplyCss: () -> Unit,
    onOpenSecondActivity: () -> Unit,
    onLogSharedPreferences: () -> Unit,
) {
    var isDarkMode by rememberSaveable { mutableStateOf(false) }

    // Preference Options state (mirrors iOS/Flutter/RN samples' allowed-tabs + initial-tab pickers).
    var showOverview by remember { mutableStateOf(true) }
    var showConsents by remember { mutableStateOf(true) }
    var showRights by remember { mutableStateOf(true) }
    var showSubscriptions by remember { mutableStateOf(true) }
    var initialTab by remember { mutableStateOf(Ketch.PreferencesTab.OVERVIEW) }

    fun allowedTabs(): List<Ketch.PreferencesTab> = buildList {
        if (showOverview) add(Ketch.PreferencesTab.OVERVIEW)
        if (showConsents) add(Ketch.PreferencesTab.CONSENTS)
        if (showRights) add(Ketch.PreferencesTab.RIGHTS)
        if (showSubscriptions) add(Ketch.PreferencesTab.SUBSCRIPTIONS)
    }

    KetchTheme(darkTheme = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            HeaderBar(
                isDarkMode = isDarkMode,
                onToggleDarkMode = { isDarkMode = it }
            )

            HorizontalDivider(
                color = if (isDarkMode) DarkDivider else LightDivider,
                thickness = 1.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                SectionHeader("Info")
                Spacer(Modifier.height(12.dp))
                InfoPanel(
                    orgCode = orgCode,
                    property = property,
                    environment = environment,
                    language = language,
                    jurisdiction = jurisdiction,
                    region = region,
                )
                Spacer(Modifier.height(24.dp))
                SectionHeader("Preference Options")
                Spacer(Modifier.height(12.dp))
                PreferenceOptions(
                    showOverview = showOverview,
                    onShowOverviewChange = { showOverview = it },
                    showConsents = showConsents,
                    onShowConsentsChange = { showConsents = it },
                    showRights = showRights,
                    onShowRightsChange = { showRights = it },
                    showSubscriptions = showSubscriptions,
                    onShowSubscriptionsChange = { showSubscriptions = it },
                    initialTab = initialTab,
                    onInitialTabChange = { initialTab = it },
                )
                Spacer(Modifier.height(24.dp))
                SectionHeader("Actions")
                Spacer(Modifier.height(16.dp))
                CardsRow(
                    onShowConsent = onShowConsent,
                    onShowPreferences = { onShowPreferences(allowedTabs(), initialTab) }
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ActionCard(
                        title = "Reload",
                        description = "Reload the SDK boot config and refresh privacy state.",
                        onExecute = onReload,
                        modifier = Modifier.weight(1f),
                    )
                    ActionCard(
                        title = "Apply CSS",
                        description = "Apply a sample CSS style override to the experience.",
                        onExecute = onApplyCss,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(24.dp))
                SectionHeader("Privacy Strings")
                Spacer(Modifier.height(12.dp))
                ActionCard(
                    title = "Shared Preferences",
                    description = "Log IAB privacy strings persisted by the SDK (TCF, US Privacy, GPP).",
                    onExecute = onLogSharedPreferences,
                    modifier = Modifier.fillMaxWidth(),
                    executeLabel = "Log Values",
                )
                Spacer(Modifier.height(24.dp))
                SectionHeader("Cross-Activity Demo")
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onOpenSecondActivity,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KetchPurple),
                ) {
                    Text("Open Second Activity", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(24.dp))
                SectionHeader("Event Log")
                Spacer(Modifier.height(12.dp))
                EventLog(
                    entries = logEntries,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@Composable
private fun InfoPanel(
    orgCode: String,
    property: String,
    environment: String,
    language: String,
    jurisdiction: String,
    region: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        InfoRow("Org Code", orgCode)
        InfoRow("Property", property)
        InfoRow("Environment", environment)
        InfoRow("Language", language)
        InfoRow("Jurisdiction", jurisdiction)
        InfoRow("Region", region)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PreferenceOptions(
    showOverview: Boolean,
    onShowOverviewChange: (Boolean) -> Unit,
    showConsents: Boolean,
    onShowConsentsChange: (Boolean) -> Unit,
    showRights: Boolean,
    onShowRightsChange: (Boolean) -> Unit,
    showSubscriptions: Boolean,
    onShowSubscriptionsChange: (Boolean) -> Unit,
    initialTab: Ketch.PreferencesTab,
    onInitialTabChange: (Ketch.PreferencesTab) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Allowed tabs",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TabCheckbox("Overview", showOverview, onShowOverviewChange)
        TabCheckbox("Consent", showConsents, onShowConsentsChange)
        TabCheckbox("Rights", showRights, onShowRightsChange)
        TabCheckbox("Subscriptions", showSubscriptions, onShowSubscriptionsChange)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Initial tab",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TabRadio("Overview", initialTab == Ketch.PreferencesTab.OVERVIEW) { onInitialTabChange(Ketch.PreferencesTab.OVERVIEW) }
        TabRadio("Consent", initialTab == Ketch.PreferencesTab.CONSENTS) { onInitialTabChange(Ketch.PreferencesTab.CONSENTS) }
        TabRadio("Rights", initialTab == Ketch.PreferencesTab.RIGHTS) { onInitialTabChange(Ketch.PreferencesTab.RIGHTS) }
        TabRadio("Subscriptions", initialTab == Ketch.PreferencesTab.SUBSCRIPTIONS) { onInitialTabChange(Ketch.PreferencesTab.SUBSCRIPTIONS) }
    }
}

@Composable
private fun TabCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TabRadio(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun HeaderBar(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Ketch Android - Jetpack Compose",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text("☀️", fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Switch(
            checked = isDarkMode,
            onCheckedChange = onToggleDarkMode,
            colors = SwitchDefaults.colors(
                checkedTrackColor = if (isDarkMode) DarkToggleTrack else LightToggleTrack,
                uncheckedTrackColor = if (isDarkMode) DarkToggleTrack else LightToggleTrack,
                checkedThumbColor = KetchPurple,
                uncheckedThumbColor = KetchPurple,
            )
        )
        Spacer(Modifier.width(4.dp))
        Text("🌙", fontSize = 16.sp)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = "▾  $title",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = KetchPurple,
    )
}

@Composable
private fun CardsRow(
    onShowConsent: () -> Unit,
    onShowPreferences: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ActionCard(
            title = "Privacy Preference Unknown",
            description = "Trigger the consent banner. This triggers automatically for new users.",
            onExecute = onShowConsent,
            modifier = Modifier.weight(1f),
        )
        ActionCard(
            title = "Preferences Opened",
            description = "Open the Ketch Privacy Center to manage consent preferences.",
            onExecute = onShowPreferences,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    onExecute: () -> Unit,
    modifier: Modifier = Modifier,
    executeLabel: String = "Execute",
) {
    Column(
        modifier = modifier
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onExecute,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KetchPurple,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(executeLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EventLog(
    entries: List<String>,
    isDarkMode: Boolean,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkMode) DarkLogBackground else LightLogBackground)
            .padding(12.dp)
    ) {
        if (entries.isEmpty()) {
            Text(
                text = "Waiting for events...",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isDarkMode) DarkLogText else LightLogText,
            )
        } else {
            LazyColumn(state = listState) {
                items(entries) { entry ->
                    Text(
                        text = entry,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDarkMode) DarkLogText else LightLogText,
                    )
                }
            }
        }
    }
}
