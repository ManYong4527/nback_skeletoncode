package mobappdev.example.nback_cimpl.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

/**
 * Placeholder GameScreen — Step 3 will replace it with the Layout A
 * grid + match buttons. Kept barebones so Step 2's navigation wiring
 * can be reviewed in isolation.
 *
 * Mid-round exit (SPEC: "round timer is cancelled cleanly when the user
 * navigates away mid-round") is enforced by:
 *  - [BackHandler] on gesture/button back,
 *  - the explicit "Back to Home" button,
 *  - and `onCleared` in [mobappdev.example.nback_cimpl.ui.viewmodels.GameVM]
 *    as a final safety net when the VM scope dies.
 *
 * `DisposableEffect` is deliberately NOT used here: it would also fire on
 * configuration change (rotation), which must not abort the round once
 * responsive layout lands in a later phase.
 */
@Composable
fun GameScreen(
    vm: GameViewModel,
    onBack: () -> Unit = {},
) {
    val state by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()

    val leaveScreen = {
        vm.abortRound()
        onBack()
    }
    BackHandler(onBack = leaveScreen)

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Game Screen (placeholder)", style = MaterialTheme.typography.titleLarge)
            Text("Round: ${state.roundIndex + 1} / ${state.totalRounds}")
            Text("Current event: ${state.currentEvent}")
            Text("Hits: ${state.hits}   Misses: ${state.misses}   FA: ${state.falseAlarms}")
            Text("Score: $score", style = MaterialTheme.typography.titleMedium)

            Button(onClick = vm::startGame, enabled = !state.isRoundActive) {
                Text("BEGIN ROUND")
            }
            Button(
                onClick = vm::registerVisualMatch,
                enabled = state.isRoundActive && !state.isWarmup,
            ) {
                Text("VISUAL MATCH")
            }
            Button(onClick = leaveScreen) {
                Text("Back to Home")
            }
        }
    }
}

@Preview
@Composable
private fun GameScreenPreview() {
    GameScreen(vm = FakeVM())
}
