package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_cancellation_and_timeouts.*
import kotlinx.coroutines.experimental.*

class CancellationAndTimeoutsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cancellation_and_timeouts)

        btn_cancelling_coroutine_execution.setOnClickListener {
            cancellingCoroutineExecution()
        }

        btn_cancellation_is_cooperative.setOnClickListener {
            cancellationIsCooperative()
        }

        btn_making_computation_code.setOnClickListener {
            makingComputationCodeCancellable()
        }

        btn_closing_resources_with_finally.setOnClickListener {
            closingResourcesWithFinally()
        }

        btn_run_non_cancellable_block.setOnClickListener {
            runNonCancellableBlock()
        }

        btn_timeout.setOnClickListener {
            //kotlinTimeout()
            kotlinTimeout2()
        }

    }


    private fun cancellingCoroutineExecution() = runBlocking {
        /*
         * In a small application the return from "main" method might sound like a good idea
         * to get all coroutines implicitly terminated but in a larger, long-running application,
         * you need finer-grained control. The launch function returns a Job that can be used
         * to cancel running coroutine:
         */
        val job = launch {
            repeat(100) { i ->
                println("I'm sleeping $i...")
                delay(250L)
            }
        }

        delay(2000L) // delay a bit

        println("main: I'm tired of waiting!")

        job.cancel() // cancels the job
        job.join() // waits for job's completion

        println("main: Now I can quit.")


        /*
         * As soon as main invokes job.cancel, we don't see any output from
         * the other coroutine because it was cancelled. There is also a Job
         * extension function cancelAndJoin that combines cancel and join invocations.
         */
    }


    private fun cancellationIsCooperative() = runBlocking {
        /*
         * Coroutine cancellation is cooperative. A coroutine code has to cooperate to be
         * cancellable. All the suspending functions in kotlinx.coroutines are cancellable.
         * They check for cancellation of coroutine and throw CancellationException when cancelled.
         *
         * However, if a coroutine is working in a computation and does not check for
         * cancellation, then it cannot be cancelled, like the following example shows:
         */

        val startTime = System.currentTimeMillis()
        val job = launch {
            var nextPrintTime = startTime
            var i = 0
            while (i < 30) {
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("I'm sleeping ${i++}...")
                    nextPrintTime += 250L
                }
            }
        }

        delay(1000L) // delay a bit

        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now i can quit")

        /*
         * Run it to see that it continues to print "I'm sleeping" even after
         * cancellation until the job completes by itself after five iterations.
         */
    }


    private fun makingComputationCodeCancellable() = runBlocking {
        /*
         * There are two approaches to making computation code cancellable.
         * The first one is to periodically invoke a suspending function that checks for cancellation.
         * There is a yield function that is a good choice for that purpose.
         * The other one is to explicitly check the cancellation status.
         * Let us try the later approach.
         *
         * Replace while (i < 5) in the previous example with while (isActive) and rerun it.
         */

        val startTime = System.currentTimeMillis()
        val job = launch {
            var nextPrintTime = startTime
            var i = 0
            while (isActive) { // cancellable computation loop
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("I'm sleeping ${i++}...")
                    nextPrintTime += 250L
                }
            }
        }

        delay(1000L) // delay a bit

        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion

        println("main: Now I can quit")
    }


    private fun closingResourcesWithFinally() = runBlocking {
        val job = launch {
            try {
                repeat(1000) { i ->
                    println("I'm sleeping $i...")
                    delay(250L)
                }
            } finally {
                println("I'm running finally")
            }
        }

        delay(1000L) // delay a bit

        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion

        println("main: Now I can quit")

        /*
         * Both join and cancelAndJoin wait for all the finalization actions to complete,
         * so the example above produces the following output:
         */
    }


    private fun runNonCancellableBlock() = runBlocking {
        /*
         * Any attempt to use a suspending function in the finally block of the previous example
         * will cause CancellationException, because the coroutine running this code is cancelled.
         * Usually, this is not a problem, since all well-behaving closing operations
         * (closing a file, cancelling a job, or closing any kind of a communication channel)
         * are usually non-blocking and do not involve any suspending functions.
         *
         * However, in the rare case when you need to suspend in the cancelled coroutine you can
         * wrap the corresponding code in withContext(NonCancellable) {...} using withContext
         * function and NonCancellable context as the following example shows:
         */

        val job = launch {
            try {
                repeat(1000) {
                    println("I'm sleeping $it...")
                    delay(250L)
                }
            } finally {
                withContext(NonCancellable) {
                    println("I'm running finally")
                    delay(1000L)
                    println("And I've just delayed for 1 sec because I'm non-cancellable")
                }
            }
        }

        delay(1300L) // delay a bit

        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion

        println("main: Now I can quit.")

    }


    private fun kotlinTimeout() = runBlocking {
        try {
            withTimeout(1300L) {
                repeat(100) {
                    println("I'm sleeping $it...")
                    delay(500L)
                }
            }
        } catch (ex: TimeoutCancellationException) {
            println("exception: $ex")
        }
    }
    private fun kotlinTimeout2() = runBlocking {
        val result = withTimeoutOrNull(1300L) {
            repeat(1000) {
                println("I'm sleeping $it...")
                delay(500L)
            }
            "Done" // will get cancelled before it produces this result
        }

        println("Result is $result")

        /*
         * There is no longer an exception when running this code
         */
    }


}

