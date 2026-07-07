package com.autoanswer.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 题库持久化仓储
 * 负责从 JSON 文件读取/写入题库数据
 */
class BankRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val fileName = "questions.json"

    /**
     * 题库文件的完整路径
     */
    private val bankFile: File
        get() = File(context.filesDir, fileName)

    /**
     * 从内部存储加载题库
     */
    fun loadFromStorage(): List<Question> {
        return try {
            val file = bankFile
            if (!file.exists()) return emptyList()
            val json = file.readText(Charsets.UTF_8)
            val type = object : TypeToken<List<Question>>() {}.type
            val list: List<Question> = gson.fromJson(json, type) ?: emptyList()
            list.filter { it.isValid() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 保存题库到内部存储
     */
    fun saveToStorage(questions: List<Question>): Boolean {
        return try {
            val json = gson.toJson(questions)
            bankFile.writeText(json, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从外部文件导入题库
     * 支持 JSON 格式：[{"question":"...","answer":"..."}]
     * @return 导入的题目列表，若解析失败返回 null
     */
    fun importFromFile(file: File): List<Question>? {
        return try {
            val json = file.readText(Charsets.UTF_8)
            val type = object : TypeToken<List<Question>>() {}.type
            val list: List<Question> = gson.fromJson(json, type) ?: return null
            list.filter { it.isValid() }.also {
                if (it.isEmpty()) return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 导出题库到外部文件
     */
    fun exportToFile(questions: List<Question>, file: File): Boolean {
        return try {
            val json = gson.toJson(questions)
            file.writeText(json, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取题库大小（字节数）
     */
    fun storageSize(): Long = bankFile.length()
}