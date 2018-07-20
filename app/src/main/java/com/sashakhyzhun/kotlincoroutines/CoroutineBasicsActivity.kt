package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log.d
import kotlinx.android.synthetic.main.activity_coroutines_bacics.*
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import kotlin.math.log

class CoroutineBasicsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coroutines_bacics)

        btn_test.setOnClickListener {
            fistCoroutine()
        }
    }

    private fun fistCoroutine() {
        launch {                   // launch new coroutine in background and continue
            delay(1000L)     // non-blocking delay for 1 second (default time unit is ms)
            println("World!")     // print after delay

        }

        println("Hello,")         // main thread continues while coroutine is delayed
        Thread.sleep(2000L) // block main thread for 2 seconds to keep JVM alive
    }



}