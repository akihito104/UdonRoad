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

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.Html;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.freshdigitable.udonroad.databinding.ViewStatusBinding;
import com.freshdigitable.udonroad.listitem.StatusListItem;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/06/08.
 */
@RunWith(AndroidJUnit4.class)
public class StatusViewInstTest {
  private ViewStatusBinding sut;

  @Before
  @UiThreadTest
  public void setup() {
    final LayoutInflater layoutInflater = LayoutInflater.from(InstrumentationRegistry.getTargetContext());
    sut = ViewStatusBinding.inflate(layoutInflater, null, false);
  }

  @Test
  public void bindStatus_normal_0RT_0fav() {
    Status status = createStatus(10);
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    final TextView account = sut.tlNames;
    final User user = status.getUser();
    final String format = String.format(account.getResources().getString(
        R.string.tweet_name_screenName),
        user.getName(), user.getScreenName());
    assertThat(account.getText().toString(), is(Html.fromHtml(format).toString()));
  }

  @Test
  public void createAtNow() {
    final Status status = createStatusWithPast(0);
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualCreatedAt(), is(formattedString(R.string.created_now)));
  }

  @Test
  public void createAt_59secondsAgo() {
    final Status status = createStatusWithPast(TimeUnit.SECONDS.toMillis(59));
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualCreatedAt(), is(formattedString(R.string.created_seconds_ago, 59)));
  }

  @Test
  public void createAt_1minuteAgo() {
    final Status status = createStatusWithPast(TimeUnit.MINUTES.toMillis(1));
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualCreatedAt(), is(formattedPlurals(R.plurals.created_minutes_ago, 1)));
  }

  @Test
  public void createAt_44minutesAgo() {
    final Status status = createStatusWithPast(TimeUnit.MINUTES.toMillis(44));
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualCreatedAt(), is(formattedPlurals(R.plurals.created_minutes_ago, 44)));
  }

  @Test
  public void createAt_1hourAgo() {
    final Status status = createStatusWithPast(TimeUnit.MINUTES.toMillis(45));
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualCreatedAt(), is(formattedPlurals(R.plurals.created_hours_ago, 1)));
  }

  @Test
  public void createAt_1hourAgo_with104minutes() {
    final Status status = createStatusWithPast(TimeUnit.MINUTES.toMillis(104));
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualCreatedAt(), is(formattedPlurals(R.plurals.created_hours_ago, 1)));
  }

  @Test
  public void createAt_2hourAgo() {
    final Status status = createStatusWithPast(TimeUnit.MINUTES.toMillis(105));
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualCreatedAt(), is(formattedPlurals(R.plurals.created_hours_ago, 2)));
  }

  @Test
  public void createAt_23hourAgo() {
    final Status status = createStatusWithPast(
        TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(44));
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualCreatedAt(), is(formattedPlurals(R.plurals.created_hours_ago, 23)));
  }

  @Test
  public void createAt_24hourAgo() {
    final Status status = createStatusWithPast(
        TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(59));
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualCreatedAt(), is(formattedPlurals(R.plurals.created_hours_ago, 24)));
  }

  @Test
  public void showVerifiedIconForVerifiedUsersTweet() {
    final User verifiedUser = UserUtil.createVerifiedUser();
    final Status status = createStatus(10000L, verifiedUser);
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualNames(), is(verifiedUser.getName() + " @" + verifiedUser.getScreenName() + " "));
  }

  @Test
  public void showProtectedIconForProtectedUsersTweet() {
    final User protectedUser = UserUtil.createProtectedUser();
    final Status status = createStatus(10001L, protectedUser);
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualNames(), is(protectedUser.getName() + " @" + protectedUser.getScreenName() + " "));
  }

  @Test
  public void showBothIconForVerifiedAndProtectedUsersTweet() {
    final User bothUser = UserUtil.createVerifiedAndProtectedUser();
    final Status status = createStatus(10002L, bothUser);
    sut.setItem(new StatusListItem(status));
    sut.executePendingBindings();

    assertThat(actualNames(), is(bothUser.getName() + " @" + bothUser.getScreenName() + "  "));
  }

  @NonNull
  private String actualNames() {
    return sut.tlNames.getText().toString();
  }

  @NonNull
  private Status createStatusWithPast(long ago) {
    final Status status = createStatus(10);
    final Date pastDate = createPastDate(ago);
    when(status.getCreatedAt()).thenReturn(pastDate);
    return status;
  }

  @NonNull
  private Date createPastDate(long ago) {
    final Date date = new Date();
    date.setTime(System.currentTimeMillis() - ago);
    return date;
  }

  @NonNull
  private String actualCreatedAt() {
    return sut.tlCreateAt.getText().toString();
  }

  @NonNull
  private String formattedString(@StringRes int res) {
    return sut.getRoot().getContext().getString(res);
  }

  @NonNull
  private String formattedString(@StringRes int res, Object... args) {
    return sut.getRoot().getContext().getString(res, args);
  }

  private String formattedPlurals(int resId, int quantity) {
    return sut.getRoot().getContext().getResources().getQuantityString(resId, quantity, quantity);
  }
}