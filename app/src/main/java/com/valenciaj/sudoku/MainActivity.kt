package com.valenciaj.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.valenciaj.sudoku.ui.FeedbackPlayer
import com.valenciaj.sudoku.ui.GameViewModel
import com.valenciaj.sudoku.ui.SudokuApp
import com.valenciaj.sudoku.ui.SudokuTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()
    private lateinit var feedbackPlayer: FeedbackPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        feedbackPlayer = FeedbackPlayer(this)
        lifecycleScope.launch {
            vm.feedback.collect { feedbackPlayer.play(it, vm.settings.value) }
        }
        setContent {
            val settings by vm.settings.collectAsState()
            SudokuTheme(settings) {
                SudokuApp(vm)
            }
        }
    }

    override fun onDestroy() {
        feedbackPlayer.release()
        super.onDestroy()
    }
}
