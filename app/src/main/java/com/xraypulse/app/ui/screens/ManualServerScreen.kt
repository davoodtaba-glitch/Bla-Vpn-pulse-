package com.xraypulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.data.model.ProtocolType
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.data.model.StreamSecurity
import com.xraypulse.app.data.model.TransportNetwork
import com.xraypulse.app.ui.theme.PulseBg
import com.xraypulse.app.ui.theme.PulseBorder
import com.xraypulse.app.ui.theme.PulseCyan
import com.xraypulse.app.ui.theme.PulseMuted
import com.xraypulse.app.ui.theme.PulseSurface2
import com.xraypulse.app.ui.theme.PulseText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualServerScreen(
    initial: ServerProfile? = null,
    onSave: (ServerProfile) -> Unit,
    onBack: () -> Unit
) {
    var remark by remember { mutableStateOf(initial?.remark ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var port by remember { mutableStateOf(initial?.port?.toString() ?: "443") }
    var uuid by remember { mutableStateOf(initial?.uuid ?: "") }
    var flow by remember { mutableStateOf(initial?.flow ?: "xtls-rprx-vision") }
    var sni by remember { mutableStateOf(initial?.sni ?: "") }
    var fingerprint by remember { mutableStateOf(initial?.fingerprint ?: "chrome") }
    var publicKey by remember { mutableStateOf(initial?.publicKey ?: "") }
    var shortId by remember { mutableStateOf(initial?.shortId ?: "") }
    var spiderX by remember { mutableStateOf(initial?.spiderX ?: "/") }
    var path by remember { mutableStateOf(initial?.path ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var network by remember { mutableStateOf(initial?.network ?: TransportNetwork.TCP) }
    var security by remember { mutableStateOf(initial?.security ?: StreamSecurity.REALITY) }
    var protocol by remember { mutableStateOf(initial?.protocol ?: ProtocolType.VLESS) }

    Column(
        Modifier
            .fillMaxSize()
            .background(PulseBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Manual config", color = PulseText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Optimized for VLESS + REALITY + Vision", color = PulseMuted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        Field("Remark", remark) { remark = it }
        EnumDropdown("Protocol", protocol, ProtocolType.entries.toList()) { protocol = it }
        Field("Address", address) { address = it }
        Field("Port", port) { port = it.filter { c -> c.isDigit() } }
        Field("UUID / Password", uuid) { uuid = it }
        EnumDropdown("Network", network, TransportNetwork.entries.toList()) { network = it }
        EnumDropdown("Security", security, StreamSecurity.entries.toList()) { security = it }
        Field("Flow (Vision)", flow) { flow = it }
        Field("SNI", sni) { sni = it }
        Field("Fingerprint (uTLS)", fingerprint) { fingerprint = it }
        if (security == StreamSecurity.REALITY) {
            Field("Public Key (pbk)", publicKey) { publicKey = it }
            Field("Short ID (sid)", shortId) { shortId = it }
            Field("SpiderX (spx)", spiderX) { spiderX = it }
        }
        Field("Path", path) { path = it }
        Field("Host", host) { host = it }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onSave(
                    ServerProfile(
                        id = initial?.id ?: 0,
                        remark = remark.ifBlank { "$address:$port" },
                        protocol = protocol,
                        address = address.trim(),
                        port = port.toIntOrNull() ?: 443,
                        uuid = uuid.trim(),
                        password = uuid.trim(),
                        flow = flow.trim(),
                        network = network,
                        security = security,
                        sni = sni.trim(),
                        fingerprint = fingerprint.trim(),
                        publicKey = publicKey.trim(),
                        shortId = shortId.trim(),
                        spiderX = spiderX.trim(),
                        path = path.trim(),
                        host = host.trim(),
                        encryption = if (protocol == ProtocolType.VLESS) "none" else "auto"
                    )
                )
            },
            enabled = address.isNotBlank() && uuid.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PulseCyan, contentColor = Color.Black)
        ) {
            Text("Save server", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PulseCyan,
            unfocusedBorderColor = PulseBorder,
            focusedTextColor = PulseText,
            unfocusedTextColor = PulseText,
            cursorColor = PulseCyan,
            focusedContainerColor = PulseSurface2,
            unfocusedContainerColor = PulseSurface2,
            focusedLabelColor = PulseMuted,
            unfocusedLabelColor = PulseMuted
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PulseCyan,
                unfocusedBorderColor = PulseBorder,
                focusedTextColor = PulseText,
                unfocusedTextColor = PulseText,
                focusedContainerColor = PulseSurface2,
                unfocusedContainerColor = PulseSurface2,
                focusedLabelColor = PulseMuted,
                unfocusedLabelColor = PulseMuted
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.name) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
