package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import android.widget.Toast

class DrawingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var path = Path()
    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.holo_blue_dark)
        style = Paint.Style.STROKE
        strokeWidth = 30f
    }

    private val framePaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 30f
    }

    // Tâm và bán kính của hình tròn
    private val circleCenter = PointF(400f, 400f) // Tâm của hình tròn
    private val circleRadius = 300f // Bán kính của hình tròn

    // Trạng thái các cạnh đã vẽ
    private var topDrawn = false
    private var rightDrawn = false
    private var bottomDrawn = false
    private var leftDrawn = false

    // Vị trí hiện tại của ngón tay người dùng
    private var lastX = 0f
    private var lastY = 0f

    // Kiểm tra trạng thái đường vẽ
    private var pathStartPoint: PointF? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ khung hình tròn để người dùng vẽ theo
        canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, framePaint)

        // Vẽ đường của người dùng
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y

                // Lưu vị trí điểm bắt đầu
                pathStartPoint = PointF(lastX, lastY)

                // Snap vị trí đầu tiên vào đường tròn
                val snappedPoint = snapToCircle(lastX, lastY)

                // Kiểm tra xem điểm bắt đầu có nằm chính xác trên đường tròn không
                if (!isPointOnEdge(snappedPoint.x, snappedPoint.y)) {
                    return false // Nếu không, không cho phép bắt đầu vẽ
                }

                path.moveTo(snappedPoint.x, snappedPoint.y)
                updateEdgeStatus(snappedPoint.x, snappedPoint.y) // Cập nhật trạng thái cạnh đã vẽ

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = event.x
                val newY = event.y

                // Snap vị trí tiếp theo vào đường tròn
                val snappedPoint = snapToCircle(newX, newY)

                // Kiểm tra nếu điểm vẽ có nằm trên đường tròn
                if (isPointOnEdge(snappedPoint.x, snappedPoint.y)) {
                    path.lineTo(snappedPoint.x, snappedPoint.y)
                    updateEdgeStatus(snappedPoint.x, snappedPoint.y) // Cập nhật trạng thái cạnh đã vẽ
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                // Kiểm tra xem tất cả các cạnh đã được vẽ chưa
                if (areAllEdgesDrawn()) {
                    onWin() // Hiển thị thông báo thành công
                } else {
                    resetCanvas() // Reset nếu chưa hoàn thành
                }
            }
        }
        return true // Trả về true để tiếp tục nhận các sự kiện vẽ
    }

    // Hàm snap vị trí vẽ của người dùng vào đường tròn
    private fun snapToCircle(x: Float, y: Float): PointF {
        val dx = x - circleCenter.x
        val dy = y - circleCenter.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        // Nếu điểm ở gần hơn bán kính, snap vào điểm trên đường tròn
        return if (distance <= circleRadius) {
            PointF(x, y) // Giữ nguyên nếu đã nằm trong vòng tròn
        } else {
            // Tính toán điểm nằm trên đường tròn
            val ratio = circleRadius / distance
            PointF(circleCenter.x + dx * ratio, circleCenter.y + dy * ratio)
        }
    }

    // Kiểm tra xem điểm có nằm trên đường tròn không
    private fun isPointOnEdge(x: Float, y: Float): Boolean {
        val dx = x - circleCenter.x
        val dy = y - circleCenter.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        return Math.abs(distance - circleRadius) < 50 // Sử dụng ngưỡng nhỏ để kiểm tra
    }

    // Cập nhật trạng thái đã vẽ của các cạnh
    private fun updateEdgeStatus(x: Float, y: Float) {
        val angle = Math.toDegrees(Math.atan2(((y - circleCenter.y).toDouble()), ((x - circleCenter.x).toDouble()))).toFloat()

        // Kiểm tra các góc để xác định các cạnh đã vẽ
        when {
            angle >= -45 && angle < 45 -> topDrawn = true // Cạnh trên
            angle >= 45 && angle < 135 -> rightDrawn = true // Cạnh phải
            angle >= 135 || angle < -135 -> bottomDrawn = true // Cạnh dưới
            angle >= -135 && angle < -45 -> leftDrawn = true // Cạnh trái
        }
    }

    // Kiểm tra xem tất cả các cạnh đã được vẽ chưa
    private fun areAllEdgesDrawn(): Boolean {
        return topDrawn && rightDrawn && bottomDrawn && leftDrawn
    }

    // Hàm khi người dùng vẽ đúng
    private fun onWin() {
        paint.color = Color.GREEN // Đổi màu sang xanh lá để hiển thị win
        invalidate()

        // Hiển thị thông báo thắng
        Toast.makeText(context, "Bạn đã vẽ thành công!", Toast.LENGTH_SHORT).show()
    }

    // Hàm reset canvas cho người dùng vẽ lại
    fun resetCanvas() {
        path.reset()
        paint.color = ContextCompat.getColor(context, android.R.color.holo_blue_dark) // Reset lại màu
        pathStartPoint = null // Reset điểm bắt đầu
        topDrawn = false
        rightDrawn = false
        bottomDrawn = false
        leftDrawn = false
        invalidate()
    }
}
