package com.autoanswer.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 内存中管理题库，支持增删改查
 * 对外暴露不可变的 StateFlow，供 UI 层观察变化
 */
class QuestionBank {

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    val size: Int get() = _questions.value.size

    /**
     * 加载题库（替换当前内存中的全部题目）
     */
    fun load(questionList: List<Question>) {
        _questions.value = questionList.toList()
    }

    /**
     * 添加单道题目
     * @return true 添加成功，false 题目已存在
     */
    fun add(question: Question): Boolean {
        if (!question.isValid()) return false
        val current = _questions.value.toMutableList()
        // 不允许完全相同的题目重复添加
        if (current.any { it.question == question.question }) return false
        current.add(question)
        _questions.value = current
        return true
    }

    /**
     * 删除指定题目的题目
     */
    fun remove(questionText: String): Boolean {
        val current = _questions.value.toMutableList()
        val removed = current.removeAll { it.question == questionText }
        if (removed) _questions.value = current
        return removed
    }

    /**
     * 更新题目
     */
    fun update(oldQuestion: String, newQuestion: Question): Boolean {
        val current = _questions.value.toMutableList()
        val index = current.indexOfFirst { it.question == oldQuestion }
        if (index == -1 || !newQuestion.isValid()) return false
        current[index] = newQuestion
        _questions.value = current
        return true
    }

    /**
     * 根据题目文本查找答案
     * @return 匹配的 Question，null 表示未找到
     */
    fun findByQuestion(text: String): Question? {
        return _questions.value.find { it.question == text }
    }

    /**
     * 根据题目文本子串查找（模糊匹配）
     * @return 第一个匹配的 Question
     */
    fun findBySubstring(text: String): Question? {
        return _questions.value.find { text.contains(it.question) || it.question.contains(text) }
    }

    /**
     * 清空题库
     */
    fun clear() {
        _questions.value = emptyList()
    }

    /**
     * 批量添加题目
     * @return 实际添加的数量
     */
    fun addAll(questionList: List<Question>): Int {
        val current = _questions.value.toMutableList()
        val existingQuestions = current.map { it.question }.toSet()
        var added = 0
        for (q in questionList) {
            if (q.isValid() && q.question !in existingQuestions) {
                current.add(q)
                added++
            }
        }
        if (added > 0) _questions.value = current
        return added
    }
}