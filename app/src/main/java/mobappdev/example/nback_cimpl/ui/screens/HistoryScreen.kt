package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import mobappdev.example.nback_cimpl.data.ScoreRecord
import mobappdev.example.nback_cimpl.data.ScoreSort
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeHistoryVM
import mobappdev.example.nback_cimpl.ui.viewmodels.HistoryViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * History screen — Advanced feature #2 UI.
 *
 * Three sort modes exposed as [FilterChip]s; picking one re-queries the
 * DAO through the ViewModel (SPEC §3.2 "sortable by score, name,
 * date"). Empty state is explicit so a fresh install doesn't look
 * broken.
 */
@Composable
fun HistoryScreen(
    vm: HistoryViewModel,
    onBack: () -> Unit = {},
) {
    val sortBy by vm.sortBy.collectAsState()
    val scores by vm.scores.collectAsState()

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Back") }
            }
            Text(
                text = "Score history",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            SortChipRow(selected = sortBy, onPick = vm::setSortBy)

            if (scores.isEmpty()) {
                EmptyState()
            } else {
                ScoreList(scores = scores)
            }
        }
        }   // close Box
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortChipRow(selected: ScoreSort, onPick: (ScoreSort) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScoreSort.values().forEach { sort ->
            FilterChip(
                selected = selected == sort,
                onClick = { onPick(sort) },
                label = { Text(sort.label) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val ScoreSort.label: String
    get() = when (this) {
        ScoreSort.Score -> "Score"
        ScoreSort.Name -> "Name"
        ScoreSort.Date -> "Date"
    }

@Composable
private fun ScoreList(scores: List<ScoreRecord>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(scores, key = { it.id }) { ScoreRow(it) }
    }
}

@Composable
private fun ScoreRow(record: ScoreRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.playerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "N=${record.nValue}   •   ${formatTimestamp(record.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = record.score.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No rounds recorded yet.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

// minSdk 26 lets us use java.time without desugaring.
private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DATE_FORMATTER)

@Preview
@Composable
private fun HistoryScreenPreview() {
    HistoryScreen(vm = FakeHistoryVM())
}
