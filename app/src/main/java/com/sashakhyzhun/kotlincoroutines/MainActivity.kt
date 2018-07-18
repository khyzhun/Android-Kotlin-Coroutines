package com.sashakhyzhun.kotlincoroutines

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.produce

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        println("created")

        //differentApproachesWithOneProblem()
        //channelExample()
        //async { threadsVScoroutines() }
        //cancellation()

    }


    private fun differentApproachesWithOneProblem() {
        val da = DiffApproaches()

        // ez and naive
        button.setOnClickListener {
            val userString = da.fetchUserString("1")
            val user = da.deserializeUser(userString)
            da.showUserData(user)
        }

        // callback
        button.setOnClickListener {
            da.fetchUserString2("1") { userString ->
                da.deserializeUser2(userString) { user ->
                    da.showUserData(user)
                }
            }
        }

        // rx
        button.setOnClickListener {
            da.fetchUserString3("1")
                    .observeOn(Schedulers.io())
                    .map { da.deserializeUser3(it) }
                    .subscribe { user ->
                        val data = user.blockingFirst()
                        da.showUserData(data)
                    }
        }

        // coroutines
        button.setOnClickListener {
            launch {
                val userString = da.fetchUserString4("1").await()
                val user = da.deserializeUser4(userString).await()
                da.showUserData(user)
            }
        }
    }


    private fun channelExample() = runBlocking {
        val channel = Channel<Int>()

        // this might be heavy CPU-consuming computation
        // or async logic, we'll just send five squares

        launch {
            for (x in 1..5) channel.send(x * x)
        }

        // here we print five received integers:
        repeat(5) {
            println(channel.receive())
        }

        println("Done!")

    }


    private suspend fun threadsVScoroutines() {
        val jobs = List(100_000) {
            launch {
                //delay()
                println(".")
            }
        }
        jobs.forEach { it.join() }
        println("threadsVScoroutines done")
    }


    private fun cancellation() {
        val job = launch {
            // do work
        }
        job.cancel()

        val job2 = launch {
            while (isActive) {
                // do work
            }
        }
    }


    private fun generator() = produce<Int> {

    }





}
