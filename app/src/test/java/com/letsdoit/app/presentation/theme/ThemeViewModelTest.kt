package com.letsdoit.app.presentation.theme

import com.letsdoit.app.domain.model.ThemeColor
import com.letsdoit.app.domain.model.ThemeMode
import com.letsdoit.app.domain.repository.PreferencesRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val preferencesRepository: PreferencesRepository = mockk(relaxed = true)
    private lateinit var viewModel: ThemeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Default mocks
        every { preferencesRepository.getThemeModeFlow() } returns flowOf(ThemeMode.SYSTEM)
        every { preferencesRepository.getThemeColorFlow() } returns flowOf(ThemeColor.PURPLE)
        every { preferencesRepository.getDynamicColorEnabledFlow() } returns flowOf(true)
        every { preferencesRepository.getThemeMode() } returns ThemeMode.SYSTEM
        every { preferencesRepository.getThemeColor() } returns ThemeColor.PURPLE
        every { preferencesRepository.isDynamicColorEnabled() } returns true

        viewModel = ThemeViewModel(preferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        // Then
        val state = viewModel.themeState.value
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(ThemeColor.PURPLE, state.themeColor)
        assertEquals(true, state.isDynamicColorEnabled)
    }

    @Test
    fun `setThemeColor updates repository`() = runTest {
        // When
        viewModel.setThemeColor(ThemeColor.BLUE)

        // Then
        // Allow time for coroutine to launch
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferencesRepository.setThemeColor(ThemeColor.BLUE) }
    }
}
