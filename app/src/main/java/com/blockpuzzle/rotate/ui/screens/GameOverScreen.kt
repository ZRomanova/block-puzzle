package com.blockpuzzle.rotate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

@Composable
fun GameOverScreen(
    level: LevelDefinition,
    score: Int,
    record: Int,
    isNewRecord: Boolean,
    onPlayAgain: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Игра окончена", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = level.name,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = level.rulesSummary(),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))
        Text(text = score.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (isNewRecord) {
            Text(text = "Новый рекорд!", style = MaterialTheme.typography.titleMedium)
        } else {
            Text(text = "Рекорд: $record", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(32.dp))

        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Играть снова")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onExitToMenu, modifier = Modifier.fillMaxWidth()) {
            Text("В меню")
        }
    }
}
