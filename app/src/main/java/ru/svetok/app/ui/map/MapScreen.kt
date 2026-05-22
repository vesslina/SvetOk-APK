package ru.svetok.app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.shape.CircleShape
import ru.svetok.app.data.outage.MapOutage
import ru.svetok.app.data.outage.OutageMapStatus
import ru.svetok.app.data.admin.TapResult
import ru.svetok.app.ui.admin.AdminViewModel

@Composable
fun MapScreen(
    onReportStreet: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    onOpenOutagesList: () -> Unit = {},
    viewModel: MapViewModel = koinViewModel(),
    adminViewModel: AdminViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adminState by adminViewModel.uiState.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showLoginDialog by remember { mutableStateOf(false) }
    var loginText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var tapHint by remember { mutableStateOf("") }

    // Close drawer on back instead of exiting the app
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // Auto-hide tap hint after 2s
    LaunchedEffect(tapHint) {
        if (tapHint.isNotEmpty()) {
            delay(2000)
            tapHint = ""
        }
    }

    LaunchedEffect(adminState.isLoggedIn) {
        if (adminState.isLoggedIn && showLoginDialog) {
            showLoginDialog = false
            loginText = ""
            passwordText = ""
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            AppDrawer(
                isAdminLoggedIn = adminState.isLoggedIn,
                isAdminAccessUnlocked = adminState.isAdminAccessUnlocked,
                adminLogin = adminState.adminLogin,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onLoginClicked = {
                    scope.launch { drawerState.close() }
                    showLoginDialog = true
                },
                onOpenAdminPanel = {
                    scope.launch { drawerState.close() }
                    onOpenAdminPanel()
                },
                onOpenOutagesList = onOpenOutagesList,
                onLogout = { adminViewModel.logout() },
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                        )
                    )
                )
        ) {
            OsmMapView(
                streets = uiState.streets,
                highlightByStreetNorm = uiState.highlightByStreetNorm,
                selectedStreetNorm = uiState.selectedStreet?.streetNorm,
                onStreetTap = viewModel::onStreetTapped,
                modifier = Modifier.fillMaxSize(),
            )

            StatusPanel(
                isLoading = uiState.isLoading,
                activeStreetCount = uiState.highlightByStreetNorm.countByStatus(OutageMapStatus.ACTIVE),
                plannedStreetCount = uiState.highlightByStreetNorm.countByStatus(OutageMapStatus.PLANNED),
                isOnline = uiState.isOnline,
                lastUpdatedLabel = uiState.lastUpdatedLabel,
                errorMessage = uiState.errorMessage,
                tapHint = tapHint,
                onRefresh = viewModel::refreshNow,
                onOpenSettings = onOpenSettings,
                onOpenMenu = { scope.launch { drawerState.open() } },
                onTitleTapped = {
                    when (val r = adminViewModel.onTitleTapped()) {
                        is TapResult.Progress -> tapHint = "Ещё ${r.remaining} нажатий"
                        is TapResult.Unlocked -> tapHint = "Режим администратора разблокирован"
                        else -> {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 18.dp),
            )

            uiState.selectedStreet?.let { selectedStreet ->
                StreetDetailsSheet(
                    selectedStreet = selectedStreet,
                    onDismiss = viewModel::clearStreetSelection,
                    onReportStreet = { onReportStreet(selectedStreet.streetName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 18.dp),
                )
            }
        }
    }

    if (showLoginDialog) {
        AdminLoginDialog(
            isLoggingIn = adminState.isLoggingIn,
            loginError = adminState.loginError,
            loginText = loginText,
            onLoginTextChange = { loginText = it },
            passwordText = passwordText,
            onPasswordTextChange = { passwordText = it },
            onConfirm = { adminViewModel.login(loginText, passwordText) },
            onDismiss = {
                showLoginDialog = false
                adminViewModel.clearLoginError()
                loginText = ""
                passwordText = ""
            },
        )
    }
}

@Composable
private fun AdminLoginDialog(
    isLoggingIn: Boolean,
    loginError: String?,
    loginText: String,
    onLoginTextChange: (String) -> Unit,
    passwordText: String,
    onPasswordTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вход для администратора") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = loginText,
                    onValueChange = onLoginTextChange,
                    label = { Text("Логин") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = passwordText,
                    onValueChange = onPasswordTextChange,
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!loginError.isNullOrBlank()) {
                    Text(
                        text = loginError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoggingIn && loginText.isNotBlank() && passwordText.isNotBlank(),
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Войти")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun AppDrawer(
    isAdminLoggedIn: Boolean,
    isAdminAccessUnlocked: Boolean,
    adminLogin: String,
    onCloseDrawer: () -> Unit,
    onLoginClicked: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onOpenOutagesList: () -> Unit,
    onLogout: () -> Unit,
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Меню СветОк",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(onClick = onCloseDrawer) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть меню")
            }
        }
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        NavigationDrawerItem(
            label = { Text("Карта отключений") },
            selected = true,
            onClick = onCloseDrawer,
            icon = { Icon(Icons.Default.Map, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        NavigationDrawerItem(
            label = { Text("Текущие отключения") },
            selected = false,
            onClick = { onOpenOutagesList(); onCloseDrawer() },
            icon = { Icon(Icons.Default.List, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        if (isAdminLoggedIn) {
            Text(
                text = "Администратор: $adminLogin",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
            )
            NavigationDrawerItem(
                label = { Text("Панель управления") },
                selected = false,
                onClick = onOpenAdminPanel,
                icon = {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Выйти из аккаунта", color = MaterialTheme.colorScheme.error) },
                selected = false,
                onClick = { onLogout(); onCloseDrawer() },
                icon = {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        } else if (isAdminAccessUnlocked) {
            NavigationDrawerItem(
                label = { Text("Войти как администратор") },
                selected = false,
                onClick = onLoginClicked,
                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun StatusPanel(
    isLoading: Boolean,
    activeStreetCount: Int,
    plannedStreetCount: Int,
    isOnline: Boolean,
    lastUpdatedLabel: String?,
    errorMessage: String?,
    tapHint: String,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
    onTitleTapped: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.widthIn(max = 360.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // ── Title row ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTitleTapped,
                        ),
                ) {
                    GlitchTitleText(
                        text = "СветОк",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (tapHint.isNotEmpty()) {
                        Text(
                            text = tapHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        )
                    } else {
                        Text(
                            text = "Карта отключений Светлограда",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, "Обновить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Notifications, "Уведомления",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, "Меню",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Status content ────────────────────────────────
            when {
                isLoading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                        Text("Загрузка...", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                errorMessage != null -> {
                    ConnectionDot(isOnline = false)
                    Text(errorMessage, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }

                else -> {
                    // Connection status
                    ConnectionDot(isOnline = isOnline, lastUpdatedLabel = lastUpdatedLabel)

                    // Outage counts or "all clear"
                    if (activeStreetCount == 0 && plannedStreetCount == 0) {
                        NoOutagesRow()
                    } else {
                        LegendRow(activeStreetCount, plannedStreetCount)
                        Text(
                            text = "Нажмите на подсвеченную улицу для деталей.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionDot(isOnline: Boolean, lastUpdatedLabel: String? = null) {
    val green = Color(0xFF2E7D32)
    val dotColor = if (isOnline) green else MaterialTheme.colorScheme.error
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = dotColor, shape = CircleShape),
        )
        Text(
            text = if (isOnline) "Онлайн" else "Нет связи с сервером",
            style = MaterialTheme.typography.labelSmall,
            color = dotColor,
        )
        if (isOnline && lastUpdatedLabel != null) {
            Text(
                text = "· $lastUpdatedLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoOutagesRow() {
    val green = Color(0xFF2E7D32)
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓", style = MaterialTheme.typography.labelMedium, color = green)
        Text("Отключений нет", style = MaterialTheme.typography.labelMedium, color = green)
    }
}

@Composable
private fun LegendRow(activeStreetCount: Int, plannedStreetCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (activeStreetCount > 0)
            LegendItem(color = Color(0xFFC53A2D), label = "Активно: $activeStreetCount")
        if (plannedStreetCount > 0)
            LegendItem(color = Color(0xFFD47A00), label = "План: $plannedStreetCount")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color = color, shape = MaterialTheme.shapes.extraSmall),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StreetDetailsSheet(
    selectedStreet: SelectedStreetUi,
    onDismiss: () -> Unit,
    onReportStreet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.widthIn(max = 420.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = selectedStreet.streetName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = selectedStreet.statusLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = selectedStreet.statusColor(),
                    )
                }
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }

            selectedStreet.outages.forEachIndexed { idx, outage ->
                val publicTitle = when (outage.status) {
                    OutageMapStatus.ACTIVE -> "Аварийное отключение #${idx + 1}"
                    OutageMapStatus.PLANNED -> "Плановое отключение #${idx + 1}"
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = outage.containerColor().copy(alpha = 0.92f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = publicTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = outage.timeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f),
                        )
                        Text(
                            text = outage.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f),
                        )
                    }
                }
            }

            Button(onClick = onReportStreet, modifier = Modifier.fillMaxWidth()) {
                Text("Сообщить о проблеме")
            }
        }
    }
}

private fun Map<String, OutageMapStatus>.countByStatus(status: OutageMapStatus): Int =
    values.count { it == status }

private fun MapOutage.containerColor(): Color =
    when (status) {
        OutageMapStatus.ACTIVE -> Color(0xFFC53A2D)
        OutageMapStatus.PLANNED -> Color(0xFFD47A00)
    }

private fun SelectedStreetUi.statusColor(): Color =
    when (status) {
        OutageMapStatus.ACTIVE -> Color(0xFFC53A2D)
        OutageMapStatus.PLANNED -> Color(0xFFD47A00)
    }

private fun SelectedStreetUi.statusLabel(): String =
    when (status) {
        OutageMapStatus.ACTIVE -> "Аварийное отключение активно"
        OutageMapStatus.PLANNED -> "Запланированное отключение"
    }
