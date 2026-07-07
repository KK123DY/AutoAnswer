package com.autoanswer.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoanswer.data.Question
import com.autoanswer.data.QuestionBank
import com.autoanswer.data.BankRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionListScreen(
    questionBank: QuestionBank,
    bankRepository: BankRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val questions by questionBank.questions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Question?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // 导入文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            importQuestions(context, it, questionBank, bankRepository)
        }
    }

    // 导出文件创建器
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            exportQuestions(context, it, questionBank, bankRepository)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题 + 操作按钮行
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "题库管理",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "共 ${questions.size} 题",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.weight(1f)
            ) {
                Text("导入", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = {
                    if (questions.isNotEmpty()) {
                        exportLauncher.launch("questions.json")
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = questions.isNotEmpty()
            ) {
                Text("导出", fontSize = 13.sp)
            }
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索题目...") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 题目列表
        if (questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📝",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "题库为空",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "点击「导入」或「添加」按钮来创建题库",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val filteredQuestions = if (searchQuery.isBlank()) {
                    questions
                } else {
                    questions.filter {
                        it.question.contains(searchQuery, ignoreCase = true) ||
                                it.answer.contains(searchQuery, ignoreCase = true)
                    }
                }

                itemsIndexed(filteredQuestions) { _, question ->
                    QuestionCard(
                        question = question,
                        onEdit = { showEditDialog = question },
                        onDelete = {
                            questionBank.remove(question.question)
                            bankRepository.saveToStorage(questionBank.questions.value)
                        }
                    )
                }
            }
        }
    }

    // 添加对话框
    if (showAddDialog) {
        QuestionEditDialog(
            title = "添加题目",
            initialQuestion = "",
            initialAnswer = "",
            onConfirm = { q, a ->
                val added = questionBank.add(Question(q, a))
                if (added) {
                    bankRepository.saveToStorage(questionBank.questions.value)
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // 编辑对话框
    showEditDialog?.let { question ->
        QuestionEditDialog(
            title = "编辑题目",
            initialQuestion = question.question,
            initialAnswer = question.answer,
            onConfirm = { q, a ->
                val updated = questionBank.update(question.question, Question(q, a))
                if (updated) {
                    bankRepository.saveToStorage(questionBank.questions.value)
                }
                showEditDialog = null
            },
            onDismiss = { showEditDialog = null }
        )
    }
}

@Composable
private fun QuestionCard(
    question: Question,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "❓ ${question.question}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✅ ${question.answer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Success
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(20.dp),
                    tint = Error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionEditDialog(
    title: String,
    initialQuestion: String,
    initialAnswer: String,
    onConfirm: (question: String, answer: String) -> Unit,
    onDismiss: () -> Unit
) {
    var questionText by remember { mutableStateOf(initialQuestion) }
    var answerText by remember { mutableStateOf(initialAnswer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("题目") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = answerText,
                    onValueChange = { answerText = it },
                    label = { Text("答案") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (questionText.isNotBlank() && answerText.isNotBlank()) {
                        onConfirm(questionText.trim(), answerText.trim())
                    }
                },
                enabled = questionText.isNotBlank() && answerText.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun importQuestions(
    context: Context,
    uri: Uri,
    questionBank: QuestionBank,
    bankRepository: BankRepository
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "import_temp.json")
        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        val imported = bankRepository.importFromFile(tempFile)
        if (imported != null) {
            val added = questionBank.addAll(imported)
            bankRepository.saveToStorage(questionBank.questions.value)
            // TODO: 显示 Snackbar 提示导入成功
        }
        tempFile.delete()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun exportQuestions(
    context: Context,
    uri: Uri,
    questionBank: QuestionBank,
    bankRepository: BankRepository
) {
    try {
        val outputStream = context.contentResolver.openOutputStream(uri)
        val tempFile = File(context.cacheDir, "export_temp.json")
        bankRepository.exportToFile(questionBank.questions.value, tempFile)
        outputStream?.use { output ->
            tempFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
        tempFile.delete()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}