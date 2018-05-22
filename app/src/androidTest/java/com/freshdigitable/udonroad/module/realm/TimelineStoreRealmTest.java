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

package com.freshdigitable.udonroad.module.realm;

import android.os.Looper;
import android.support.test.runner.AndroidJUnit4;

import com.freshdigitable.udonroad.MockMainApplication;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;
import com.freshdigitable.udonroad.util.StorageUtil;
import com.freshdigitable.udonroad.util.TwitterResponseMock;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class TimelineStoreRealmTest {

  private TimelineStoreRealm sut;
  private UpdateSubjectFactory updateSubjectFactory;
  private WritableTimelineRealm writableSut;

  @Rule
  public RealmTestRule rule = new RealmTestRule();

  @Before
  public void setup() {
    StorageUtil.initStorage();

    final AppSettingStore appSettingStore = MockMainApplication.getApp().sharedPreferenceModule.appSettingStore;
    final ConfigStore configStore = new ConfigStoreRealm(appSettingStore);
    updateSubjectFactory = new UpdateSubjectFactory();

    sut = new TimelineStoreRealm(updateSubjectFactory,
        new StatusCacheRealm(configStore, appSettingStore), appSettingStore);
    sut.open("home");

    writableSut = new WritableTimelineRealm(
        new StatusCacheRealm(configStore, appSettingStore), configStore, appSettingStore);
    writableSut.open("home");
  }

  @After
  public void tearDown() {
    writableSut.close();
    sut.close();
    updateSubjectFactory.clear();
  }

  @Test
  public void upsert20ItemsAreSortedByIdDesc() {
    final int expectedSize = 20;
    final ResponseList<Status> responseList = createResponseList(expectedSize);

    writableSut.observeUpsert(responseList)
        .subscribe(TestObserver.create(rule.getObserver()));
    rule.loop();

    assertThat(sut.getItemCount(), is(expectedSize));
    Collections.sort(responseList, (o1, o2) -> (int) (o2.getId() - o1.getId()));
    for (int i = 0; i < expectedSize; i++) {
      final Status actual = sut.get(i);
      final Status expected = responseList.get(i);
      assertThat(actual.getId(), is(expected.getId()));
    }
  }

  private ResponseList<Status> createResponseList(int size) {
    final User userA = UserUtil.createUserA();
    final List<Status> responseList = new ArrayList<>();
    for (int i = 1; i <= size; i++) {
      final Status status = createStatus(i * 1000L, userA);
      responseList.add(status);
    }
    return TwitterResponseMock.createResponseList(responseList);
  }

  static class RealmTestRule extends TestWatcher {
    @Override
    protected void starting(Description description) {
      super.starting(description);
      Looper.prepare();
    }

    @Override
    protected void finished(Description description) {
      super.finished(description);
      quit();
    }

    void loop() {
      Looper.loop();
    }

    void quit() {
      Looper.myLooper().quit();
    }

    <T> Observer<T> getObserver() {
      return new Observer<T>() {
        @Override
        public void onSubscribe(Disposable d) { }
        @Override
        public void onNext(T t) { }
        @Override
        public void onError(Throwable e) { }

        @Override
        public void onComplete() {
          quit();
        }
      };
    }
  }
}