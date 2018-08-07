package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_shared_mutable_state_and_concurrency.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.sync.Mutex
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntConsumer
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.system.measureTimeMillis

class SharedMutableStateAndConcurrency : AppCompatActivity() {

    /**
     * The problem
     * Volatiles are of no help
     * Thread-safe data structures
     * Thread confinement fine-grained
     * Thread confinement coarse-grained
     * Mutual exclusion
     * Actors
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shared_mutable_state_and_concurrency)

        btn_the_problem.setOnClickListener { theProblem() }
        btn_volatiles_are_of_no_help.setOnClickListener { volatilesAreOfNoHelp() }
        btn_thread_safe_data_structures.setOnClickListener { threadSafeDataStructures() }
        btn_thread_confinement_fine_grained.setOnClickListener { threadConfinementFineGrained() }
        btn_thread_confinement_coarse_grained.setOnClickListener { threadConfinementCoarseGrained() }
        btn_mutual_exclusion.setOnClickListener { mutualExclusion() }
        btn_actors.setOnClickListener { actors() }

    }

    private suspend fun massiveRun(context: CoroutineContext, action: suspend () -> Unit) {
        val n = 1000
        val k = 1000
        val time = measureTimeMillis {
            val jobs = List(n) {
                launch(context) {
                    repeat(k) { action() }
                }
            }
            jobs.forEach { it.join() }
        }
        println("Completed ${n * k} actions in $time ms")
    }
    /**
     * Let us launch a thousand coroutines all doing the same action thousand times
     * (for a total of a million executions).
     * We'll also measure their completion time for further comparisons:
     */
    private var aCounter = 0
    private fun theProblem() = runBlocking {
        aCounter = 0
        massiveRun(context = CommonPool) {
            aCounter++
        }
        println("Counter = $aCounter")

        /**
         * Note: if you have an old system with 2 or fewer CPUs, then you will consistently
         * see 1000000, because CommonPool is running in only one thread in this case.
         * To reproduce the problem you'll need to make the following change:
         *
         * val mtContext = newFixedThreadPoolContext(2, "mtPool") // explicitly define context with two threads
         * var counter = 0
         *
         * fun main(args: Array<String>) = runBlocking<Unit> {
         *    massiveRun(mtContext) { // use it instead of CommonPool in this sample and below
         *       counter++
         *    }
         *    println("Counter = $counter")
         * }
         */
    }

    /**
     * There is common misconception that making a variable volatile solves concurrency problem.
     * Let us try it:
     */
    @Volatile
    private var bCounter = 0

    private fun volatilesAreOfNoHelp() = runBlocking {
        bCounter = 0
        massiveRun(CommonPool) {
            bCounter++
        }
        println("Counter = $bCounter")

        /**
         * This code works slower, but we still don't get "Counter = 1000000" at the end,
         * because volatile variables guarantee linearizable (this is a technical term for
         * "atomic") reads and writes to the corresponding variable, but do not provide atomicity
         * of larger actions (increment in our case).
         */
    }

    /**
     * The general solution that works both for threads and for coroutines is to use a
     * thread-safe (aka synchronized, linearizable, or atomic) data structure that provides
     * all the necessarily synchronization for the corresponding operations that needs to be
     * performed on a shared state. In the case of a simple counter we can use AtomicInteger
     * class which has atomic incrementAndGet operations:
     */
    private var cCounter = AtomicInteger()

    private fun threadSafeDataStructures() = runBlocking {
        cCounter = AtomicInteger(0)
        massiveRun(CommonPool) {
            cCounter.incrementAndGet()
        }
        println("Counter = ${cCounter.get()}")

        // ### Completed 1000000 actions in 416 ms

        /**
         * This is the fastest solution for this particular problem.
         * It works for plain counters, collections, queues and other standard data structures
         * and basic operations on them. However, it does not easily scale to complex state or
         * to complex operations that do not have ready-to-use thread-safe implementations.
         */
    }

    /**
     * Thread confinement is an approach to the problem of shared mutable state where
     * all access to the particular shared state is confined to a single thread.
     * It is typically used in UI applications, where all UI state is confined to the
     * single event-dispatch/application thread.
     * It is easy to apply with coroutines by using a single-threaded context:
     */
    private val aCounterContext = newSingleThreadContext("CounterContext")
    private var dCounter = 0
    private fun threadConfinementFineGrained() = runBlocking {
        dCounter = 0
        massiveRun(CommonPool) { // run each coroutine in CommonPool
            withContext(aCounterContext) { // but confine each increment to the single-threaded context
                dCounter++
            }
        }
        println("Counter = $dCounter")
        // ### Completed 1000000 actions in 28541 ms

        /**
         * This code works very slowly, because it does fine-grained thread-confinement.
         * Each individual increment switches from multi-threaded CommonPool context
         * to the single-threaded context using withContext block.
         */
    }


    /**
     * In practice, thread confinement is performed in large chunks, e.g. big
     * pieces of state-updating business logic are confined to the single thread.
     *
     * The following example does it like that, running each
     * coroutine in the single-threaded context to start with.
     */
    private val bCounterContext = newSingleThreadContext("CounterContext")
    private var eCounter = 0
    private fun threadConfinementCoarseGrained() = runBlocking {
        eCounter = 0
        massiveRun(bCounterContext) {
            eCounter++
        }
        println("Counter = $eCounter")
        // ### Completed 1000000 actions in 542 ms

        /**
         * This now works much faster and produces correct result.
         */
    }

    /**
     * Mutual exclusion solution to the problem is to protect all modifications of the
     * shared state with a critical section that is never executed concurrently.
     * In a blocking world you'd typically use synchronized or ReentrantLock for that.
     * Coroutine's alternative is called Mutex.
     * It has lock and unlock functions to delimit a critical section.
     *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * The key difference is that Mutex.lock() is a suspending function. *
     *                 It does not block a thread.                       *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *
     * There is also withLock extension function that conveniently represents
     *   mutex.lock();
     *   try { ... }
     *   finally { mutex.unlock() }
     * pattern:
     */
    private val mutex = Mutex()
    var fCounter = 0
    private fun mutualExclusion() = runBlocking {
        fCounter = 0
        massiveRun(CommonPool) {
            fCounter++
        }
        println("Counter = $fCounter")

        /**
         * The locking in this example is fine-grained, so it pays the price.
         * However, it is a good choice for some situations where you absolutely
         * must modify some shared state periodically,
         * but there is no natural thread that this state is confined to.
         */
    }

    /**
     * An actor is an entity made up of a combination of a coroutine, the state that is
     * confined and encapsulated into this coroutine, and a channel to communicate with other
     * coroutines. A simple actor can be written as a function, but an actor with a complex
     * state is better suited for a class.
     *
     * There is an actor coroutine builder that conveniently combines actor's mailbox channel
     * into its scope to receive messages from and combines the send channel into the resulting
     * job object, so that a single reference to the actor can be carried around as its handle.
     *
     * The first step of using an actor is to define a class of messages that an actor is going
     * to process. Kotlin's sealed classes are well suited for that purpose. We define CounterMsg
     * sealed class with IncCounter message to increment a counter and GetCounter message to get
     * its value. The later needs to send a response.
     *
     * A CompletableDeferred communication primitive, that represents a single value
     * that will be known (communicated) in the future, is used here for that purpose.
     *
     */

    //Message types for counterActor


    private fun actors() = runBlocking {
        val counter = counterActors() // create the actor
        massiveRun(CommonPool) {
            counter.send(IncCounter)
        }
        // send a message to get a counter value from an actor
        val response = CompletableDeferred<Int>()
        counter.send(GetCounter(response))

        println("Counter = ${response.await()}")
        counter.close() // shutdown the actor
    }

    /**
     * Then we define a function that launches an actor using an actor coroutine builder:
     */
    private fun counterActors() = actor<CounterMsg> {
        var counter = 0 // actor state
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is IncCounter -> counter++
                is GetCounter -> msg.response.complete(counter)
            }
        }
    }


}

/**
 * Related to function "actors()"
 */
sealed class CounterMsg
object IncCounter : CounterMsg() // one-way message to increment counter
class GetCounter(val response: CompletableDeferred<Int>): CounterMsg() // a request with reply
