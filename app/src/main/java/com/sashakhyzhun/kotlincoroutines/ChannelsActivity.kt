package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_channels.*
import kotlinx.coroutines.experimental.cancelChildren
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.coroutineContext

class ChannelsActivity : AppCompatActivity() {

    /**
     * Channel basics
     * Closing and iteration over channels
     * Building channel producers
     * Pipelines
     * Prime numbers with pipeline
     * Fan-out
     * Fan-in
     * Buffered channels
     * Ticker channels
     * Channels are fair
     */


    /**
     * Channels.
     * Deferred values provide a convenient way to transfer a single value between coroutines.
     * Channels provide a way to transfer a stream of values.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels)

        btn_channel_basics.setOnClickListener { channelBasics() }
        btn_closing_and_iteration_over_channels.setOnClickListener { closingAndIterationOverChannels() }
        btn_building_channel_producers.setOnClickListener { buildingChannelProducers() }
        btn_pipelines.setOnClickListener { pipelines() }
        btn_prime_numbers_with_pipeline.setOnClickListener { primeNumbersWithPipeline() }
        btn_fan_out.setOnClickListener { fanOut() }
        btn_fan_in.setOnClickListener { fanIn() }
        btn_buffered_channels.setOnClickListener { bufferedChannels() }
        btn_ticker_channels.setOnClickListener { tickerChannels() }
        btn_channels_are_fair.setOnClickListener { channelsAreFair() }


    }

    /**
     * A Channel is conceptually very similar to BlockingQueue.
     * One key difference is that instead of a blocking put operation it has a suspending send,
     * and instead of a blocking take operation it has a suspending receive.
     */
    private fun channelBasics() = runBlocking {
        val channel = Channel<Int>()
        launch {
            // this might be heavy CPU-consuming computation
            // or async logic, we'll just send five squares
            for (x in 1..5) { channel.send(x * x) }
        }

        // here we print five received integers:
        repeat(5) { println(channel.receive()) }
        println("Done")
    }

    /**
     * Unlike a queue, a channel can be closed to indicate that no more elements are coming.
     * On the receiver side it is convenient to use a regular for loop to receive elements
     * from the channel.
     * Conceptually, a close is like sending a special close token to the channel.
     * The iteration stops as soon as this close token is received, so there is a guarantee
     * that all previously sent elements before the close are received:
     */
    private fun closingAndIterationOverChannels() = runBlocking {
        val channel = Channel<Int>()
        launch {
            for (x in 1..5) {
                channel.send(x * x)
            }
            channel.close() // we're done sending
        }

        //try { channel.send(228) }
        //catch (e: Exception) { e.printStackTrace() } // we can't do it

        // here we print received values using `for` loop (until the channel is closed)
        for (y in channel) {
            println(y)
        }
        println("Done")
    }

    /**
     * The pattern where a coroutine is producing a sequence of elements is quite common.
     * This is a part of producer-consumer pattern that is often found in concurrent code.
     * You could abstract such a producer into a function that takes channel as its parameter,
     *
     * --> but this goes contrary to common sense that results must be returned from functions. <--
     *
     * There is a convenience coroutine builder named produce that makes it easy
     * to do it right on producer side, and an extension function consumeEach,
     * that replaces a for loop on the consumer side:
     */
    private fun buildingChannelProducers() = runBlocking {

        fun produceSquares() = produce<Int> { for (x in 1..5) send(x * x) }

        val squares = produceSquares()
        squares.consumeEach { println(it) }

        println("Done!")

    }


    /**
     * A pipeline is a pattern where one coroutine is producing,
     * possibly infinite, stream of values:
     */
    private fun pipelines() = produce<Int> {

        fun produceNumbers() = produce<Int> {
            var x = 1; while (x < 228) send(x++) // infinity stream of integers starting from 1
        }
        fun square(numbers: ReceiveChannel<Int>) = produce<Int> {
            for (x in numbers) send(x * x)
        }


        val numbers = produceNumbers() // produces integers from 1 and on
        val squares = square(numbers)  // squares integers

        for (i in 1..10) println(squares.receive()) // print first five.

        println("Done!") // we are done
        squares.cancel() // need to cancel these coroutines in a larger app
        numbers.cancel()


        /**
         * We don't have to cancel these coroutines in this example app, because coroutines
         * are like daemon threads, but in a larger app we'll need to stop our pipeline if
         * we don't need it anymore.
         * Alternatively, we could have run pipeline coroutines as children of a
         * main coroutine as is demonstrated in the following example.
         */
    }


    /**
     * The following example prints the first ten prime numbers, running the whole pipeline
     * in the context of the main thread. Since all the coroutines are launched as children
     * of the main runBlocking coroutine in its coroutineContext, we don't have to keep an
     * explicit list of all the coroutines we have started.
     * We use cancelChildren extension function to cancel all the children coroutines.
     */
    private fun primeNumbersWithPipeline() = runBlocking {
        /**
         * Let's take pipelines to the extreme with an example that generates prime numbers
         * using a pipeline of coroutines. We start with an infinite sequence of numbers.
         * This time we introduce an explicit context parameter and pass it to produce builder,
         * so that caller can control where our coroutines run:
         */
        fun numbersFrom(context: CoroutineContext, start: Int) = produce<Int> {
            var x = start
            while (true) send(x++) // infinite stream of integers from start.
        }

        /**
         * The following pipeline stage filters an incoming stream of numbers,
         * removing all the numbers that are divisible by the given prime number:
         */
        fun filter(context: CoroutineContext, numbers: ReceiveChannel<Int>, prime: Int) = produce(context) {
            for (x in numbers) if (x % prime != 0) send(x)
        }
        /**
         * Now we build our pipeline by starting a stream of numbers from 2, taking a prime number
         * from the current channel, and launching new pipeline stage for each prime number found:
         */
        // numbersFrom(2) -> filter(2) -> filter(3) -> filter(5) -> filter(7) ...

        var cur = numbersFrom(coroutineContext, 2)
        for (i in 1..10) {
            val prime = cur.receive()
            println("prime=$prime")
            cur = filter(coroutineContext, cur, prime)
        }
        coroutineContext.cancelChildren() // cancel all children to let main finish

        /**
         * Note, that you can build the same pipeline using buildIterator coroutine builder
         * from the standard library.
         *
         * Replace:
         *   - produce with buildIterator,
         *   - send with yield,
         *   - receive with next,
         *   - ReceiveChannel with Iterator,
         *   - and get rid of the context.
         * You will not need runBlocking either.
         *
         * However, the benefit of a pipeline that uses channels as shown above is that
         * it can actually use multiple CPU cores if you run it in CommonPool context.
         *
         * Anyway, this is an extremely impractical way to find prime numbers.
         * In practice, pipelines do involve some other suspending invocations
         * (like asynchronous calls to remote services) and these pipelines cannot be built
         * using buildSequence/buildIterator, because they do not allow arbitrary suspension, unlike produce, which is fully asynchronous.
         */
    }

    /**
     * Multiple coroutines may receive from the same channel, distributing work between
     * themselves. Let us start with a producer coroutine that is periodically producing
     * integers (ten numbers per second):
     */
    private fun fanOut() = runBlocking {
        fun produceNumbers() = produce<Int> {
            var x = 1
            while (true) { send(x++); delay(500) }
        }
        fun launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
            for (msg in channel) println("Processor #$id received $msg")
        }

        val producer = produceNumbers()
        repeat(5) { launchProcessor(it, producer) }
        delay(4000)
        producer.cancel() // cancel producer coroutine and thus kill them all

        /**
         * Note, that cancelling a producer coroutine closes its channel, thus eventually
         * terminating iteration over the channel that processor coroutines are doing.
         *
         * Also, pay attention to how we explicitly iterate over channel with for loop to
         * perform fan-out in launchProcessor code. Unlike consumeEach, this for loop pattern
         * is perfectly safe to use from multiple coroutines. If one of the processor coroutines
         * fails, then others would still be processing the channel, while a processor that is
         * written via consumeEach always consumes (cancels) the underlying channel on its normal
         * or abnormal termination.
         */
    }


    /**
     * Now, let us see what happens if we launch a couple of coroutines sending strings (in this
     * example we launch them in the context of the main thread as main coroutine's children):
     */
    private fun fanIn() = runBlocking {
        /**
         * Multiple coroutines may send to the same channel. For example,
         * let us have a channel of strings, and a suspending function that repeatedly
         * sends a specified string to this channel with a specified delay:
         */
        suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
            while (true) { delay(time); channel.send(s) }
        }

        val channel = Channel<String>()
        launch(coroutineContext) { sendString(channel, "foo", 200L) }
        launch(coroutineContext) { sendString(channel, "bar!", 500L) }
        repeat(6) { // receive first six
            println(channel.receive())
        }
        coroutineContext.cancelChildren()
    }


    /**
     * The channels shown so far had no buffer. Unbuffered channels transfer elements when
     * sender and receiver meet each other (aka rendezvous). If send is invoked first, then
     * it is suspended until receive is invoked, if receive is invoked first, it is suspended
     * until send is invoked.
     * Both Channel() factory function and produce builder take an optional capacity parameter
     * to specify buffer size. Buffer allows senders to send multiple elements before suspending,
     * similar to the BlockingQueue with a specified capacity, which blocks when buffer is full.
     */
    private fun bufferedChannels() = runBlocking {
        val channel = Channel<Int>(4) // create buffered channel
        val senser = launch(coroutineContext) { // launch sender coroutine
            repeat(10) {
                println("Sending $it") // print before sensing each element
                channel.send(it) // will suspend when buffer is full
            }
        }
        // don't receive anything... just wait.....
        delay(1000)
        senser.cancel() // cancel sender coroutine
    }

    private fun tickerChannels() = runBlocking {

    }

    private fun channelsAreFair() = runBlocking {

    }



}

