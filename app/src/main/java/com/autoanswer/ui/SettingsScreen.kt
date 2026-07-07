package com.autoanswer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoanswer.AutoAnswerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    // 点击延迟设置
    var clickDelay by remember { mutableStateOf(500L) }
    var delayText by remember { mutableStateOf("500") }

    // 匹配模式设置
    var isExactOnly by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 点击延迟设置
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "点击延迟",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "匹配成功后点击答案前的等待时间（毫秒）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = delayText,
                        onValueChange = { value ->
                            val filtered = value.filter { it.isDigit() }
                            delayText = filtered
                            val ms = filtered.toLongOrNull()
                            if (ms != null && ms in 0..5000) {
                                clickDelay = ms
                                AutoAnswerService.setClickDelay(ms)
                            }
                        },
                        label = { Text("毫秒") },
                        suffix = { Text("ms") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(160.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "推荐: 300-1000ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 匹配模式设置
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "匹配模式",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "选择题目匹配策略",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isExactOnly,
                        onClick = { isExactOnly = false }
                    )
                    Text(
                        text = "精确优先 + 子串降级（推荐）",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isExactOnly,
                        onClick = { isExactOnly = true }
                    )
                    Text(
                        text = "仅精确匹配",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isExactOnly) "仅当屏幕文本与题库题目完全一致时才匹配"
                    else "先尝试精确匹配，未命中时自动降级为子串匹配",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 关于
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "自动答题助手 v1.0",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "基于 Android AccessibilityService 实现",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "⚠️ 仅用于学习和合法用途",
                    style = MaterialTheme.typography.bodySmall,
                    color = Error
                )
            }
        }
    }
}