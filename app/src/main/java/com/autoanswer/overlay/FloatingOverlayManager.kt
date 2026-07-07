package com.autoanswer.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.autoanswer.AutoAnswerService
import com.autoanswer.R

/**
 * 悬浮控制球管理器
 *
 * 在全局显示一个小悬浮按钮，点击后触发扫描答题。
 * 支持拖拽移动位置。
 */
class FloatingOverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var isShowing = false

    // 位置参数
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // 日志回调
    private var logCallback: ((String) -> Unit)? = null

    fun setLogCallback(callback: ((String) -> Unit)?) {
        logCallback = callback
    }

    /**
     * 显示悬浮球
     */
    fun show() {
        if (isShowing) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.overlay_floating_ball, null) as FrameLayout
        overlayView = view

        // 悬浮球上的文字
        val ballText = view.findViewById<TextView>(R.id.ball_text)
        ballText?.text = "答"

        // 触摸事件：处理拖拽和点击
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // 点击
                        onBallClicked()
                    }
                    true
                }
                else -> false
            }
        }

        // 设置窗口参数
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            dpToPx(56f).toInt(),  // 宽度
            dpToPx(56f).toInt(),  // 高度
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(20f).toInt()  // 默认左侧 20dp
            y = dpToPx(200f).toInt() // 默认上方 200dp
        }

        try {
            windowManager.addView(view, params)
            isShowing = true
            logCallback?.invoke("悬浮球已显示")
        } catch (e: Exception) {
            logCallback?.invoke("悬浮球显示失败：${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 隐藏悬浮球
     */
    fun hide() {
        if (!isShowing) return
        try {
            overlayView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        overlayView = null
        isShowing = false
        logCallback?.invoke("悬浮球已隐藏")
    }

    /**
     * 当前是否显示
     */
    fun isVisible(): Boolean = isShowing

    /**
     * 切换显示状态
     */
    fun toggle() {
        if (isShowing) hide() else show()
    }

    // ========== 内部方法 ==========

    private fun onBallClicked() {
        logCallback?.invoke("悬浮球点击，触发扫描...")
        AutoAnswerService.triggerScan()
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    // 保持对 params 的引用，用于更新位置
    private lateinit var params: WindowManager.LayoutParams
}