package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_select_expression.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.selects.select
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.coroutineContext

class SelectExpression : AppCompatActivity() {

    /**
     * Selecting from channels
     * Selecting on close
     * Selecting to send
     * Selecting deferred values
     * Switch over a channel of deferred values
     */


    /**
     * Select expression makes it possible to await multiple suspending functions
     * simultaneously and select the first one that becomes available.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_expression)

        btn_selecting_from_channels.setOnClickListener { selectingFromChannels() }
        btn_selecting_on_close.setOnClickListener { selectingOnClose() }
        btn_selecting_to_send.setOnClickListener { selectingToSend() }
        btn_selecting_deferred_values.setOnClickListener { selectingDeferredValues() }
        btn_switch_over_a_channel_of_deferred_values.setOnClickListener {
            switchOverAChannelOfDeferredValues()
        }
    }

    /**
     * Let us have two producers of strings: fizz and buzz.
     * The fizz produces "Fizz" string every 300 ms:
     */
    private fun selectingFromChannels() = runBlocking {
        fun fizz(context: CoroutineContext) = produce<String>(context) {
            while (true) { // sends "Fizz" every 300 ms.
                delay(300)
                send("Fizz")
            }
        }
        fun buzz(context: CoroutineContext) = produce<String>(context) {
            while (true) { // sens "Buzz" every 500 ms.
                delay(500)
                send("Buzz")
            }
        }
        suspend fun selectFizzBuzz(fizz: ReceiveChannel<String>, buzz: ReceiveChannel<String>) {
            select<Unit> { // <Unit> means that this select expression does not produce any result
                fizz.onReceive { value -> // this is the first select clause
                    println("fizz -> '$value'")
                }
                buzz.onReceive { value -> // this is the second select clause
                    println("buzz -> '$value'")
                }
            }
        }


        val fizz = fizz(coroutineContext)
        val buzz = buzz(coroutineContext)
        repeat(7) {
            selectFizzBuzz(fizz, buzz)
        }
        coroutineContext.cancelChildren() // cancel fizz & buzz coroutines.

        /**
         * result:
         *
         * fizz -> 'Fizz'
         * buzz -> 'Buzz!'
         * fizz -> 'Fizz'
         * fizz -> 'Fizz'
         * buzz -> 'Buzz!'
         * fizz -> 'Fizz'
         * buzz -> 'Buzz!'
         */
    }


    /**
     * The onReceive clause in select fails when the channel is closed causing the corresponding
     * select to throw an exception. We can use onReceiveOrNull clause to perform a specific
     * action when the channel is closed. The following example also shows that select is
     * an expression that returns the result of its selected clause:
     */
    private fun selectingOnClose() = runBlocking {
        suspend fun selectAorB(a: ReceiveChannel<String>, b: ReceiveChannel<String>): String =
                select<String> {
                    a.onReceiveOrNull {
                        if (it == null) {
                            "Channel 'a' is closed"
                        } else {
                            "a -> '$it'"
                        }
                    }
                    b.onReceiveOrNull {
                        if (it == null) {
                            "Channel 'b' is closed"
                        } else {
                            "b -> '$it'"
                        }
                    }
                }

        val a = produce<String>(coroutineContext) { repeat(4) { send("Hello $it") } }
        val b = produce<String>(coroutineContext) { repeat(4) { send("World $it") } }

        repeat(8) { // print first eight results
            println(selectAorB(a, b))
        }

        coroutineContext.cancelChildren()

        /**
         * result:
         *
         * a -> 'Hello 0'
         * a -> 'Hello 1'
         * b -> 'World 0'
         * a -> 'Hello 2'
         * a -> 'Hello 3'
         * b -> 'World 1'
         * Channel 'a' is closed
         * Channel 'a' is closed
         *
         * There are couple of observations to make out of it.
         *
         * First of all, select is biased to the first clause. When several clauses are
         * selectable at the same time, the first one among them gets selected.
         * Here, both channels are constantly producing strings, so a channel, being the first
         * clause in select, wins. However, because we are using unbuffered channel, the a gets
         * suspended from time to time on its send invocation and gives a chance for b to send, too.
         *
         * The second observation, is that onReceiveOrNull gets immediately selected when
         * the channel is already closed.
         */
    }


    /**
     * Select expression has onSend clause that can be used for a great good in
     * combination with a biased nature of selection.
     *
     * Let us write an example of producer of integers that sends its values to a side
     * channel when the consumers on its primary channel cannot keep up with it:
     */
    private fun selectingToSend() = runBlocking {
        fun produceNumbers(context: CoroutineContext, side: SendChannel<Int>) =
                produce<Int>(context) {
                    for (num in 1..10) { // produce 10 numbers from 1 to 10
                        delay(100) // every 100 ms

                        select<Unit> {
                            onSend(num) {} // send to the primary channel
                            side.onSend(num) {} // or to the side channel
                        }
                    }
                }

        val side = Channel<Int>() // allocate side channel.
        launch(coroutineContext) { // this is a very fast consumer for the side channel
            side.consumeEach { println("Side channel has $it") }
        }

        produceNumbers(coroutineContext, side).consumeEach {
            println("Consuming $it")
            delay(250) // let us digest the consumed number properly do not harry
        }

        println("Done consuming")
        coroutineContext.cancelChildren()

    }


    /**
     * Now the main function awaits for the first of them to complete and counts the number
     * of deferred values that are still active. Note, that we've used here the fact that
     * select expression is a Kotlin DSL, so we can provide clauses for it using an arbitrary code.
     * In this case we iterate over a list of deferred values to provide onAwait clause for each
     * deferred value.
     */
    private fun selectingDeferredValues() = runBlocking {
        /**
         * Deferred values can be selected using onAwait clause. Let us start with
         * an async function that returns a deferred string value after a random delay:
         */
        fun mDelay(timeDelay: Int) = async {
            delay(time = timeDelay.toLong())
            "Waited for $timeDelay ms"
        }
        /** Let us start a dozen of them with a random delay. */
        fun asyncStringsList(): List<Deferred<String>> {
            val random = Random(10)
            return List(size = 50, init = { // amount of items
                val n = random.nextInt(1000)
                mDelay(timeDelay = n)
            })
        }

        val list = asyncStringsList()
        val result = select<String> {
            list.withIndex().forEach { (index, deferred) ->
                deferred.onAwait { answer ->
                    "Deferred $index produced answer '$answer'"
                }
            }
        }
        println(result)
        val countActive = list.count { it.isActive }
        println("$countActive coroutines are still alive")

        /**
         * 1. We create a list of Deferred<String>
         * 2. Fill it with 'waited for $n ms' after needed delay
         * 3. select this list
         * 4. and print 'deferred $i ...' for each element after `onAwait`.
         * 5. then print if its alive
         */

    }

    /**
     * Let us write a channel producer function that consumes a channel of deferred string values,
     * waits for each received deferred value, but only until the next deferred value comes over
     * or the channel is closed.
     * This example puts together onReceiveOrNull and onAwait clauses in the same select:
     */
    private fun switchOverAChannelOfDeferredValues() = runBlocking {
        fun switchMapDeferreds(input: ReceiveChannel<Deferred<String>>) = produce<String> {
            var current = input.receive() // start with first received deferred value
            while (isActive) { // loop while not cancelled/closed
                val next = select<Deferred<String>?> { // return next deferred value from this select or null
                    input.onReceiveOrNull { update ->
                        update // replaces next value to wait
                    }
                    current.onAwait { value ->
                        send(value) // send value that current deferred has produced
                        input.receiveOrNull() // and use the next deferred from the input channel
                    }
                }
                if (next == null) {
                    println("Channel was closed")
                    break // out of loop
                } else {
                    current = next
                }
            }
        }

        fun asyncString(str: String, time: Long) = async {
            delay(time)
            str
        }

        val channel = Channel<Deferred<String>>() // the channel for test
        launch(coroutineContext) { // launch printing coroutine
            for (s in switchMapDeferreds(channel)) println(s) // print each received string
        }

        channel.send(asyncString("BEGIN", 100))
        delay(200) // enough time for "Begin" to be produced

        channel.send(asyncString("Slow", 500))
        delay(100) // not enough time to produce slow

        channel.send(asyncString("Replace", 100))
        delay(500) // give it time before the last one

        channel.send(asyncString("End", 500))
        delay(1000) // give it time to process

        channel.close()
        delay(500) // and wait some time to let it finish


    }




}

