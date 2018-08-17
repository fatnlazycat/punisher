package org.foundation101.karatel;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.foundation101.karatel.activity.ChangePasswordActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class Open_ForgotPassword_Test {
    @Rule
    public ActivityTestRule<ChangePasswordActivity> activityTestRule
            = new ActivityTestRule<>(ChangePasswordActivity.class);

    @Test
    public void openForgotPasswordTest() {
        onView((withId(R.id.forgotPasswButton))).perform(click());
        onView(withId(R.id.llActivityForgotPassword)).check(matches(isDisplayed()));
    }

}
