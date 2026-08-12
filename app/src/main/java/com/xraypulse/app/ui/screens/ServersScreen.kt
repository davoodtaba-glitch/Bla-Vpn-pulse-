package com.xraypulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.data.model.Subscription
import com.xraypulse.app.ui.components.GlassCard
import com.xraypulse.app.ui.components.NeonIcon
import com.xraypulse.app.ui.components.ServerListItem
import com.xraypulse.app.ui.i18n.t
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.LocalPalette

/** Tab filter: all servers, one subscription, or manual (no sub). */
private sealed class ServerTab {
    data object All : ServerTab()
    data object Manual : ServerTab()
    data class Sub(val id: Long, val name: String) : ServerTab()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    servers: List<ServerProfile>,
    subscriptions: List<Subscription> = emptyList(),
    selectedId: Long?,
    search: String,
    sortByDelay: Boolean,
    isTesting: Boolean,
    testingServerId: Long?,
    onSearch: (String) -> Unit,
    onSelect: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDeleteMany: (Set<Long>) -> Unit,
    onDeleteAll: () -> Unit,
    onDeleteInvalid: () -> Unit,
    onTestAll: () -> Unit,
    onTestOne: (ServerProfile) -> Unit,
    onEdit: (Long) -> Unit,
    onSortByDelay: (Boolean) -> Unit,
    onCancelTesting: () -> Unit = {},
    onRefreshSubscription: (Long) -> Unit = {},
    onDeleteSubscription: (Long) -> Unit = {},
    onRenameSubscription: (Long, String) -> Unit = { _, _ -> },
    onUpdateSubscription: (Long, String, String) -> Unit = { _, _, _ -> }
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }
    var multiSelect by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(setOf<Long>()) }
    var tab by remember { mutableStateOf<ServerTab>(ServerTab.All) }
    var editSub by remember { mutableStateOf<Subscription?>(null) }
    var editName by remember { mutableStateOf("") }
    var editUrl by remember { mutableStateOf("") }
    var confirmDeleteSub by remember { mutableStateOf<Subscription?>(null) }

    // Stop ping tests when leaving this screen
    DisposableEffect(Unit) {
        onDispose { onCancelTesting() }
    }

    val tabs = remember(subscriptions) {
        buildList {
            add(ServerTab.All)
            subscriptions.forEach { add(ServerTab.Sub(it.id, it.name.ifBlank { "Subscription" })) }
            add(ServerTab.Manual)
        }
    }

    // Drop invalid tab if subscription removed
    val activeTab = when (val t = tab) {
        is ServerTab.Sub -> if (subscriptions.any { it.id == t.id }) t else ServerTab.All
        else -> t
    }.also { if (it != tab) tab = it }

    val byTab = when (val t = activeTab) {
        is ServerTab.All -> servers
        is ServerTab.Manual -> servers.filter { it.subscriptionId == null }
        is ServerTab.Sub -> servers.filter { it.subscriptionId == t.id }
    }

    val filtered = if (search.isBlank()) byTab else byTab.filter {
        it.displayTitle().contains(search, true) ||
            it.address.contains(search, true) ||
            it.protocolBadge().contains(search, true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (multiSelect) t("select_servers") else t("subscriptions"),
                    color = p.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        multiSelect -> "${checked.size} ${t("selected_count")} · ${filtered.size}"
                        isTesting -> t("testing_connections")
                        else -> "${filtered.size} ${t("configurations")}"
                    },
                    color = when {
                        multiSelect -> accent
                        isTesting -> p.warning
                        else -> p.muted
                    },
                    fontSize = 13.sp,
                    fontWeight = if (multiSelect || isTesting) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Row {
                if (multiSelect) {
                    IconButton(onClick = {
                        checked = if (checked.size == filtered.size) emptySet()
                        else filtered.map { it.id }.toSet()
                    }) {
                        NeonIcon(Icons.Rounded.SelectAll, t("select_all"))
                    }
                    IconButton(
                        onClick = { if (checked.isNotEmpty()) confirmDeleteSelected = true },
                        enabled = checked.isNotEmpty()
                    ) {
                        NeonIcon(
                            Icons.Rounded.Delete,
                            t("delete_selected"),
                            tintOverride = if (checked.isNotEmpty()) p.error else p.muted
                        )
                    }
                    IconButton(onClick = {
                        multiSelect = false
                        checked = emptySet()
                    }) {
                        NeonIcon(Icons.Rounded.Close, t("cancel"))
                    }
                } else {
                    IconButton(onClick = { multiSelect = true }) {
                        NeonIcon(Icons.Rounded.SelectAll, t("multi_select"))
                    }
                    IconButton(onClick = onTestAll, enabled = !isTesting) {
                        NeonIcon(
                            Icons.Rounded.NetworkCheck,
                            t("test_all"),
                            tintOverride = if (isTesting) p.warning else null
                        )
                    }
                    IconButton(onClick = { confirmDeleteAll = true }) {
                        NeonIcon(Icons.Rounded.DeleteSweep, t("delete_all"), tintOverride = p.error)
                    }
                }
            }
        }

        // Subscription tabs
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { item ->
                val selected = when {
                    activeTab is ServerTab.All && item is ServerTab.All -> true
                    activeTab is ServerTab.Manual && item is ServerTab.Manual -> true
                    activeTab is ServerTab.Sub && item is ServerTab.Sub && activeTab.id == item.id -> true
                    else -> false
                }
                val label = when (item) {
                    is ServerTab.All -> t("tab_all")
                    is ServerTab.Manual -> t("tab_manual")
                    is ServerTab.Sub -> item.name
                }
                val count = when (item) {
                    is ServerTab.All -> servers.size
                    is ServerTab.Manual -> servers.count { it.subscriptionId == null }
                    is ServerTab.Sub -> servers.count { it.subscriptionId == item.id }
                }
                FilterChip(
                    selected = selected,
                    onClick = {
                        tab = item
                        checked = emptySet()
                    },
                    label = { Text("$label ($count)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent.copy(0.25f),
                        selectedLabelColor = accent,
                        containerColor = p.surface2,
                        labelColor = p.text
                    )
                )
            }
        }

        // Management actions for selected subscription
        val selectedSub = (activeTab as? ServerTab.Sub)?.let { st ->
            subscriptions.find { it.id == st.id }
        }
        if (selectedSub != null) {
            Spacer(Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        selectedSub.name.ifBlank { t("subscription") },
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { onRefreshSubscription(selectedSub.id) }) {
                            NeonIcon(Icons.Rounded.Refresh, t("update_subscription"), size = 22.dp)
                        }
                        IconButton(onClick = {
                            editSub = selectedSub
                            editName = selectedSub.name
                            editUrl = selectedSub.url
                        }) {
                            NeonIcon(Icons.Rounded.Edit, t("edit_subscription"), size = 22.dp)
                        }
                        IconButton(onClick = { confirmDeleteSub = selectedSub }) {
                            NeonIcon(Icons.Rounded.Delete, t("delete"), tintOverride = p.error, size = 22.dp)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(t("update_subscription"), color = p.muted, fontSize = 11.sp)
                        Text(t("edit_subscription"), color = p.muted, fontSize = 11.sp)
                        Text(t("delete"), color = p.muted, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = sortByDelay,
                onClick = { onSortByDelay(!sortByDelay) },
                label = { Text(t("sort_by_delay")) },
                leadingIcon = { Icon(Icons.Rounded.Sort, null, Modifier.height(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(0.25f),
                    selectedLabelColor = accent
                )
            )
            if (multiSelect) {
                FilterChip(
                    selected = false,
                    onClick = { if (checked.isNotEmpty()) confirmDeleteSelected = true },
                    enabled = checked.isNotEmpty(),
                    label = { Text("${t("delete_selected")} (${checked.size})") },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.height(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = p.error.copy(0.18f),
                        labelColor = p.error
                    )
                )
            } else {
                FilterChip(
                    selected = false,
                    onClick = onDeleteInvalid,
                    enabled = !isTesting,
                    label = { Text(if (isTesting) t("testing_connections") else t("delete_invalid")) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.height(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (isTesting) p.warning.copy(0.2f) else p.surface2,
                        labelColor = if (isTesting) p.warning else p.text
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = search,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(t("search_servers"), color = p.muted) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = p.muted) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = p.border,
                focusedTextColor = p.text,
                unfocusedTextColor = p.text,
                cursorColor = accent,
                focusedContainerColor = p.surface2,
                unfocusedContainerColor = p.surface2
            )
        )
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t("no_servers_yet"), color = p.text, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(t("import_hint_empty"), color = p.muted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { server ->
                    if (multiSelect) {
                        ServerListItem(
                            server = server,
                            selected = server.id == selectedId,
                            testing = isTesting && testingServerId == server.id,
                            multiSelect = true,
                            checked = server.id in checked,
                            onClick = {
                                checked = if (server.id in checked) checked - server.id
                                else checked + server.id
                            },
                            onLongClick = {},
                            onTest = {},
                            onEdit = {}
                        )
                    } else {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onDelete(server.id)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(p.error.copy(0.25f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Rounded.Delete, null, tint = p.error)
                                }
                            },
                            enableDismissFromStartToEnd = false
                        ) {
                            ServerListItem(
                                server = server,
                                selected = server.id == selectedId,
                                testing = isTesting && testingServerId == server.id,
                                onClick = { onSelect(server.id) },
                                onLongClick = {
                                    multiSelect = true
                                    checked = setOf(server.id)
                                },
                                onEdit = { onEdit(server.id) },
                                onTest = { onTestOne(server) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text(t("delete_all_title")) },
            text = { Text(t("delete_all_body")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    onDeleteAll()
                }) { Text(t("delete_all"), color = p.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text(t("cancel")) }
            }
        )
    }
    if (confirmDeleteSub != null) {
        val sub = confirmDeleteSub!!
        AlertDialog(
            onDismissRequest = { confirmDeleteSub = null },
            title = { Text(t("delete")) },
            text = { Text(sub.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSubscription(sub.id)
                    confirmDeleteSub = null
                    tab = ServerTab.All
                }) { Text(t("delete"), color = p.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSub = null }) { Text(t("cancel")) }
            }
        )
    }

    if (editSub != null) {
        AlertDialog(
            onDismissRequest = { editSub = null },
            title = { Text(t("edit_subscription")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(t("name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text(t("subscription_url")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val s = editSub!!
                    onUpdateSubscription(s.id, editName, editUrl)
                    editSub = null
                }) { Text(t("update_subscription")) }
            },
            dismissButton = {
                TextButton(onClick = { editSub = null }) { Text(t("cancel")) }
            }
        )
    }

    if (confirmDeleteSelected) {
        AlertDialog(
            onDismissRequest = { confirmDeleteSelected = false },
            title = { Text(t("delete_selected_title")) },
            text = { Text(t("delete_selected_body").replace("{n}", checked.size.toString())) },
            confirmButton = {
                TextButton(onClick = {
                    val ids = checked
                    confirmDeleteSelected = false
                    multiSelect = false
                    checked = emptySet()
                    onDeleteMany(ids)
                }) { Text(t("delete"), color = p.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSelected = false }) { Text(t("cancel")) }
            }
        )
    }
}
