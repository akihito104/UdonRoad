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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
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
  public RealmTestRule rule = new RealmTestRule() {
    @Override
    void before() {
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

    @Override
    void after() {
      writableSut.close();
      sut.close();
      updateSubjectFactory.clear();
    }
  };

  @Test
  public void upsert20ItemsAreSortedByIdDesc() {
    final int expectedSize = 20;
    final ResponseList<Status> responseList = createResponseList(expectedSize);

    writableSut.observeUpsert(responseList)
        .subscribe(new CompletableObserver() {
          @Override
          public void onSubscribe(Disposable d) { }

          @Override
          public void onComplete() {
            assertThat(sut.getItemCount(), is(expectedSize));
            Collections.sort(responseList, (o1, o2) -> (int) (o2.getId() - o1.getId()));
            for (int i = 0; i < expectedSize; i++) {
              final Status actual = sut.get(i);
              final Status expected = responseList.get(i);
              assertThat(actual.getId(), is(expected.getId()));
            }
            rule.testComplete();
          }

          @Override
          public void onError(Throwable e) {
            rule.testComplete();
          }
        });
  }

  @Test
  public void upsert1ItemForAlreadyInserted20ItemAreSortedByIdDesc() {
    final int expectedSize = 20;
    final ResponseList<Status> responseList = createResponseList(expectedSize);
    final ResponseList<Status> afterUpserted = createResponseList(1, 50);

    writableSut.observeUpsert(responseList)
        .andThen(writableSut.observeUpsert(afterUpserted))
        .subscribe(new CompletableObserver() {
          @Override
          public void onSubscribe(Disposable d) { }

          @Override
          public void onComplete() {
            final ArrayList<Status> expected = new ArrayList<>();
            expected.addAll(responseList);
            expected.addAll(afterUpserted);
            Collections.sort(expected, (o1, o2) -> (int) (o2.getId() - o1.getId()));
            assertThat(sut.getItemCount(), is(expectedSize + 1));
            for (int i = 0; i < expectedSize; i++) {
              final Status a = sut.get(i);
              final Status e = expected.get(i);
              assertThat(a.getId(), is(e.getId()));
            }
            rule.testComplete();
          }

          @Override
          public void onError(Throwable e) {
            rule.testComplete();
          }
        });
  }

  private ResponseList<Status> createResponseList(int size) {
    return createResponseList(size, 0);
  }

  private ResponseList<Status> createResponseList(int size, int offset) {
    final User userA = UserUtil.createUserA();
    final List<Status> responseList = new ArrayList<>();
    for (int i = 1; i <= size; i++) {
      final Status status = createStatus(i * 1000L + offset, userA);
      responseList.add(status);
    }
    return TwitterResponseMock.createResponseList(responseList);
  }

  static abstract class RealmTestRule implements TestRule {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    @Override
    public Statement apply(Statement base, Description description) {
      return new RealmTestStatement(base, countDownLatch) {
        @Override
        void before() throws Throwable {
          RealmTestRule.this.before();
        }

        @Override
        void after() throws Throwable {
          RealmTestRule.this.after();
        }
      };
    }

    void testComplete() {
      countDownLatch.countDown();
    }

    abstract void before() throws Throwable;

    abstract void after() throws Throwable;
  }

  static abstract class RealmTestStatement extends Statement {
    private Statement base;
    private final CountDownLatch countDownLatch;
    private Looper looper;
    final CountDownLatch tearDownDone = new CountDownLatch(1);

    RealmTestStatement(Statement base, CountDownLatch countDownLatch) {
      this.base = base;
      this.countDownLatch = countDownLatch;
    }

    abstract void before() throws Throwable;

    abstract void after() throws Throwable;

    @Override
    public void evaluate() throws Throwable {
      final ExecutorService executorService = Executors.newSingleThreadExecutor();
      final Future<Throwable> testTask = executorService.submit(() -> {
        Throwable t = null;
        Looper.prepare();
        looper = Looper.myLooper();
        try {
          before();
          base.evaluate();
          Looper.loop();
        } catch (Throwable throwable) {
          t = throwable;
          countDownLatch.countDown();
        } finally {
          try {
            after();
          } catch (Throwable throwable) {
            if (t == null) {
              t = throwable;
            }
          } finally {
            tearDownDone.countDown();
          }
        }
        return t;
      });

      try {
        countDownLatch.await(3, TimeUnit.SECONDS);
      } finally {
        looper.quit();
        tearDownDone.await(3, TimeUnit.SECONDS);
        final Throwable throwable = testTask.get();
        executorService.shutdownNow();
        if (throwable != null) {
          throw throwable;
        }
      }
    }
  }
}