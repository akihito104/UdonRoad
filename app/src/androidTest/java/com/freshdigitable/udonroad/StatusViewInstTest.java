/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.Html;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by akihit on 2016/06/08.
 */
@RunWith(AndroidJUnit4.class)
public class StatusViewInstTest {
  private StatusView sut;

  @Before
  public void setup() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
      @Override
      public void run() {
        sut = new StatusView(InstrumentationRegistry.getTargetContext());
      }
    });
  }

  @Test
  public void bindStatus_normal_0RT_0fav() throws Exception {
    Status status = createStatus(10);
    sut.bindStatus(status);
    final TextView account = (TextView) sut.findViewById(R.id.tl_names);
    final User user = status.getUser();
    final String format = String.format(account.getResources().getString(
        R.string.tweet_name_screenName),
        user.getName(), user.getScreenName());
    assertThat(account.getText().toString(), is(Html.fromHtml(format).toString()));
  }
}