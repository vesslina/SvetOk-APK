package ru.svetok.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.svetok.app.data.admin.AdminObject
import ru.svetok.app.data.admin.OutageType
import ru.svetok.app.data.admin.ScopeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOutageScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var selectedType by remember { mutableStateOf(OutageType.EMERGENCY) }
    var selectedScopeTab by remember { mutableIntStateOf(0) }
    var selectedObject by remember { mutableStateOf<AdminObject?>(null) }
    var objectListExpanded by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var startsNow by remember { mutableStateOf(true) }
    var startsAtText by remember { mutableStateOf("") }
    var endsNever by remember { mutableStateOf(true) }
    var endsAtText by remember { mutableStateOf("") }

    val scopeTypes = ScopeType.entries
    val currentScope = scopeTypes.getOrElse(selectedScopeTab) { ScopeType.TP }

    val availableObjects = when (currentScope) {
        ScopeType.TP -> uiState.objects?.tps
        ScopeType.FEEDER -> uiState.objects?.feeders
        ScopeType.SUBSTATION -> uiState.objects?.substations
    } ?: emptyList()

    val filteredObjects = if (searchQuery.isBlank()) availableObjects
        else availableObjects.filter { it.name.contains(searchQuery, ignoreCase = true) }

    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) {
            viewModel.consumeCreateSuccess()
            onBack()
        }
    }

    LaunchedEffect(uiState.createError) {
        uiState.createError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.consumeCreateError()
        }
    }

    // Load TP/Feeder/Substation objects on first open
    LaunchedEffect(Unit) {
        viewModel.loadObjects()
    }

    // Reset object selection and expand list when scope tab changes
    LaunchedEffect(selectedScopeTab) {
        selectedObject = null
        objectListExpanded = true
        searchQuery = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создание отключения") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Type selector ──────────────────────────────────
            item {
                SectionLabel("Тип отключения")
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.selectableGroup(),
                ) {
                    OutageType.entries.forEach { type ->
                        val isSelected = selectedType == type
                        val activeColor = if (type == OutageType.EMERGENCY) Color(0xFFC53A2D)
                                          else Color(0xFFD47A00)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) activeColor else Color.Transparent)
                                .border(1.dp, if (isSelected) activeColor else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .selectable(selected = isSelected, onClick = { selectedType = type }, role = Role.RadioButton)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // ── Scope tabs ─────────────────────────────────────
            item {
                SectionLabel("Выберите объект")
                Spacer(Modifier.height(6.dp))
                ScrollableTabRow(selectedTabIndex = selectedScopeTab, edgePadding = 0.dp) {
                    scopeTypes.forEachIndexed { index, scope ->
                        Tab(
                            selected = selectedScopeTab == index,
                            onClick = { selectedScopeTab = index },
                            text = { Text(scope.label) },
                        )
                    }
                }
            }

            // ── Object list (collapsed or expanded) ───────────
            if (!objectListExpanded && selectedObject != null) {
                // Collapsed: show only selected item + "Change" button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = selectedObject!!.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        TextButton(
                            onClick = { objectListExpanded = true },
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.size(4.dp))
                            Text("Изменить")
                        }
                    }
                }
            } else {
                // Expanded: search field + scrollable list
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Поиск по названию...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                    )
                }

                when {
                    uiState.isLoadingObjects -> item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }

                    filteredObjects.isEmpty() -> item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Нет данных" else "Не найдено: «$searchQuery»",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    else -> {
                        // Limit visible items to keep the list manageable
                        val visibleObjects = if (searchQuery.isBlank()) filteredObjects.take(30)
                                             else filteredObjects
                        items(visibleObjects, key = { it.id }) { obj ->
                            ObjectListItem(
                                obj = obj,
                                isSelected = selectedObject?.id == obj.id,
                                onClick = {
                                    selectedObject = obj
                                    objectListExpanded = false
                                    searchQuery = ""
                                },
                            )
                            HorizontalDivider()
                        }
                        if (searchQuery.isBlank() && filteredObjects.size > 30) {
                            item {
                                Text(
                                    text = "Показано 30 из ${filteredObjects.size}. Введите название для поиска.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ── Reason ─────────────────────────────────────────
            item {
                SectionLabel("Причина (необязательно)")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Авария на линии, плановые работы...") },
                    minLines = 2,
                )
            }

            // ── Starts at ──────────────────────────────────────
            item {
                SectionLabel("Начало отключения")
                Spacer(Modifier.height(4.dp))
                TimeOptionRow(
                    optionALabel = "Сейчас",
                    optionBLabel = "Указать время",
                    useOptionA = startsNow,
                    onOptionASelected = { startsNow = true },
                    onOptionBSelected = { startsNow = false },
                )
                if (!startsNow) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = startsAtText,
                        onValueChange = { startsAtText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Время начала") },
                        placeholder = { Text("14:30 03.05.26") },
                        singleLine = true,
                    )
                }
            }

            // ── Ends at ────────────────────────────────────────
            item {
                SectionLabel("Окончание отключения")
                Spacer(Modifier.height(4.dp))
                TimeOptionRow(
                    optionALabel = "До распоряжения",
                    optionBLabel = "Указать время",
                    useOptionA = endsNever,
                    onOptionASelected = { endsNever = true },
                    onOptionBSelected = { endsNever = false },
                )
                if (!endsNever) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = endsAtText,
                        onValueChange = { endsAtText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Время окончания") },
                        placeholder = { Text("18:00 03.05.26") },
                        singleLine = true,
                    )
                }
            }

            // ── Submit ─────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        val obj = selectedObject ?: return@Button
                        viewModel.createOutage(
                            outageType = selectedType,
                            scope = currentScope,
                            scopeRefId = obj.id,
                            reason = reason,
                            startsNow = startsNow,
                            startsAtText = startsAtText,
                            endsNever = endsNever,
                            endsAtText = endsAtText,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    enabled = selectedObject != null && !uiState.isCreating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ),
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = if (selectedObject == null) "Выберите объект выше" else "Сохранить отключение",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ObjectListItem(
    obj: AdminObject,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = obj.name, style = MaterialTheme.typography.bodyMedium)
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun TimeOptionRow(
    optionALabel: String,
    optionBLabel: String,
    useOptionA: Boolean,
    onOptionASelected: () -> Unit,
    onOptionBSelected: () -> Unit,
) {
    Column(modifier = Modifier.selectableGroup()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = useOptionA, onClick = onOptionASelected, role = Role.RadioButton)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = useOptionA, onClick = onOptionASelected)
            Text(optionALabel, modifier = Modifier.padding(start = 4.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = !useOptionA, onClick = onOptionBSelected, role = Role.RadioButton)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = !useOptionA, onClick = onOptionBSelected)
            Text(optionBLabel, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
