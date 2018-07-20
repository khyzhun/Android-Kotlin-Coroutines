package com.sashakhyzhun.kotlincoroutines

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.sashakhyzhun.kotlincoroutines.trash.ChannelExampleActivity
import com.sashakhyzhun.kotlincoroutines.trash.DifferentRestApproachesActivity
import com.sashakhyzhun.kotlincoroutines.trash.ThreadsVsCoroutines
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.produce
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCoroutineBasics.setOnClickListener {
            startActivity<CoroutineBasicsActivity>()
        }

        btnCancellationAndTimeouts.setOnClickListener {
            startActivity<CancellationAndTimeoutsActivity>()
        }

        btnComposingSuspendingFunctions.setOnClickListener {
            startActivity<ComposingSuspendingFunctionsActivity>()
        }

        btnCoroutineContextAndDispatchers.setOnClickListener {
            startActivity<CoroutineContextAndDispatchersActivity>()
        }

        btnChannels.setOnClickListener {
            startActivity<ChannelsActivity>()
        }

        btnSharedMutableStateAndConcurrency.setOnClickListener {
            startActivity<SharedMutableStateAndConcurrency>()
        }

        btnSelectExpression.setOnClickListener {
            startActivity<SelectExpression>()
        }

    }


}
