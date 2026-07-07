package com.autoanswer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.autoanswer.data.QuestionBank
import com.autoanswer.engine.Matcher
import com.autoanswer.engine.MatchResult

/**
 * 无障碍服务核心
 *
 * 功能：
 * 1. 监听窗口变化，提取屏幕文本
 * 2. 使用 Matcher 匹配题库
 * 3. 自动点击正确答案
 */
class AutoAnswerService : AccessibilityService() {

    companion object {
        private var instance: AutoAnswerService? = null

        /** 当前服务是否正在运行 */
        fun isRunning(): Boolean = instance != null

        /** 触发扫描当前屏幕（由悬浮球调用） */
        fun triggerScan() {
            instance?.performScan()
        }

        /** 设置 Matcher 引用（由 Activity 初始化时调用） */
        fun setMatcher(matcher: Matcher?) {
            instance?.matcher = matcher
        }

        /** 设置日志回调 */
        private var logCallback: ((String) -> Unit)? = null

        fun setLogCallback(callback: ((String) -> Unit)?) {
            instance?.let { it.logCallback = callback }
            logCallback = callback
        }

        /** 设置点击延迟（毫秒） */
        private var clickDelayMs: Long = 500

        fun setClickDelay(delayMs: Long) {
            clickDelayMs = delayMs
        }
    }

    private var matcher: Matcher? = null
    private var scanPending = false
    private val handler = Handler(Looper.getMainLooper())
    private val logCallback: ((String) -> Unit)? get() = Companion.logCallback

    // ========== 服务生命周期 ==========

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        addLog("无障碍服务已连接")
        startForegroundService()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        handler.removeCallbacksAndMessages(null)
        addLog("无障碍服务已断开")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        // 如果扫描未挂起，忽略
        if (!scanPending) return
        scanPending = false

        addLog("检测到窗口变化，开始扫描...")
        handler.postDelayed({
            performScanInternal(event)
        }, 300) // 等待 300ms 让窗口渲染稳定
    }

    override fun onInterrupt() {
        addLog("无障碍服务被中断")
    }

    // ========== 扫描逻辑 ==========

    /**
     * 触发扫描（由外部调用）
     */
    fun performScan() {
        scanPending = true
        addLog("扫描已触发，等待窗口事件...")
    }

    /**
     * 内部扫描执行
     */
    private fun performScanInternal(event: AccessibilityEvent? = null) {
        val root = rootInActiveWindow ?: run {
            addLog("无法获取当前窗口的根节点")
            return
        }

        // 1. 收集屏幕上的所有文本
        val screenTexts = mutableListOf<String>()
        collectTexts(root, screenTexts)
        addLog("收集到 ${screenTexts.size} 个文本片段")

        if (screenTexts.isEmpty()) {
            addLog("屏幕上未找到任何文本")
            root.recycle()
            return
        }

        // 2. 用 Matcher 匹配
        val matcher = this.matcher
        if (matcher == null) {
            addLog("Matcher 未初始化，请先在 App 中加载题库")
            root.recycle()
            return
        }

        val result = matcher.findMatch(screenTexts)
        if (!result.matched || result.question == null) {
            addLog("未找到匹配的题目")
            root.recycle()
            return
        }

        val answer = result.question!!.answer
        addLog("匹配成功！[${result.matchType.name}] 答案：$answer")

        // 3. 在屏幕上找到答案按钮并点击
        val clickDelay = Companion.clickDelayMs
        handler.postDelayed({
            findAndClickAnswer(root, answer)
            root.recycle()
        }, clickDelay)
    }

    // ========== 辅助方法 ==========

    /**
     * 递归遍历节点树，收集所有文本
     */
    private fun collectTexts(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        if (node == null) return

        // 收集本节点的文本
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            texts.add(text.trim())
        }

        // 收集 contentDescription
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank()) {
            texts.add(desc.trim())
        }

        // 递归子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectTexts(child, texts)
                child.recycle()
            }
        }
    }

    /**
     * 在节点树中找到包含答案文本的元素并点击
     */
    private fun findAndClickAnswer(root: AccessibilityNodeInfo, answerText: String): Boolean {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        findNodesByText(root, answerText, candidates)

        if (candidates.isEmpty()) {
            // 尝试子串匹配
            findNodesBySubstring(root, answerText, candidates)
        }

        if (candidates.isEmpty()) {
            addLog("未找到包含答案「$answerText」的可点击元素")
            // fallback: 尝试用 Gesture 点击屏幕中心附近
            addLog("尝试点击屏幕上所有可点击元素中文本最匹配的...")
            return findAllClickableAndTry(root, answerText)
        }

        // 优先点击最具体的节点（文本匹配度最高）
        val target = candidates.minByOrNull {
            // 优先选择文本完全匹配的节点
            val nodeText = it.text?.toString() ?: it.contentDescription?.toString() ?: ""
            if (nodeText == answerText) 0 else 1
        }

        if (target != null) {
            addLog("找到目标元素：${target.text ?: target.contentDescription}")
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            addLog("✅ 已点击「${answerText}」")
            return true
        }

        return false
    }

    /**
     * 递归查找文本匹配的节点
     */
    private fun findNodesByText(
        node: AccessibilityNodeInfo,
        targetText: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null) return

        val nodeText = node.text?.toString()
        val nodeDesc = node.contentDescription?.toString()

        if (targetText == nodeText || targetText == nodeDesc) {
            if (node.isClickable) {
                results.add(node)
            } else {
                // 找到最近的可点击父节点
                var parent = node
                while (parent != null && !parent.isClickable) {
                    val p = parent.parent
                    if (p != parent) parent = p else break
                }
                if (parent != null && parent.isClickable) {
                    results.add(parent)
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findNodesByText(child, targetText, results)
                child.recycle()
            }
        }
    }

    /**
     * 递归查找文本包含目标子串的节点
     */
    private fun findNodesBySubstring(
        node: AccessibilityNodeInfo,
        targetText: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null) return

        val nodeText = node.text?.toString()
        val nodeDesc = node.contentDescription?.toString()
        val allText = listOfNotNull(nodeText, nodeDesc)

        if (allText.any { it.any { targetText.contains(it) || it.contains(targetText) } }) {
            if (node.isClickable) {
                results.add(node)
            } else {
                var parent = node
                while (parent != null && !parent.isClickable) {
                    val p = parent.parent
                    if (p != parent) parent = p else break
                }
                if (parent != null && parent.isClickable) {
                    results.add(parent)
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findNodesBySubstring(child, targetText, results)
                child.recycle()
            }
        }
    }

    /**
     * 兜底：遍历所有可点击元素，尝试匹配文本
     */
    private fun findAllClickableAndTry(
        root: AccessibilityNodeInfo,
        answerText: String
    ): Boolean {
        val clickables = mutableListOf<Pair<AccessibilityNodeInfo, String>>()
        collectClickables(root, answerText, clickables)

        if (clickables.isEmpty()) {
            addLog("⚠️ 未找到任何可点击元素")
            return false
        }

        // 按文本相似度排序
        clickables.sortByDescending { (_, text) ->
            text.commonPrefixWith(answerText).length
        }

        val (target, text) = clickables.first()
        addLog("尝试点击：$text")
        target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        addLog("✅ 已点击「$text」")
        return true
    }

    private fun collectClickables(
        node: AccessibilityNodeInfo,
        answerText: String,
        results: MutableList<Pair<AccessibilityNodeInfo, String>>
    ) {
        if (node == null) return

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val displayText = text ?: desc ?: ""

        if (node.isClickable && displayText.isNotBlank()) {
            results.add(node to displayText)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectClickables(child, answerText, results)
                child.recycle()
            }
        }
    }

    // ========== 日志 ==========

    private fun addLog(message: String) {
        logCallback?.invoke(message)
    }

    // ========== 前台服务 ==========

    private fun startForegroundService() {
        val channelId = "auto_answer_service"
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("自动答题助手")
            .setContentText("正在后台运行")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification)
        }
    }
}