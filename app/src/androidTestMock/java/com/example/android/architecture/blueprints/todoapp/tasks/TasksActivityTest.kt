/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.example.android.architecture.blueprints.todoapp.tasks

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.ServiceLocator
import com.example.android.architecture.blueprints.todoapp.TasksActivity
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import com.example.android.architecture.blueprints.todoapp.util.saveTaskBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Large End-to-End test for the tasks module.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TasksActivityTest {

    private lateinit var repository: TasksRepository

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TasksActivity>()
    private val activity by lazy { composeTestRule.activity }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Before
    fun init() {
        // Run on UI thread to make sure the same instance of the SL is used.
        runOnUiThread {
            ServiceLocator.createDataBase(getApplicationContext(), inMemory = true)
            repository = ServiceLocator.provideTasksRepository(getApplicationContext())
        }
    }

    @After
    fun reset() {
        runOnUiThread {
            ServiceLocator.resetRepository()
        }
    }

    @Test
    fun editTask() {
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION"))
        composeTestRule.waitForIdle()

        // Click on the task on the list and verify that all the data is correct
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
        composeTestRule.onNodeWithText("TITLE1").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
        composeTestRule.onNodeWithText("DESCRIPTION").assertIsDisplayed()
        composeTestRule.onNode(isToggleable()).assertIsOff()

        // Click on the edit button, edit, and save
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.edit_task))
            .performClick()
        findTextField("TITLE1").performTextReplacement("NEW TITLE")
        findTextField("DESCRIPTION").performTextReplacement("NEW DESCRIPTION")
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.cd_save_task))
            .performClick()

        // Verify task is displayed on screen in the task list.
        composeTestRule.onNodeWithText("NEW TITLE").assertIsDisplayed()
        // Verify previous task is not displayed
        composeTestRule.onNodeWithText("TITLE1").assertDoesNotExist()
    }

    @Test
    fun createOneTask_deleteTask() {
        // Add active task
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.add_task))
            .performClick()
        findTextField(R.string.title_hint).performTextInput("TITLE1")
        findTextField(R.string.description_hint).performTextInput("DESCRIPTION")
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.cd_save_task))
            .performClick()

        // Open it in details view
        composeTestRule.onNodeWithText("TITLE1").performClick()
        // Click delete task in menu
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_delete_task))
            .performClick()

        // Verify it was deleted
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_filter))
            .performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.nav_all)).performClick()
        composeTestRule.onNodeWithText("TITLE1").assertDoesNotExist()
    }

    @Test
    fun createTwoTasks_deleteOneTask() {
        repository.saveTaskBlocking(Task("TITLE1", "DESCRIPTION"))
        repository.saveTaskBlocking(Task("TITLE2", "DESCRIPTION"))
        composeTestRule.waitForIdle()

        // Open the second task in details view
        composeTestRule.onNodeWithText("TITLE2").assertIsDisplayed()
        composeTestRule.onNodeWithText("TITLE2").performClick()
        // Click delete task in menu
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_delete_task))
            .performClick()

        // Verify only one task was deleted
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_filter))
            .performClick()
        composeTestRule.onNodeWithText(activity.getString(R.string.nav_all)).performClick()
        composeTestRule.onNodeWithText("TITLE1").assertIsDisplayed()
        composeTestRule.onNodeWithText("TITLE2").assertDoesNotExist()
    }

    @Test
    fun markTaskAsCompleteOnDetailScreen_taskIsCompleteInList() {
        // Add 1 active task
        val taskTitle = "COMPLETED"
        repository.saveTaskBlocking(Task(taskTitle, "DESCRIPTION"))
        composeTestRule.waitForIdle()

        // Click on the task on the list
        composeTestRule.onNodeWithText(taskTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(taskTitle).performClick()

        // Click on the checkbox in task details screen
        composeTestRule.onNode(isToggleable()).performClick()

        // Click on the navigation up button to go back to the list
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_back))
            .performClick()

        // Check that the task is marked as completed
        composeTestRule.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun markTaskAsActiveOnDetailScreen_taskIsActiveInList() {
        // Add 1 completed task
        val taskTitle = "ACTIVE"
        repository.saveTaskBlocking(Task(taskTitle, "DESCRIPTION", true))
        composeTestRule.waitForIdle()

        // Click on the task on the list
        composeTestRule.onNodeWithText(taskTitle).performClick()

        // Click on the checkbox in task details screen
        composeTestRule.onNode(isToggleable()).performClick()

        // Click on the navigation up button to go back to the list
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_back))
            .performClick()

        // Check that the task is marked as active
        composeTestRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun markTaskAsCompleteAndActiveOnDetailScreen_taskIsActiveInList() {
        // Add 1 active task
        val taskTitle = "ACT-COMP"
        repository.saveTaskBlocking(Task(taskTitle, "DESCRIPTION"))
        composeTestRule.waitForIdle()

        // Click on the task on the list
        composeTestRule.onNodeWithText(taskTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(taskTitle).performClick()

        // Click on the checkbox in task details screen
        composeTestRule.onNode(isToggleable()).performClick()
        // Click again to restore it to original state
        composeTestRule.onNode(isToggleable()).performClick()

        // Click on the navigation up button to go back to the list
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_back))
            .performClick()

        // Check that the task is marked as active
        composeTestRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun markTaskAsActiveAndCompleteOnDetailScreen_taskIsCompleteInList() {
        // Add 1 completed task
        val taskTitle = "COMP-ACT"
        repository.saveTaskBlocking(Task(taskTitle, "DESCRIPTION", true))
        composeTestRule.waitForIdle()

        // Click on the task on the list
        composeTestRule.onNodeWithText(taskTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(taskTitle).performClick()
        // Click on the checkbox in task details screen
        composeTestRule.onNode(isToggleable()).performClick()
        // Click again to restore it to original state
        composeTestRule.onNode(isToggleable()).performClick()

        // Click on the navigation up button to go back to the list
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_back))
            .performClick()

        // Check that the task is marked as active
        composeTestRule.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun createTask() {
        // Click on the "+" button, add details, and save
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.add_task))
            .performClick()
        findTextField(R.string.title_hint).performTextInput("title")
        findTextField(R.string.description_hint).performTextInput("description")
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.cd_save_task))
            .performClick()

        // Then verify task is displayed on screen
        composeTestRule.onNodeWithText("title").assertIsDisplayed()
    }

    private fun findTextField(textId: Int): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasSetTextAction() and hasText(activity.getString(textId))
        )
    }

    private fun findTextField(text: String): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasSetTextAction() and hasText(text)
        )
    }
}