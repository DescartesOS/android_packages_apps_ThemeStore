/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.axion.axthemestore.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.axion.axthemestore.R
import com.android.axion.axthemestore.navbar.NavbarStyle
import com.android.axion.axthemestore.navbar.NavbarStyleController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NavbarUiState(
    val styles: List<NavbarStyle> = emptyList(),
    val activePackage: String? = null,
    val loading: Boolean = true,
    val applyingPackage: String? = null,
    val error: Int? = null,
)

class NavbarViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = NavbarStyleController(application)
    private val _state = MutableStateFlow(NavbarUiState())
    val state = _state.asStateFlow()
    private var operation: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (operation?.isActive == true) return
        operation =
            viewModelScope.launch {
                _state.update { it.copy(loading = true, error = null) }
                try {
                    val result = withContext(Dispatchers.IO) { controller.load() }
                    _state.update {
                        it.copy(styles = result.styles, activePackage = result.activePackage)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Could not load navbar styles", e)
                    _state.update { it.copy(error = R.string.navbar_load_failed) }
                } finally {
                    _state.update { it.copy(loading = false) }
                }
            }
    }

    fun apply(style: NavbarStyle) {
        if (operation?.isActive == true || style.packageName == _state.value.activePackage) return
        operation =
            viewModelScope.launch {
                _state.update { it.copy(applyingPackage = style.packageName, error = null) }
                try {
                    val active = withContext(Dispatchers.IO) { controller.apply(style.packageName) }
                    _state.update { it.copy(activePackage = active) }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Could not apply navbar style", e)
                    val active =
                        withContext(Dispatchers.IO) {
                            runCatching { controller.activePackage() }.getOrNull()
                        }
                    _state.update {
                        it.copy(activePackage = active, error = R.string.navbar_apply_failed)
                    }
                } finally {
                    _state.update { it.copy(applyingPackage = null) }
                }
            }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        private const val TAG = "NavbarViewModel"
    }
}
