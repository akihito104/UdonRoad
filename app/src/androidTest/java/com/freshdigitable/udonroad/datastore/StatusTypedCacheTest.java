/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

import com.freshdigitable.udonroad.util.StorageUtil;
import com.freshdigitable.udonroad.util.TestInjectionUtil;
import com.freshdigitable.udonroad.util.TwitterResponseMock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import twitter4j.Status;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by akihit on 2018/01/24.
 */
@RunWith(AndroidJUnit4.class)
public class StatusTypedCacheTest {
  @Inject
  TypedCache<Status> sut;

  @Before
  public void setup() {
    TestInjectionUtil.getComponent().inject(this);
    StorageUtil.initStorage();
    sut.open();
  }

  @After
  public void tearDown() {
    sut.close();
  }

  @Test
  public void insertStatus_andThen_findSameOne() {
    final Status target = TwitterResponseMock.createStatus(200);

    sut.insert(target);

    final Status actual = sut.find(200);
    assertThat(actual, is(notNullValue()));
    assertThat(actual.getId(), is(equalTo(target.getId())));
    assertThat(actual.getUser(), is(notNullValue()));
  }

  @Test
  public void upsertStatus_andThen_findSameOne() {
    final Status target = TwitterResponseMock.createStatus(200);

    sut.upsert(target);

    final Status actual = sut.find(200);
    assertThat(actual, is(notNullValue()));
    assertThat(actual.getId(), is(equalTo(target.getId())));
    assertThat(actual.getUser(), is(notNullValue()));
  }
}