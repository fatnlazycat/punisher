package org.foundation101.karatel

import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.foundation101.karatel.activity.MainActivity
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivity_Test {
    @Rule @JvmField
    val testRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    fun getDrawerItemTitle(i: Int) = KaratelApplication.getInstance().resources.getStringArray(R.array.drawerMenuItems)[i]

    @Test
    fun openFragmentsFromDrawer() {

        //keys - fragment numbers, values - array of views that should be displayed
        val data = mapOf(
            0 to arrayOf(R.id.llFragmentMain),
            1 to arrayOf(R.id.rlListOfRequests),
            2 to arrayOf(R.id.llComplainsBook),
            3 to arrayOf(R.id.lvVideos)
        )

        data.map {
            onView(withId(R.id.drawerLayoutMainActivity)).perform(DrawerActions.open())

            onData(`is`(getDrawerItemTitle(it.key)))
                    .inAdapterView(withId(R.id.drawerListView))
                    .perform(click())

            it.value
        }
        .flatMap { it.toList() }
        .forEach {
            onView(withId(it)).check(matches(isDisplayed()))
        }
    }

    fun openRequestListFromDrawer() {

    }

    fun openComplainsBookFromDrawer() {

    }
}