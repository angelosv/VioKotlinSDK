package com.reachu.demoapp.viaplay.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reachu.demoapp.viaplay.models.CastingContestEvent
import io.reachu.VioCore.managers.CampaignManager
import io.reachu.VioDesignSystem.Components.CachedAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CastingContestModal(
    contests: List<CastingContestEvent>,
    initialIndex: Int = 0,
    onDismiss: () -> Void
) {
    val campaignManager = remember { CampaignManager.shared }
    val currentCampaign by campaignManager.currentCampaign.collectAsState()
    val pagerState = rememberPagerState(initialPage = initialIndex) { contests.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Campaign Logo
                CachedAsyncImage(
                    url = currentCampaign?.campaignLogo,
                    contentDescription = "Campaign Logo",
                    modifier = Modifier.height(30.dp),
                    placeholder = { Box(Modifier.size(30.dp)) }
                )

                Spacer(modifier = Modifier.weight(1f))

                if (contests.isNotEmpty()) {
                    Text(
                        text = contests[pagerState.currentPage].title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Close Button
                IconButton(
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.opacity(0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Horizontal Pager for swiping between contests
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ContestContent(
                    contest = contests[page],
                    currentCampaignLogo = currentCampaign?.campaignLogo,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
fun ContestContent(
    contest: CastingContestEvent,
    currentCampaignLogo: String?,
    onDismiss: () -> Void
) {
    val scope = rememberCoroutineScope()
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    var showPhoneInput by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Mock questions based on Swift reference
    val championsLeagueQuestions = listOf(
        QuizQuestion("Hvilken klubb har vunnet flest Champions League-titler?", listOf("Real Madrid", "Barcelona", "Bayern München")),
        QuizQuestion("I hvilket år ble Champions League opprettet?", listOf("1992", "1985", "2000")),
        QuizQuestion("Hvor mange lag deltar i Champions League-gruppespillet?", listOf("32", "24", "16"))
    )

    val matchQuestions = listOf(
        QuizQuestion("Hvilket lag scoret første mål i kampen?", listOf("Barcelona", "PSG", "Ingen mål ennå")),
        QuizQuestion("Hvor mange gule kort ble det utdelt i første omgang?", listOf("2", "1", "0")),
        QuizQuestion("Hvilken spiller scoret det første mål?", listOf("A. Diallo", "B. Mbeumo", "Ingen mål"))
    )

    val questions = if (contest.contestType.lowercase() == "giveaway") championsLeagueQuestions else matchQuestions
    val currentQuestion = if (currentQuestionIndex < questions.size) questions[currentQuestionIndex] else null

    // Content Area
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 40.dp)
    ) {
                item {
                    // Contest Info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = contest.description,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = contest.prize,
                            color = Color(0xFFFFA500), // Orange
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Optional Image from metadata
                    if (contest.metadata?.get("imageAsset") != null) {
                        // In a real app we'd load an actual image, for now using a placeholder box or similar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Gray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Contest Image", color = Color.White.copy(alpha = 0.5f))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!showPhoneInput) {
                        // Questions Section
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            // Progress Indicator
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                questions.indices.forEach { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (index <= currentQuestionIndex) Color(0xFFFFA500) else Color.White.copy(alpha = 0.2f))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            currentQuestion?.let { question ->
                                Text(
                                    text = "Spørsmål ${currentQuestionIndex + 1} av ${questions.size}",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = question.question,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                // Options
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    question.options.forEachIndexed { optionIndex, optionText ->
                                        QuizOptionButton(
                                            text = optionText,
                                            isSelected = selectedAnswers[currentQuestionIndex] == optionIndex,
                                            onTap = {
                                                selectedAnswers[currentQuestionIndex] = optionIndex
                                                scope.launch {
                                                    delay(500)
                                                    if (currentQuestionIndex < questions.size - 1) {
                                                        currentQuestionIndex++
                                                    } else {
                                                        showPhoneInput = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Phone Input Section
                        PhoneInputView(
                            phoneNumber = phoneNumber,
                            onPhoneNumberChange = { phoneNumber = it },
                            isSubmitting = isSubmitting,
                            onSubmit = {
                                isSubmitting = true
                                scope.launch {
                                    delay(1500)
                                    isSubmitting = false
                                    onDismiss()
                                }
                            }
                        )
                    }
            }
        }
    }

data class QuizQuestion(val question: String, val options: List<String>)

@Composable
fun QuizOptionButton(
    text: String,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    Button(
        onClick = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFFFA500) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFFFA500).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFFFA500),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PhoneInputView(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Skriv inn telefonnummeret ditt",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Vi kontakter deg hvis du vinner!",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Phone input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = Color(0xFFFFA500),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Submit Button
        Button(
            onClick = onSubmit,
            enabled = phoneNumber.isNotEmpty() && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFA500),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            elevation = null
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "Send inn",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun Color.opacity(alpha: Float): Color = copy(alpha = alpha)
