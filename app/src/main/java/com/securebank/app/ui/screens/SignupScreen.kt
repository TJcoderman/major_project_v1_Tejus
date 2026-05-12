package com.securebank.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.securebank.app.data.model.PinKeystrokeEvent
import com.securebank.app.data.model.TouchType
import com.securebank.app.sensor.TouchDataCollector
import com.securebank.app.ui.components.CustomPinPad
import com.securebank.app.ui.theme.*
import com.securebank.app.ui.viewmodel.SignupViewModel
import com.securebank.app.ui.viewmodel.SignupViewModel.EnrollmentStep
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    viewModel: SignupViewModel = hiltViewModel(),
    touchDataCollector: TouchDataCollector,
    onSignupComplete: () -> Unit,
    onBack: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val signupSuccess by viewModel.signupSuccess.collectAsState()
    val stepProgress by viewModel.stepProgress.collectAsState()
    var showCancelEnrollmentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(signupSuccess) {
        if (signupSuccess) {
            kotlinx.coroutines.delay(1500)
            onSignupComplete()
        }
    }

    val handleBack: () -> Unit = {
        when (currentStep) {
            EnrollmentStep.FORM -> onBack()
            EnrollmentStep.SAVING,
            EnrollmentStep.COMPLETE -> Unit
            else -> {
                if (!viewModel.previousStep()) {
                    onBack()
                }
            }
        }
    }

    BackHandler(enabled = currentStep != EnrollmentStep.SAVING && currentStep != EnrollmentStep.COMPLETE) {
        handleBack()
    }

    if (showCancelEnrollmentDialog) {
        AlertDialog(
            onDismissRequest = { showCancelEnrollmentDialog = false },
            title = { Text("Cancel enrollment?") },
            text = { Text("Your account details will stay on this screen, but the behavioral samples collected so far will be discarded.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelEnrollmentDialog = false
                        viewModel.cancelEnrollment()
                    }
                ) {
                    Text("Cancel Enrollment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelEnrollmentDialog = false }) {
                    Text("Keep Going")
                }
            }
        )
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
                .imePadding()
                .padding(24.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = handleBack,
                    enabled = currentStep != EnrollmentStep.SAVING && currentStep != EnrollmentStep.COMPLETE
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = CloudWhite)
                }
                Text(
                    text = when (currentStep) {
                        EnrollmentStep.FORM -> "Create Account"
                        EnrollmentStep.COMPLETE -> "Welcome!"
                        else -> "Behavioral Enrollment"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CloudWhite,
                    modifier = Modifier.weight(1f)
                )
                if (currentStep != EnrollmentStep.FORM &&
                    currentStep != EnrollmentStep.SAVING &&
                    currentStep != EnrollmentStep.COMPLETE
                ) {
                    TextButton(onClick = { showCancelEnrollmentDialog = true }) {
                        Text("Cancel", color = Coral, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step indicator
            if (currentStep != EnrollmentStep.FORM && currentStep != EnrollmentStep.COMPLETE) {
                EnrollmentStepIndicator(currentStep)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Progress text
            if (stepProgress.isNotEmpty()) {
                Text(
                    text = stepProgress,
                    color = EmeraldBright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentStep) {
                    EnrollmentStep.FORM -> SignupForm(
                        viewModel = viewModel,
                        errorMessage = errorMessage,
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxSize()
                    )
                    EnrollmentStep.PIN_ENTRY -> PinEnrollmentStep(viewModel)
                    EnrollmentStep.TAP_TARGETS -> TapEnrollmentStep(viewModel, touchDataCollector)
                    EnrollmentStep.SWIPE_TEST -> SwipeEnrollmentStep(viewModel, touchDataCollector)
                    EnrollmentStep.HOLD_PHONE -> HoldPhoneStep(viewModel)
                    EnrollmentStep.SAVING -> SavingStep()
                    EnrollmentStep.COMPLETE -> CompleteStep()
                }
            }
        }
    }
}

@Composable
private fun EnrollmentStepIndicator(currentStep: EnrollmentStep) {
    val steps = listOf(
        EnrollmentStep.PIN_ENTRY to "PIN",
        EnrollmentStep.TAP_TARGETS to "Tap",
        EnrollmentStep.SWIPE_TEST to "Swipe",
        EnrollmentStep.HOLD_PHONE to "Hold"
    )
    val currentIndex = steps.indexOfFirst { it.first == currentStep }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, (_, label) ->
            val isActive = index <= currentIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Emerald else ObsidianSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentIndex) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("${index + 1}", color = if (isActive) Color.White else MutedGray, fontSize = 12.sp)
                    }
                }
                Text(label, color = if (isActive) CloudWhite else MutedGray, fontSize = 10.sp)
            }
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(if (index < currentIndex) Emerald else ObsidianBorder)
                )
            }
        }
    }
}

@Composable
private fun SignupForm(
    viewModel: SignupViewModel,
    errorMessage: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val fullName by viewModel.fullName.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val pin by viewModel.pin.collectAsState()
    val accountNumber by viewModel.accountNumber.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Account Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CloudWhite)

                    // Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { viewModel.onFullNameChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Outlined.Person, null, tint = MutedGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = signupFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Username
                    OutlinedTextField(
                        value = username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Outlined.AccountCircle, null, tint = MutedGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = signupFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password (min 6 chars)") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = MutedGray) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    "Toggle", tint = MutedGray
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = signupFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // PIN
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { viewModel.onPinChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("6-digit PIN") },
                        leadingIcon = { Icon(Icons.Outlined.Pin, null, tint = MutedGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = signupFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Account Number
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { viewModel.onAccountNumberChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Account Number") },
                        leadingIcon = { Icon(Icons.Outlined.CreditCard, null, tint = MutedGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.submitForm() }),
                        colors = signupFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Error
                    AnimatedVisibility(visible = errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Coral.copy(alpha = 0.15f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = Coral, modifier = Modifier.size(20.dp))
                            Text(errorMessage ?: "", color = Coral, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.submitForm() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue to Enrollment", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Info card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmeraldMuted.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Fingerprint, null, tint = Emerald, modifier = Modifier.size(24.dp))
                    Column {
                        Text("Behavioral Enrollment", fontWeight = FontWeight.SemiBold, color = CloudWhite, fontSize = 14.sp)
                        Text("After filling the form, you'll complete a short calibration to create your unique behavioral profile.", color = CloudGray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinEnrollmentStep(viewModel: SignupViewModel) {
    val currentPinInput by viewModel.currentPinInput.collectAsState()
    val pinAttemptNumber by viewModel.pinAttemptNumber.collectAsState()
    val pin by viewModel.pin.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Pin, null, tint = Emerald, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enter Your PIN", style = MaterialTheme.typography.titleMedium, color = CloudWhite, fontWeight = FontWeight.Bold)
        Text("Type the PIN you chose during signup", color = MutedGray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(32.dp))

        CustomPinPad(
            pinLength = 6,
            currentPin = currentPinInput,
            targetPin = pin,
            sessionId = "enrollment_pin",
            attemptNumber = pinAttemptNumber,
            onDigitPressed = { event -> viewModel.onPinDigitPressed(event) },
            onPinComplete = { enteredPin -> viewModel.onPinEntryComplete(enteredPin) },
            onBackspace = { viewModel.onPinBackspace() }
        )
    }
}

@Composable
private fun TapEnrollmentStep(viewModel: SignupViewModel, touchDataCollector: TouchDataCollector) {
    val tapCount by viewModel.tapCount.collectAsState()

    // Collect touch events from the collector and forward to viewModel
    LaunchedEffect(Unit) {
        touchDataCollector.touchEvents.collect { touchData ->
            viewModel.onEnrollmentTouchEvent(touchData)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.TouchApp, null, tint = Emerald, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Tap the Targets", style = MaterialTheme.typography.titleMedium, color = CloudWhite, fontWeight = FontWeight.Bold)
        Text("Tap naturally — we're learning your touch pattern", color = MutedGray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(32.dp))

        // Grid of tap targets
        val targetPositions = remember { listOf(0, 1, 2, 3, 4, 5, 6, 7, 8) }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..2) {
                        val idx = row * 3 + col
                        val isTapped = tapCount > idx
                        TapTarget(
                            isTapped = isTapped,
                            onTap = { offset ->
                                // Touch events come through the collector
                            },
                            touchDataCollector = touchDataCollector
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TapTarget(
    isTapped: Boolean,
    onTap: (Offset) -> Unit,
    touchDataCollector: TouchDataCollector
) {
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (isTapped) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "tapScale"
    )

    Box(
        modifier = Modifier
            .size((80 * scale).dp)
            .clip(CircleShape)
            .background(
                if (isTapped)
                    Brush.radialGradient(listOf(EmeraldDark, EmeraldMuted))
                else
                    Brush.radialGradient(listOf(ObsidianSurface, ObsidianBorder))
            )
            .border(2.dp, if (isTapped) Emerald else ObsidianBorder, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTap(offset)
                    touchDataCollector.onTouchStart(offset, 0.5f, 1f)
                    scope.launch {
                        touchDataCollector.onTouchEnd(offset, 0.5f, 1f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isTapped) {
            Icon(Icons.Default.Check, null, tint = EmeraldBright, modifier = Modifier.size(24.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MutedGray.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
private fun SwipeEnrollmentStep(viewModel: SignupViewModel, touchDataCollector: TouchDataCollector) {
    val swipeCount by viewModel.swipeCount.collectAsState()
    val scope = rememberCoroutineScope()
    var startPos by remember { mutableStateOf(Offset.Zero) }
    var lastPos by remember { mutableStateOf(Offset.Zero) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var lastDirection by remember { mutableStateOf("Ready") }

    val prompts = remember {
        listOf(
            SwipeDemoPrompt(
                title = "Approve a Safe Transfer",
                subtitle = "Swipe right with your natural speed",
                direction = "RIGHT",
                icon = Icons.Default.ArrowForward,
                color = Emerald
            ),
            SwipeDemoPrompt(
                title = "Reject a Suspicious Payee",
                subtitle = "Swipe left like you would dismiss a card",
                direction = "LEFT",
                icon = Icons.Default.ArrowBack,
                color = Coral
            ),
            SwipeDemoPrompt(
                title = "Reveal Security Insights",
                subtitle = "Swipe up to open the behavior trail",
                direction = "UP",
                icon = Icons.Default.KeyboardArrowUp,
                color = Gold
            ),
            SwipeDemoPrompt(
                title = "Archive a Low-Risk Alert",
                subtitle = "Swipe down with a relaxed gesture",
                direction = "DOWN",
                icon = Icons.Default.KeyboardArrowDown,
                color = CloudGray
            )
        )
    }
    val activePrompt = prompts[swipeCount.coerceAtMost(prompts.lastIndex)]

    LaunchedEffect(Unit) {
        touchDataCollector.touchEvents.collect { touchData ->
            viewModel.onEnrollmentTouchEvent(touchData)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Swipe, null, tint = Emerald, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Swipe Challenge",
                style = MaterialTheme.typography.titleMedium,
                color = CloudWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Move the card in each direction so we learn your speed, pressure proxy, and rhythm",
                color = MutedGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            prompts.forEachIndexed { index, prompt ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(6.dp)
                        .width(if (index == swipeCount.coerceAtMost(prompts.lastIndex)) 28.dp else 14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                index < swipeCount -> Emerald
                                index == swipeCount.coerceAtMost(prompts.lastIndex) -> prompt.color
                                else -> ObsidianBorder
                            }
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ObsidianSurface,
                            ObsidianSurface.copy(alpha = 0.72f)
                        )
                    )
                )
                .border(2.dp, ObsidianBorder, RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            startPos = offset
                            lastPos = offset
                            dragOffset = Offset.Zero
                            touchDataCollector.onTouchStart(offset, 0.62f, 1f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                            lastPos = change.position
                            touchDataCollector.onTouchMove(change.position)
                            lastDirection = describeSwipeDirection(dragOffset)
                        },
                        onDragEnd = {
                            val endPosition = lastPos.takeIf { it != Offset.Zero } ?: (startPos + dragOffset)
                            scope.launch {
                                touchDataCollector.onTouchEnd(endPosition, 0.62f, 1f)
                            }
                            dragOffset = Offset.Zero
                            lastDirection = "Captured"
                        },
                        onDragCancel = {
                            dragOffset = Offset.Zero
                            lastDirection = "Try again"
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Target: ${activePrompt.direction}",
                    color = activePrompt.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text("Last motion: $lastDirection", color = MutedGray, fontSize = 11.sp)
            }

            SwipeGhostCard(
                title = prompts[(swipeCount + 1).coerceAtMost(prompts.lastIndex)].title,
                color = ObsidianBorder,
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .offset(y = 18.dp)
            )

            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset {
                        IntOffset(
                            dragOffset.x.roundToInt(),
                            dragOffset.y.roundToInt()
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = ObsidianLight),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(activePrompt.color.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activePrompt.icon,
                                contentDescription = null,
                                tint = activePrompt.color,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                activePrompt.title,
                                color = CloudWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(activePrompt.subtitle, color = CloudGray, fontSize = 12.sp)
                        }
                    }

                    Divider(color = ObsidianBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SwipeMetric("Speed", "live")
                        SwipeMetric("Direction", activePrompt.direction.lowercase())
                        SwipeMetric("Sample", "${swipeCount + 1}/${prompts.size}")
                    }
                }
            }
        }

        Text(
            text = "Captured swipes: $swipeCount/${prompts.size}",
            color = EmeraldBright,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class SwipeDemoPrompt(
    val title: String,
    val subtitle: String,
    val direction: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun SwipeGhostCard(
    title: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = title,
            color = MutedGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(18.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun SwipeMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MutedGray, fontSize = 10.sp)
        Text(value, color = CloudWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun describeSwipeDirection(offset: Offset): String {
    val absX = kotlin.math.abs(offset.x)
    val absY = kotlin.math.abs(offset.y)
    if (absX < 24f && absY < 24f) return "Starting"

    return if (absX > absY) {
        if (offset.x > 0f) "right" else "left"
    } else {
        if (offset.y > 0f) "down" else "up"
    }
}

@Composable
private fun HoldPhoneStep(viewModel: SignupViewModel) {
    val secondsRemaining by viewModel.holdSecondsRemaining.collectAsState()
    val holdComplete by viewModel.holdComplete.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Emerald.copy(alpha = if (holdComplete) 0.6f else pulseAlpha),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (holdComplete) {
                Icon(Icons.Default.CheckCircle, null, tint = EmeraldBright, modifier = Modifier.size(80.dp))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhoneAndroid, null, tint = CloudWhite, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${secondsRemaining}s", color = CloudWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (holdComplete) "Motion captured!" else "Hold your phone naturally",
            style = MaterialTheme.typography.titleMedium,
            color = CloudWhite, fontWeight = FontWeight.Bold
        )
        Text(
            text = if (holdComplete) "Saving your behavioral profile..." else "We're capturing your natural holding posture",
            color = MutedGray, fontSize = 13.sp
        )
    }
}

@Composable
private fun SavingStep() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Emerald, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Creating your account...", color = CloudWhite, fontSize = 16.sp)
        }
    }
}

@Composable
private fun CompleteStep() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, tint = EmeraldBright, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Account Created!", style = MaterialTheme.typography.headlineSmall, color = CloudWhite, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Your behavioral profile has been saved.", color = MutedGray, fontSize = 14.sp)
            Text("Redirecting to login...", color = EmeraldBright, fontSize = 14.sp)
        }
    }
}

@Composable
private fun signupFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Emerald,
    unfocusedBorderColor = ObsidianBorder,
    focusedLabelColor = Emerald,
    unfocusedLabelColor = MutedGray,
    cursorColor = Emerald,
    focusedTextColor = CloudWhite,
    unfocusedTextColor = CloudGray
)
