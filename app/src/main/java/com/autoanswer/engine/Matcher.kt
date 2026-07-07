package com.autoanswer.engine

import com.autoanswer.data.Question
import com.autoanswer.data.QuestionBank

/**
 * 匹配结果
 */
data class MatchResult(
    val matched: Boolean,
    val question: Question? = null,
    val matchType: MatchType = MatchType.NONE
)

enum class MatchType {
    /** 精确匹配 */
    EXACT,
    /** 子串匹配（屏幕文本包含题目，或题目包含屏幕文本） */
    SUBSTRING,
    /** 未匹配 */
    NONE
}

/**
 * 题目匹配引擎
 * 将屏幕文本与题库进行匹配，支持精确匹配和子串匹配
 */
class Matcher(private val questionBank: QuestionBank) {

    /**
     * 匹配模式
     */
    enum class Mode {
        /** 仅精确匹配 */
        EXACT,
        /** 精确优先，未命中则降级为子串匹配 */
        EXACT_THEN_SUBSTRING
    }

    private var mode: Mode = Mode.EXACT_THEN_SUBSTRING

    /**
     * 设置匹配模式
     */
    fun setMode(mode: Mode) {
        this.mode = mode
    }

    /**
     * 从屏幕文本中查找匹配的题目
     *
     * 屏幕文本是 AccessibilityService 从当前窗口收集的所有可见文本。
     * 算法：
     * 1. 将屏幕文本按行拆分
     * 2. 对每一行尝试精确匹配
     * 3. 若未命中且模式为 EXACT_THEN_SUBSTRING，进行子串匹配
     * 4. 子串匹配时，检查行文本是否包含题目，或题目是否包含行文本
     *
     * @param screenTexts 从屏幕收集到的所有文本片段
     * @return 匹配结果
     */
    fun findMatch(screenTexts: List<String>): MatchResult {
        if (screenTexts.isEmpty() || questionBank.size == 0) {
            return MatchResult(false)
        }

        // 1. 精确匹配：逐行与题库比对
        for (text in screenTexts) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) continue

            val exact = questionBank.findByQuestion(trimmed)
            if (exact != null) {
                return MatchResult(matched = true, question = exact, matchType = MatchType.EXACT)
            }
        }

        // 如果模式是精确匹配，到此为止
        if (mode == Mode.EXACT) {
            return MatchResult(false)
        }

        // 2. 子串匹配
        for (text in screenTexts) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) continue

            val substring = questionBank.findBySubstring(trimmed)
            if (substring != null) {
                return MatchResult(matched = true, question = substring, matchType = MatchType.SUBSTRING)
            }
        }

        return MatchResult(false)
    }

    /**
     * 从单段完整文本中查找匹配
     * 将文本按常见分隔符拆分后匹配
     */
    fun findMatch(screenText: String): MatchResult {
        // 按常见分隔符拆分
        val lines = screenText.split("\n", "，", "。", "？", "?", "！", "!")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return findMatch(lines)
    }
}