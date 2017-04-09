/*
 * Copyright (c) 2016. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;

import twitter4j.Status;
import twitter4j.User;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

/**
 * Created by akihit on 2016/06/10.
 */
@RunWith(Enclosed.class)
public class StatusViewTest {
  private static final int rtColor = ContextCompat.getColor(RuntimeEnvironment.application,
      R.color.twitter_action_retweeted);
  private static final int normalColor = ContextCompat.getColor(RuntimeEnvironment.application,
      R.color.twitter_action_normal);

  @RunWith(RobolectricTestRunner.class)
  public static abstract class Base {

    StatusView sut;
    protected Status status;

    @Before
    public void setup() {
      sut = new StatusView(RuntimeEnvironment.application);
      status = create();
    }

    TextView findTextView(@IdRes int id) {
      return (TextView) sut.findViewById(id);
    }

    String getStringFrom(@IdRes int id) {
      return findTextView(id).getText().toString();
    }

    int getVisibilityFrom(@IdRes int id) {
      return sut.findViewById(id).getVisibility();
    }

    int getTextColorFrom(@IdRes int id) {
      final TextView v = findTextView(id);
      final ColorStateList textColors = v.getTextColors();
      return textColors.getColorForState(v.getDrawableState(), 0);
    }

    String fromFormattingResource(@StringRes int id, Object... item) {
      final String format = RuntimeEnvironment.application.getString(id);
      return String.format(format, item);
    }

    String makeNames(User user) {
      final String s = fromFormattingResource(R.string.tweet_name_screenName,
          user.getName(), user.getScreenName());
      return Html.fromHtml(s).toString();
    }

    void assertTextColor(@IdRes int id, @ColorInt int expected) {
      final int actual = getTextColorFrom(id);
      assertThat("actual color: " + Integer.toHexString(actual), actual, is(expected));
    }

    protected Status create() {
      final Status mockStatus = mock(Status.class);
      stub(mockStatus.getText()).toReturn("tweet text is here.");
      stub(mockStatus.getCreatedAt()).toReturn(new Date());
      stub(mockStatus.getSource())
          .toReturn("<a href=\"http://twitter.com/akihito104\" rel=\"nofollow\">Udonroad</a>");
      final User userMock = mock(User.class);
      stub(userMock.getName()).toReturn("akihito104");
      stub(userMock.getScreenName()).toReturn("akihito matsuda");
      stub(mockStatus.getUser()).toReturn(userMock);
      return mockStatus;
    }
  }

  public static class WhenStatusIsNormal extends Base {
    @Test
    public void bindStatus_normal_0RT_0Fav() throws Exception {
      stub(status.getRetweetCount()).toReturn(0);
      stub(status.getFavoriteCount()).toReturn(0);

      sut.bindStatus(status);

      commonAsserts();
      assertThat(getVisibilityFrom(R.id.tl_rtcount), is(View.GONE));
      assertThat(getVisibilityFrom(R.id.tl_favcount), is(View.GONE));
    }

    @Test
    public void bindStatus_normal_1RT_0fav() {
      stub(status.getRetweetCount()).toReturn(1);
      stub(status.getFavoriteCount()).toReturn(0);

      sut.bindStatus(status);

      commonAsserts();
      assertThat(getVisibilityFrom(R.id.tl_rtcount), is(View.VISIBLE));
      assertThat(getStringFrom(R.id.tl_rtcount), is("1"));
      assertThat(getVisibilityFrom(R.id.tl_favcount), is(View.GONE));
    }

    @Test
    public void bindStatus_normal_0RT_1fav() {
      stub(status.getRetweetCount()).toReturn(0);
      stub(status.getFavoriteCount()).toReturn(1);

      sut.bindStatus(status);

      commonAsserts();
      assertThat(getVisibilityFrom(R.id.tl_rtcount), is(View.GONE));
      assertThat(getVisibilityFrom(R.id.tl_favcount), is(View.VISIBLE));
      assertThat(getStringFrom(R.id.tl_favcount), is("1"));
    }

    @Test
    public void bindStatus_normal_1RT_1fav() {
      stub(status.getRetweetCount()).toReturn(1);
      stub(status.getFavoriteCount()).toReturn(1);

      sut.bindStatus(status);

      commonAsserts();
      assertThat(getVisibilityFrom(R.id.tl_rtcount), is(View.VISIBLE));
      assertThat(getStringFrom(R.id.tl_rtcount), is("1"));
      assertThat(getVisibilityFrom(R.id.tl_favcount), is(View.VISIBLE));
      assertThat(getStringFrom(R.id.tl_favcount), is("1"));
    }

    private void commonAsserts() {
      final User user = status.getUser();
      assertThat(getStringFrom(R.id.tl_tweet), is(status.getText()));
      assertThat(getStringFrom(R.id.tl_via),
          is(fromFormattingResource(R.string.tweet_via, "Udonroad")));
      assertThat(getStringFrom(R.id.tl_names), is(makeNames(user)));
      assertTextColor(R.id.tl_tweet, Color.GRAY);
      assertTextColor(R.id.tl_names, Color.GRAY);
      assertTextColor(R.id.tl_create_at, Color.GRAY);
    }
  }

  public static class WhenStatusIsRetweeted extends Base {
    @Override
    protected Status create() {
      final Status retweetedStatus = super.create();
      stub(retweetedStatus.isRetweeted()).toReturn(true);
      stub(retweetedStatus.getRetweetCount()).toReturn(1);

      final Status mockStatus = mock(Status.class);
      stub(mockStatus.getRetweetedStatus()).toReturn(retweetedStatus);
      stub(mockStatus.isRetweet()).toReturn(true);
      stub(mockStatus.getCreatedAt()).toReturn(new Date());
      final User user = mock(User.class);
      stub(user.getScreenName()).toReturn("matsuda104");
      stub(mockStatus.getUser()).toReturn(user);
      return mockStatus;
    }

    @Test
    public void bindStatus_0fav() {
      final Status retweetedStatus = status.getRetweetedStatus();
      stub(retweetedStatus.getFavoriteCount()).toReturn(0);

      sut.bindStatus(status);

      final User retweetedStatusUser = retweetedStatus.getUser();
      assertThat(getStringFrom(R.id.tl_names), is(makeNames(retweetedStatusUser)));
      assertThat(getStringFrom(R.id.tl_tweet), is(retweetedStatus.getText()));
      assertTextColor(R.id.tl_tweet, rtColor);
      assertTextColor(R.id.tl_create_at, rtColor);
      assertTextColor(R.id.tl_names, rtColor);

      assertThat(getVisibilityFrom(R.id.tl_rtcount), is(View.VISIBLE));
      assertThat(getStringFrom(R.id.tl_rtcount), is("1"));
      assertThat(getVisibilityFrom(R.id.tl_favcount), is(View.GONE));
    }
  }
}