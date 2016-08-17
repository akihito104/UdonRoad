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