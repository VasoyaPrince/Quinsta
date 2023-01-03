package com.example.quinsta.model

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt


class CropUtils {
    class CropImageView : AppCompatImageView {
        private var mBorderPaint: Paint? = null
        private var mGuidelinePaint: Paint? = null
        private var mCornerPaint: Paint? = null
        private var mSurroundingAreaOverlayPaint: Paint? = null
        private var mHandleRadius = 0f
        private var mSnapRadius = 0f
        private var mCornerThickness = 0f
        private var mBorderThickness = 0f
        private var mCornerLength = 0f
        private var mBitmapRect = RectF()
        private val mTouchOffset = PointF()
        private var mPressedHandle: Handle? = null
        private var mFixAspectRatio = false
        private var mAspectRatioX = 1
        private var mAspectRatioY = 1
        private var mGuidelinesMode = 1

        constructor(context: Context) : super(context) {
            init(context, null)
        }

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            init(context, attrs)
        }

        constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
        ) {
            init(context, attrs)
        }

        private fun init(context: Context, attrs: AttributeSet?) {
            //final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, 0, 0);
            mGuidelinesMode = 1
            mFixAspectRatio = false
            mAspectRatioX = 1
            mAspectRatioY = 1
            val resources = context.resources
            mBorderPaint = PaintUtil.newBorderPaint(resources)
            mGuidelinePaint = PaintUtil.newGuidelinePaint(resources)
            mSurroundingAreaOverlayPaint = PaintUtil.newSurroundingAreaOverlayPaint(resources)
            mCornerPaint = PaintUtil.newCornerPaint(resources)
            mHandleRadius = 24f
            mSnapRadius = 3f
            mBorderThickness = 3f
            mCornerThickness = 5f
            mCornerLength = 20f
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            mBitmapRect = bitmapRect
            initCropWindow(mBitmapRect)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            drawDarkenedSurroundingArea(canvas)
            drawGuidelines(canvas)
            drawBorder(canvas)
            drawCorners(canvas)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            return if (!isEnabled) {
                false
            } else when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDown(event.x, event.y)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                    onActionUp()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    onActionMove(event.x, event.y)
                    parent.requestDisallowInterceptTouchEvent(true)
                    true
                }
                else -> false
            }
        }

        fun setGuidelines(guidelinesMode: Int) {
            mGuidelinesMode = guidelinesMode
            invalidate()
        }

        fun setFixedAspectRatio(fixAspectRatio: Boolean) {
            mFixAspectRatio = fixAspectRatio
            requestLayout()
        }

        fun setAspectRatio(aspectRatioX: Int, aspectRatioY: Int) {
            require(!(aspectRatioX <= 0 || aspectRatioY <= 0)) { "Cannot set aspect ratio value to a number less than or equal to 0." }
            mAspectRatioX = aspectRatioX
            mAspectRatioY = aspectRatioY
            if (mFixAspectRatio) {
                requestLayout()
            }
        }

        val croppedImage: Bitmap?
            get() {
                val drawable = drawable
                if (drawable == null || drawable !is BitmapDrawable) {
                    return null
                }
                val matrixValues = FloatArray(9)
                imageMatrix.getValues(matrixValues)
                val scaleX = matrixValues[Matrix.MSCALE_X]
                val scaleY = matrixValues[Matrix.MSCALE_Y]
                val transX = matrixValues[Matrix.MTRANS_X]
                val transY = matrixValues[Matrix.MTRANS_Y]
                val bitmapLeft: Float = if (transX < 0) abs(transX) else 0F
                val bitmapTop: Float = if (transY < 0) abs(transY) else 0F
                val originalBitmap = drawable.bitmap
                val cropX = (bitmapLeft + Edge.LEFT.coordinate) / scaleX
                val cropY = (bitmapTop + Edge.TOP.coordinate) / scaleY
                val cropWidth = (Edge.width / scaleX).coerceAtMost(originalBitmap.width - cropX)
                val cropHeight = (Edge.height / scaleY).coerceAtMost(originalBitmap.height - cropY)
                return Bitmap.createBitmap(
                    originalBitmap,
                    cropX.toInt(),
                    cropY.toInt(),
                    cropWidth.toInt(),
                    cropHeight.toInt()
                )
            }
        private val bitmapRect: RectF
            private get() {
                val drawable = drawable ?: return RectF()
                val matrixValues = FloatArray(9)
                imageMatrix.getValues(matrixValues)
                val scaleX = matrixValues[Matrix.MSCALE_X]
                val scaleY = matrixValues[Matrix.MSCALE_Y]
                val transX = matrixValues[Matrix.MTRANS_X]
                val transY = matrixValues[Matrix.MTRANS_Y]
                val drawableIntrinsicWidth = drawable.intrinsicWidth
                val drawableIntrinsicHeight = drawable.intrinsicHeight
                val drawableDisplayWidth = (drawableIntrinsicWidth * scaleX).roundToInt()
                val drawableDisplayHeight = (drawableIntrinsicHeight * scaleY).roundToInt()
                val left = transX.coerceAtLeast(0f)
                val top = transY.coerceAtLeast(0f)
                val right = (left + drawableDisplayWidth).coerceAtMost(width.toFloat())
                val bottom = (top + drawableDisplayHeight).coerceAtMost(height.toFloat())
                return RectF(left, top, right, bottom)
            }

        private fun initCropWindow(bitmapRect: RectF) {
            if (mFixAspectRatio) {
                initCropWindowWithFixedAspectRatio(bitmapRect)
            } else {
                val horizontalPadding = 0.1f * bitmapRect.width()
                val verticalPadding = 0.1f * bitmapRect.height()
                Edge.LEFT.coordinate = bitmapRect.left + horizontalPadding
                Edge.TOP.coordinate = bitmapRect.top + verticalPadding
                Edge.RIGHT.coordinate = bitmapRect.right - horizontalPadding
                Edge.BOTTOM.coordinate = bitmapRect.bottom - verticalPadding
            }
        }

        private fun initCropWindowWithFixedAspectRatio(bitmapRect: RectF) {
            if (AspectRatioUtil.calculateAspectRatio(bitmapRect) > targetAspectRatio) {
                val cropWidth = AspectRatioUtil.calculateWidth(
                    bitmapRect.height(),
                    targetAspectRatio
                )
                Edge.LEFT.coordinate = bitmapRect.centerX() - cropWidth / 2f
                Edge.TOP.coordinate = bitmapRect.top
                Edge.RIGHT.coordinate = bitmapRect.centerX() + cropWidth / 2f
                Edge.BOTTOM.coordinate = bitmapRect.bottom
            } else {
                val cropHeight = AspectRatioUtil.calculateHeight(
                    bitmapRect.width(),
                    targetAspectRatio
                )
                Edge.LEFT.coordinate = bitmapRect.left
                Edge.TOP.coordinate = bitmapRect.centerY() - cropHeight / 2f
                Edge.RIGHT.coordinate = bitmapRect.right
                Edge.BOTTOM.coordinate = bitmapRect.centerY() + cropHeight / 2f
            }
        }

        private fun drawDarkenedSurroundingArea(canvas: Canvas) {
            val bitmapRect = mBitmapRect
            val left = Edge.LEFT.coordinate
            val top = Edge.TOP.coordinate
            val right = Edge.RIGHT.coordinate
            val bottom = Edge.BOTTOM.coordinate
            canvas.drawRect(
                bitmapRect.left, bitmapRect.top, bitmapRect.right, top,
                mSurroundingAreaOverlayPaint!!
            )
            canvas.drawRect(
                bitmapRect.left, bottom, bitmapRect.right, bitmapRect.bottom,
                mSurroundingAreaOverlayPaint!!
            )
            canvas.drawRect(bitmapRect.left, top, left, bottom, mSurroundingAreaOverlayPaint!!)
            canvas.drawRect(right, top, bitmapRect.right, bottom, mSurroundingAreaOverlayPaint!!)
        }

        private fun drawGuidelines(canvas: Canvas) {
            if (!shouldGuidelinesBeShown()) {
                return
            }
            val left = Edge.LEFT.coordinate
            val top = Edge.TOP.coordinate
            val right = Edge.RIGHT.coordinate
            val bottom = Edge.BOTTOM.coordinate
            val oneThirdCropWidth = Edge.width / 3
            val x1 = left + oneThirdCropWidth
            canvas.drawLine(x1, top, x1, bottom, mGuidelinePaint!!)
            val x2 = right - oneThirdCropWidth
            canvas.drawLine(x2, top, x2, bottom, mGuidelinePaint!!)
            val oneThirdCropHeight = Edge.height / 3
            val y1 = top + oneThirdCropHeight
            canvas.drawLine(left, y1, right, y1, mGuidelinePaint!!)
            val y2 = bottom - oneThirdCropHeight
            canvas.drawLine(left, y2, right, y2, mGuidelinePaint!!)
        }

        private fun drawBorder(canvas: Canvas) {
            canvas.drawRect(
                Edge.LEFT.coordinate,
                Edge.TOP.coordinate,
                Edge.RIGHT.coordinate,
                Edge.BOTTOM.coordinate,
                mBorderPaint!!
            )
        }

        private fun drawCorners(canvas: Canvas) {
            val left = Edge.LEFT.coordinate
            val top = Edge.TOP.coordinate
            val right = Edge.RIGHT.coordinate
            val bottom = Edge.BOTTOM.coordinate
            val lateralOffset = (mCornerThickness - mBorderThickness) / 2f
            val startOffset = mCornerThickness - mBorderThickness / 2f
            canvas.drawLine(
                left - lateralOffset, top - startOffset, left - lateralOffset, top + mCornerLength,
                mCornerPaint!!
            )
            canvas.drawLine(
                left - startOffset, top - lateralOffset, left + mCornerLength, top - lateralOffset,
                mCornerPaint!!
            )
            canvas.drawLine(
                right + lateralOffset,
                top - startOffset,
                right + lateralOffset,
                top + mCornerLength,
                mCornerPaint!!
            )
            canvas.drawLine(
                right + startOffset,
                top - lateralOffset,
                right - mCornerLength,
                top - lateralOffset,
                mCornerPaint!!
            )
            canvas.drawLine(
                left - lateralOffset,
                bottom + startOffset,
                left - lateralOffset,
                bottom - mCornerLength,
                mCornerPaint!!
            )
            canvas.drawLine(
                left - startOffset,
                bottom + lateralOffset,
                left + mCornerLength,
                bottom + lateralOffset,
                mCornerPaint!!
            )
            canvas.drawLine(
                right + lateralOffset,
                bottom + startOffset,
                right + lateralOffset,
                bottom - mCornerLength,
                mCornerPaint!!
            )
            canvas.drawLine(
                right + startOffset,
                bottom + lateralOffset,
                right - mCornerLength,
                bottom + lateralOffset,
                mCornerPaint!!
            )
        }

        private fun shouldGuidelinesBeShown(): Boolean {
            return (mGuidelinesMode == GUIDELINES_ON
                    || mGuidelinesMode == GUIDELINES_ON_TOUCH && mPressedHandle != null)
        }

        private val targetAspectRatio: Float
            private get() = mAspectRatioX / mAspectRatioY.toFloat()

        private fun onActionDown(x: Float, y: Float) {
            val left = Edge.LEFT.coordinate
            val top = Edge.TOP.coordinate
            val right = Edge.RIGHT.coordinate
            val bottom = Edge.BOTTOM.coordinate
            mPressedHandle =
                HandleUtil.getPressedHandle(x, y, left, top, right, bottom, mHandleRadius)
            if (mPressedHandle != null) {
                HandleUtil.getOffset(mPressedHandle, x, y, left, top, right, bottom, mTouchOffset)
                invalidate()
            }
        }

        private fun onActionUp() {
            if (mPressedHandle != null) {
                mPressedHandle = null
                invalidate()
            }
        }

        private fun onActionMove(x: Float, y: Float) {
            var x = x
            var y = y
            if (mPressedHandle == null) {
                return
            }
            x += mTouchOffset.x
            y += mTouchOffset.y
            if (mFixAspectRatio) {
                mPressedHandle!!.updateCropWindow(x, y, targetAspectRatio, mBitmapRect, mSnapRadius)
            } else {
                mPressedHandle!!.updateCropWindow(x, y, mBitmapRect, mSnapRadius)
            }
            invalidate()
        }

        companion object {
            private val TAG = CropImageView::class.java.name
            const val GUIDELINES_OFF = 0
            const val GUIDELINES_ON_TOUCH = 1
            const val GUIDELINES_ON = 2
        }
    }

    object PaintUtil {
        fun newBorderPaint(resources: Resources?): Paint {
            val paint = Paint()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.parseColor("#AAFFFFFF")
            return paint
        }

        fun newGuidelinePaint(resources: Resources?): Paint {
            val paint = Paint()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.parseColor("#AAFFFFFF")
            return paint
        }

        fun newSurroundingAreaOverlayPaint(resources: Resources?): Paint {
            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#B0000000")
            return paint
        }

        fun newCornerPaint(resources: Resources?): Paint {
            val paint = Paint()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            paint.color = Color.parseColor("#FFFFFF")
            return paint
        }
    }

    object MathUtil {
        fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val side1 = x2 - x1
            val side2 = y2 - y1
            return sqrt((side1 * side1 + side2 * side2).toDouble()).toFloat()
        }
    }

    object HandleUtil {
        fun getPressedHandle(
            x: Float,
            y: Float,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            targetRadius: Float
        ): Handle? {
            var closestHandle: Handle? = null
            var closestDistance = Float.POSITIVE_INFINITY
            val distanceToTopLeft = MathUtil.calculateDistance(x, y, left, top)
            if (distanceToTopLeft < closestDistance) {
                closestDistance = distanceToTopLeft
                closestHandle = Handle.TOP_LEFT
            }
            val distanceToTopRight = MathUtil.calculateDistance(x, y, right, top)
            if (distanceToTopRight < closestDistance) {
                closestDistance = distanceToTopRight
                closestHandle = Handle.TOP_RIGHT
            }
            val distanceToBottomLeft = MathUtil.calculateDistance(x, y, left, bottom)
            if (distanceToBottomLeft < closestDistance) {
                closestDistance = distanceToBottomLeft
                closestHandle = Handle.BOTTOM_LEFT
            }
            val distanceToBottomRight = MathUtil.calculateDistance(x, y, right, bottom)
            if (distanceToBottomRight < closestDistance) {
                closestDistance = distanceToBottomRight
                closestHandle = Handle.BOTTOM_RIGHT
            }
            if (closestDistance <= targetRadius) {
                return closestHandle
            }
            if (isInHorizontalTargetZone(x, y, left, right, top, targetRadius)) {
                return Handle.TOP
            } else if (isInHorizontalTargetZone(x, y, left, right, bottom, targetRadius)) {
                return Handle.BOTTOM
            } else if (isInVerticalTargetZone(x, y, left, top, bottom, targetRadius)) {
                return Handle.LEFT
            } else if (isInVerticalTargetZone(x, y, right, top, bottom, targetRadius)) {
                return Handle.RIGHT
            }
            return if (isWithinBounds(x, y, left, top, right, bottom)) {
                Handle.CENTER
            } else null
        }

        fun getOffset(
            handle: Handle?,
            x: Float,
            y: Float,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            touchOffsetOutput: PointF
        ) {
            var touchOffsetX = 0f
            var touchOffsetY = 0f
            when (handle) {
                Handle.TOP_LEFT -> {
                    touchOffsetX = left - x
                    touchOffsetY = top - y
                }
                Handle.TOP_RIGHT -> {
                    touchOffsetX = right - x
                    touchOffsetY = top - y
                }
                Handle.BOTTOM_LEFT -> {
                    touchOffsetX = left - x
                    touchOffsetY = bottom - y
                }
                Handle.BOTTOM_RIGHT -> {
                    touchOffsetX = right - x
                    touchOffsetY = bottom - y
                }
                Handle.LEFT -> {
                    touchOffsetX = left - x
                    touchOffsetY = 0f
                }
                Handle.TOP -> {
                    touchOffsetX = 0f
                    touchOffsetY = top - y
                }
                Handle.RIGHT -> {
                    touchOffsetX = right - x
                    touchOffsetY = 0f
                }
                Handle.BOTTOM -> {
                    touchOffsetX = 0f
                    touchOffsetY = bottom - y
                }
                Handle.CENTER -> {
                    val centerX = (right + left) / 2
                    val centerY = (top + bottom) / 2
                    touchOffsetX = centerX - x
                    touchOffsetY = centerY - y
                }
                else -> {}
            }
            touchOffsetOutput.x = touchOffsetX
            touchOffsetOutput.y = touchOffsetY
        }

        private fun isInHorizontalTargetZone(
            x: Float,
            y: Float,
            handleXStart: Float,
            handleXEnd: Float,
            handleY: Float,
            targetRadius: Float
        ): Boolean {
            return x > handleXStart && x < handleXEnd && abs(y - handleY) <= targetRadius
        }

        private fun isInVerticalTargetZone(
            x: Float,
            y: Float,
            handleX: Float,
            handleYStart: Float,
            handleYEnd: Float,
            targetRadius: Float
        ): Boolean {
            return abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd
        }

        private fun isWithinBounds(
            x: Float,
            y: Float,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float
        ): Boolean {
            return x in left..right && y >= top && y <= bottom
        }
    }

    object AspectRatioUtil {
        fun calculateAspectRatio(left: Float, top: Float, right: Float, bottom: Float): Float {
            val width = right - left
            val height = bottom - top
            return width / height
        }

        fun calculateAspectRatio(rect: RectF): Float {
            return rect.width() / rect.height()
        }

        fun calculateLeft(
            top: Float,
            right: Float,
            bottom: Float,
            targetAspectRatio: Float
        ): Float {
            val height = bottom - top
            return right - targetAspectRatio * height
        }

        fun calculateTop(
            left: Float,
            right: Float,
            bottom: Float,
            targetAspectRatio: Float
        ): Float {
            val width = right - left
            return bottom - width / targetAspectRatio
        }

        fun calculateRight(
            left: Float,
            top: Float,
            bottom: Float,
            targetAspectRatio: Float
        ): Float {
            val height = bottom - top
            return targetAspectRatio * height + left
        }

        fun calculateBottom(
            left: Float,
            top: Float,
            right: Float,
            targetAspectRatio: Float
        ): Float {
            val width = right - left
            return width / targetAspectRatio + top
        }

        fun calculateWidth(height: Float, targetAspectRatio: Float): Float {
            return targetAspectRatio * height
        }

        fun calculateHeight(width: Float, targetAspectRatio: Float): Float {
            return width / targetAspectRatio
        }
    }

    enum class Handle(private val mHelper: HandleHelper) {
        TOP_LEFT(CornerHandleHelper(Edge.TOP, Edge.LEFT)), TOP_RIGHT(
            CornerHandleHelper(
                Edge.TOP,
                Edge.RIGHT
            )
        ),
        BOTTOM_LEFT(
            CornerHandleHelper(
                Edge.BOTTOM, Edge.LEFT
            )
        ),
        BOTTOM_RIGHT(CornerHandleHelper(Edge.BOTTOM, Edge.RIGHT)), LEFT(
            VerticalHandleHelper(
                Edge.LEFT
            )
        ),
        TOP(HorizontalHandleHelper(Edge.TOP)), RIGHT(VerticalHandleHelper(Edge.RIGHT)), BOTTOM(
            HorizontalHandleHelper(
                Edge.BOTTOM
            )
        ),
        CENTER(CenterHandleHelper());

        fun updateCropWindow(
            x: Float,
            y: Float,
            imageRect: RectF,
            snapRadius: Float
        ) {
            mHelper.updateCropWindow(x, y, imageRect, snapRadius)
        }

        fun updateCropWindow(
            x: Float,
            y: Float,
            targetAspectRatio: Float,
            imageRect: RectF?,
            snapRadius: Float
        ) {
            mHelper.updateCropWindow(x, y, targetAspectRatio, imageRect, snapRadius)
        }
    }

    internal abstract class HandleHelper(
        private val mHorizontalEdge: Edge?,
        private val mVerticalEdge: Edge?
    ) {
        private val activeEdges: EdgePair = EdgePair(mHorizontalEdge, mVerticalEdge)

        fun updateCropWindow(
            x: Float,
            y: Float,
            imageRect: RectF,
            snapRadius: Float
        ) {
            val activeEdges = activeEdges
            val primaryEdge = activeEdges.primary
            val secondaryEdge = activeEdges.secondary
            primaryEdge?.adjustCoordinate(
                x,
                y,
                imageRect,
                snapRadius,
                UNFIXED_ASPECT_RATIO_CONSTANT
            )
            secondaryEdge?.adjustCoordinate(
                x,
                y,
                imageRect,
                snapRadius,
                UNFIXED_ASPECT_RATIO_CONSTANT
            )
        }

        abstract fun updateCropWindow(
            x: Float,
            y: Float,
            targetAspectRatio: Float,
            imageRect: RectF?,
            snapRadius: Float
        )

        fun getActiveEdges(x: Float, y: Float, targetAspectRatio: Float): EdgePair {
            val potentialAspectRatio = getAspectRatio(x, y)
            if (potentialAspectRatio > targetAspectRatio) {
                activeEdges.primary = mVerticalEdge
                activeEdges.secondary = mHorizontalEdge
            } else {
                activeEdges.primary = mHorizontalEdge
                activeEdges.secondary = mVerticalEdge
            }
            return activeEdges
        }

        private fun getAspectRatio(x: Float, y: Float): Float {
            val left = if (mVerticalEdge == Edge.LEFT) x else Edge.LEFT.coordinate
            val top = if (mHorizontalEdge == Edge.TOP) y else Edge.TOP.coordinate
            val right = if (mVerticalEdge == Edge.RIGHT) x else Edge.RIGHT.coordinate
            val bottom = if (mHorizontalEdge == Edge.BOTTOM) y else Edge.BOTTOM.coordinate
            return AspectRatioUtil.calculateAspectRatio(left, top, right, bottom)
        }

        companion object {
            private const val UNFIXED_ASPECT_RATIO_CONSTANT = 1f
        }

    }

    internal class HorizontalHandleHelper(private val mEdge: Edge) :
        HandleHelper(mEdge, null) {
        override fun updateCropWindow(
            x: Float,
            y: Float,
            targetAspectRatio: Float,
            imageRect: RectF?,
            snapRadius: Float
        ) {
            if (imageRect != null) {
                mEdge.adjustCoordinate(x, y, imageRect, snapRadius, targetAspectRatio)
            }
            var left = Edge.LEFT.coordinate
            var right = Edge.RIGHT.coordinate
            val targetWidth = AspectRatioUtil.calculateWidth(Edge.height, targetAspectRatio)
            val difference = targetWidth - Edge.width
            val halfDifference = difference / 2
            left -= halfDifference
            right += halfDifference
            Edge.LEFT.coordinate = left
            Edge.RIGHT.coordinate = right
            if (Edge.LEFT.isOutsideMargin(imageRect!!, snapRadius)
                && !mEdge.isNewRectangleOutOfBounds(Edge.LEFT, imageRect, targetAspectRatio)
            ) {
                val offset = Edge.LEFT.snapToRect(imageRect)
                Edge.RIGHT.offset(-offset)
                mEdge.adjustCoordinate(targetAspectRatio)
            }
            if (Edge.RIGHT.isOutsideMargin(imageRect, snapRadius)
                && !mEdge.isNewRectangleOutOfBounds(Edge.RIGHT, imageRect, targetAspectRatio)
            ) {
                val offset = Edge.RIGHT.snapToRect(imageRect)
                Edge.LEFT.offset(-offset)
                mEdge.adjustCoordinate(targetAspectRatio)
            }
        }
    }

    internal class VerticalHandleHelper(private val mEdge: Edge) :
        HandleHelper(null, mEdge) {
        override fun updateCropWindow(
            x: Float,
            y: Float,
            targetAspectRatio: Float,
            imageRect: RectF?,
            snapRadius: Float
        ) {
            mEdge.adjustCoordinate(x, y, imageRect!!, snapRadius, targetAspectRatio)
            var top = Edge.TOP.coordinate
            var bottom = Edge.BOTTOM.coordinate
            val targetHeight = AspectRatioUtil.calculateHeight(Edge.width, targetAspectRatio)
            val difference = targetHeight - Edge.height
            val halfDifference = difference / 2
            top -= halfDifference
            bottom += halfDifference
            Edge.TOP.coordinate = top
            Edge.BOTTOM.coordinate = bottom
            if (Edge.TOP.isOutsideMargin(imageRect, snapRadius)
                && !mEdge.isNewRectangleOutOfBounds(Edge.TOP, imageRect, targetAspectRatio)
            ) {
                val offset = Edge.TOP.snapToRect(imageRect)
                Edge.BOTTOM.offset(-offset)
                mEdge.adjustCoordinate(targetAspectRatio)
            }
            if (Edge.BOTTOM.isOutsideMargin(imageRect, snapRadius)
                && !mEdge.isNewRectangleOutOfBounds(Edge.BOTTOM, imageRect, targetAspectRatio)
            ) {
                val offset = Edge.BOTTOM.snapToRect(imageRect)
                Edge.TOP.offset(-offset)
                mEdge.adjustCoordinate(targetAspectRatio)
            }
        }
    }

    internal class CenterHandleHelper : HandleHelper(null, null) {
        override fun updateCropWindow(
            x: Float,
            y: Float,
            targetAspectRatio: Float,
            imageRect: RectF?,
            snapRadius: Float
        ) {
            val left = Edge.LEFT.coordinate
            val top = Edge.TOP.coordinate
            val right = Edge.RIGHT.coordinate
            val bottom = Edge.BOTTOM.coordinate
            val currentCenterX = (left + right) / 2
            val currentCenterY = (top + bottom) / 2
            val offsetX = x - currentCenterX
            val offsetY = y - currentCenterY
            Edge.LEFT.offset(offsetX)
            Edge.TOP.offset(offsetY)
            Edge.RIGHT.offset(offsetX)
            Edge.BOTTOM.offset(offsetY)
            if (Edge.LEFT.isOutsideMargin(imageRect!!, snapRadius)) {
                val offset = Edge.LEFT.snapToRect(imageRect)
                Edge.RIGHT.offset(offset)
            } else if (Edge.RIGHT.isOutsideMargin(imageRect, snapRadius)) {
                val offset = Edge.RIGHT.snapToRect(imageRect)
                Edge.LEFT.offset(offset)
            }
            if (Edge.TOP.isOutsideMargin(imageRect, snapRadius)) {
                val offset = Edge.TOP.snapToRect(imageRect)
                Edge.BOTTOM.offset(offset)
            } else if (Edge.BOTTOM.isOutsideMargin(imageRect, snapRadius)) {
                val offset = Edge.BOTTOM.snapToRect(imageRect)
                Edge.TOP.offset(offset)
            }
            updateCropWindow(x, y, imageRect, snapRadius)
        }

    }

    internal class CornerHandleHelper(horizontalEdge: Edge?, verticalEdge: Edge?) :
        HandleHelper(horizontalEdge, verticalEdge) {
        override fun updateCropWindow(
            x: Float,
            y: Float,
            targetAspectRatio: Float,
            imageRect: RectF?,
            snapRadius: Float
        ) {
            val activeEdges = getActiveEdges(x, y, targetAspectRatio)
            val primaryEdge = activeEdges.primary
            val secondaryEdge = activeEdges.secondary
            primaryEdge!!.adjustCoordinate(x, y, imageRect!!, snapRadius, targetAspectRatio)
            secondaryEdge!!.adjustCoordinate(targetAspectRatio)
            if (secondaryEdge.isOutsideMargin(imageRect, snapRadius)) {
                secondaryEdge.snapToRect(imageRect)
                primaryEdge.adjustCoordinate(targetAspectRatio)
            }
        }

    }

    enum class Edge {
        LEFT, TOP, RIGHT, BOTTOM;

        var coordinate = 0f

        fun offset(distance: Float) {
            coordinate += distance
        }

        fun adjustCoordinate(
            x: Float,
            y: Float,
            imageRect: RectF,
            imageSnapRadius: Float,
            aspectRatio: Float
        ) {
            coordinate = when (this) {
                LEFT -> adjustLeft(x, imageRect, imageSnapRadius, aspectRatio)
                TOP -> adjustTop(y, imageRect, imageSnapRadius, aspectRatio)
                RIGHT -> adjustRight(x, imageRect, imageSnapRadius, aspectRatio)
                BOTTOM -> adjustBottom(y, imageRect, imageSnapRadius, aspectRatio)
            }
        }

        fun adjustCoordinate(aspectRatio: Float) {
            val left = coordinate
            val top = coordinate
            val right = coordinate
            val bottom = coordinate
            coordinate = when (this) {
                LEFT -> AspectRatioUtil.calculateLeft(top, right, bottom, aspectRatio)
                TOP -> AspectRatioUtil.calculateTop(left, right, bottom, aspectRatio)
                RIGHT -> AspectRatioUtil.calculateRight(left, top, bottom, aspectRatio)
                BOTTOM -> AspectRatioUtil.calculateBottom(left, top, right, aspectRatio)
            }
        }

        fun isNewRectangleOutOfBounds(edge: Edge, imageRect: RectF, aspectRatio: Float): Boolean {
            val offset = edge.snapOffset(imageRect)
            when (this) {
                LEFT -> if (edge == TOP) {
                    val top = imageRect.top
                    val bottom = coordinate - offset
                    val right = coordinate
                    val left = AspectRatioUtil.calculateLeft(top, right, bottom, aspectRatio)
                    return isOutOfBounds(top, left, bottom, right, imageRect)
                } else if (edge == BOTTOM) {
                    val bottom = imageRect.bottom
                    val top = coordinate - offset
                    val right = coordinate
                    val left = AspectRatioUtil.calculateLeft(top, right, bottom, aspectRatio)
                    return isOutOfBounds(top, left, bottom, right, imageRect)
                }
                TOP -> if (edge == LEFT) {
                    val left = imageRect.left
                    val right = coordinate - offset
                    val bottom = coordinate
                    val top = AspectRatioUtil.calculateTop(left, right, bottom, aspectRatio)
                    return isOutOfBounds(top, left, bottom, right, imageRect)
                } else if (edge == RIGHT) {
                    val right = imageRect.right
                    val left = coordinate - offset
                    val bottom = coordinate
                    val top = AspectRatioUtil.calculateTop(left, right, bottom, aspectRatio)
                    return isOutOfBounds(top, left, bottom, right, imageRect)
                }
                RIGHT -> if (edge == TOP) {
                    val top = imageRect.top
                    val bottom = coordinate - offset
                    val left = coordinate
                    val right = AspectRatioUtil.calculateRight(left, top, bottom, aspectRatio)
                    return isOutOfBounds(top, left, bottom, right, imageRect)
                } else if (edge == BOTTOM) {
                    val bottom = imageRect.bottom
                    val top = coordinate - offset
                    val left = coordinate
                    val right = AspectRatioUtil.calculateRight(left, top, bottom, aspectRatio)
                    return isOutOfBounds(top, left, bottom, right, imageRect)
                }
                BOTTOM -> if (edge == LEFT) {
                    val left = imageRect.left
                    val right = coordinate - offset
                    val top = coordinate
                    val bottom = AspectRatioUtil.calculateBottom(left, top, right, aspectRatio)
                    return isOutOfBounds(top, left, bottom, right, imageRect)
                } else if (edge == RIGHT) {
                    val right = imageRect.right
                    val left = coordinate - offset
                    val top = coordinate
                    val bottom = AspectRatioUtil.calculateBottom(left, top, right, aspectRatio)
                    return isOutOfBounds(top, left, bottom, right, imageRect)
                }
            }
            return true
        }

        private fun isOutOfBounds(
            top: Float,
            left: Float,
            bottom: Float,
            right: Float,
            imageRect: RectF
        ): Boolean {
            return top < imageRect.top || left < imageRect.left || bottom > imageRect.bottom || right > imageRect.right
        }

        fun snapToRect(imageRect: RectF): Float {
            val oldCoordinate = coordinate
            coordinate = when (this) {
                LEFT -> imageRect.left
                TOP -> imageRect.top
                RIGHT -> imageRect.right
                BOTTOM -> imageRect.bottom
            }
            return coordinate - oldCoordinate
        }

        private fun snapOffset(imageRect: RectF): Float {
            val oldCoordinate = coordinate
            val newCoordinate: Float = when (this) {
                LEFT -> imageRect.left
                TOP -> imageRect.top
                RIGHT -> imageRect.right
                else -> imageRect.bottom
            }
            return newCoordinate - oldCoordinate
        }

        fun isOutsideMargin(rect: RectF, margin: Float): Boolean {
            val result: Boolean = when (this) {
                LEFT -> coordinate - rect.left < margin
                TOP -> coordinate - rect.top < margin
                RIGHT -> rect.right - coordinate < margin
                else -> rect.bottom - coordinate < margin
            }
            return result
        }

        companion object {
            private const val MIN_CROP_LENGTH_PX = 40
            val width: Float
                get() = RIGHT.coordinate - LEFT.coordinate
            val height: Float
                get() = BOTTOM.coordinate - TOP.coordinate

            private fun adjustLeft(
                x: Float,
                imageRect: RectF,
                imageSnapRadius: Float,
                aspectRatio: Float
            ): Float {
                val resultX: Float
                if (x - imageRect.left < imageSnapRadius) {
                    resultX = imageRect.left
                } else {
                    var resultXHoriz = Float.POSITIVE_INFINITY
                    var resultXVert = Float.POSITIVE_INFINITY
                    if (x >= RIGHT.coordinate - MIN_CROP_LENGTH_PX) {
                        resultXHoriz = RIGHT.coordinate - MIN_CROP_LENGTH_PX
                    }
                    if ((RIGHT.coordinate - x) / aspectRatio <= MIN_CROP_LENGTH_PX) {
                        resultXVert = RIGHT.coordinate - MIN_CROP_LENGTH_PX * aspectRatio
                    }
                    resultX = x.coerceAtMost(resultXHoriz.coerceAtMost(resultXVert))
                }
                return resultX
            }

            private fun adjustRight(
                x: Float,
                imageRect: RectF,
                imageSnapRadius: Float,
                aspectRatio: Float
            ): Float {
                val resultX: Float
                if (imageRect.right - x < imageSnapRadius) {
                    resultX = imageRect.right
                } else {
                    var resultXHoriz = Float.NEGATIVE_INFINITY
                    var resultXVert = Float.NEGATIVE_INFINITY
                    if (x <= LEFT.coordinate + MIN_CROP_LENGTH_PX) {
                        resultXHoriz = LEFT.coordinate + MIN_CROP_LENGTH_PX
                    }
                    if ((x - LEFT.coordinate) / aspectRatio <= MIN_CROP_LENGTH_PX) {
                        resultXVert = LEFT.coordinate + MIN_CROP_LENGTH_PX * aspectRatio
                    }
                    resultX = x.coerceAtLeast(resultXHoriz.coerceAtLeast(resultXVert))
                }
                return resultX
            }

            private fun adjustTop(
                y: Float,
                imageRect: RectF,
                imageSnapRadius: Float,
                aspectRatio: Float
            ): Float {
                val resultY: Float
                if (y - imageRect.top < imageSnapRadius) {
                    resultY = imageRect.top
                } else {
                    var resultYVert = Float.POSITIVE_INFINITY
                    var resultYHoriz = Float.POSITIVE_INFINITY
                    if (y >= BOTTOM.coordinate - MIN_CROP_LENGTH_PX) resultYHoriz =
                        BOTTOM.coordinate - MIN_CROP_LENGTH_PX
                    if ((BOTTOM.coordinate - y) * aspectRatio <= MIN_CROP_LENGTH_PX) resultYVert =
                        BOTTOM.coordinate - MIN_CROP_LENGTH_PX / aspectRatio
                    resultY = y.coerceAtMost(resultYHoriz.coerceAtMost(resultYVert))
                }
                return resultY
            }

            private fun adjustBottom(
                y: Float,
                imageRect: RectF,
                imageSnapRadius: Float,
                aspectRatio: Float
            ): Float {
                val resultY: Float
                if (imageRect.bottom - y < imageSnapRadius) {
                    resultY = imageRect.bottom
                } else {
                    var resultYVert = Float.NEGATIVE_INFINITY
                    var resultYHoriz = Float.NEGATIVE_INFINITY
                    if (y <= TOP.coordinate + MIN_CROP_LENGTH_PX) {
                        resultYVert = TOP.coordinate + MIN_CROP_LENGTH_PX
                    }
                    if ((y - TOP.coordinate) * aspectRatio <= MIN_CROP_LENGTH_PX) {
                        resultYHoriz = TOP.coordinate + MIN_CROP_LENGTH_PX / aspectRatio
                    }
                    resultY = y.coerceAtLeast(resultYHoriz.coerceAtLeast(resultYVert))
                }
                return resultY
            }
        }
    }

    class EdgePair(var primary: Edge?, var secondary: Edge?)
}
