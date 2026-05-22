package ru.svetok.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.svetok.app.data.onboarding.OnboardingPrefs
import ru.svetok.app.ui.map.GlitchTitleText

private data class OnboardingPage(
    val icon: ImageVector?,
    val useGlitchTitle: Boolean,
    val title: String,
    val subtitle: String,
    val description: String,
)

private val PAGES = listOf(
    OnboardingPage(
        icon = null,
        useGlitchTitle = true,
        title = "СветОк",
        subtitle = "Карта отключений электроснабжения",
        description = "Всегда знай, когда и где нет света\nв Светлограде — в реальном времени.",
    ),
    OnboardingPage(
        icon = Icons.Default.Map,
        useGlitchTitle = false,
        title = "Карта улиц",
        subtitle = "",
        description = "Красные линии — аварийное отключение.\nОранжевые — плановые работы.\n\nНажми на улицу, чтобы узнать подробности и время восстановления.",
    ),
    OnboardingPage(
        icon = Icons.Default.Notifications,
        useGlitchTitle = false,
        title = "Будь в курсе",
        subtitle = "",
        description = "Включи push-уведомления и получай\nоповещения об отключениях в своём районе первым.\n\nНашёл проблему? Отправь жалобу прямо из приложения.",
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val prefs: OnboardingPrefs = koinInject()
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()

    val isLastPage = pagerState.currentPage == PAGES.lastIndex

    // Request notification permission when user reaches the last (notifications) slide
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by system */ }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page == PAGES.lastIndex && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A0000), Color(0xFF2D0000)),
                )
            ),
    ) {
        // Skip button top-right
        if (!isLastPage) {
            TextButton(
                onClick = {
                    prefs.markCompleted()
                    onFinish()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Text("Пропустить", color = Color.White.copy(alpha = 0.6f))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { pageIndex ->
                OnboardingPageContent(page = PAGES[pageIndex])
            }

            // Dots indicator
            Row(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(PAGES.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        label = "dot",
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.3f)
                            ),
                    )
                }
            }

            // Navigation button
            Button(
                onClick = {
                    if (isLastPage) {
                        prefs.markCompleted()
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = if (isLastPage) "Начать →" else "Далее",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icon or glitch title
        if (page.useGlitchTitle) {
            GlitchTitleText(
                text = page.title,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                glitchIntervalMs = 8_000L,
            )
        } else {
            page.icon?.let { icon ->
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (page.subtitle.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = page.subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
        )
    }
}
