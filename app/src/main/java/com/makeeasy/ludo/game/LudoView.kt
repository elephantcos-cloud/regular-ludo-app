package com.makeeasy.ludo.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.makeeasy.ludo.R

class LudoView(c: Context, attrs: AttributeSet) : View(c, attrs) {

    private var height = 0
    private var width = 0
    private var top = 0
    private var bottom = 0
    private var d = 0
    private val stroke = 1
    private val corner = 5f
    private lateinit var mPaint: Paint
    private lateinit var mBorder: Paint
    private val redColor = Color.parseColor("#fe0000")
    private val dimRedColor = Color.parseColor("#FF8484")
    private val greenColor = Color.parseColor("#007d22")
    private val dimGreenColor = Color.parseColor("#8AFFAA")
    private val blueColor = Color.parseColor("#0054ff")
    private val dimBlueColor = Color.parseColor("#85ADFF")
    private val yellowColor = Color.parseColor("#ffd403")
    private val dimYellowColor = Color.parseColor("#FFEA84")
    private val bmpStarB = BitmapFactory.decodeResource(context.resources, R.drawable.star_black)
    private val bmpStarW = BitmapFactory.decodeResource(context.resources,R.drawable.star_white)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        assign(canvas)
        maker(canvas)
    }

    private fun assign(c: Canvas) {
        mPaint = Paint()
        mBorder = Paint()
        mPaint.style = Paint.Style.FILL
        mBorder.style = Paint.Style.STROKE
        mBorder.color = Color.DKGRAY
        mBorder.strokeWidth = stroke.toFloat()
        width = c.width
        height = c.height
        d = width / 15
        top = (height - width) / 2
        bottom = (height + width) / 2
    }

    private fun maker(c: Canvas) {
        val scaledBmpB = Bitmap.createScaledBitmap(bmpStarB,d,d,true)
        val scaledBmpW = Bitmap.createScaledBitmap(bmpStarW,d,d,true)
        mPaint.color = Color.WHITE
        c.drawRect(0f, top.toFloat(), width.toFloat(), bottom.toFloat(),mPaint)
        // Draw Red Gutty Design
        mPaint.color = redColor
        c.drawRect(0f,top.toFloat(), (6 * d).toFloat(), (top + 6 * d).toFloat(),mPaint)
        c.drawRect(0f,top.toFloat(), (6 * d).toFloat(), (top + 6 * d).toFloat(),mBorder)
        mPaint.color = dimRedColor
        c.drawCircle((3 * d).toFloat(),(top + 3 * d).toFloat(),(3 * d - 20).toFloat(),mPaint)
        c.drawCircle((3 * d).toFloat(), (top + 3 * d).toFloat(), (3 * d - 20).toFloat(),mBorder)
        // Draw Green Gutty Design
        mPaint.color = greenColor
        c.drawRect((9 * d).toFloat(),top.toFloat(),width.toFloat(),(top + 6 * d).toFloat(),mPaint)
        c.drawRect((9 * d).toFloat(),top.toFloat(),width.toFloat(),(top + 6 * d).toFloat(),mBorder)
        mPaint.color = dimGreenColor
        c.drawCircle((12 * d).toFloat(),(top + 3 * d).toFloat(),(3 * d - 20).toFloat(),mPaint)
        c.drawCircle((12 * d).toFloat(), (top + 3 * d).toFloat(), (3 * d - 20).toFloat(),mBorder)
        // Draw Yellow Gutty Design
        mPaint.color = yellowColor
        c.drawRect(0f,(top + 9 * d).toFloat(),(6 * d).toFloat(),bottom.toFloat(),mPaint)
        c.drawRect(0f,(top + 9 * d).toFloat(),(6 * d).toFloat(),bottom.toFloat(),mBorder)
        mPaint.color = dimYellowColor
        c.drawCircle((3 * d).toFloat(),(bottom - 3 * d).toFloat(),(3 * d - 20).toFloat(),mPaint)
        c.drawCircle((3 * d).toFloat(),(bottom - 3 * d).toFloat(),(3 * d - 20).toFloat(),mBorder)
        // Draw Blue Gutty Design
        mPaint.color = blueColor
        c.drawRect((9 * d).toFloat(),(top + 9 * d).toFloat(),width.toFloat(),bottom.toFloat(),mPaint)
        c.drawRect((9 * d).toFloat(),(top + 9 * d).toFloat(),width.toFloat(),bottom.toFloat(),mBorder)
        mPaint.color = dimBlueColor
        c.drawCircle((12 * d).toFloat(),(bottom - 3 * d).toFloat(),(3 * d - 20).toFloat(),mPaint)
        c.drawCircle((12 * d).toFloat(),(bottom - 3 * d).toFloat(),(3 * d - 20).toFloat(),mBorder)
        // Draw All Triangle Home in Center
        mPaint.color = redColor
        val vertRed = floatArrayOf((6 * d).toFloat(),(top + 6 * d).toFloat(),(6 * d).toFloat(),(top + 9 * d).toFloat(),(7.5 * d).toFloat(),(top + 7.5 * d).toFloat())
        c.drawVertices(Canvas.VertexMode.TRIANGLES,vertRed.size,vertRed,0,null,0,null,0,null,0,0,mPaint)
        mPaint.color = yellowColor
        val vertYellow = floatArrayOf((6 * d).toFloat(),(top + 9 * d).toFloat(),(9 * d).toFloat(),(top + 9 * d).toFloat(),(7.5 * d).toFloat(),(top + 7.5 * d).toFloat())
        c.drawVertices(Canvas.VertexMode.TRIANGLES,vertYellow.size,vertYellow,0,null,0,null,0,null,0,0,mPaint)
        mPaint.color = blueColor
        val vertBlue = floatArrayOf((9 * d).toFloat(),(top + 9 * d).toFloat(),(9 * d).toFloat(),(top + 6 * d).toFloat(),(7.5 * d).toFloat(),(top + 7.5 * d).toFloat())
        c.drawVertices(Canvas.VertexMode.TRIANGLES,vertBlue.size,vertBlue,0,null,0,null,0,null,0,0,mPaint)
        mPaint.color = greenColor
        val vertGreen = floatArrayOf((6 * d).toFloat(),(top + 6 * d).toFloat(),(9 * d).toFloat(),(top + 6 * d).toFloat(),(7.5 * d).toFloat(),(top + 7.5 * d).toFloat())
        c.drawVertices(Canvas.VertexMode.TRIANGLES,vertGreen.size,vertGreen,0,null,0,null,0,null,0,0,mPaint)
        // Draw Two Line to Divide Center Home
        c.drawLine((6 * d).toFloat(),(top + 6 * d).toFloat(),(9 * d).toFloat(),(top + 9 * d).toFloat(),mBorder)
        c.drawLine((9 * d).toFloat(),(top + 6 * d).toFloat(),(6 * d).toFloat(),(top + 9 * d).toFloat(),mBorder)
        // Draw All Path With Star
        for(i in 0 until 6) {
            when (i) {
                1 -> {
                    mPaint.color = redColor
                    c.drawRect((i * d).toFloat(),(top + 6 * d).toFloat(),(d + i * d).toFloat(),(top + 7 * d).toFloat(),mPaint)
                    c.drawBitmap(scaledBmpW,(i * d).toFloat(),(top + 6 * d).toFloat(),null)
                    mPaint.color = greenColor
                    c.drawRect((8 * d).toFloat(),(top + i * d).toFloat(),(9 * d).toFloat(),(top + d + i * d).toFloat(),mPaint)
                    c.drawBitmap(scaledBmpW,(8 * d).toFloat(),(top + i * d).toFloat(),null)
                }
                2 -> {
                    c.drawBitmap(scaledBmpB,(i * d).toFloat(),(top + 8 * d).toFloat(),null)
                    c.drawBitmap(scaledBmpB,(6 * d).toFloat(),(top + i * d).toFloat(),null)
                }
                3 -> {
                    c.drawBitmap(scaledBmpB,(9 * d + i * d).toFloat(),(top + 6 * d).toFloat(),null)
                    c.drawBitmap(scaledBmpB,(8 * d).toFloat(),(top + 9 * d + i * d).toFloat(),null)
                }
                4 -> {
                    mPaint.color = blueColor
                    c.drawRect((9 * d + i * d).toFloat(),(top + 8 * d).toFloat(),(10 * d + i * d).toFloat(),(top + 9 * d).toFloat(),mPaint)
                    c.drawBitmap(scaledBmpW,(9 * d + i * d).toFloat(),(top + 8 * d).toFloat(),null)
                    mPaint.color = yellowColor
                    c.drawRect((6 * d).toFloat(),(top + 9 * d + i * d).toFloat(),(7 * d).toFloat(),(top + 10 * d + i * d).toFloat(),mPaint)
                    c.drawBitmap(scaledBmpW,(6 * d).toFloat(),(top + 9 * d + i * d).toFloat(),null)
                }
            }
            c.drawRect((i * d).toFloat(),(top + 6 * d).toFloat(),(d + i * d).toFloat(),(top + 7 * d).toFloat(),mBorder)
            c.drawRect((i * d).toFloat(),(top + 8 * d).toFloat(),(d + i * d).toFloat(),(top + 9 * d).toFloat(),mBorder)
            c.drawRect((9 * d + i * d).toFloat(),(top + 6 * d).toFloat(),(10 * d + i * d).toFloat(),(top + 7 * d).toFloat(),mBorder)
            c.drawRect((9 * d + i * d).toFloat(),(top + 8 * d).toFloat(),(10 * d + i * d).toFloat(),(top + 9 * d).toFloat(),mBorder)
            c.drawRect((6 * d).toFloat(),(top + i * d).toFloat(),(7 * d).toFloat(),(top + d + i * d).toFloat(),mBorder)
            c.drawRect((8 * d).toFloat(),(top + i * d).toFloat(),(9 * d).toFloat(),(top + d + i * d).toFloat(),mBorder)
            c.drawRect((6 * d).toFloat(),(top + 9 * d + i * d).toFloat(),(7 * d).toFloat(),(top + 10 * d + i * d).toFloat(),mBorder)
            c.drawRect((8 * d).toFloat(),(top + 9 * d + i * d).toFloat(),(9 * d).toFloat(),(top + 10 * d + i * d).toFloat(),mBorder)
        }
        // Draw Four Center First Tile
        c.drawRect(0f,(top + 7 * d).toFloat(),d.toFloat(),(top + 8 * d).toFloat(),mBorder)
        c.drawRect((7 * d).toFloat(),(top + 14 * d).toFloat(),(8 * d).toFloat(),(top + 15 * d).toFloat(),mBorder)
        c.drawRect((14 * d).toFloat(),(top + 7 * d).toFloat(),(15 * d).toFloat(),(top + 8 * d).toFloat(),mBorder)
        c.drawRect((7 * d).toFloat(),top.toFloat(),(8 * d).toFloat(),(top + d).toFloat(),mBorder)
        // Draw All Home Path
        for(i in 0 until 5) {
            mPaint.color = redColor
            c.drawRect((d + i * d).toFloat(),(top + 7 * d).toFloat(),(2 * d + i * d).toFloat(),(top + 8 * d).toFloat(),mPaint)
            c.drawRect((d + i * d).toFloat(),(top + 7 * d).toFloat(),(2 * d + i * d).toFloat(),(top + 8 * d).toFloat(),mBorder)
            mPaint.color = yellowColor
            c.drawRect((7 * d).toFloat(),(top + 9 * d + i * d).toFloat(),(8 * d).toFloat(),(top + 10 * d + i * d).toFloat(),mPaint)
            c.drawRect((7 * d).toFloat(),(top + 9 * d + i * d).toFloat(),(8 * d).toFloat(),(top + 10 * d + i * d).toFloat(),mBorder)
            mPaint.color = blueColor
            c.drawRect((9 * d + i * d).toFloat(),(top + 7 * d).toFloat(),(10 * d + i * d).toFloat(),(top + 8 * d).toFloat(),mPaint)
            c.drawRect((9 * d + i * d).toFloat(),(top + 7 * d).toFloat(),(10 * d + i * d).toFloat(),(top + 8 * d).toFloat(),mBorder)
            mPaint.color = greenColor
            c.drawRect((7 * d).toFloat(),(top + d + i * d).toFloat(),(8 * d).toFloat(),(top + 2 * d + i * d).toFloat(),mPaint)
            c.drawRect((7 * d).toFloat(),(top + d + i * d).toFloat(),(8 * d).toFloat(),(top + 2 * d + i * d).toFloat(),mBorder)
        }
        //Draw Red Circle for Red Gutty
        mPaint.color = redColor
        c.drawCircle((d*2).toFloat(),(top+d*2).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*2).toFloat(),(top+d*2).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*4).toFloat(),(top+d*2).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*4).toFloat(),(top+d*2).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*2).toFloat(),(top+d*4).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*2).toFloat(),(top+d*4).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*4).toFloat(),(top+d*4).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*4).toFloat(),(top+d*4).toFloat(),(d/2).toFloat(),mBorder)
        //Draw Yellow Circle for Yellow Gutty
        mPaint.color = yellowColor
        c.drawCircle((d*2).toFloat(),(top+d*11).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*2).toFloat(),(top+d*11).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*4).toFloat(),(top+d*11).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*4).toFloat(),(top+d*11).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*2).toFloat(),(top+d*13).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*2).toFloat(),(top+d*13).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*4).toFloat(),(top+d*13).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*4).toFloat(),(top+d*13).toFloat(),(d/2).toFloat(),mBorder)
        //Draw Green Circle for Green Gutty
        mPaint.color = greenColor
        c.drawCircle((d*11).toFloat(),(top+d*2).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*11).toFloat(),(top+d*2).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*13).toFloat(),(top+d*2).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*13).toFloat(),(top+d*2).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*11).toFloat(),(top+d*4).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*11).toFloat(),(top+d*4).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*13).toFloat(),(top+d*4).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*13).toFloat(),(top+d*4).toFloat(),(d/2).toFloat(),mBorder)
        //Draw Blue Circle for Blue Gutty
        mPaint.color = blueColor
        c.drawCircle((d*11).toFloat(),(top+d*11).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*11).toFloat(),(top+d*11).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*13).toFloat(),(top+d*11).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*13).toFloat(),(top+d*11).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*11).toFloat(),(top+d*13).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*11).toFloat(),(top+d*13).toFloat(),(d/2).toFloat(),mBorder)
        c.drawCircle((d*13).toFloat(),(top+d*13).toFloat(),(d/2).toFloat(),mPaint)
        c.drawCircle((d*13).toFloat(),(top+d*13).toFloat(),(d/2).toFloat(),mBorder)
    }
}