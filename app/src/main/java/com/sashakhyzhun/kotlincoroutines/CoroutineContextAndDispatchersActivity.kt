package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_coroutine_context_and_dispatchers.*
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.coroutineContext

class CoroutineContextAndDispatchersActivity : AppCompatActivity() {

    /**
     * Dispatchers and threads
     * Unconfined vs confined dispatcher
     * Debugging coroutines and threads
     * Jumping between threads
     * Job in the context
     * Children of a coroutine
     * Combining contexts
     * Parental responsibilities
     * Naming coroutines for debugging
     * Cancellation via explicit job
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coroutine_context_and_dispatchers)

        btn_dispatchers_and_threads.setOnClickListener { dispatchersAndThreads() }

        btn_unconfined_vs_confined_dispatcher.setOnClickListener { unconfinedVsConfinedDispatcher() }

        btn_debugging_coroutines_and_threads.setOnClickListener { debuggingCoroutinesAndThreads() }

        btn_jumping_between_threads.setOnClickListener { jumpingBetweenThreads() }

        btn_job_in_the_context.setOnClickListener { jobInTheContext() }

        btn_children_of_a_coroutine.setOnClickListener { childrenOfACoroutine() }

        btn_combining_contexts.setOnClickListener { combiningContexts() }

        btn_parental_responsibilities.setOnClickListener { parentalResponsibilities() }

        btn_naming_coroutines_for_debugging.setOnClickListener { namingCoroutinesForDebugging() }

        btn_cancellation_via_explicit_job.setOnClickListener { cancellationViewExplicitJob() }

    }

    /**
     * Coroutine context includes a coroutine dispatcher (see CoroutineDispatcher) that
     * determines what thread or threads the corresponding coroutine uses for its execution.
     * Coroutine dispatcher can confine coroutine execution to a specific thread,
     * dispatch it to a thread pool, or let it run unconfined.
     * All coroutines builders like launch and async accept an optional CoroutineContext
     * parameter that can be used to explicitly specify the dispatcher for new coroutine
     * and other context elements. Try the following example:
     */
    private fun dispatchersAndThreads() = runBlocking {
        val jobs = arrayListOf<Job>()
        jobs += launch(Unconfined) { // not confined -- will work with main thread
            println("'Unconfined': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(coroutineContext) { // context of the parent, runBlocking coroutine
            println("'CoroutineContext': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(CommonPool) { // will get dispatched to ForkJoinPool.commonPool (or equivalent)
            println("'CommonPool': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(CommonPool) { // context of the parent, runBlocking coroutine
            println("'CommonPool': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(newSingleThreadContext("MyOwnThread")) { // will get its own new thread
            println("'newSTC': I'm working in thread ${Thread.currentThread().name}")
        }

        jobs.forEach { it.join() }

        /**
         * The default dispatcher that we've used in previous sections is represented by
         * DefaultDispatcher, which is equal to CommonPool in the current implementation.
         * So, launch { ... } is the same as launch(DefaultDispatcher) { ... }, which is the same
         * as launch(CommonPool) { ... }.
         *
         * The difference between parent coroutineContext and Unconfined context will be shown
         * later. Note, that newSingleThreadContext creates a new thread, which is a very expensive
         * resource. In a real application it must be either released, when no longer needed,
         * using close function, or stored in a top-level variable and reused throughout the
         * application.
         */
    }

    /**
     * The Unconfined coroutine dispatcher starts coroutine in the caller thread, but only
     * until the first suspension point. After suspension it resumes in the thread that is fully
     * determined by the suspending function that was invoked. Unconfined dispatcher is
     * appropriate when coroutine does not consume CPU time nor updates any shared data (like UI)
     * that is confined to a specific thread.
     * On the other side, coroutineContext property, that is available inside any coroutine,
     * is a reference to a context of this particular coroutine. This way, a parent context
     * can be inherited. The default dispatcher for runBlocking coroutine, in particular,
     * is confined to the invoker thread, so inheriting it has the effect of
     * confining execution to this thread with a predictable FIFO scheduling.
     *
     */
    private fun unconfinedVsConfinedDispatcher() = runBlocking {
        val jobs = arrayListOf<Job>()

        jobs += launch(Unconfined) {
            println("'Unconfined': I'm working in thread ${Thread.currentThread().name}")
            delay(500)
            println("'Unconfined': After delay in thread ${Thread.currentThread().name}")
        }

        jobs += launch(coroutineContext) {
            println("'coroutineContext': I'm working in thread ${Thread.currentThread().name}")
            delay(1000)
            println("'coroutineContext': After delay in thread ${Thread.currentThread().name}")
        }

        jobs.forEach { it.join() }

        /**
         * So, the coroutine that had inherited coroutineContext of runBlocking {...}
         * continues to execute in the main thread, while the unconfined one had resumed
         * in the default executor thread that delay function is using.
         */
    }

    /**
     * Coroutines can suspend on one thread and resume on another thread with Unconfined dispatcher
     * or with a default multi-threaded dispatcher. Even with a single-threaded dispatcher
     * it might be hard to figure out what coroutine was doing, where, and when.
     * The common approach to debugging applications with threads is to print the thread name
     * in the log file on each log statement. This feature is universally supported by logging
     * frameworks. When using coroutines, the thread name alone does not give much of a context,
     * so kotlinx.coroutines includes debugging facilities to make it easier.
     */
    private fun debuggingCoroutinesAndThreads() = runBlocking {
        val a = async(coroutineContext) {
            log("I'm computing a piece of the answer")
            6
        }
        val b = async(coroutineContext) {
            log("I'm computing another piece of the answer")
            7
        }
        log("The answer is ${a.await() * b.await()}")

        /**
         * There are three coroutines.
         * The main coroutine (#1)  —— runBlocking one, and two coroutines computing deferred
         * values a (#2) and b (#3) –— They are all executing in the context of runBlocking
         *                             and are confined to the main thread.
         */
    }

    private fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    /**
     * Run the following code with -Dkotlinx.coroutines.debug JVM option:
     */
    private fun jumpingBetweenThreads() {
        newSingleThreadContext("Ctx1").use { ctx1 ->
            newSingleThreadContext("Ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    log("Started in ctx1")

                    withContext(ctx2) {
                        log("Working i ctx2")
                    }
                    log("Back to ctx1")
                }
            }
        }
        /**
         * It demonstrates several new techniques. One is using runBlocking with an
         * explicitly specified context, and the other one is using withContext function
         * to change a context of a coroutine while still staying in the same coroutine
         */
    }


    /**
     * The coroutine's Job is part of its context. The coroutine can retrieve it from its own
     * context using `coroutineContext[Job]` expression:
     */
    private fun jobInTheContext() = runBlocking {
        println("My job is ${coroutineContext[Job]}")
        /**
         * It produces something like that when running in debug mode:
         * My job is "coroutine#1":BlockingCoroutine{Active}@6d311334
         *
         * So, isActive in CoroutineScope is just a convenient shortcut
         * for coroutineContext[Job]?.isActive == true.
         */
    }


    /**
     * When coroutineContext of a coroutine is used to launch another coroutine,
     * the Job of the new coroutine becomes a child of the parent coroutine's job.
     * When the parent coroutine is cancelled, all its children are recursively cancelled, too.
     */
    private fun childrenOfACoroutine() = runBlocking {
        // launch a coroutine to process some kind of incoming request
        val request = launch {
            // it spawns two other jobs, one with its separate context
            val job1 = launch {
                println("job1: I have my own context and execute independently!")
                delay(1000)
                println("job1: I am not affected by cancellation of the request")
            }
            // and the other inherits the parent context
            val job2 = launch {
                delay(100)
                println("job2: I am a child of the request coroutine")
                delay(1000)
                println("job2: I will not execute this line if my parent request is cancelled")
            }
            // request completes when both its sub-jobs complete:
            job1.join()
            job2.join()
        }
        delay(500)
        request.cancel() // cancel processing of the request
        delay(1000) // delay a second to see what happens
        println("main: Who has survived request cancellation?")
    }

    /**
     * Coroutine contexts can be combined using + operator.
     * The context on the right-hand side replaces relevant entries of
     * the context on the left-hand side. For example, a Job of the parent
     * coroutine can be inherited, while its dispatcher replaced:
     */
    private fun combiningContexts() = runBlocking {
        val request = launch(coroutineContext) { // use the context if `runBlocking`
            // spawns CPU-intensive child job in CommonPool !!!
            val job = launch(coroutineContext + CommonPool) {
                println("job: I am a child of the request coroutine, but with a different dispatcher")
                delay(1000)
                println("job: I will not execute this line if my parent request is cancelled")
            }
            job.join()
        }
        delay(500)
        request.cancel() // cancel processing of the request
        delay(1000) // delay a second to see what happens
        println("main: Who has survived request cancellation?")
    }

    /**
     * A parent coroutine always waits for completion of all its children.
     * Parent does not have to explicitly track all the children it launches
     * and it does not have to use Job.join to wait for them at the end:
     */
    private fun parentalResponsibilities() = runBlocking {
        val request = launch {
            repeat(3) { // launch a few children jobs
                launch(coroutineContext) {
                    delay((it + 1) * 200L) // variable delay 200ms, 400ms, 600ms
                    println("Coroutine $it is done")
                }
            }
            println("request: I'm done and I don't explicitly join my children that are still active")
        }
        request.join() // wait for completion of the request, including all its children
        println("Now processing of the request is complete")

        /*
         * The result is going to be:
         * request: I'm done and I don't explicitly join my children that are still active
         * Coroutine 0 is done
         * Coroutine 1 is done
         * Coroutine 2 is done
         * Now processing of the request is complete
         */
    }

    /**
     * Automatically assigned ids are good when coroutines log often and you just need to
     * correlate log records coming from the same coroutine. However, when coroutine is tied
     * to the processing of a specific request or doing some specific background task, it is
     * better to name it explicitly for debugging purposes. CoroutineName context element
     * serves the same function as a thread name. It'll get displayed in the thread name that
     * is executing this coroutine when debugging mode is turned on.
     *
     * The following example demonstrates this concept:
     */
    private fun namingCoroutinesForDebugging() = runBlocking(CoroutineName("main")) {
        log2("Started main coroutine")

        // run two background value computations
        val v1 = async(CoroutineName("v1coroutine")) {
            delay(500)
            log2("Computing v1")
            252
        }
        val v2 = async(CoroutineName("v2coroutine")) {
            delay(1000)
            log2("Computing v2")
            6
        }

        log2("The answer for v1 / v2 = ${v1.await() / v2.await()}")

        /*
         * The output it produces with -Dkotlinx.coroutines.debug JVM option is similar to:
         * [main @main#1] Started main coroutine
         * [ForkJoinPool.commonPool-worker-1 @v1coroutine#2] Computing v1
         * [ForkJoinPool.commonPool-worker-2 @v2coroutine#3] Computing v2
         * [main @main#1] The answer for v1 / v2 = 42
         */
    }

    private fun log2(msg: String) = println("[${Thread.currentThread().name}] | $msg")


    //todo: IMPORTANT:
    // All of coroutines must be cancelled when activity is destroyed to avoid memory leaks.

    /**
     * Let us put our knowledge about contexts, children and jobs together. Assume that our
     * application has an object with a lifecycle, but that object is not a coroutine.
     * For example, we are writing an Android application and launch various coroutines in the
     * context of an Android activity to perform asynchronous operations to fetch and update data,
     * do animations, etc. All of these coroutines must be cancelled when activity is destroyed
     * to avoid memory leaks.
     *
     * We can manage a lifecycle of our coroutines by creating an instance of Job that is tied
     * to the lifecycle of our activity. A job instance is created using Job() factory function
     * as the following example shows. For convenience, rather than using
     * launch(coroutineContext + job) expression, we can write
     * launch(coroutineContext, parent = job) to make explicit the fact that the parent job
     * is being used.
     *
     * Now, a single invocation of Job.cancel cancels all the children we've launched.
     * Moreover, Job.join waits for all of them to complete,
     *           so we can also use cancelAndJoin here in this example:
     */
    private fun cancellationViewExplicitJob() = runBlocking {
        val job = Job() // create a job object to manage our lifecycle
        // now launch ten coroutines for a demo, each working for a different time
        val coroutines = List(10) { i ->
            // they are all children of our job object

            // we use the context of main runBlocking thread, but with our parent job
            launch(context = coroutineContext, parent = job) {
                delay((i + 1) * 200L) //variable delay 200ms, 400 ms, ... etc
            }
        }

        println("Launched ${coroutines.size} coroutines")
        delay(500L) // delay for half a second
        println("Cancelling the job!")
        job.cancelAndJoin() // cancel all our coroutines and wait for all of them to complete


        /**
         * As you can see, only the first three coroutines had printed a message and the others
         * were cancelled by a single invocation of job.cancelAndJoin(). So all we need to do
         * in our hypothetical Android application is to create a parent job object when
         * activity is created, use it for child coroutines, and cancel it when activity is
         * destroyed. We cannot join them in the case of Android lifecycle, since it is
         * synchronous, but this joining ability is useful when building backend services to
         * ensure bounded resource usage.
         */
    }



}

