package com.freshdigitable.udonroad;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.CallSuper;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.lifecycle.Stage;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.oauth.OAuthActivity;
import com.freshdigitable.udonroad.util.IdlingResourceUtil;
import com.freshdigitable.udonroad.util.PerformUtil;
import com.freshdigitable.udonroad.util.StorageUtil;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;

import twitter4j.Twitter;
import twitter4j.TwitterStream;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.AssertionUtil.checkMainActivityTitle;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getActivityStageIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofQuotedStatusView;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2017/04/20.
 */
@RunWith(Enclosed.class)
public class OAuthActivityInstTest {
  public static class WhenLaunch extends Base {
    @Rule
    public final IntentsTestRule<MainActivity> rule
        = new IntentsTestRule<>(MainActivity.class, false, false);

    @Test
    public void launchMainActivityWithNoAccessToken_then_OAuthActivityIsLaunched() {
      rule.launchActivity(new Intent());
      onView(withId(R.id.oauth_start)).check(matches(isDisplayed()));
    }

    @Override
    void tearDownActivity() {
      IdlingResourceUtil.ActivityWaiter.create(OAuthActivity.class).waitForDestroyed();
    }
  }

  public static class WhenResume extends Base {
    private static final String VALID_PIN = "0000000";
    @Rule
    public final IntentsTestRule<OAuthActivity> rule
        = new IntentsTestRule<>(OAuthActivity.class, false, false);
    private String authorizationUrl;

    @Test
    public void resumeWithValidToken() {
      verify(twitter, times(0)).setOAuthAccessToken(any(AccessToken.class));
      intending(hasData(Uri.parse(authorizationUrl)))
          .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, new Intent()));
      onView(withId(R.id.oauth_start)).perform(click());

      onView(withId(R.id.oauth_pin)).perform(typeText(VALID_PIN));
      onView(withId(R.id.oauth_send_pin)).perform(click());

      runWithIdlingResource(
          getActivityStageIdlingResource("launchMain", MainActivity.class, Stage.RESUMED), () ->
              checkMainActivityTitle(R.string.title_home));

      intended(hasComponent(MainActivity.class.getName()));
      InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
        try {
          appSetting.open();
          assertThat(appSetting.getCurrentUserAccessToken().getToken(), is("valid.token"));
          assertThat(appSetting.getCurrentUserAccessToken().getTokenSecret(), is("valid.secret"));
          assertThat(appSetting.getCurrentUserId(), is(100L));
        } finally {
          appSetting.close();
        }
      });
       verify(twitter, times(1)).setOAuthAccessToken(any(AccessToken.class));
    }

    @Test
    public void favDemoTweet() {
      PerformUtil.selectItemViewAt(1);
      onView(withId(R.id.ffab)).check(matches(isDisplayed()));
      PerformUtil.favo();
      onView(withText(R.string.msg_oauth_fav)).check(matches(isDisplayed()));
    }

    @Test
    public void favDemoQuotedTweet() {
      PerformUtil.selectQuotedItemView(ofQuotedStatusView(withText(R.string.oauth_demo_quoted)));
      onView(withId(R.id.ffab)).check(matches(isDisplayed()));
      PerformUtil.favo();
      onView(withText(R.string.msg_oauth_fav)).check(matches(isDisplayed()));
    }

    @Test
    public void clickUserIcon() {
      PerformUtil.clickUserIconAt(1);
      onView(withText(R.string.msg_oauth_user_icon)).check(matches(isDisplayed()));
    }

    @Before
    public void setup() throws Exception {
      super.setup();
      final RequestToken requestToken = new RequestToken("req.token", "req.token.secret");
      authorizationUrl = requestToken.getAuthorizationURL();
      when(twitter.getOAuthRequestToken(eq("oob"))).thenReturn(requestToken);

      final AccessToken accessToken = mock(AccessToken.class);
      when(accessToken.getUserId()).thenReturn(100L);
      when(accessToken.getToken()).thenReturn("valid.token");
      when(accessToken.getTokenSecret()).thenReturn("valid.secret");
      when(twitter.getOAuthAccessToken(ArgumentMatchers.<RequestToken>any(), eq(VALID_PIN)))
          .thenReturn(accessToken);

      final Intent launch = OAuthActivity.createIntent(InstrumentationRegistry.getTargetContext(), MainActivity.class);
      rule.launchActivity(launch);
    }

    @Override
    void tearDownActivity() {
      IdlingResourceUtil.ActivityWaiter.create(MainActivity.class).waitForDestroyed();
    }
  }

  static abstract class Base {
    AppSettingStore appSetting;
    Twitter twitter;
    TwitterStream twitterStream;

    @Before
    @CallSuper
    public void setup() throws Exception {
      StorageUtil.initStorage();
      final MockMainApplication app = MockMainApplication.getApp();
      twitter = app.twitterApiModule.twitter;
      twitterStream = app.twitterApiModule.twitterStream;
      appSetting = app.sharedPreferenceModule.appSettingStore;
      app.twitterApiModule.setup();

      appSetting.open();
      appSetting.clear();
      appSetting.close();
      when(twitter.getAPIConfiguration()).thenThrow(IllegalStateException.class);
      when(twitter.getId()).thenThrow(IllegalStateException.class);
      when(twitter.showUser(anyLong())).thenAnswer(invocation -> {
        final long id = invocation.getArgument(0);
        if (id == 100) {
          return UserUtil.builder(100, "user100").name("user 100").build();
        }
        throw new IllegalStateException();
      });
      when(twitter.verifyCredentials()).thenThrow(IllegalStateException.class);
    }

    @After
    @CallSuper
    public void tearDown() throws Exception {
      MockMainApplication.getApp().twitterApiModule.reset();
      tearDownActivity();
      StorageUtil.checkAllRealmInstanceClosed();
    }

    abstract void tearDownActivity() throws Exception;
  }
}
