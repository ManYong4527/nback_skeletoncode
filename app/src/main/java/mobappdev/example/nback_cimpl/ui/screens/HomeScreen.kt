package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

/**
 * Home screen.
 *
 * Mandatory SPEC items covered here:
 *  - High-score readout (DataStore-backed; survives process death).
 *  - Stimulus-type picker — Visual / Audio / Dual. Audio and Dual are
 *    shown but disabled this phase; the information architecture is
 *    final so adding TTS later won't shuffle the layout.
 *  - Current settings readout: N value, event duration, events per
 *    round. Sourced from the ViewModel (hard-coded this phase, moves to
 *    DataStore when the Settings screen lands).
 *  - START ROUND button routes to the Game screen via [onStartClicked].
 *
 * Settings and History text buttons are placeholders — they surface a
 * "available in next phase" snackbar so the IA for the advanced features
 * is visible without the screens themselves existing yet.
 */
@Composable
fun HomeScreen(
    vm: GameViewModel,
    onStartClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    onHistoryClicked: () -> Unit = {},
) {
    val highscore by vm.highscore.collectAsState()
    val gameState by vm.gameState.collectAsState()
    val settings by vm.settings.collectAsState()

    Scaffold { padding ->
        // Tablet / landscape: cap content width and center so lines of
        // text don't stretch edge-to-edge on wide screens (SPEC advanced
        // feature #3, responsive layout).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                // fillMaxWidth (not fillMaxSize) + verticalScroll so the
                // button row below the picker stays reachable on short
                // viewports (landscape phone). Anchoring START at the
                // bottom via Spacer(weight) breaks under scroll — the
                // button is now just below the picker, which is fine.
                modifier = Modifier
                    .widthIn(max = CONTENT_MAX_WIDTH)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
            Text(
                text = "N-Back",
                style = MaterialTheme.typography.displaySmall,
            )

            HighScoreBanner(highscore = highscore)

            SettingsCard(
                nBack = settings.nBack,
                eventIntervalMs = settings.eventDurationMs,
                totalEvents = settings.eventsPerRound,
            )

            StimulusTypePicker(
                selected = gameState.gameType,
                onPick = vm::setGameType,
            )

            Button(
                onClick = onStartClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
            ) {
                Text("START ROUND", style = MaterialTheme.typography.titleLarge)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onSettingsClicked,
                    modifier = Modifier.weight(1f),
                ) { Text("Settings") }
                TextButton(
                    onClick = onHistoryClicked,
                    modifier = Modifier.weight(1f),
                ) { Text("History") }
            }
            }   // close Column
        }       // close Box
    }           // close Scaffold lambda
}               // close fun HomeScreen

// Max content width for wide-screen layouts (tablet / landscape).
// Keeps cards, sliders and text blocks readable instead of stretching
// edge-to-edge.
private val CONTENT_MAX_WIDTH = 560.dp

@Composable
private fun HighScoreBanner(highscore: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "HIGH SCORE",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = highscore.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SettingsCard(nBack: Int, eventIntervalMs: Long, totalEvents: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Current settings", style = MaterialTheme.typography.titleMedium)
            SettingRow(name = "N", value = nBack.toString())
            SettingRow(name = "Event duration", value = "$eventIntervalMs ms")
            SettingRow(name = "Events per round", value = totalEvents.toString())
        }
    }
}

@Composable
private fun SettingRow(name: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StimulusTypePicker(
    selected: GameType,
    onPick: (GameType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Stimulus type", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StimulusChip(
                label = "Visual",
                selected = selected == GameType.Visual,
                enabled = true,
                onClick = { onPick(GameType.Visual) },
                modifier = Modifier.weight(1f),
            )
            StimulusChip(
                label = "Audio",
                selected = selected == GameType.Audio,
                enabled = false, // SPEC Path 2: TTS pending.
                onClick = { onPick(GameType.Audio) },
                modifier = Modifier.weight(1f),
            )
            StimulusChip(
                label = "Dual",
                selected = selected == GameType.AudioVisual,
                enabled = false, // SPEC Path 2.
                onClick = { onPick(GameType.AudioVisual) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StimulusChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun HomeScreenPreview() {
    Surface { HomeScreen(vm = FakeVM(), onStartClicked = {}) }
}
