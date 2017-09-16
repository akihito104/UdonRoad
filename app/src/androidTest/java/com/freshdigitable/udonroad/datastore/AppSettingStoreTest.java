/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.datastore;

import android.support.test.runner.AndroidJUnit4;

import com.freshdigitable.udonroad.util.TestInjectionUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import javax.inject.Inject;

import twitter4j.auth.AccessToken;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2017/09/09.
 */
@RunWith(AndroidJUnit4.class)
public class AppSettingStoreTest {
  @Inject
  AppSettingStore sut;

  @Before
  public void setup() {
    TestInjectionUtil.getComponent().inject(this);
    sut.open();
    sut.clear();
    sut.close();
  }

  @Test
  public void getCurrentAccessToken() {
    sut.open();

    final AccessToken at = Mockito.mock(AccessToken.class);
    when(at.getUserId()).thenReturn(1000L);
    when(at.getToken()).thenReturn("valid.token");
    when(at.getTokenSecret()).thenReturn("valid.token.secret");
    sut.storeAccessToken(at);
    sut.setCurrentUserId(1000L);

    final AccessToken currentUserAccessToken = sut.getCurrentUserAccessToken();
    assertThat(at.getToken(), is(currentUserAccessToken.getToken()));
    assertThat(at.getTokenSecret(), is(currentUserAccessToken.getTokenSecret()));

    sut.close();
  }

  @Test
  public void getCurrentAccessTokenAfterChangedCurrentUser() {
    sut.open();

    final AccessToken at = Mockito.mock(AccessToken.class);
    when(at.getUserId()).thenReturn(1000L);
    when(at.getToken()).thenReturn("valid.token");
    when(at.getTokenSecret()).thenReturn("valid.token.secret");
    sut.storeAccessToken(at);
    sut.setCurrentUserId(1000L);
    final AccessToken at2 = Mockito.mock(AccessToken.class);
    when(at2.getUserId()).thenReturn(2000L);
    when(at2.getToken()).thenReturn("valid.token.2");
    when(at2.getTokenSecret()).thenReturn("valid.token.secret.2");
    sut.storeAccessToken(at2);
    sut.setCurrentUserId(2000L);

    final AccessToken currentUserAccessToken2 = sut.getCurrentUserAccessToken();
    assertThat(at2.getToken(), is(currentUserAccessToken2.getToken()));
    assertThat(at2.getTokenSecret(), is(currentUserAccessToken2.getTokenSecret()));

    sut.setCurrentUserId(1000L);
    final AccessToken currentUserAccessToken = sut.getCurrentUserAccessToken();
    assertThat(at.getToken(), is(currentUserAccessToken.getToken()));
    assertThat(at.getTokenSecret(), is(currentUserAccessToken.getTokenSecret()));

    sut.close();
  }
}
