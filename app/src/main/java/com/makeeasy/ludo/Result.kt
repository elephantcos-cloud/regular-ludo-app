package com.makeeasy.ludo

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView

@SuppressLint("SetTextI18n")
class Result(ctx:Context, private val winner:MutableList<Int>): Dialog(ctx) {

    init {
        setContentView(R.layout.result)
        window!!.setBackgroundDrawableResource(android.R.color.transparent)
        setCancelable(false)
        when (winner.size) {
            3 -> {
                findViewById<TextView>(R.id.p1).text = "Player ${winner[0]}"
                findViewById<TextView>(R.id.p2).text = "Player ${winner[1]}"
                findViewById<TextView>(R.id.p3).text = "Player ${winner[2]}"
                findViewById<TextView>(R.id.p4).text = "Player ${lastOne()}"
            }
            2 -> {
                findViewById<TextView>(R.id.p1).text = "Player ${winner[0]}"
                findViewById<TextView>(R.id.p2).text = "Player ${winner[1]}"
                findViewById<TextView>(R.id.p3).text = "Player ${lastOne()}"
                findViewById<TextView>(R.id.p4).visibility = View.GONE
            }
            1 -> {
                findViewById<TextView>(R.id.p1).text = "Player ${winner[0]}"
                findViewById<TextView>(R.id.p2).visibility = View.GONE
                findViewById<TextView>(R.id.p3).text = "Player ${lastOne()}"
                findViewById<TextView>(R.id.p4).visibility = View.GONE
            }
        }
        findViewById<Button>(R.id.close).setOnClickListener { dismiss() }
    }

    private fun lastOne():Int {
        when (winner.size) {
            3 -> {
                return if (!winner.contains(1)) {
                    1
                } else if (!winner.contains(2)) {
                    2
                } else if (!winner.contains(3)) {
                    3
                } else {
                    4
                }
            }
            2 -> {
                return if (!winner.contains(1)) {
                    1
                } else if (!winner.contains(2)) {
                    2
                } else {
                    3
                }
            }
            1 -> {
                return if (!winner.contains(1)) {
                    1
                } else {
                    3
                }
            }
            else -> {
                return 0
            }
        }
    }
}