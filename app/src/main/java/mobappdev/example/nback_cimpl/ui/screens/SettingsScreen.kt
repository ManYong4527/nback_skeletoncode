package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.domain.GameSettings
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

/**
 * Settings screen — Advanced feature #1 (persistent round parameters).
 *
 * Save-on-change: every slider commits via `onValueChangeFinished`, every
 * chip commits on click. There is no explicit Save button, which keeps
 * "unsaved changes" out of the mental model and matches Android system
 * settings conventions.
 *
 * Local [draft] state is keyed on the persisted settings so a round that
 * finishes while the player is in Settings pushes any sanitized server
 * value back into the sliders instead of silently drifting.
 *
 * SPEC-pinned ranges live in [GameSettings] companion constants, so the
 * sliders here and the sanitize-on-load logic in the repository can
 * never go out of sync.
 */
@Composable
fun SettingsScreen(
    vm: GameViewModel,
    onBack: () -> Unit = {},
) {
    val persisted by vm.settings.collectAsState()
    var draft by remember(persisted) { mutableStateOf(persisted) }

    val commit: (GameSettings) -> Unit = { next ->
        draft = next
        vm.saveSettings(next)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Back") }
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.displaySmall,
            )

            IntSliderRow(
                label = "N",
                value = draft.nBack,
                range = GameSettings.N_BACK_RANGE,
                valueFormatter = { it.toString() },
                onDraftChange = { draft = draft.copy(nBack = it) },
                onCommit = { commit(draft) },
            )

            IntSliderRow(
                label = "Event duration",
                value = draft.eventDurationMs.toInt(),
                range = GameSettings.EVENT_DURATION_MS_RANGE.first.toInt()..
                    GameSettings.EVENT_DURATION_MS_RANGE.last.toInt(),
                step = 100,
                valueFormatter = { "$it ms" },
                onDraftChange = { draft = draft.copy(eventDurationMs = it.toLong()) },
                onCommit = { commit(draft) },
            )

            IntSliderRow(
                label = "Events per round",
                value = draft.eventsPerRound,
                range = GameSettings.EVENTS_PER_ROUND_RANGE,
                valueFormatter = { it.toString() },
                onDraftChange = { draft = draft.copy(eventsPerRound = it) },
                onCommit = { commit(draft) },
            )

            IntSliderRow(
                label = "Letter pool size",
                value = draft.letterCount,
                range = GameSettings.LETTER_COUNT_RANGE,
                valueFormatter = {
                    val letters = GameSettings.LETTER_POOL.take(it).joinToString(" ")
                    "$it   ($letters)"
                },
                onDraftChange = { draft = draft.copy(letterCount = it) },
                onCommit = { commit(draft) },
            )

            GridSizePicker(
                selected = draft.gridSize,
                onPick = { commit(draft.copy(gridSize = it)) },
            )
        }
    }
}

@Composable
private fun IntSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    valueFormatter: (Int) -> String,
    onDraftChange: (Int) -> Unit,
    onCommit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                valueFormatter(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        // Material Slider "steps" = number of discrete snap points BETWEEN
        // min and max, NOT counting the endpoints. For a range of size `n`
        // with granularity `step`, that's (n / step) - 1.
        val sliderSteps = ((range.last - range.first) / step) - 1
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = range.first + ((raw - range.first) / step).toInt() * step
                onDraftChange(snapped.coerceIn(range))
            },
            onValueChangeFinished = onCommit,
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = sliderSteps.coerceAtLeast(0),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GridSizePicker(selected: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Grid size", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GameSettings.GRID_SIZE_OPTIONS.forEach { size ->
                FilterChip(
                    selected = selected == size,
                    onClick = { onPick(size) },
                    label = { Text("${size}×${size}") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen(vm = FakeVM())
}
