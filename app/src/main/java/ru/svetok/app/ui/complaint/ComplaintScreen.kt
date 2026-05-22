package ru.svetok.app.ui.complaint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.svetok.app.data.complaint.HttpComplaintRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintScreen(
    streetName: String?,
    onBack: () -> Unit,
    repository: HttpComplaintRepository = koinInject(),
) {
    var house by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Жалоба") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Сообщить о проблеме на улице",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = streetName?.takeIf { it.isNotBlank() }
                    ?: "Улица не выбрана. Жалоба будет отправлена без адресной привязки.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = streetName.orEmpty(),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Улица") },
                enabled = false,
            )

            OutlinedTextField(
                value = house,
                onValueChange = { house = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Дом (необязательно)") },
                enabled = !isSubmitting && !isSuccess,
                singleLine = true,
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Что случилось") },
                enabled = !isSubmitting && !isSuccess,
                minLines = 5,
                supportingText = {
                    Text("Например: света нет дольше заявленного времени, искрит линия, напряжение скачет.")
                },
            )

            if (isSuccess) {
                Text(
                    text = "Жалоба отправлена. Спасибо.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isSubmitting = true
                        val result = repository.submitComplaint(
                            street = streetName,
                            house = house,
                            message = message,
                        )
                        isSubmitting = false
                        if (result.isSuccess) {
                            isSuccess = true
                            message = ""
                            house = ""
                        }
                        snackbarHostState.showSnackbar(result.message)
                    }
                },
                enabled = !isSubmitting && !isSuccess && message.trim().length >= 3,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Отправить жалобу")
                }
            }

            if (isSuccess) {
                TextButton(onClick = onBack) {
                    Text("Вернуться к карте")
                }
            }
        }
    }
}
