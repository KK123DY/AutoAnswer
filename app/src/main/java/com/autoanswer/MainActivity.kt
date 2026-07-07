package com.autoanswer

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.autoanswer.data.BankRepository
import com.autoanswer.data.QuestionBank
import com.autoanswer.engine.Matcher
import com.autoanswer.overlay.FloatingOverlayManager
import com.autoanswer.ui.AutoAnswerTheme
import com.autoanswer.ui.MainScreen
import com.autoanswer.ui.QuestionListScreen
import com.autoanswer.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    // 题库相关
    private lateinit var questionBank: QuestionBank
    private lateinit var bankRepository: BankRepository
    private lateinit var matcher: Matcher

    // 悬浮球
    private lateinit var floatingOverlay: FloatingOverlayManager

    // 日志
    private val logs = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化题库和匹配引擎
        questionBank = QuestionBank()
        bankRepository = BankRepository(applicationContext)
        matcher = Matcher(questionBank)

        // 加载持久化的题库
        val saved = bankRepository.loadFromStorage()
        if (saved.isNotEmpty()) {
            questionBank.load(saved)
        }

        // 将 Matcher 注入到 Service
        AutoAnswerService.setMatcher(matcher)

        // 初始化悬浮球
        floatingOverlay = FloatingOverlayManager(applicationContext)
        floatingOverlay.setLogCallback { msg ->
            addLog(msg)
        }

        // 设置 Service 日志回调
        AutoAnswerService.setLogCallback { msg ->
            addLog(msg)
        }

        setContent {
            AutoAnswerTheme {
                MainApp(
                    questionBank = questionBank,
                    bankRepository = bankRepository,
                    logs = logs.toList(),
                    onClearLogs = { logs.clear() },
                    onToggleOverlay = { toggleOverlay() },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingOverlay.hide()
    }

    private fun toggleOverlay() {
        if (floatingOverlay.isVisible()) {
            floatingOverlay.hide()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(this)
            ) {
                // 请求悬浮窗权限
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
                addLog("请授予悬浮窗权限后重试")
                return
            }
            floatingOverlay.show()
        }
    }

    private fun addLog(message: String) {
        synchronized(logs) {
            logs.add(message)
            if (logs.size > 200) {
                logs.removeAt(0)
            }
        }
    }
}

// 导航项
private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
private fun MainApp(
    questionBank: QuestionBank,
    bankRepository: BankRepository,
    logs: List<String>,
    onClearLogs: () -> Unit,
    onToggleOverlay: () -> Unit
) {
    val navItems = listOf(
        NavItem("主页", Icons.Default.Home, "home"),
        NavItem("题库", Icons.Default.List, "bank"),
        NavItem("设置", Icons.Default.Settings, "settings")
    )

    var selectedTab by remember { mutableStateOf(0) }
    var overlayVisible by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> MainScreen(
                    overlayVisible = overlayVisible,
                    onToggleOverlay = {
                        onToggleOverlay()
                        overlayVisible = !overlayVisible
                    },
                    logs = logs,
                    onClearLogs = onClearLogs
                )
                1 -> QuestionListScreen(
                    questionBank = questionBank,
                    bankRepository = bankRepository
                )
                2 -> SettingsScreen()
            }
        }
    }
}