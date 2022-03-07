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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.drawerlayout.widget.DrawerLayout
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.pressBack
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the [DrawerLayout] layout component in [TasksActivity] which manages
 * navigation within the app.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppNavigationTest {

    private lateinit var tasksRepository: TasksRepository

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TasksActivity>()
    private val activity by lazy { composeTestRule.activity }

    @Before
    fun init() {
        // Run on UI thread to make sure the same instance of the SL is used.
        runOnUiThread {
            ServiceLocator.createDataBase(getApplicationContext(), inMemory = true)
            tasksRepository = ServiceLocator.provideTasksRepository(getApplicationContext())
        }
    }

    @After
    fun reset() {
        runOnUiThread {
            ServiceLocator.resetRepository()
        }
    }

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

    @Test
    fun drawerNavigationFromTasksToStatistics() {
        openDrawer()
        // Start statistics screen.
        composeTestRule.onNodeWithText(activity.getString(R.string.statistics_title)).performClick()
        // Check that statistics screen was opened.
        composeTestRule.onNodeWithText(activity.getString(R.string.statistics_no_tasks))
            .assertIsDisplayed()

        openDrawer()
        // Start tasks screen.
        composeTestRule.onNodeWithText(activity.getString(R.string.list_title)).performClick()
        // Check that tasks screen was opened.
        composeTestRule.onNodeWithText(activity.getString(R.string.no_tasks_all))
            .assertIsDisplayed()
    }

    @Test
    fun tasksScreen_clickOnAndroidHomeIcon_OpensNavigation() {
        // Check that left drawer is closed at startup
        composeTestRule.onNodeWithText(activity.getString(R.string.list_title))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(activity.getString(R.string.statistics_title))
            .assertIsNotDisplayed()

        openDrawer()

        // Check if drawer is open
        composeTestRule.onNodeWithText(activity.getString(R.string.list_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(activity.getString(R.string.statistics_title))
            .assertIsDisplayed()
    }

    @Test
    fun statsScreen_clickOnAndroidHomeIcon_OpensNavigation() {
        // When the user navigates to the stats screen
        openDrawer()
        composeTestRule.onNodeWithText(activity.getString(R.string.statistics_title)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(activity.getString(R.string.list_title))
            .assertIsNotDisplayed()

        openDrawer()

        // Check if drawer is open
        composeTestRule.onNodeWithText(activity.getString(R.string.list_title)).assertIsDisplayed()
        assertTrue(
            composeTestRule.onAllNodesWithText(activity.getString(R.string.statistics_title))
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun taskDetailScreen_doubleUIBackButton() {
        val task = Task("UI <- button", "Description")
        tasksRepository.saveTaskBlocking(task)
        composeTestRule.waitForIdle()

        // Click on the task on the list
        composeTestRule.onNodeWithText("UI <- button").assertIsDisplayed()
        composeTestRule.onNodeWithText("UI <- button").performClick()

        // Click on the edit task button
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.edit_task))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.edit_task))
            .performClick()

        // Confirm that if we click "<-" once, we end up back at the task details page
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_back))
            .performClick()
        composeTestRule.onNodeWithText("UI <- button").assertIsDisplayed()

        // Confirm that if we click "<-" a second time, we end up back at the home screen
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.menu_back))
            .performClick()
        composeTestRule.onNodeWithText("All Tasks").assertIsDisplayed()
    }

    @Test
    fun taskDetailScreen_doubleBackButton() {
        val task = Task("Back button", "Description")
        tasksRepository.saveTaskBlocking(task)
        composeTestRule.waitForIdle()

        // Click on the task on the list
        composeTestRule.onNodeWithText("Back button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back button").performClick()
        // Click on the edit task button
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.edit_task))
            .performClick()

        // Confirm that if we click back once, we end up back at the task details page
        pressBack()
        composeTestRule.onNodeWithText("Back button").assertIsDisplayed()

        // Confirm that if we click back a second time, we end up back at the home screen
        pressBack()
        composeTestRule.onNodeWithText("All Tasks").assertIsDisplayed()
    }

    private fun openDrawer() {
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.open_drawer))
            .performClick()
    }
}
