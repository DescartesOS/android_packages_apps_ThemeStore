/*
 * Copyright (C) 2025 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.axion.axthemestore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.axion.axthemestore.ui.MainScreen
import com.android.axion.axthemestore.ui.theme.AxThemeStoreTheme
import com.android.axion.axthemestore.viewmodel.FontViewModel
import com.android.axion.axthemestore.viewmodel.ThemeStoreViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ThemeStoreViewModel by viewModels()
    private val fontViewModel: FontViewModel by viewModels()
    private var fontLaunchRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null && intent.action == ACTION_FONTS) fontLaunchRequest++

        setContent {
            AxThemeStoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen(viewModel, fontViewModel, fontLaunchRequest)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_FONTS) fontLaunchRequest++
    }

    companion object {
        const val ACTION_FONTS = "com.android.axion.axthemestore.action.FONTS"
    }
}
