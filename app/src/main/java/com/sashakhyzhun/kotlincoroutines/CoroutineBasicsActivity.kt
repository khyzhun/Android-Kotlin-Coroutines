package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log.d
import kotlinx.android.synthetic.main.activity_coroutines_bacics.*
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import timber.log.Timber
import kotlin.concurrent.thread
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


        /*
         * If you start by replacing launch by thread, the compiler produces the following error:
         * Error: Kotlin: Suspend functions are only allowed to be called from a coroutine or another suspend function
         *
         * That is because delay is a special suspending function that does not block a thread,
         * but suspends coroutine and it can be only used from a coroutine.
         **/

        //thread {
        //    delay(2000L)
        //    println("World! [2]")
        //}
        //println("Hello, [2] ")
        //Thread.sleep(2000L)
    }


    private fun bridgingBlockingAndNonBlockingWorlds() {
        launch {                 // launch new coroutine in background and continue
            delay(1000L)
            println("World!")
        }

        println("Hello,")       // main thread continues here immediately
        runBlocking {           // but this expression blocks the main thread
            delay(2000L)  // ... while we delay for 2 seconds to keep JVM alive
        }
    }





}