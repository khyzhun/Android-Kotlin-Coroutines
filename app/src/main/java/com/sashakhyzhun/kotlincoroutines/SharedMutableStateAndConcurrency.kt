package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_shared_mutable_state_and_concurrency.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
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

        btn_the_problem.setOnClickListener {
            runBlocking {
                var counter = 0
                theProblem(context = CommonPool) {
                    counter++
                }
            }
        }
        btn_volatiles_are_of_no_help.setOnClickListener { volatilesAreOfNoHelp() }
        btn_thread_safe_data_structures.setOnClickListener { threadSafeDataStructures() }
        btn_thread_confinement_fine_grained.setOnClickListener { threadConfinementFineGrained() }
        btn_thread_confinement_coarse_grained.setOnClickListener { threadConfinementCoarseGrained() }
        btn_mutual_exclusion.setOnClickListener { mutualExclusion() }
        btn_actors.setOnClickListener { actors() }

    }

    /**
     * Let us launch a thousand coroutines all doing the same action thousand times
     * (for a total of a million executions).
     * We'll also measure their completion time for further comparisons:
     */
    private suspend fun theProblem(context: CoroutineContext, action: suspend () -> Unit) {
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
        print("Completed ${n * k} actions in $time ms")
    }

    private fun volatilesAreOfNoHelp() {}

    private fun threadSafeDataStructures() {}

    private fun threadConfinementFineGrained() {}

    private fun threadConfinementCoarseGrained() {}

    private fun mutualExclusion() {}

    private fun actors() {}


}

