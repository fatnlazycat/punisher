package org.foundation101.karatel

import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf

object TestUtils {
    fun checkViewArrayIsDisplayed(views: Array<Int>) = views.forEach {
        Espresso.onView(ViewMatchers.withId(it)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun checkViewArrayIsDisplayedWithScroll(views: Array<Int>) = views.forEach {
        Espresso.onView(ViewMatchers.withId(it))
                .perform(ScrollToAction())
                //.perform(ViewActions.scrollTo())
                //.perform(ViewActions.swipeUp())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun checkChildViewArrayIsDisplayedWithScroll(views: Array<Int>) = views.forEach {
        Espresso.onView(ViewMatchers.withChild(ViewMatchers.withId(it)))
                .perform(ViewActions.scrollTo())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}

class ScrollToAction(
    private val original: androidx.test.espresso.action.ScrollToAction = androidx.test.espresso.action.ScrollToAction()
) : ViewAction by original {

    override fun getConstraints(): Matcher<View> = anyOf(
        allOf(
            withEffectiveVisibility(Visibility.VISIBLE),
            isDescendantOfA(isAssignableFrom(NestedScrollView::class.java))
        ),
        original.constraints
    )
}