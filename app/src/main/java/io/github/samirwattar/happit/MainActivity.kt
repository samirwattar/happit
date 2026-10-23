package io.github.samirwattar.happit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.github.samirwattar.happit.ui.AppViewModel
import io.github.samirwattar.happit.ui.HappitRoot

/** AppCompatActivity so the in-app language switch works on every Android version. */
class MainActivity : AppCompatActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { HappitRoot(vm) }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }
}
