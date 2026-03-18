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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    logEntries: List<String>,
    onShowConsent: () -> Unit,
    onShowPreferences: () -> Unit,
) {
    var isDarkMode by rememberSaveable { mutableStateOf(false) }

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
                SectionHeader("Experience Functions")
                Spacer(Modifier.height(16.dp))
                CardsRow(
                    onShowConsent = onShowConsent,
                    onShowPreferences = onShowPreferences
                )
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
            Text("Execute", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
