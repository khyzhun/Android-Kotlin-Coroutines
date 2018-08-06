package com.sashakhyzhun.kotlincoroutines

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_shared_mutable_state_and_concurrency.*

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

    /**
     * Let us launch a thousand coroutines all doing the same action thousand times
     * (for a total of a million executions).
     * We'll also measure their completion time for further comparisons:
     */
    private fun theProblem() {}

    private fun volatilesAreOfNoHelp() {}

    private fun threadSafeDataStructures() {}

    private fun threadConfinementFineGrained() {}

    private fun threadConfinementCoarseGrained() {}

    private fun mutualExclusion() {}

    private fun actors() {}


}

