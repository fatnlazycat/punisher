package org.foundation101.karatel

import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.ViewMatchers

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