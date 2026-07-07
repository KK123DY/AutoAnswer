package com.autoanswer.data

/**
 * 题目数据模型
 * @param question 题目文本
 * @param answer 正确答案文本
 */
data class Question(
    val question: String,
    val answer: String
) {
    /**
     * 校验题目是否合法
     */
    fun isValid(): Boolean =
        question.isNotBlank() && answer.isNotBlank()
}