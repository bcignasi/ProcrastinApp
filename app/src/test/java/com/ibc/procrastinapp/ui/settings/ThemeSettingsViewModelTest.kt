package com.ibc.procrastinapp.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import com.ibc.procrastinapp.data.preferences.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSettingsViewModelTest {
    private val store = TestPreferencesStore()
    private val preferences = ThemePreferences(store)
    private val viewModelStore = ViewModelStore()
    private lateinit var viewModel: ThemeSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = ThemeSettingsViewModel(preferences)
        viewModelStore.put("theme", viewModel)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadPreservesThePhoneThemeUntilTheUserChooses() = runTest {
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.darkTheme)
    }

    @Test
    fun switchingUpdatesTheVisibleAndStoredSelection() = runTest {
        advanceUntilIdle()

        for (darkTheme in listOf(true, false)) {
            viewModel.setDarkTheme(darkTheme)
            advanceUntilIdle()

            assertEquals(darkTheme, viewModel.uiState.value.darkTheme)
            assertEquals(darkTheme, preferences.darkTheme.first())
            assertFalse(viewModel.uiState.value.isSaving)
            assertFalse(viewModel.uiState.value.saveFailed)
        }
    }

    @Test
    fun failedSaveKeepsThePreviousThemeAndAllowsRetrying() = runTest {
        advanceUntilIdle()
        viewModel.setDarkTheme(false)
        advanceUntilIdle()
        store.failWrites = true

        viewModel.setDarkTheme(true)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.darkTheme)
        assertEquals(false, preferences.darkTheme.first())
        assertTrue(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.isSaving)

        store.failWrites = false
        viewModel.setDarkTheme(true)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.darkTheme)
        assertFalse(viewModel.uiState.value.saveFailed)
    }

    private class TestPreferencesStore : DataStore<Preferences> {
        override val data = MutableStateFlow<Preferences>(emptyPreferences())
        var failWrites = false

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (failWrites) throw IOException("Storage unavailable")
            return transform(data.value).also { data.value = it }
        }
    }
}
