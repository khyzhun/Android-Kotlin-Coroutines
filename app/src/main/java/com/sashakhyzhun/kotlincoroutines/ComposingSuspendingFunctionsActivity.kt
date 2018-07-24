package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_composing_suspending_functions.*
import kotlinx.coroutines.experimental.*
import kotlin.system.measureTimeMillis

class ComposingSuspendingFunctionsActivity : AppCompatActivity() {

    /**
     * Sequential by default
     * Concurrent using async
     * Lazily started async
     * Async-style functions
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_composing_suspending_functions)

        btn_sequential_by_default.setOnClickListener { sequentialByDefault() }

        btn_concurrent_using_async.setOnClickListener { concurrentUsingAsync() }

        btn_lazily_started_async.setOnClickListener { lazilyStartedAsync() }

        btn_async_style_functions.setOnClickListener { asyncStyleFunctions() }

    }


    /**
     * Assume that we have two suspending functions defined elsewhere that do something useful
     * like some kind of remote service call or computation. We just pretend they are useful,
     * but actually each one just delays for a second for the purpose of this example:
     */
    private suspend fun doSomethingUsefulOne(): Int { delay(1000L); return 13 }
    private suspend fun doSomethingUsefulTwo(): Int { delay(1000L); return 29 }


    /**
     * What do we do if need to invoke them sequentially -- first doSomethingUsefulOne and then
     * doSomethingUsefulTwo and compute the sum of their results? In practice we do this if we
     * use the results of the first function to make a decision on whether we need to invoke
     * the second one or to decide on how to invoke it.
     * We just use a normal sequential invocation, because the code in the coroutine, just like
     * in the regular code, is sequential by default. The following example demonstrates it by
     * measuring the total time it takes to execute both suspending functions:
     */
    private fun sequentialByDefault() = runBlocking {
        val time = measureTimeMillis {
            val one = doSomethingUsefulOne()
            val two = doSomethingUsefulTwo()
            println("The answer is ${one + two}")
        }
        println("completed in $time ms")
    }


    /**
     * What if there are no dependencies between invocation of doSomethingUsefulOne and
     * doSomethingUsefulTwo and we want to get the answer faster, by doing both concurrently?
     * This is where async comes to help.
     *
     * Conceptually, async is just like launch. It starts a separate coroutine which is a
     * light-weight thread that works concurrently with all the other coroutines.
     *
     * The difference is that launch returns a Job and does not carry any resulting value,
     * while async returns a Deferred -- a light-weight non-blocking future that represents
     * a promise to provide a result later.
     *
     * You can use .await() on a deferred value to get its eventual result,
     * but Deferred is also a Job, so you can cancel it if needed.
     */
    private fun concurrentUsingAsync() = runBlocking {
        val time = measureTimeMillis {
            val one = async { doSomethingUsefulOne() }
            val two = async { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")

        /**
         * This is twice as fast, because we have concurrent execution of two coroutines.
         * Note, that concurrency with coroutines is always explicit.
         */
    }


    /**
     * There is a laziness option to async using an optional start parameter with a value of
     * CoroutineStart.LAZY. It starts coroutine only when its result is needed by some await
     * or if a start function is invoked.
     * Run the following example that differs from the previous one only by this option:
     */
    private fun lazilyStartedAsync() = launch {
        val time = measureTimeMillis {
            val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
            val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")

        /**
         * So, we are back to sequential execution, because we first start and await for one,
         * and then start and await for two. It is not the intended use-case for laziness.
         * It is designed as a replacement for the standard lazy function in cases when
         * computation of the value involves suspending functions.
         */
    }



    /**
     * We can define async-style functions that invoke doSomethingUsefulOne and
     * doSomethingUsefulTwo asynchronously using async coroutine builder.
     * It is a good style to name such functions with "Async" suffix to highlight the fact that
     * they only start asynchronous computation and one needs to use the resulting deferred
     * value to get the result.
     */
    private fun somethingUsefulOneAsync() = async { doSomethingUsefulOne() }
    private fun somethingUsefulTwoAsync() = async { doSomethingUsefulTwo() }

    /**
     * Note, that these xxxAsync functions are not suspending functions.
     * They can be used from anywhere. However, their use always implies asynchronous
     * (here meaning concurrent) execution of their action with the invoking code.
     *
     * The following example shows their use outside of coroutine:
     */
    // note, that we don't have `runBlocking` to the right of `main` in this example
    private fun asyncStyleFunctions() {
        val time = measureTimeMillis {
            // we can initiate async actions outside of a coroutine
            val one = somethingUsefulOneAsync()
            val two = somethingUsefulTwoAsync()
            // but waiting for a result must involve either suspending or blocking.
            // here we use `runBlocking { ... }` to block the main thread while waiting for the result
            runBlocking {
                println("The answer is ${one.await() + two.await()}")
            }
        }
        println("Completed in $time ms")
    }

}

