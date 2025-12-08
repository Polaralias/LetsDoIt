package com.letsdoit.app.presentation.taskdetails

import androidx.lifecycle.SavedStateHandle
import com.letsdoit.app.core.util.Constants
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.usecase.project.GetSelectedProjectUseCase
import com.letsdoit.app.domain.usecase.task.CreateTaskUseCase
import com.letsdoit.app.domain.usecase.task.GetTaskUseCase
import com.letsdoit.app.domain.usecase.task.UpdateTaskUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class TaskDetailViewModelTest {

    private val getTaskUseCase: GetTaskUseCase = mockk()
    private val createTaskUseCase: CreateTaskUseCase = mockk(relaxed = true)
    private val updateTaskUseCase: UpdateTaskUseCase = mockk(relaxed = true)
    private val getSelectedProjectUseCase: GetSelectedProjectUseCase = mockk()
    private lateinit var viewModel: TaskDetailViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when taskId is new, init creates empty task`() = runTest {
        every { getSelectedProjectUseCase.getSync() } returns Constants.DEMO_LIST_ID
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "new"))
        viewModel = TaskDetailViewModel(getTaskUseCase, createTaskUseCase, updateTaskUseCase, getSelectedProjectUseCase, savedStateHandle)

        val state = viewModel.uiState.value
        assertNotNull(state.task)
        assertEquals("", state.task?.title)
        assertEquals(Constants.DEMO_LIST_ID, state.task?.listId)
        assertNull(state.loadError)
    }

    @Test
    fun `when taskId is valid, init loads task`() = runTest {
        val taskId = "123"
        val task = Task(
            id = taskId,
            listId = "list1",
            title = "Existing Task",
            description = "Desc",
            status = "Open",
            dueDate = null,
            priority = 1
        )
        coEvery { getTaskUseCase(taskId) } returns task
        every { getSelectedProjectUseCase.getSync() } returns "list1"

        val savedStateHandle = SavedStateHandle(mapOf("taskId" to taskId))
        viewModel = TaskDetailViewModel(getTaskUseCase, createTaskUseCase, updateTaskUseCase, getSelectedProjectUseCase, savedStateHandle)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(task, state.task)
        assertNull(state.loadError)
    }

    @Test
    fun `onTitleChange updates task title`() = runTest {
        every { getSelectedProjectUseCase.getSync() } returns Constants.DEMO_LIST_ID
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "new"))
        viewModel = TaskDetailViewModel(getTaskUseCase, createTaskUseCase, updateTaskUseCase, getSelectedProjectUseCase, savedStateHandle)

        viewModel.onTitleChange("New Title")

        assertEquals("New Title", viewModel.uiState.value.task?.title)
    }

    @Test
    fun `saveTask calls create when new`() = runTest {
        every { getSelectedProjectUseCase.getSync() } returns Constants.DEMO_LIST_ID
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "new"))
        viewModel = TaskDetailViewModel(getTaskUseCase, createTaskUseCase, updateTaskUseCase, getSelectedProjectUseCase, savedStateHandle)

        viewModel.onTitleChange("Task to Save")
        viewModel.saveTask()

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { createTaskUseCase(any()) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saveTask sets saveError when fails`() = runTest {
        every { getSelectedProjectUseCase.getSync() } returns Constants.DEMO_LIST_ID
        val savedStateHandle = SavedStateHandle(mapOf("taskId" to "new"))
        viewModel = TaskDetailViewModel(getTaskUseCase, createTaskUseCase, updateTaskUseCase, getSelectedProjectUseCase, savedStateHandle)

        coEvery { createTaskUseCase(any()) } throws RuntimeException("Error")

        viewModel.onTitleChange("Task to Save")
        viewModel.saveTask()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Error", viewModel.uiState.value.saveError)
    }
}
