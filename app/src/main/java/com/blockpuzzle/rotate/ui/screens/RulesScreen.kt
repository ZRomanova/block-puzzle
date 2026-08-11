package com.blockpuzzle.rotate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** "Правила" — a plain-language walkthrough for a friend who's never seen this game before. */
@Composable
fun RulesScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text("Правила", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))

        RuleSection(title = "Цель игры") {
            Text(
                "Заполняйте клетками фигур целые строки и столбцы на поле — они " +
                    "очищаются, а вы получаете очки. Игра заканчивается, когда ни " +
                    "одна из трёх фигур в лотке внизу не помещается на поле.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        RuleSection(title = "Как играть") {
            BulletText("Перетащите фигуру из лотка внизу экрана на поле.")
            BulletText("Если в уровне включено вращение (обычно да — это и есть фишка игры), перед установкой фигуру можно повернуть на 90° кнопкой со стрелкой под ней.")
            BulletText("Пока подсвечено зелёным — можно поставить сюда фигуру; красным — нельзя.")
            BulletText("Когда все три фигуры в лотке использованы, приходит новая тройка.")
            BulletText("Кнопка «отменить» в правом верхнем углу откатывает последний ход.")
        }

        RuleSection(title = "Что значат настройки уровня") {
            BulletText("Однотонный / Цветной — в цветном режиме собранная линия одного цвета даёт дополнительные очки.")
            BulletText("Случайный / Хитрый — Случайный алгоритм выдаёт фигуры наугад; Хитрый специально подбирает фигуры похитрее, но старается не сделать игру совсем безнадёжной.")
            BulletText("Размер поля у каждого уровня свой — от 5×5 до 8×8.")
            BulletText("Вращение — если выключено, кнопки поворота нет: фигуру нужно ставить ровно в том виде, в каком она выпала.")
            BulletText("Отражение — если включено, у несимметричных фигур есть шанс появиться в зеркальном виде; если выключено, фигура всегда выпадает такой, какой её нарисовали.")
        }

        RuleSection(title = "Свои уровни") {
            Text(
                "В разделе «Конструктор» можно создать собственный уровень: выбрать " +
                    "размер поля, режим и алгоритм, а также нарисовать свои фигуры " +
                    "и задать, как часто каждая из них будет выпадать.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        RuleSection(title = "Рекорды") {
            Text(
                "У каждого уровня свой рекорд и своя незавершённая партия — можно " +
                    "в любой момент выйти в меню, и игра сохранится там, где вы её " +
                    "остановили.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RuleSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun BulletText(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("•  ", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
