package com.freshdigitable.udonroad;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.intent.matcher.IntentMatchers;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.util.StorageUtil;
import com.freshdigitable.udonroad.util.TestInjectionUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;

import java.util.Collection;

import javax.inject.Inject;

import twitter4j.Twitter;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2017/04/20.
 */
@RunWith(Enclosed.class)
public class OAuthActivityInstTest {
  public static class WhenLaunch {
    @Rule
    public final IntentsTestRule<MainActivity> rule
        = new IntentsTestRule<>(MainActivity.class, false, false);

    @Inject
    AppSettingStore appSetting;
    @Inject
    Twitter twitter;

    @Before
    public void setup() throws Exception {
      StorageUtil.initStorage();
      TestInjectionUtil.getComponent().inject(this);
      appSetting.open();
      appSetting.clear();
      appSetting.close();
      when(twitter.getAPIConfiguration()).thenThrow(IllegalStateException.class);
      when(twitter.getId()).thenThrow(IllegalStateException.class);
      when(twitter.showUser(anyLong())).thenThrow(IllegalStateException.class);
      when(twitter.verifyCredentials()).thenThrow(IllegalStateException.class);
    }

    @After
    public void tearDown() throws Exception {
      reset(twitter);
      InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
        final Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
        assertThat(activities.size(), is(not(0)));
        final Activity oAuthActivity = findActivity(activities, OAuthActivity.class);
        assertThat(oAuthActivity, is(not(nullValue())));
        oAuthActivity.finish();
      });
      Thread.sleep(800);
      StorageUtil.checkAllRealmInstanceCleared();
    }

    @Test
    public void launchMainActivityWithNoAccessToken_then_OAuthActivityIsLaunched() {
      rule.launchActivity(new Intent());
      onView(withId(R.id.button_oauth)).check(matches(isDisplayed()));
    }
  }

  private static Activity findActivity(Collection<Activity> activities, Class<? extends Activity> clz) {
    for (Activity activity : activities) {
      if (activity.getClass().isAssignableFrom(clz)) {
        return activity;
      }
    }
    return null;
  }

  public static class WhenResume {
    @Rule
    public final IntentsTestRule<OAuthActivity> rule
        = new IntentsTestRule<>(OAuthActivity.class, false, false);

    @Inject
    AppSettingStore appSetting;
    @Inject
    Twitter twitter;

    @Before
    public void setup() throws Exception {
      StorageUtil.initStorage();
      TestInjectionUtil.getComponent().inject(this);
      appSetting.open();
      appSetting.clear();
      appSetting.close();
      when(twitter.getAPIConfiguration()).thenThrow(IllegalStateException.class);
      when(twitter.getId()).thenThrow(IllegalStateException.class);
      when(twitter.showUser(anyLong())).thenThrow(IllegalStateException.class);
      when(twitter.verifyCredentials()).thenThrow(IllegalStateException.class);

      final AccessToken accessToken = mock(AccessToken.class);
      when(accessToken.getUserId()).thenReturn(100L);
      when(accessToken.getToken()).thenReturn("valid.token");
      when(accessToken.getTokenSecret()).thenReturn("valid.secret");
      when(twitter.getOAuthAccessToken(ArgumentMatchers.<RequestToken>any(), eq("valid.token")))
          .thenReturn(accessToken);
    }

    @After
    public void tearDown() throws Exception {
      reset(twitter);
      InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
        final Collection<Activity> resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
        final Activity activity = findActivity(resumedActivities, MainActivity.class);
        assertThat(activity, is(notNullValue()));
        activity.finish();
      });
      Thread.sleep(1000);
      StorageUtil.checkAllRealmInstanceCleared();
    }

    @Test
    public void resumeWithValidToken() throws Exception {
      final Intent launch = new Intent();
      launch.putExtra("redirect", MainActivity.class);
      rule.launchActivity(launch);

      Intent home = new Intent();
      home.setAction(Intent.ACTION_MAIN);
      home.addCategory(Intent.CATEGORY_HOME);
      home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      InstrumentationRegistry.getTargetContext().startActivity(home);
      Thread.sleep(500);

      final String callbackUrl = InstrumentationRegistry.getTargetContext().getString(R.string.callback_url);
      final Uri data = Uri.parse(callbackUrl + "?oauth_verifier=valid.token");
      assertThat(data.getQueryParameter("oauth_verifier"), is("valid.token"));
      final Intent fromBrowser = new Intent(Intent.ACTION_VIEW, data);
      fromBrowser.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
      rule.getActivity().startActivity(fromBrowser);
      Thread.sleep(500);

      try{
        appSetting.open();
        assertThat(appSetting.getCurrentUserAccessToken().getToken(), is("valid.token"));
        assertThat(appSetting.getCurrentUserAccessToken().getTokenSecret(), is("valid.secret"));
        assertThat(appSetting.getCurrentUserId(), is(100L));
      } finally {
        appSetting.close();
      }
      intended(IntentMatchers.hasComponent(MainActivity.class.getName()));

      final IdlingResource idlingResource = new IdlingResource() {
        @Override
        public String getName() {
          return "launchMain";
        }

        @Override
        public boolean isIdleNow() {
          final Collection<Activity> resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
          for (Activity a : resumedActivities) {
            if (a instanceof MainActivity) {
              return true;
            }
          }
          return false;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {

        }
      };
      try {
        Espresso.registerIdlingResources(idlingResource);
        onView(withText("Home")).check(matches(isDisplayed()));
      } finally {
        Espresso.unregisterIdlingResources(idlingResource);
      }
    }
  }
}
