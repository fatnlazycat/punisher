package org.foundation101.karatel;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.foundation101.karatel.activity.ChangePasswordActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

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
