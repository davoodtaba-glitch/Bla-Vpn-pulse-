package com.xraypulse.app.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.ui.components.GlassCard
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.PulseBg
import com.xraypulse.app.ui.theme.PulseBorder
import com.xraypulse.app.ui.theme.PulseMuted
import com.xraypulse.app.ui.theme.PulseSurface2
import com.xraypulse.app.ui.theme.PulseText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(val packageName: String, val label: String, val isSystem: Boolean)

@Composable
fun PerAppProxyScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit
) {
    val context = LocalContext.current
    val accent = LocalAccent.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .map {
                    AppEntry(
                        packageName = it.packageName,
                        label = pm.getApplicationLabel(it).toString(),
                        isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
    }

    val filtered = apps.filter { entry ->
        (showSystem || !entry.isSystem) &&
            (query.isBlank() ||
                entry.label.contains(query, true) ||
                entry.packageName.contains(query, true))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PulseBg)
            .padding(16.dp)
    ) {
        Text("Per-app proxy", color = PulseText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Choose which apps use the VPN", color = PulseMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable per-app proxy", color = PulseText, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.perAppProxy,
                        onCheckedChange = { onUpdate { s -> s.copy(perAppProxy = it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent, checkedTrackColor = accent.copy(0.35f))
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.perAppMode == "bypass",
                        onClick = { onUpdate { it.copy(perAppMode = "bypass") } },
                        enabled = settings.perAppProxy,
                        label = { Text("Bypass selected") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(0.25f),
                            selectedLabelColor = accent
                        )
                    )
                    FilterChip(
                        selected = settings.perAppMode == "proxy",
                        onClick = { onUpdate { it.copy(perAppMode = "proxy") } },
                        enabled = settings.perAppProxy,
                        label = { Text("Only selected") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(0.25f),
                            selectedLabelColor = accent
                        )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (settings.perAppMode == "proxy")
                        "Only checked apps go through VPN"
                    else
                        "Checked apps bypass VPN (direct)",
                    color = PulseMuted,
                    fontSize = 12.sp
                )
                Text(
                    "${settings.perAppPackages.size} apps selected",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search apps…", color = PulseMuted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = PulseBorder,
                focusedTextColor = PulseText,
                unfocusedTextColor = PulseText,
                focusedContainerColor = PulseSurface2,
                unfocusedContainerColor = PulseSurface2
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Text("Show system apps", color = PulseMuted, modifier = Modifier.weight(1f), fontSize = 13.sp)
            Switch(
                checked = showSystem,
                onCheckedChange = { showSystem = it },
                colors = SwitchDefaults.colors(checkedThumbColor = accent, checkedTrackColor = accent.copy(0.35f))
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.packageName }) { app ->
                val checked = settings.perAppPackages.contains(app.packageName)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(app.label, color = PulseText, fontSize = 14.sp)
                        Text(app.packageName, color = PulseMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = checked,
                        enabled = settings.perAppProxy,
                        onCheckedChange = { on ->
                            onUpdate { s ->
                                val set = s.perAppPackages.toMutableSet()
                                if (on) set.add(app.packageName) else set.remove(app.packageName)
                                s.copy(perAppPackages = set)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent, checkedTrackColor = accent.copy(0.35f))
                    )
                }
            }
        }
    }
}
