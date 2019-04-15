package org.foundation101.karatel

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers

object TestUtils {
    fun checkViewArrayIsDisplayed(views: Array<Int>) = views.forEach {
        Espresso.onView(ViewMatchers.withId(it)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun checkViewArrayIsDisplayedWithScroll(views: Array<Int>) = views.forEach {
        Espresso.onView(ViewMatchers.withId(it))
                .perform(ViewActions.scrollTo())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun checkChildViewArrayIsDisplayedWithScroll(views: Array<Int>) = views.forEach {
        Espresso.onView(ViewMatchers.withChild(ViewMatchers.withId(it)))
                .perform(ViewActions.scrollTo())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}