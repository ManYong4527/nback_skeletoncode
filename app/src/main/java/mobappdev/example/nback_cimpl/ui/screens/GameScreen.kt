package mobappdev.example.nback_cimpl.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameState
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.PendingSave

/**
 * Layout A — Grid-first Dual N-Back game surface.
 *
 * Mandatory SPEC items covered here:
 *  - 3×3 visual grid with one cell highlighted per tick (1:1 aspect).
 *  - Live state readout: event number, correct responses, score.
 *  - Match buttons with ≥48dp touch target (we use 64dp for comfort).
 *  - Non-offensive FA feedback: button container color pulses to error
 *    for ~180 ms, driven by [GameState.visualFalseAlarmPulse] /
 *    [GameState.audioFalseAlarmPulse] monotonic counters.
 *  - Warmup window visually flagged and match buttons disabled.
 *  - Round timer cancelled when the screen is left mid-round: see the
 *    [BackHandler] and the explicit "Back to Home" text button, both of
 *    which route through [leaveScreen] → `vm.abortRound()`.
 *
 * Audio modality is deliberately shown but disabled this phase — the
 * UI information architecture is final; only playback + scoring wait
 * for Path 2.
 */
@Composable
fun GameScreen(
    vm: GameViewModel,
    onBack: () -> Unit = {},
) {
    val state by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()
    val settings by vm.settings.collectAsState()
    val pendingSave by vm.pendingSave.collectAsState()

    val leaveScreen = {
        vm.abortRound()
        onBack()
    }
    BackHandler(onBack = leaveScreen)

    pendingSave?.let { pending ->
        SaveRoundDialog(
            pending = pending,
            onConfirm = { name -> vm.commitPendingSave(name) },
            onDismiss = { vm.discardPendingSave() },
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = leaveScreen) { Text("← Back") }
            }

            LiveStatsStrip(state = state, score = score, nBack = settings.nBack)
            WarmupBanner(visible = state.isWarmup && state.isRoundActive)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                VisualGrid(state = state, gridSize = settings.gridSize)
            }

            BottomActionBar(
                state = state,
                onBegin = vm::startGame,
                onVisualMatch = vm::registerVisualMatch,
                onAudioMatch = vm::registerAudioMatch,
            )
        }
    }
}

/** Top strip: `N | Event i/total | Correct | Score`. Satisfies SPEC live-state readout. */
@Composable
private fun LiveStatsStrip(state: GameState, score: Int, nBack: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatItem(label = "N", value = nBack.toString())
        val eventLabel =
            if (state.totalRounds == 0) "—"
            else "${state.roundIndex + 1}/${state.totalRounds}"
        StatItem(label = "Event", value = eventLabel)
        StatItem(label = "Correct", value = state.hits.toString())
        StatItem(label = "Score", value = score.toString())
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun WarmupBanner(visible: Boolean) {
    if (!visible) return
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Warm-up • first n events — no matches possible yet",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** M×M grid, 1:1 aspect, highlights the cell whose index == current visualIndex. */
@Composable
private fun VisualGrid(state: GameState, gridSize: Int) {
    val side = gridSize
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (row in 0 until side) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0 until side) {
                    val index = row * side + col
                    val lit = state.currentEvent?.visualIndex == index
                    GridCell(
                        isLit = lit,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GridCell(isLit: Boolean, modifier: Modifier = Modifier) {
    // Fix A: off-state uses the surface background so the gap between
    // ticks reads as *empty* rather than merely a different tint. The
    // persistent outlineVariant border keeps the grid structure visible
    // when no cell is lit.
    val targetColor =
        if (isLit) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 120),
        label = "cellColor",
    )

    // Fix B: onset pulse. Every false → true transition briefly dips the
    // cell to 0.88 and springs back to 1.0, so two consecutive stimuli on
    // the SAME cell can be told apart even when the dim gap is short.
    var scaleTarget by remember { mutableStateOf(1f) }
    LaunchedEffect(isLit) {
        if (isLit) {
            scaleTarget = 0.88f
            delay(60L)
            scaleTarget = 1f
        }
    }
    val cellScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = tween(durationMillis = 120),
        label = "cellScale",
    )

    val borderColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .scale(cellScale)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
    )
}

/** When a round is active: AUDIO + VISUAL match buttons. Otherwise: BEGIN ROUND. */
@Composable
private fun BottomActionBar(
    state: GameState,
    onBegin: () -> Unit,
    onVisualMatch: () -> Unit,
    onAudioMatch: () -> Unit,
) {
    if (!state.isRoundActive) {
        Button(
            onClick = onBegin,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
        ) {
            Text("BEGIN ROUND", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val inputsEnabled = !state.isWarmup

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MatchButton(
            label = "AUDIO",
            subtitle = "soon",
            pulseKey = state.audioFalseAlarmPulse,
            enabled = false, // Path 1: audio deferred
            onClick = onAudioMatch,
            modifier = Modifier.weight(1f),
        )
        MatchButton(
            label = "VISUAL",
            subtitle = null,
            pulseKey = state.visualFalseAlarmPulse,
            enabled = inputsEnabled,
            onClick = onVisualMatch,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Match button that briefly pulses to the error color on an incorrect
 * tap. [pulseKey] is a monotonic counter from the VM — every increment
 * replays the 180 ms animation without the VM owning animation state.
 */
@Composable
private fun MatchButton(
    label: String,
    subtitle: String?,
    pulseKey: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPulse by remember { mutableStateOf(false) }
    LaunchedEffect(pulseKey) {
        if (pulseKey > 0) {
            showPulse = true
            delay(180L)
            showPulse = false
        }
    }

    val containerTarget = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        showPulse -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val container by animateColorAsState(
        targetValue = containerTarget,
        animationSpec = tween(durationMillis = 180),
        label = "matchBtn",
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.heightIn(min = 64.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Round-end name-entry prompt. SPEC §3.2: "Player inputs name upon
 * game completion."
 *
 * "Skip" discards without writing to Room — equivalent to anonymous
 * play; "Save" writes a trimmed name (defaults to "Anonymous" on empty
 * input inside the VM, so an eager tap still produces a visible row).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveRoundDialog(
    pending: PendingSave,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Round complete") },
        text = {
            Column {
                Text(
                    "Score: ${pending.score}   •   N = ${pending.nValue}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Skip") }
        },
    )
}

@Preview
@Composable
private fun GameScreenPreview() {
    GameScreen(vm = FakeVM())
}
