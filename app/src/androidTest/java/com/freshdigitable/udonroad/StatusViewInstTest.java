/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import twitter4j.Status;
import twitter4j.User;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

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
    Status status = create();
    sut.bindStatus(status);
    final TextView account = (TextView) sut.findViewById(R.id.tl_displayname);
    assertThat(account.getText().toString(), is(status.getUser().getScreenName()));
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