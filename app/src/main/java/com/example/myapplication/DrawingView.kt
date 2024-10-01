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
                lastEdge = getEdgeIndex(snappedPoint.x, snappedPoint.y) ?: return false

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = event.x
                val newY = event.y

                // Snap vị trí tiếp theo vào cạnh của hình vuông
                val snappedPoint = snapToSquare(newX, newY)

                // Kiểm tra nếu điểm vẽ có nằm trên cạnh
                if (isPointOnEdge(snappedPoint.x, snappedPoint.y)) {
                    val currentEdge = getEdgeIndex(snappedPoint.x, snappedPoint.y)

                    // Nếu người dùng đang cố gắng chuyển sang cạnh khác
                    if (lastEdge != null && lastEdge != currentEdge) {
                        // Kiểm tra xem đã hoàn thành cạnh hiện tại chưa
                        if (!isEdgeCompleted(lastEdge!!)) {
                            return false // Nếu chưa hoàn thành cạnh, không cho phép vẽ sang cạnh khác
                        }
                    }

                    // Nếu người dùng đang vẽ trên cùng một cạnh hoặc đã hoàn thành cạnh
                    if (lastEdge == currentEdge) {
                        path.lineTo(snappedPoint.x, snappedPoint.y)
                        updateEdgeStatus(snappedPoint.x, snappedPoint.y)

                        // Cập nhật cạnh cuối cùng
                        lastEdge = currentEdge
                    } else if (isEdgeCompleted(lastEdge!!)) {
                        // Cạnh hiện tại đã hoàn thành, cho phép vẽ lên cạnh kề
                        path.lineTo(snappedPoint.x, snappedPoint.y)
                        updateEdgeStatus(snappedPoint.x, snappedPoint.y)

                        // Cập nhật cạnh cuối cùng
                        lastEdge = currentEdge
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                // Kiểm tra xem đường vẽ có kín không
                if (areAllEdgesDrawn() && isPathClosed() && !isPathIntersecting()) {
                    onWin()
                } else {
                    resetCanvas()
                }
            }
        }
        return true // Trả về true để tiếp tục nhận các sự kiện vẽ
    }

    // Kiểm tra xem đường vẽ có giao nhau không
    private fun isPathIntersecting(): Boolean {
        // Lấy kích thước của đường vẽ
        val pathMeasure = PathMeasure(path, false)

        // Nếu không có đường vẽ, không giao nhau
        if (pathMeasure.length == 0f) return false

        // Tạo một mảng để lưu các điểm trên đường vẽ
        val points = mutableListOf<PointF>()

        // Đo chiều dài của đường vẽ và lấy các điểm
        var distance = 0f
        while (distance < pathMeasure.length) {
            val point = FloatArray(2)
            pathMeasure.getPosTan(distance, point, null)
            points.add(PointF(point[0], point[1]))
            distance += 5f // Lấy các điểm cách nhau 5px
        }

        // Kiểm tra xem các điểm có trùng nhau không
        val uniquePoints = points.distinct()
        return uniquePoints.size < points.size // Nếu số điểm duy nhất ít hơn, có nghĩa là có giao nhau
    }

    // Cập nhật trạng thái đã vẽ của các cạnh


    // Kiểm tra xem cạnh đã được hoàn thành chưa
    private fun isEdgeCompleted(edge: Int): Boolean {
        return when (edge) {
            0 -> topDrawn // Cạnh trên
            1 -> rightDrawn // Cạnh phải
            2 -> bottomDrawn // Cạnh dưới
            3 -> leftDrawn // Cạnh trái
            else -> false
        }
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
        return (y in squareFrame.top..(squareFrame.top + 50) && x in squareFrame.left..squareFrame.right) || // Cạnh trên
                (x in squareFrame.right..(squareFrame.right + 50) && y in squareFrame.top..squareFrame.bottom) || // Cạnh phải
                (y in squareFrame.bottom..(squareFrame.bottom + 50) && x in squareFrame.left..squareFrame.right) || // Cạnh dưới
                (x in squareFrame.left..(squareFrame.left + 50) && y in squareFrame.top..squareFrame.bottom) // Cạnh trái
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
        val isEndCloseToStart = (pathStartPoint?.x?.let { Math.abs(it - endPoint[0]) < 50 } == true) &&
                (pathStartPoint?.y?.let { Math.abs(it - endPoint[1]) < 50 } == true)

        // Kiểm tra xem đường vẽ có giao nhau không
        val hasIntersection = isPathIntersecting()

        // Đường vẽ được coi là khép kín nếu hoặc là hai đầu gần nhau hoặc chúng đã giao nhau
        return isEndCloseToStart || hasIntersection
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
