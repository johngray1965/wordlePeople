package io.legere.wordlepeople

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.legere.wordlepeople.ui.main.MainFragment
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }

        Timber.plant(Timber.DebugTree())
    }
}
