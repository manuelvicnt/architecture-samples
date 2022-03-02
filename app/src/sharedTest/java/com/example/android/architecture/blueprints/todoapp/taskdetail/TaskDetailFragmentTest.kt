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
package com.example.android.architecture.blueprints.todoapp.taskdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.Navigation.findNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.ServiceLocator
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.FakeRepository
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.tasks.TasksActivity
import com.example.android.architecture.blueprints.todoapp.util.saveTaskBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for the Task Details screen.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class TaskDetailFragmentTest {

    private lateinit var repository: TasksRepository

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TasksActivity>()

    @Before
    fun initRepository() {
        repository = FakeRepository()
        ServiceLocator.tasksRepository = repository
    }

    @After
    fun cleanupDb() = runBlockingTest {
        ServiceLocator.resetRepository()
    }

    @Test
    fun activeTaskDetails_DisplayedInUi() {
        // GIVEN - Add active (incomplete) task to the DB
        val activeTask = Task("Active Task", "AndroidX Rocks", false)
        repository.saveTaskBlocking(activeTask)

        // WHEN - Details fragment launched to display task
        launchFragment(activeTask)

        // THEN - Task details are displayed on the screen
        // make sure that the title/description are both shown and correct
        composeTestRule.onNodeWithText("Active Task").assertIsDisplayed()
        composeTestRule.onNodeWithText("AndroidX Rocks").assertIsDisplayed()
        // and make sure the "active" checkbox is shown unchecked
        composeTestRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun completedTaskDetails_DisplayedInUi() {
        // GIVEN - Add completed task to the DB
        val completedTask = Task("Completed Task", "AndroidX Rocks", true)
        repository.saveTaskBlocking(completedTask)

        // WHEN - Details fragment launched to display task
        launchFragment(completedTask)

        // THEN - Task details are displayed on the screen
        // make sure that the title/description are both shown and correct
        composeTestRule.onNodeWithText("Completed Task").assertIsDisplayed()
        composeTestRule.onNodeWithText("AndroidX Rocks").assertIsDisplayed()
        // and make sure the "active" checkbox is shown unchecked
        composeTestRule.onNode(isToggleable()).assertIsOn()
    }

    private fun launchFragment(activeTask: Task) {
        val bundle = TaskDetailFragmentArgs(activeTask.id).toBundle()
        composeTestRule.activityRule.scenario.onActivity {
            findNavController(it, R.id.nav_host_fragment).apply {
                setGraph(R.navigation.nav_graph)
                navigate(R.id.task_detail_fragment_dest, bundle)
            }
        }
    }
}
