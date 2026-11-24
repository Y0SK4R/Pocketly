package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.domain.SupportedCurrencies
import com.example.ui.components.InfoBadgePill
import com.example.ui.components.LocalPocketlyFeedback
import com.example.ui.components.MascotMood
import com.example.ui.components.PocketlyCard
import com.example.ui.components.PocketlyMascot
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 3-step first-launch Onboarding:
 * 1. Welcome with sleepy coin-pouch mascot & offline privacy promise
 * 2. Pick your currency
 * 3. Create a PIN (confirm twice) + optional Fingerprint, or Skip for PIN-less mode
 */
@Composable
fun OnboardingScreen(
    biometricAvailable: Boolean,
    onComplete: (currency: String, pin: String?, biometric: Boolean) -> Unit
) {
    val colors = PocketlyTheme.colors
    var step by remember { mutableIntStateOf(0) }
    var selectedCurrency by remember { mutableStateOf("USD") }
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStage by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var offerBiometricStep by remember { mutableStateOf(false) }
    var enableBiometric by remember { mutableStateOf(biometricAvailable) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (step == 2) {
                    Brush.verticalGradient(listOf(colors.lockWash.copy(alpha = 0.55f), colors.lockBg, colors.lockBg))
                } else {
                    Brush.verticalGradient(listOf(colors.bg, colors.bg))
                }
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicator pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                repeat(3) { idx ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (idx == step) 28.dp else 10.dp)
                            .clip(CircleShape)
                            .background(if (idx <= step) colors.mint else colors.line)
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "onboarding_step",
                modifier = Modifier.weight(1f)
            ) { currentStep ->
                when (currentStep) {
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            PocketlyMascot(
                                mood = MascotMood.SLEEPY,
                                modifier = Modifier.size(168.dp)
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "Meet Pocketly",
                                style = PocketlyTextStyles.screenTitle,
                                color = colors.ink,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Log a purchase in seconds, see your budget pace clearly, and watch your savings grow.",
                                style = PocketlyTextStyles.body,
                                color = colors.inkMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            InfoBadgePill(
                                text = "Local data only · Your data never leaves this phone",
                                icon = Icons.Rounded.Lock
                            )
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { step = 1 },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.mint, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("onboarding_continue_welcome")
                            ) {
                                Text("Get started", style = PocketlyTextStyles.button)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    1 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Pick your currency",
                                style = PocketlyTextStyles.screenTitle,
                                color = colors.ink
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "You can change this anytime in Profile.",
                                style = PocketlyTextStyles.body,
                                color = colors.inkMuted
                            )
                            Spacer(Modifier.height(16.dp))
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(SupportedCurrencies.all) { cur ->
                                    val selected = cur.code == selectedCurrency
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (selected) colors.mintSoft else colors.surface)
                                            .border(
                                                width = if (selected) 1.5.dp else if (colors.isDark) 1.dp else 0.dp,
                                                color = if (selected) colors.mint else colors.line,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .clickable { selectedCurrency = cur.code }
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                            .testTag("currency_option_${cur.code}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) colors.mint else colors.surface2),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cur.symbol.trim(),
                                                style = PocketlyTextStyles.rowTitle,
                                                color = if (selected) Color.White else colors.ink
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cur.name, style = PocketlyTextStyles.rowTitle, color = colors.ink)
                                            Text(cur.code, style = PocketlyTextStyles.caption, color = colors.inkMuted)
                                        }
                                        if (selected) {
                                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = colors.mint)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { step = 2 },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.mint, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("onboarding_continue_currency")
                            ) {
                                Text("Continue", style = PocketlyTextStyles.button)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    else -> {
                        if (offerBiometricStep) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                PocketlyMascot(mood = MascotMood.PERK, modifier = Modifier.size(120.dp))
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = "Use fingerprint to unlock?",
                                    style = PocketlyTextStyles.screenTitle,
                                    color = colors.ink,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Your PIN stays ready as a backup anytime.",
                                    style = PocketlyTextStyles.body,
                                    color = colors.inkMuted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(24.dp))
                                PocketlyCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = colors.mint)
                                            Text("Fingerprint unlock", style = PocketlyTextStyles.rowTitle, color = colors.ink)
                                        }
                                        Switch(
                                            checked = enableBiometric,
                                            onCheckedChange = { enableBiometric = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.mint)
                                        )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = { onComplete(selectedCurrency, firstPin, enableBiometric) },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.mint, contentColor = Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("onboarding_finish_pin")
                                ) {
                                    Text("Finish setup", style = PocketlyTextStyles.button)
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        } else {
                            val currentDigits = if (isConfirmStage) confirmPin else firstPin
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { onComplete(selectedCurrency, null, false) },
                                        modifier = Modifier.testTag("onboarding_skip_pin")
                                    ) {
                                        Text("Skip", style = PocketlyTextStyles.button, color = colors.inkMuted)
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (isConfirmStage) "Confirm your PIN" else "Create a PIN",
                                    style = PocketlyTextStyles.screenTitle,
                                    color = colors.ink
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = pinError ?: "Optional 4-digit passcode to keep Pocketly private.",
                                    style = PocketlyTextStyles.label,
                                    color = if (pinError != null) colors.coral else colors.inkMuted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(24.dp))
                                PinDotsRow(length = currentDigits.length, shakeTrigger = if (pinError != null) 1 else 0)
                                Spacer(Modifier.weight(1f))
                                PinKeypadGrid(
                                    showFingerprint = false,
                                    enabled = true,
                                    onDigit = { d ->
                                        pinError = null
                                        if (!isConfirmStage) {
                                            if (firstPin.length < 4) {
                                                firstPin += d
                                                if (firstPin.length == 4) isConfirmStage = true
                                            }
                                        } else {
                                            if (confirmPin.length < 4) {
                                                confirmPin += d
                                                if (confirmPin.length == 4) {
                                                    if (confirmPin == firstPin) {
                                                        if (biometricAvailable) {
                                                            offerBiometricStep = true
                                                        } else {
                                                            onComplete(selectedCurrency, firstPin, false)
                                                        }
                                                    } else {
                                                        pinError = "That PIN doesn't match. Try again."
                                                        confirmPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onBackspace = {
                                        pinError = null
                                        if (isConfirmStage) {
                                            if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                                            else isConfirmStage = false
                                        } else if (firstPin.isNotEmpty()) {
                                            firstPin = firstPin.dropLast(1)
                                        }
                                    },
                                    onFingerprint = {}
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pocketly Lock Screen:
 * Warm cream canvas with peach top wash (dark bronze in dark mode), 4 animated dots,
 * 3x4 soft rounded keypad, fingerprint trigger, escalating cooldown after 5 wrong attempts,
 * and the sleepy coin-pouch mascot resting in the bottom-right corner.
 */
@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    lockoutUntilEpochMs: Long,
    onVerifyPin: suspend (String) -> Pair<Boolean, Long>,
    onTriggerBiometric: () -> Unit,
    onUnlocked: () -> Unit
) {
    val colors = PocketlyTheme.colors
    val scope = rememberCoroutineScope()
    val feedback = LocalPocketlyFeedback.current

    var digits by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf(MascotMood.SLEEPY) }
    var shakeCount by remember { mutableIntStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var remainingCooldownSec by remember { mutableLongStateOf(0L) }
    var dynamicLockoutUntil by remember(lockoutUntilEpochMs) { mutableLongStateOf(lockoutUntilEpochMs) }

    // Trigger biometric automatically once on open if enabled
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            delay(250L)
            onTriggerBiometric()
        }
    }

    // Live cooldown ticker if locked out after 5 wrong attempts
    LaunchedEffect(dynamicLockoutUntil) {
        while (true) {
            val now = System.currentTimeMillis()
            val rem = ((dynamicLockoutUntil - now) / 1000L).coerceAtLeast(0L)
            remainingCooldownSec = rem
            if (rem <= 0L) break
            delay(500L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.lockWash.copy(alpha = 0.65f), colors.lockBg, colors.lockBg)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = "Enter your PIN",
                style = PocketlyTextStyles.screenTitle,
                color = colors.ink
            )
            Spacer(Modifier.height(8.dp))
            val subtitle = when {
                remainingCooldownSec > 0L -> "Too many attempts. Try again in ${remainingCooldownSec}s."
                errorMsg != null -> errorMsg!!
                else -> "Unlock Pocketly"
            }
            Text(
                text = subtitle,
                style = PocketlyTextStyles.label,
                color = if (remainingCooldownSec > 0L || errorMsg != null) colors.coral else colors.inkMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            PinDotsRow(length = digits.length, shakeTrigger = shakeCount)

            Spacer(Modifier.weight(1f))

            PinKeypadGrid(
                showFingerprint = biometricEnabled,
                enabled = remainingCooldownSec <= 0L,
                onDigit = { d ->
                    if (digits.length >= 4 || remainingCooldownSec > 0L) return@PinKeypadGrid
                    errorMsg = null
                    digits += d
                    if (digits.length == 4) {
                        val candidate = digits
                        scope.launch {
                            val (ok, cooldownMs) = onVerifyPin(candidate)
                            if (ok) {
                                feedback.confirm()
                                mood = MascotMood.PERK
                                delay(220L)
                                onUnlocked()
                            } else {
                                mood = MascotMood.OOPS
                                shakeCount++
                                feedback.deleteOrReject()
                                if (cooldownMs > 0L) {
                                    dynamicLockoutUntil = System.currentTimeMillis() + cooldownMs
                                } else {
                                    errorMsg = "That PIN doesn't match. Try again."
                                }
                                delay(360L)
                                digits = ""
                                delay(900L)
                                mood = MascotMood.SLEEPY
                            }
                        }
                    }
                },
                onBackspace = {
                    if (digits.isNotEmpty()) digits = digits.dropLast(1)
                },
                onFingerprint = onTriggerBiometric
            )

            Spacer(Modifier.height(10.dp))

            // Sleepy coin-pouch mascot in bottom-right corner above gesture area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PocketlyMascot(
                    mood = mood,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }
}

@Composable
fun PinDotsRow(
    length: Int,
    shakeTrigger: Int,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0 && !reduceMotion) {
            val offsets = listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)
            offsets.forEach { target ->
                shakeOffset.animateTo(target, tween(45))
            }
        }
    }

    Row(
        modifier = modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { idx ->
            val filled = idx < length
            val scale by animateFloatAsState(
                targetValue = if (filled && !reduceMotion) 1.18f else 1f,
                animationSpec = spring(stiffness = 520f, dampingRatio = 0.55f),
                label = "pin_dot_$idx"
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (filled) colors.mint else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (filled) colors.mint else colors.inkMuted.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun PinKeypadGrid(
    showFingerprint: Boolean,
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onFingerprint: () -> Unit
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surface.copy(alpha = if (colors.isDark) 0.85f else 0.92f))
                            .clickable(enabled = enabled) {
                                feedback.scrubTick()
                                onDigit(digit)
                            }
                            .testTag("pin_key_$digit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit,
                            style = PocketlyTextStyles.mediumNumber,
                            color = colors.ink
                        )
                    }
                }
            }
        }
        // Bottom row: Backspace, 0, Fingerprint
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface.copy(alpha = if (colors.isDark) 0.55f else 0.65f))
                    .clickable(enabled = enabled) {
                        feedback.scrubTick()
                        onBackspace()
                    }
                    .testTag("pin_key_backspace")
                    .semantics { contentDescription = "Backspace" },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Delete digit", tint = colors.ink)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface.copy(alpha = if (colors.isDark) 0.85f else 0.92f))
                    .clickable(enabled = enabled) {
                        feedback.scrubTick()
                        onDigit("0")
                    }
                    .testTag("pin_key_0"),
                contentAlignment = Alignment.Center
            ) {
                Text("0", style = PocketlyTextStyles.mediumNumber, color = colors.ink)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (showFingerprint) colors.mintSoft else Color.Transparent
                    )
                    .clickable(enabled = showFingerprint && enabled) {
                        feedback.tap()
                        onFingerprint()
                    }
                    .testTag("pin_key_fingerprint")
                    .semantics { contentDescription = "Unlock with fingerprint" },
                contentAlignment = Alignment.Center
            ) {
                if (showFingerprint) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = "Fingerprint unlock", tint = colors.mint, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
