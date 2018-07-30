package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_channels.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

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
        squares.consumeEach {  }

    }

    private fun pipelines() = runBlocking {

    }

    private fun primeNumbersWithPipeline() = runBlocking {

    }

    private fun fanOut() = runBlocking {

    }

    private fun fanIn() = runBlocking {

    }

    private fun bufferedChannels() = runBlocking {

    }

    private fun tickerChannels() = runBlocking {

    }

    private fun channelsAreFair() = runBlocking {

    }



}

