package com.example.drawpath

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
        strokeWidth = 20f
    }

    private val framePaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 18f
    }

    // Vị trí khung hình vuông mà người dùng cần vẽ
    private val squareFrame = RectF(100f, 100f, 700f, 700f)

    // Trạng thái đã vẽ các cạnh
    private var topDrawn = false
    private var rightDrawn = false
    private var bottomDrawn = false
    private var leftDrawn = false

    // Vị trí hiện tại của ngón tay người dùng
    private var lastX = 0f
    private var lastY = 0f

    // Kiểm tra trạng thái đường vẽ
    private var pathStartPoint: PointF? = null
    private var lastEdge: Int? = null // Lưu trạng thái cạnh cuối cùng đã vẽ

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ khung hình vuông để người dùng vẽ theo
        canvas.drawRect(squareFrame, framePaint)

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

                // Snap vị trí đầu tiên vào cạnh của hình vuông
                val snappedPoint = snapToSquare(lastX, lastY)

                // Kiểm tra xem điểm bắt đầu có nằm chính xác trên cạnh không
                if (!isPointOnEdge(snappedPoint.x, snappedPoint.y)) {
                    return false // Nếu không, không cho phép bắt đầu vẽ
                }

                path.moveTo(snappedPoint.x, snappedPoint.y)

                // Lưu cạnh cuối cùng mà người dùng bắt đầu vẽ
                lastEdge = getEdgeIndex(snappedPoint.x, snappedPoint.y)

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = event.x
                val newY = event.y

                // Snap vị trí tiếp theo vào cạnh của hình vuông
                val snappedPoint = snapToSquare(newX, newY)

                // Kiểm tra nếu điểm vẽ có nằm trên cạnh
                if (isPointOnEdge(snappedPoint.x, snappedPoint.y)) {
                    // Chặn việc người dùng vẽ từ một cạnh đến cạnh khác mà không hoàn thành góc
                    if (lastEdge != null && lastEdge != getEdgeIndex(snappedPoint.x, snappedPoint.y)) {
                        if (!isCornerCompleted(lastEdge!!, snappedPoint)) {
                            return false
                        }
                    }

                    path.lineTo(snappedPoint.x, snappedPoint.y)
                    updateEdgeStatus(snappedPoint.x, snappedPoint.y)

                    // Cập nhật cạnh cuối cùng
                    lastEdge = getEdgeIndex(snappedPoint.x, snappedPoint.y)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                // Kiểm tra xem đường vẽ có kín không
                if (areAllEdgesDrawn() && isPathClosed()) {
                    onWin()
                } else {
                    resetCanvas()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    // Hàm snap vị trí vẽ của người dùng vào cạnh của hình vuông
    private fun snapToSquare(x: Float, y: Float): PointF {
        val snappedX = when {
            x <= squareFrame.left + 50 -> squareFrame.left
            x >= squareFrame.right - 50 -> squareFrame.right
            else -> x
        }

        val snappedY = when {
            y <= squareFrame.top + 50 -> squareFrame.top
            y >= squareFrame.bottom - 50 -> squareFrame.bottom
            else -> y
        }

        return PointF(snappedX, snappedY)
    }

    // Kiểm tra xem điểm có nằm trên cạnh hình vuông không
    private fun isPointOnEdge(x: Float, y: Float): Boolean {
        return (y in squareFrame.top..(squareFrame.top + 50) && x in squareFrame.left..squareFrame.right) || // Top edge
                (x in squareFrame.right..(squareFrame.right + 50) && y in squareFrame.top..squareFrame.bottom) || // Right edge
                (y in squareFrame.bottom..(squareFrame.bottom + 50) && x in squareFrame.left..squareFrame.right) || // Bottom edge
                (x in squareFrame.left..(squareFrame.left + 50) && y in squareFrame.top..squareFrame.bottom) // Left edge
    }

    // Cập nhật trạng thái đã vẽ của các cạnh
    private fun updateEdgeStatus(x: Float, y: Float) {
        if (y in squareFrame.top..(squareFrame.top + 50) && !topDrawn) {
            topDrawn = true
        } else if (x in squareFrame.right..(squareFrame.right + 50) && !rightDrawn) {
            rightDrawn = true
        } else if (y in squareFrame.bottom..(squareFrame.bottom + 50) && !bottomDrawn) {
            bottomDrawn = true
        } else if (x in squareFrame.left..(squareFrame.left + 50) && !leftDrawn) {
            leftDrawn = true
        }
    }

    // Kiểm tra xem tất cả các cạnh đã được vẽ chưa
    private fun areAllEdgesDrawn(): Boolean {
        return topDrawn && rightDrawn && bottomDrawn && leftDrawn
    }

    // Kiểm tra xem đường vẽ có kín không
    private fun isPathClosed(): Boolean {
        val pathMeasure = PathMeasure(path, false)

        if (pathMeasure.length == 0f) return false

        // Lấy vị trí điểm cuối cùng của đường vẽ
        val endPoint = FloatArray(2)
        pathMeasure.getPosTan(pathMeasure.length, endPoint, null)

        // Kiểm tra xem điểm cuối cùng có gần với điểm bắt đầu không
        return (pathStartPoint?.x?.let { Math.abs(it - endPoint[0]) < 50 } == true) &&
                (pathStartPoint?.y?.let { Math.abs(it - endPoint[1]) < 50 } == true)
    }

    // Hàm để xác định chỉ số của cạnh mà người dùng đang vẽ
    private fun getEdgeIndex(x: Float, y: Float): Int? {
        return when {
            y in squareFrame.top..(squareFrame.top + 50) -> 0 // Cạnh trên
            x in squareFrame.right..(squareFrame.right + 50) -> 1 // Cạnh phải
            y in squareFrame.bottom..(squareFrame.bottom + 50) -> 2 // Cạnh dưới
            x in squareFrame.left..(squareFrame.left + 50) -> 3 // Cạnh trái
            else -> null
        }
    }

    // Kiểm tra xem góc có được vẽ hoàn chỉnh không
    private fun isCornerCompleted(edge: Int, snappedPoint: PointF): Boolean {
        return when (edge) {
            0 -> (snappedPoint.x == squareFrame.left || snappedPoint.x == squareFrame.right) // Cạnh trên
            1 -> (snappedPoint.y == squareFrame.top || snappedPoint.y == squareFrame.bottom) // Cạnh phải
            2 -> (snappedPoint.x == squareFrame.left || snappedPoint.x == squareFrame.right) // Cạnh dưới
            3 -> (snappedPoint.y == squareFrame.top || snappedPoint.y == squareFrame.bottom) // Cạnh trái
            else -> false
        }
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
        lastEdge = null // Reset trạng thái cạnh cuối
        invalidate()
    }
}
