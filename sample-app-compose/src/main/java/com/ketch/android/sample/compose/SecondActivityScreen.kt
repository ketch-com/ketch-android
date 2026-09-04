package com.ketch.android.sample.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ketch.android.sample.compose.ui.theme.KetchPurple
import com.ketch.android.sample.compose.ui.theme.LightLogBackground
import com.ketch.android.sample.compose.ui.theme.LightLogText

@Composable
fun SecondActivityScreen(
    logEntries: List<String>,
    onShowConsent: () -> Unit,
    onShowPreferences: () -> Unit,
    onTriggerFunction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Second Activity",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Uses the same Ketch instance created in Application. Init and load() happen in MainActivity.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onShowConsent,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KetchPurple),
        ) {
            Text("Show Consent")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onShowPreferences,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KetchPurple),
        ) {
            Text("Show Preferences")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onTriggerFunction,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KetchPurple),
        ) {
            Text("Trigger Custom Function")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Event Log",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = KetchPurple,
        )
        Spacer(Modifier.height(12.dp))
        EventLogPanel(entries = logEntries)
    }
}

@Composable
private fun EventLogPanel(entries: List<String>) {
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
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(LightLogBackground)
            .padding(12.dp)
    ) {
        if (entries.isEmpty()) {
            Text(
                text = "Waiting for events...",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = LightLogText,
            )
        } else {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(entries) { entry ->
                    Text(
                        text = entry,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LightLogText,
                    )
                }
            }
        }
    }
}
