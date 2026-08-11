package com.blockpuzzle.rotate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockpuzzle.rotate.domain.LevelDefinition
import com.blockpuzzle.rotate.ui.components.rulesSummary

/** The two entry points into the app plus a top-5 leaderboard for a quick "beat my record" replay. */
@Composable
fun MenuScreen(
    topLevels: List<LevelDefinition>,
    records: Map<String, Int>,
    onOpenLevelList: () -> Unit,
    onOpenConstructor: () -> Unit,
    onOpenRules: () -> Unit,
    onQuickPlay: (LevelDefinition) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Block Puzzle",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "у каждого уровня свои правила",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(48.dp))

            Button(onClick = onOpenLevelList, modifier = Modifier.fillMaxWidth()) {
                Text("Играть")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenConstructor, modifier = Modifier.fillMaxWidth()) {
                Text("Конструктор")
            }

            if (topLevels.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                TopRecordsCard(topLevels = topLevels, records = records, onQuickPlay = onQuickPlay)
            }
        }

        IconButton(onClick = onOpenRules, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Правила игры")
        }
    }
}

@Composable
private fun TopRecordsCard(
    topLevels: List<LevelDefinition>,
    records: Map<String, Int>,
    onQuickPlay: (LevelDefinition) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Топ-5 по рекорду", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            topLevels.forEachIndexed { index, level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickPlay(level) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${index + 1}",
                        modifier = Modifier.width(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(level.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            level.rulesSummary(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        (records[level.tag] ?: 0).toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.PlayArrow, contentDescription = "Играть", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
