package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_coroutines_bacics.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

class CoroutineBasicsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coroutines_bacics)

        btn_first_coroutine.setOnClickListener {
            fistCoroutine()
        }

        btn_bridging_blocking_and_non_blocking.setOnClickListener {
            bridgingBlockingAndNonBlockingWorlds()
        }

        btn_waiting_for_a_job.setOnClickListener {
            waitingForAJob()
        }

        btn_extract_function_refactoring.setOnClickListener {
            extractRefactoring()
        }

        btn_coroutines_are_light_weight.setOnClickListener {
            coroutineAreLightWeight()
        }

        btn_coroutines_are_like_daemon_threads.setOnClickListener {
            coroutinesAreLikeDaemonThreads()
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

        // This example can be also rewritten in a more idiomatic way,
        // using runBlocking to wrap the execution of the main function:
        fun bridgingBlocking2() = runBlocking {
            launch {
                delay(1000L)
                println("World!")
            }
            println("Hello,")
            delay(2000L)

            /*
             * Here runBlocking<Unit> { ... } works as an adaptor that is used to start
             * the top-level main coroutine. We explicitly specify its Unit return type,
             * because a well-formed main function in Kotlin has to return Unit.
             */
        }
    }


    private fun waitingForAJob() = runBlocking {
        val job = launch {
            delay(3000L)
            println("World!")
        }

        println("Hello,")
        job.join()

        /*
         * Now the result is still the same, but the code of the main coroutine is not
         * tied to the duration of the background job in any way. Much better.
         */
    }


    private fun extractRefactoring() = runBlocking {
        /*
         * Let's extract the block of code inside launch { ... } into a separate function.
         * When you perform "Extract function" refactoring on this code you get a new function
         * with suspend modifier. That is your first suspending function.
         *
         * Suspending functions can be used inside coroutines just like regular functions,
         * but their additional feature is that they can, in turn, use other suspending functions,
         * like delay in this example, to suspend execution of a coroutine.
         */

        val job = launch { doWork() }
        println("Hello, ")
        job.join()
    }
    private suspend fun doWork() { println("World!") }


    private fun coroutineAreLightWeight() = runBlocking {
        val jobs = List(100_000) { // launch a lot of coroutines and list their jobs
            launch {
                delay(1000L)
                print(".")
            }
        }
        jobs.forEach {
            it.join() // wait for all jobs to complete.
        }

        /*
         * It launches 100K coroutines and, after a second, each coroutine prints a dot.
         * Now, try that with threads.
         * What would happen? (Most likely your code will produce some sort of out-of-memory error)
         */
    }


    private fun coroutinesAreLikeDaemonThreads() = runBlocking {
        launch {
            repeat(1000) { i ->
                println("I'm sleeping $i...")
                delay(500L)
            }
        }
        delay(1300) // just quit after delay.
    }














}