package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DetoxSession
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetoxDashboard(
    viewModel: DetoxViewModel,
    modifier: Modifier = Modifier
) {
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val targetSeconds by viewModel.targetSeconds.collectAsStateWithLifecycle()
    val selectedSessionType by viewModel.sessionType.collectAsStateWithLifecycle()
    val currentQuote by viewModel.currentQuote.collectAsStateWithLifecycle()

    val sessions by viewModel.sessionsState.collectAsStateWithLifecycle()
    val streak by viewModel.streakState.collectAsStateWithLifecycle()
    val totalHoursSaved by viewModel.totalTimeSavedState.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showChallengeLogDialog by remember { mutableStateOf<ChallengePreset?>(null) }

    // Display finished/save modal
    val showSaveDialog = timerState is TimerState.Finished

    // Format remaining time nicely
    val minutesLeft = remainingSeconds / 60
    val secondsLeft = remainingSeconds % 60
    val timeDisplay = String.format("%02d:%02d", minutesLeft, secondsLeft)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "InstaDetox",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showClearHistoryDialog = true },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All Detox History",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("dashboard_scroll_container"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // STATS ROW SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // STREAK CARD
                    StatCard(
                        title = "Detox Streak",
                        value = "$streak ${if (streak == 1) "Day" else "Days"}",
                        icon = Icons.Default.LocalFireDepartment,
                        iconColor = Color(0xFFF97316),
                        modifier = Modifier.weight(1f)
                    )

                    // HOURS SAVED CARD
                    StatCard(
                        title = "Focus Restored",
                        value = if (totalHoursSaved < 0.1) String.format("%.2f hrs", totalHoursSaved) else String.format("%.1f hrs", totalHoursSaved),
                        icon = Icons.Default.HourglassEmpty,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // PRIMARY COUNTDOWN TIMER CANVAS
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = if (targetSeconds > 0) {
                        remainingSeconds.toFloat() / targetSeconds.toFloat()
                    } else {
                        1f
                    }

                    // Elegant circular track
                    val traceColor = MaterialTheme.colorScheme.surfaceVariant
                    val strokeColor = MaterialTheme.colorScheme.primary

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Circular track
                            drawCircle(
                                color = traceColor,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Clean gradient progress indicator
                            drawArc(
                                color = strokeColor,
                                startAngle = -90f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Centered countdown contents
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = selectedSessionType.uppercase(Locale.getDefault()),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = timeDisplay,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (timerState) {
                                    is TimerState.Idle -> "Ready to disconnect"
                                    is TimerState.Running -> "Focus active..."
                                    is TimerState.Paused -> "Fasting paused"
                                    is TimerState.Finished -> "Done"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // TIMER CONTROLS Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (timerState) {
                        is TimerState.Idle -> {
                            Button(
                                onClick = { viewModel.startTimer() },
                                modifier = Modifier
                                    .testTag("start_button")
                                    .padding(horizontal = 8.dp)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start Timer")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unplug", fontWeight = FontWeight.Bold)
                            }
                        }
                        is TimerState.Running -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { viewModel.pauseTimer() },
                                    modifier = Modifier
                                        .testTag("pause_button")
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause Timer")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.resetTimer() },
                                    modifier = Modifier
                                        .testTag("reset_button")
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset Timer")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is TimerState.Paused -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { viewModel.startTimer() },
                                    modifier = Modifier
                                        .testTag("resume_button")
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume Timer")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.resetTimer() },
                                    modifier = Modifier
                                        .testTag("reset_button")
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset Timer")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is TimerState.Finished -> {
                            // Handled by popup automatically
                        }
                    }
                }
            }

            // INTERVAL PRESETS CHIPS (Scrollable when Idle)
            if (timerState is TimerState.Idle) {
                item {
                    Column {
                        Text(
                            text = "Set Fast Duration",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val presets = listOf(
                                TimerPreset("15m", 900L, "Quick Pause"),
                                TimerPreset("45m", 2700L, "Study Shield"),
                                TimerPreset("90m", 5400L, "Dinner Peace"),
                                TimerPreset("3h", 10800L, "Deep Break")
                            )

                            presets.forEach { preset ->
                                val selected = targetSeconds == preset.seconds
                                val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.setTimerDuration(preset.seconds, preset.type)
                                        }
                                        .testTag("preset_chip_${preset.label}"),
                                    colors = CardDefaults.cardColors(containerColor = containerColor),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = preset.label,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = preset.type,
                                            color = textColor.copy(alpha = 0.8f),
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ROTATING CONSCIOUS QUOTE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Mindful Shield Insights",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (currentQuote.isEmpty()) "Take a deep breath and connect with the world around you." else "“$currentQuote”",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ROTATE MINDFUL INSIGHT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .clickable { viewModel.rotateQuote() }
                                .padding(vertical = 4.dp, horizontal = 12.dp)
                                .testTag("rotate_quote_button")
                        )
                    }
                }
            }

            // MINDFUL INTERACTIVE MICRO-CHALLENGES (Replacement Activities)
            item {
                Column {
                    Text(
                        text = "Social Alternative Actions",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Text(
                        text = "Reclaim immediate focus. When you feel the craving to scroll Instagram, complete one of these micro-actions instead:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val challenges = listOf(
                        ChallengePreset("Morning Sanctuary", "morning_sanctuary", "1 hr without screens after waking up.", 60L),
                        ChallengePreset("Nature Walk", "nature_walk", "Step outside, observe the tree/sky for 10 min.", 10L),
                        ChallengePreset("Real Sync", "real_sync", "Send a real SMS or call a friend without scrolling.", 5L),
                        ChallengePreset("Book Breakaway", "book_breakaway", "Read 5 full physical pages of text.", 15L),
                        ChallengePreset("Placid Meal", "placid_meal", "Indulge in a beautiful dinner completely phone-free.", 30L)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        challenges.forEach { challenge ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showChallengeLogDialog = challenge }
                                    .testTag("challenge_item_${challenge.tag}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = challenge.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = challenge.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Log Completed Challenge",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // COMPLETED EXPERIENCES FEED (HISTORY LIST)
            item {
                Text(
                    text = "Your Mindful Safehouse Logs",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (sessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = "Quiet Empty State",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Your Offline Logs Are Empty",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Log a deep fast session or click any replacement action above to start recording screen-free victories.",
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    HistoryLogItem(
                        session = session,
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }
        }
    }

    // SAVE COMPLETED SESSION DIALOG (From Timer)
    if (showSaveDialog) {
        var reflection by remember { mutableStateOf("") }
        var selectedMood by remember { mutableStateOf("Calm") }

        AlertDialog(
            onDismissRequest = { /* Force reflection to maintain state save */ },
            title = {
                Text(
                    text = "Detox Solitude Complete 🎉",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "You completed your screen-free interval! Take a second to save how you feel offline:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Mood selector
                    Text(
                        text = "What is your mood right now?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val moods = listOf("Calm" to "🧘", "Refreshed" to "🌿", "Productive" to "⚡", "Grateful" to "❤️")
                        moods.forEach { (moodName, emoji) ->
                            val active = selectedMood == moodName
                            val background = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val colorText = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(background)
                                    .clickable { selectedMood = moodName }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(emoji, fontSize = 18.sp)
                                    Text(moodName, fontSize = 9.sp, color = colorText, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Reflection Text Field
                    OutlinedTextField(
                        value = reflection,
                        onValueChange = { reflection = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("reflection_input"),
                        placeholder = { Text("What did you do with your offline time? (e.g. cooked, read, took a calm breath...)", fontSize = 13.sp) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reflectionText.value = reflection
                        viewModel.moodRating.value = selectedMood
                        viewModel.saveCompletedSession()
                    },
                    modifier = Modifier.testTag("save_session_button")
                ) {
                    Text("Save Log", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // INSTANT CHALLENGE SAVE DIALOG
    showChallengeLogDialog?.let { challenge ->
        var challengeReflection by remember { mutableStateOf("") }
        var challengeMood by remember { mutableStateOf("Refreshed") }

        AlertDialog(
            onDismissRequest = { showChallengeLogDialog = null },
            title = {
                Text(
                    text = "Complete: ${challenge.name}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Nice! You completed the challenge. Add a brief note of your real-world progress:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Mood selector
                    Text(
                        text = "Your current state:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val moods = listOf("Refreshed" to "🌿", "Calm" to "🧘", "Productive" to "⚡", "Grateful" to "❤️")
                        moods.forEach { (moodName, emoji) ->
                            val active = challengeMood == moodName
                            val background = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val colorText = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(background)
                                    .clickable { challengeMood = moodName }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(emoji, fontSize = 18.sp)
                                    Text(moodName, fontSize = 9.sp, color = colorText, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = challengeReflection,
                        onValueChange = { challengeReflection = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .testTag("challenge_reflection_input"),
                        placeholder = { Text("What happened? (e.g. Saw birds, spoke to roommate, drank tea...)") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reflectionValue = challengeReflection.trim().ifEmpty { "Completed: ${challenge.name}" }
                        viewModel.saveInstantChallenge(
                            challengeName = challenge.name,
                            durationMins = challenge.durationMins,
                            mood = challengeMood,
                            reflection = reflectionValue
                        )
                        showChallengeLogDialog = null
                    },
                    modifier = Modifier.testTag("save_challenge_button")
                ) {
                    Text("Confirm Complete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChallengeLogDialog = null },
                    modifier = Modifier.testTag("cancel_challenge_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // CLEAR SYSTEM DETOX HISTORY CONFIRM DIALOG
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    text = "Clear All Detox Data?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text("This action will erase your entire screen-free history log, resetting your current streak and total hours saved count. This is irreversible.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearHistoryDialog = false },
                    modifier = Modifier.testTag("dismiss_clear_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
fun HistoryLogItem(
    session: DetoxSession,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(session.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(session.timestamp))
    }

    val moodEmoji = when (session.moodRating) {
        "Calm" -> "🧘"
        "Refreshed" -> "🌿"
        "Productive" -> "⚡"
        "Grateful" -> "❤️"
        else -> "✨"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("history_item_${session.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mood bubble
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = moodEmoji,
                    fontSize = 22.sp,
                    modifier = Modifier.testTag("mood_emoji_${session.id}")
                )
            }

            // Central details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = session.sessionType,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDuration(session.durationSeconds),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.reflectionText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = session.reflectionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.testTag("reflection_text_${session.id}")
                    )
                }
            }

            // Quick deletion trigger
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_item_button_${session.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Log Record",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hrs > 0 -> "${hrs}h ${mins}m"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}

data class TimerPreset(
    val label: String,
    val seconds: Long,
    val type: String
)

data class ChallengePreset(
    val name: String,
    val tag: String,
    val description: String,
    val durationMins: Long
)
