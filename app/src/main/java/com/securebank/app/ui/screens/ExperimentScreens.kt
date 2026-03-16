package com.securebank.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.securebank.app.data.model.*
import com.securebank.app.sensor.TouchDataCollector
import com.securebank.app.ui.components.CustomPinPad
import com.securebank.app.ui.components.PinResultIndicator
import com.securebank.app.ui.components.TouchCaptureWrapper
import com.securebank.app.ui.theme.*
import com.securebank.app.ui.viewmodel.ExperimentViewModel

/**
 * ============================================
 * EXPERIMENT HUB SCREEN
 * ============================================
 * Main screen for managing the controlled experiment.
 * Handles participant management, session launching, and data export.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentHubScreen(
    viewModel: ExperimentViewModel = hiltViewModel(),
    onStartSession: (ExperimentSessionType, String?) -> Unit,
    onBack: () -> Unit
) {
    val participants by viewModel.participants.collectAsState()
    val currentParticipant by viewModel.currentParticipant.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showImpostorDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Obsidian, ObsidianLight, Obsidian)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = CloudWhite)
                }
                Text(
                    text = "Research Experiment",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = CloudWhite
                )
                IconButton(onClick = { viewModel.exportAllData() }) {
                    Icon(Icons.Default.FileDownload, "Export All", tint = Emerald)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status message
            if (statusMessage.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldMuted.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier.padding(12.dp),
                        color = EmeraldBright,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Participants Section
            SectionHeader(
                title = "Participants",
                count = participants.size,
                onAdd = { showAddDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (participants.isEmpty()) {
                EmptyStateCard("No participants yet. Tap + to add one.")
            } else {
                participants.forEach { participant ->
                    ParticipantCard(
                        participant = participant,
                        isSelected = currentParticipant?.participantId == participant.participantId,
                        onSelect = { viewModel.selectParticipant(participant) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Session Controls (only when a participant is selected)
            AnimatedVisibility(visible = currentParticipant != null) {
                Column {
                    Text(
                        text = "Start Session for ${currentParticipant?.name ?: ""}",
                        fontWeight = FontWeight.SemiBold,
                        color = CloudWhite,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Enrollment button
                    SessionButton(
                        text = "Enrollment Session",
                        description = "Capture baseline behavior (PIN + typing + touch + motion)",
                        icon = Icons.Default.PersonAdd,
                        color = Emerald,
                        enabled = !(currentParticipant?.enrollmentComplete ?: false),
                        onClick = { onStartSession(ExperimentSessionType.ENROLLMENT, null) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Genuine session button
                    SessionButton(
                        text = "Genuine Session",
                        description = "Same user interacts — should be accepted",
                        icon = Icons.Default.VerifiedUser,
                        color = EmeraldBright,
                        enabled = currentParticipant?.enrollmentComplete ?: false,
                        onClick = { onStartSession(ExperimentSessionType.GENUINE, null) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Impostor session button
                    SessionButton(
                        text = "Impostor Session",
                        description = "Different user on this participant's profile — should be rejected",
                        icon = Icons.Default.Warning,
                        color = Coral,
                        enabled = currentParticipant?.enrollmentComplete ?: false,
                        onClick = { showImpostorDialog = true }
                    )
                }
            }
        }

        // Loading overlay
        if (isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Obsidian.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Emerald)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Exporting data...", color = CloudWhite)
                }
            }
        }
    }

    // Add Participant Dialog
    if (showAddDialog) {
        AddParticipantDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name ->
                viewModel.addParticipant(name)
                showAddDialog = false
            }
        )
    }

    // Impostor Target Selection Dialog
    if (showImpostorDialog) {
        ImpostorTargetDialog(
            participants = participants.filter {
                it.participantId != currentParticipant?.participantId && it.enrollmentComplete
            },
            onDismiss = { showImpostorDialog = false },
            onSelect = { targetId ->
                onStartSession(ExperimentSessionType.IMPOSTOR, targetId)
                showImpostorDialog = false
            }
        )
    }
}

/**
 * ============================================
 * EXPERIMENT SESSION SCREEN
 * ============================================
 * Active experiment session with task progression.
 */
@Composable
fun ExperimentSessionScreen(
    viewModel: ExperimentViewModel = hiltViewModel(),
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val session by viewModel.currentSession.collectAsState()
    val currentTask by viewModel.currentTask.collectAsState()
    val currentPin by viewModel.currentPin.collectAsState()
    val pinAttemptNumber by viewModel.pinAttemptNumber.collectAsState()
    val pinResults by viewModel.pinAttemptResults.collectAsState()
    val promptIndex by viewModel.currentPromptIndex.collectAsState()
    val typedText by viewModel.typedText.collectAsState()
    val progress by viewModel.sessionProgress.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    val activeSession = session ?: return

    // Check if session is complete
    LaunchedEffect(activeSession.isComplete) {
        if (activeSession.isComplete) {
            // Wait a bit for the user to see the completion state
            kotlinx.coroutines.delay(2000)
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Obsidian, ObsidianLight, Obsidian)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Session Header
            SessionHeader(
                sessionType = activeSession.sessionType,
                participantId = activeSession.participantId,
                progress = progress,
                currentTask = currentTask,
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Task Content
            Box(modifier = Modifier.weight(1f)) {
                when (currentTask) {
                    ExperimentTaskType.PIN_ENTRY -> {
                        PinEntryTask(
                            currentPin = currentPin,
                            attemptNumber = pinAttemptNumber,
                            maxAttempts = if (activeSession.sessionType == ExperimentSessionType.ENROLLMENT)
                                PinConfig.PIN_REPETITIONS_ENROLLMENT
                            else PinConfig.PIN_REPETITIONS_SESSION,
                            sessionId = activeSession.sessionId,
                            results = pinResults,
                            onDigitPressed = { viewModel.onPinDigitPressed(it) },
                            onBackspace = { viewModel.onPinBackspace() },
                            onPinComplete = { viewModel.onPinComplete(it) }
                        )
                    }
                    ExperimentTaskType.TEXT_TYPING -> {
                        TextTypingTask(
                            prompts = if (activeSession.sessionType == ExperimentSessionType.ENROLLMENT)
                                PromptedTexts.enrollmentTexts
                            else PromptedTexts.sessionTexts,
                            currentPromptIndex = promptIndex,
                            typedText = typedText,
                            onTextChange = { old, new -> viewModel.onTextChanged(old, new) },
                            onSubmit = { viewModel.submitTypedText() }
                        )
                    }
                    ExperimentTaskType.TOUCH_INTERACTION -> {
                        TouchInteractionTask(
                            touchDataCollector = viewModel.touchDataCollector,
                            onComplete = { viewModel.completeTouchTask() }
                        )
                    }
                    ExperimentTaskType.FREE_BROWSING -> {
                        FreeBrowsingTask(
                            touchDataCollector = viewModel.touchDataCollector,
                            onComplete = { viewModel.completeFreeBrowsingTask() }
                        )
                    }
                }
            }

            // Status bar
            if (statusMessage.isNotBlank()) {
                Text(
                    text = statusMessage,
                    color = MutedGray,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ========================================
// TASK COMPOSABLES
// ========================================

@Composable
private fun PinEntryTask(
    currentPin: String,
    attemptNumber: Int,
    maxAttempts: Int,
    sessionId: String,
    results: List<PinAttemptResult>,
    onDigitPressed: (PinKeystrokeEvent) -> Unit,
    onBackspace: () -> Unit,
    onPinComplete: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Instructions
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "PIN Entry Task",
                fontWeight = FontWeight.Bold,
                color = CloudWhite,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter PIN: ${PinConfig.DEFAULT_PIN}",
                color = EmeraldBright,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Attempt $attemptNumber of $maxAttempts",
                color = MutedGray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Recent results
        if (results.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                results.takeLast(3).forEach { result ->
                    PinResultIndicator(
                        isCorrect = result.isCorrect,
                        attemptNumber = result.attemptNumber,
                        totalAttempts = maxAttempts
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom PIN Pad
        CustomPinPad(
            pinLength = PinConfig.PIN_LENGTH,
            currentPin = currentPin,
            targetPin = PinConfig.DEFAULT_PIN,
            sessionId = sessionId,
            attemptNumber = attemptNumber,
            onDigitPressed = onDigitPressed,
            onPinComplete = onPinComplete,
            onBackspace = onBackspace
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextTypingTask(
    prompts: List<String>,
    currentPromptIndex: Int,
    typedText: String,
    onTextChange: (String, String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Text Typing Task",
            fontWeight = FontWeight.Bold,
            color = CloudWhite,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Sentence ${currentPromptIndex + 1} of ${prompts.size}",
            color = MutedGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Prompt to type
        Card(
            colors = CardDefaults.cardColors(
                containerColor = ObsidianSurface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = prompts.getOrElse(currentPromptIndex) { "" },
                color = Gold,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp),
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input field
        OutlinedTextField(
            value = typedText,
            onValueChange = { new ->
                onTextChange(typedText, new)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            label = { Text("Type the sentence above") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald,
                unfocusedBorderColor = ObsidianBorder,
                focusedLabelColor = Emerald,
                unfocusedLabelColor = MutedGray,
                cursorColor = Emerald,
                focusedTextColor = CloudWhite,
                unfocusedTextColor = CloudGray
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSubmit,
            enabled = typedText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = if (currentPromptIndex < prompts.size - 1) "Next Sentence" else "Complete Task",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * ============================================
 * TAP THE TARGETS — MINI-GAME
 * ============================================
 * Whack-a-mole style game where targets appear at random positions.
 * Captures varied touch coordinates, pressure, and timing for research.
 */
@Composable
private fun TouchInteractionTask(
    touchDataCollector: TouchDataCollector,
    onComplete: () -> Unit
) {
    var tapsCompleted by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var missedTargets by remember { mutableIntStateOf(0) }
    val requiredTaps = 15

    // Target state
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var targetY by remember { mutableFloatStateOf(0.5f) }
    var targetVisible by remember { mutableStateOf(false) }
    var targetSize by remember { mutableIntStateOf(70) }
    var targetKey by remember { mutableIntStateOf(0) }

    // Colors cycle for visual variety
    val targetColors = remember {
        listOf(Emerald, EmeraldBright, Coral, Color(0xFF7C4DFF), Color(0xFFFFB300))
    }
    var colorIndex by remember { mutableIntStateOf(0) }

    // Spawn a new target at random position
    fun spawnTarget() {
        targetX = (0.1f + Math.random().toFloat() * 0.8f)
        targetY = (0.1f + Math.random().toFloat() * 0.7f)
        targetSize = (55 + (Math.random() * 30).toInt())
        colorIndex = (colorIndex + 1) % targetColors.size
        targetVisible = true
        targetKey++
    }

    // Auto-spawn first target
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        spawnTarget()
    }

    // Target timeout — disappears after 2.5s if not tapped
    LaunchedEffect(targetKey) {
        if (targetVisible && tapsCompleted < requiredTaps) {
            kotlinx.coroutines.delay(2500)
            if (targetVisible) {
                targetVisible = false
                missedTargets++
                streak = 0
                kotlinx.coroutines.delay(600)
                if (tapsCompleted < requiredTaps) spawnTarget()
            }
        }
    }

    TouchCaptureWrapper(
        modifier = Modifier.fillMaxSize(),
        touchDataCollector = touchDataCollector
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "🎯 Tap the Targets",
                fontWeight = FontWeight.Bold,
                color = CloudWhite,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap each target before it disappears!",
                color = MutedGray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$tapsCompleted/$requiredTaps", color = EmeraldBright, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Taps", color = MutedGray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Score", color = MutedGray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${streak}🔥", color = Coral, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Streak", color = MutedGray, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = tapsCompleted.toFloat() / requiredTaps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Emerald,
                trackColor = ObsidianBorder
            )

            // Game area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (tapsCompleted >= requiredTaps) {
                    // Completion message
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("✅ Complete!", color = EmeraldBright, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Score: $score | Missed: $missedTargets", color = MutedGray, fontSize = 14.sp)
                    }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1500)
                        onComplete()
                    }
                } else if (targetVisible) {
                    // Animated target
                    val scale = remember { androidx.compose.animation.core.Animatable(0f) }
                    LaunchedEffect(targetKey) {
                        scale.snapTo(0f)
                        scale.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(200))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(
                                x = (targetX * 250).dp,
                                y = (targetY * 350).dp
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(targetSize.dp * scale.value)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            targetColors[colorIndex],
                                            targetColors[colorIndex].copy(alpha = 0.4f)
                                        )
                                    )
                                )
                                .pointerInput(targetKey) {
                                    detectTapGestures(
                                        onTap = {
                                            if (targetVisible) {
                                                targetVisible = false
                                                tapsCompleted++
                                                streak++
                                                score += (10 + streak * 2)
                                                if (tapsCompleted < requiredTaps) {
                                                    // Spawn next after brief delay
                                                    targetX = (0.1f + Math.random().toFloat() * 0.8f)
                                                    targetY = (0.1f + Math.random().toFloat() * 0.7f)
                                                    targetSize = (55 + (Math.random() * 30).toInt())
                                                    colorIndex = (colorIndex + 1) % targetColors.size
                                                    targetVisible = true
                                                    targetKey++
                                                }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎯",
                                fontSize = (targetSize / 3).sp
                            )
                        }
                    }
                } else {
                    // Waiting for next target
                    Text(
                        text = "Get ready...",
                        color = MutedGray.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * ============================================
 * MOCK TRANSACTION REVIEW
 * ============================================
 * Scrollable list of fake banking transactions.
 * Users tap to expand, swipe to categorize — captures natural interaction data.
 */
@Composable
private fun FreeBrowsingTask(
    touchDataCollector: TouchDataCollector,
    onComplete: () -> Unit
) {
    var timeRemaining by remember { mutableIntStateOf(45) }
    var reviewedCount by remember { mutableIntStateOf(0) }

    // Mock transaction data
    val transactions = remember {
        listOf(
            Triple("Amazon Purchase", "₹2,499.00", "Today, 2:30 PM"),
            Triple("Swiggy Food Order", "₹456.00", "Today, 12:15 PM"),
            Triple("ATM Withdrawal", "₹5,000.00", "Today, 10:00 AM"),
            Triple("Netflix Subscription", "₹649.00", "Yesterday"),
            Triple("Unknown Transfer", "₹15,000.00", "Yesterday"),
            Triple("Electricity Bill", "₹1,230.00", "2 days ago"),
            Triple("Flipkart Refund", "+₹899.00", "2 days ago"),
            Triple("Uber Ride", "₹187.00", "3 days ago"),
            Triple("Suspicious Login Attempt", "₹0.00", "3 days ago"),
            Triple("PhonePe Transfer", "₹3,000.00", "4 days ago"),
            Triple("Zomato Gold", "₹299.00", "4 days ago"),
            Triple("International Wire", "₹45,000.00", "5 days ago"),
            Triple("Salary Credit", "+₹52,000.00", "5 days ago"),
            Triple("Google Play", "₹159.00", "6 days ago"),
            Triple("Petrol Pump", "₹1,500.00", "1 week ago"),
        )
    }

    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }
    val categorized = remember { mutableStateMapOf<Int, String>() }

    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timeRemaining--
        }
        onComplete()
    }

    TouchCaptureWrapper(
        modifier = Modifier.fillMaxSize(),
        touchDataCollector = touchDataCollector
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "🏦 Review Transactions",
                fontWeight = FontWeight.Bold,
                color = CloudWhite,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tap to view details · Swipe to categorize",
                    color = MutedGray,
                    fontSize = 12.sp
                )
                Text(
                    text = "${timeRemaining}s",
                    color = if (timeRemaining < 10) Coral else Emerald,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress
            LinearProgressIndicator(
                progress = 1f - (timeRemaining / 45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Emerald,
                trackColor = ObsidianBorder
            )

            Text(
                text = "$reviewedCount of ${transactions.size} reviewed",
                color = MutedGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable transaction list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(transactions.size) { index ->
                    val (name, amount, date) = transactions[index]
                    val isExpanded = expandedStates[index] == true
                    val category = categorized[index]
                    val isSuspicious = name.contains("Unknown") || name.contains("Suspicious") || name.contains("International")
                    val isCredit = amount.startsWith("+")

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (category) {
                                "safe" -> EmeraldMuted.copy(alpha = 0.2f)
                                "suspicious" -> Coral.copy(alpha = 0.15f)
                                else -> ObsidianSurface
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(index) {
                                detectTapGestures(
                                    onTap = {
                                        expandedStates[index] = !(expandedStates[index] ?: false)
                                        if (!categorized.containsKey(index)) {
                                            reviewedCount++
                                        }
                                    }
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        color = if (isSuspicious) Coral else CloudWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = date,
                                        color = MutedGray,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = amount,
                                    color = if (isCredit) EmeraldBright else CloudWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            // Expanded details
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = ObsidianBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Transaction ID: TXN${(100000 + index * 7919) % 999999}",
                                    color = MutedGray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (isSuspicious) "⚠️ Flagged for review" else "✓ Verified transaction",
                                    color = if (isSuspicious) Coral else EmeraldBright,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                // Categorize buttons
                                if (category == null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                categorized[index] = "safe"
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = EmeraldBright
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("✓ Legitimate", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                categorized[index] = "suspicious"
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Coral
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("⚠ Suspicious", fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = if (category == "safe") "Marked: Legitimate ✓" else "Marked: Suspicious ⚠",
                                        color = if (category == "safe") EmeraldBright else Coral,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skip button
            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Skip", fontSize = 12.sp)
            }
        }
    }
}

// ========================================
// HELPER COMPOSABLES
// ========================================

@Composable
private fun SessionHeader(
    sessionType: ExperimentSessionType,
    participantId: String,
    progress: Float,
    currentTask: ExperimentTaskType,
    onBack: () -> Unit
) {
    val (typeLabel, typeColor) = when (sessionType) {
        ExperimentSessionType.ENROLLMENT -> "ENROLLMENT" to Emerald
        ExperimentSessionType.GENUINE -> "GENUINE" to EmeraldBright
        ExperimentSessionType.IMPOSTOR -> "IMPOSTOR" to Coral
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = CloudWhite)
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = typeColor.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = typeLabel,
                    color = typeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Text(text = participantId, color = MutedGray, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Task indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ExperimentTaskType.entries.forEach { task ->
                val isActive = task == currentTask
                val isComplete = task.ordinal < currentTask.ordinal
                val taskLabel = when (task) {
                    ExperimentTaskType.PIN_ENTRY -> "PIN"
                    ExperimentTaskType.TEXT_TYPING -> "Type"
                    ExperimentTaskType.TOUCH_INTERACTION -> "Touch"
                    ExperimentTaskType.FREE_BROWSING -> "Free"
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = taskLabel,
                        color = when {
                            isActive -> Emerald
                            isComplete -> EmeraldBright
                            else -> DimGray
                        },
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .height(3.dp)
                            .width(60.dp)
                            .background(
                                when {
                                    isActive -> Emerald
                                    isComplete -> EmeraldBright.copy(alpha = 0.5f)
                                    else -> ObsidianBorder
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionHeader(title: String, count: Int, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = CloudWhite,
                fontSize = 18.sp
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Badge(containerColor = Emerald) {
                    Text(count.toString())
                }
            }
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.PersonAdd, "Add", tint = Emerald)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParticipantCard(
    participant: Participant,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EmeraldMuted.copy(alpha = 0.4f) else ObsidianSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (participant.enrollmentComplete)
                        Icons.Default.CheckCircle else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (participant.enrollmentComplete) EmeraldBright else MutedGray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${participant.participantId}: ${participant.name}",
                        color = CloudWhite,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${participant.sessionCount} sessions" +
                                if (participant.enrollmentComplete) " • Enrolled" else " • Not enrolled",
                        color = MutedGray,
                        fontSize = 12.sp
                    )
                }
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = Emerald)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionButton(
    text: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) color.copy(alpha = 0.15f) else ObsidianSurface.copy(alpha = 0.5f),
            disabledContainerColor = ObsidianSurface.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) color else DimGray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = text,
                    color = if (enabled) CloudWhite else DimGray,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = if (enabled) MutedGray else DimGray.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            color = MutedGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddParticipantDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Participant", color = CloudWhite) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Participant Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedLabelColor = Emerald,
                    unfocusedLabelColor = MutedGray,
                    cursorColor = Emerald,
                    focusedTextColor = CloudWhite,
                    unfocusedTextColor = CloudGray
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onAdd(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Add", color = Emerald)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedGray)
            }
        },
        containerColor = ObsidianLight
    )
}

@Composable
private fun ImpostorTargetDialog(
    participants: List<Participant>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Target Profile", color = CloudWhite) },
        text = {
            if (participants.isEmpty()) {
                Text(
                    "No other enrolled participants available.\nEnroll at least 2 participants first.",
                    color = MutedGray
                )
            } else {
                Column {
                    Text(
                        "Select whose profile this impostor will try to mimic:",
                        color = CloudGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    participants.forEach { p ->
                        TextButton(
                            onClick = { onSelect(p.participantId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${p.participantId}: ${p.name}",
                                color = Coral
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedGray)
            }
        },
        containerColor = ObsidianLight
    )
}
