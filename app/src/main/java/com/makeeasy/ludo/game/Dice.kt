package com.makeeasy.ludo.game

import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.makeeasy.ludo.R
import java.util.Random

class Dice: AppCompatImageView {

    private var isActive = false
    private var rolling: MediaPlayer
    private val diceList = intArrayOf(R.drawable.d1,R.drawable.d2,R.drawable.d3,R.drawable.d4,R.drawable.d5,R.drawable.d6,R.drawable.d7,R.drawable.d8,R.drawable.d9,R.drawable.d1)
    private val diceResult = intArrayOf(R.drawable.one,R.drawable.two,R.drawable.three,R.drawable.four,R.drawable.five,R.drawable.six)
    interface OnRollingCompleteListener {
        fun onComplete(num:Int)
    }

    constructor(ctx: Context) : super(ctx) {
        setImageResource(0)
        rolling = MediaPlayer.create(ctx,R.raw.rolling)
    }

    constructor(ctx: Context,attrs: AttributeSet?) : super(ctx,attrs) {
        setImageResource(0)
        rolling = MediaPlayer.create(ctx,R.raw.rolling)
    }

    fun reset() {
        isActive = false
        setImageResource(0)
        isClickable = false
    }

    fun active() {
        setImageResource(R.drawable.one)
        isClickable = true
        isActive = true
    }

    fun startRolling(onRollingCompleteListener: OnRollingCompleteListener) {
        if(isActive) {
            var i = 0
            isActive = false
            object : CountDownTimer(900, 100) {
                override fun onTick(millisUntilFinished: Long) {
                    setImageResource(diceList[i])
                    i++
                }
                override fun onFinish() {
                    val result = Random().nextInt(diceResult.size)
                    val n = result + 1
                    setImageResource(diceResult[result])
                    Handler(Looper.myLooper()!!).postDelayed({
                        onRollingCompleteListener.onComplete(n)
                    }, 800)
                }
            }.start()
            rolling.start()
        }
    }
}