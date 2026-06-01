package com.makeeasy.ludo

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.TextView

@SuppressLint("SetTextI18n")
class MenuDialog(ctx:Context): Dialog(ctx) {

    init {
        setContentView(R.layout.menu)
        window!!.setBackgroundDrawableResource(android.R.color.transparent)
        setCancelable(false)
        findViewById<Button>(R.id.close).setOnClickListener { dismiss() }
        findViewById<TextView>(R.id.contact).setOnClickListener {
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://google.com")))
        }
        findViewById<TextView>(R.id.policy).setOnClickListener {
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://google.com")))
        }
        findViewById<TextView>(R.id.rule).setOnClickListener {
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://google.com")))
        }
        findViewById<TextView>(R.id.about).setOnClickListener {
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://google.com")))
        }
    }
}