package com.valenciaj.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.valenciaj.sudoku.ui.GameViewModel
import com.valenciaj.sudoku.ui.SudokuApp
import com.valenciaj.sudoku.ui.SudokuTheme

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsState()
            SudokuTheme(settings) {
                SudokuApp(vm)
            }
        }
    }
}
