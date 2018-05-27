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
import io.reactivex.CompletableSource;
import io.reactivex.disposables.Disposable;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class TimelineStoreRealmTest {

  private TimelineStoreRealm sut;
  private UpdateSubjectFactory updateSubjectFactory;
  private WritableTimelineRealm writableSut;
  private StatusCacheRealm poolSut;

  @Rule
  public RealmTestRule rule = new RealmTestRule() {
    @Override
    void before() {
      StorageUtil.initStorage();

      final AppSettingStore appSettingStore = MockMainApplication.getApp().sharedPreferenceModule.appSettingStore;
      final ConfigStore configStore = new ConfigStoreRealm(appSettingStore);
      updateSubjectFactory = new UpdateSubjectFactory();

      poolSut = new StatusCacheRealm(configStore, appSettingStore);
      sut = new TimelineStoreRealm(updateSubjectFactory, poolSut, appSettingStore);
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

    final ArrayList<Status> expected = new ArrayList<>(responseList);
    Collections.sort(expected, (o1, o2) -> (int) (o2.getId() - o1.getId()));

    writableSut.observeUpsert(responseList)
        .subscribe(getObserver(() ->
            checkListItems(expectedSize, expected)));
  }

  @Test
  public void upsert1ItemForAlreadyInserted20ItemAreSortedByIdDesc() {
    final int expectedSize = 20;
    final ResponseList<Status> responseList = createResponseList(expectedSize);
    final ResponseList<Status> afterUpserted = createResponseList(1, 50);

    final ArrayList<Status> expected = new ArrayList<>(responseList);
    expected.addAll(afterUpserted);
    Collections.sort(expected, (o1, o2) -> (int) (o2.getId() - o1.getId()));

    writableSut.observeUpsert(responseList)
        .andThen(writableSut.observeUpsert(afterUpserted))
        .subscribe(getObserver(() ->
            checkListItems(expectedSize + 1, expected)));
  }

  @Test
  public void delete1ItemForAlreadyInserted20ItemAreSortedByIdDesc() {
    final int expectedSize = 19;
    final ResponseList<Status> responseList = createResponseList(20);
    final long deletedId = responseList.get(0).getId();

    final List<Status> expected = new ArrayList<>(responseList);
    expected.remove(responseList.get(0));
    Collections.sort(expected, (o1, o2) -> (int) (o2.getId() - o1.getId()));

    writableSut.observeUpsert(responseList)
        .andThen((CompletableSource) cs -> {
          writableSut.delete(deletedId);
          cs.onComplete();
        })
        .subscribe(getObserver(() ->
            checkListItems(expectedSize, expected)));
  }

  @Test
  public void delete1ItemFromPool() {
    final int expectedSize = 19;
    final ResponseList<Status> responseList = createResponseList(20);
    final long deletedId = responseList.get(0).getId();

    final List<Status> expected = new ArrayList<>(responseList);
    expected.remove(responseList.get(0));
    Collections.sort(expected, (o1, o2) -> (int) (o2.getId() - o1.getId()));

    writableSut.observeUpsert(responseList)
        .andThen((CompletableSource) cs -> {
          poolSut.observeById(deletedId)
              .subscribe(s -> { }, t -> { }, cs::onComplete);
          poolSut.delete(deletedId);
        })
        .subscribe(getObserver(() -> {
          final StatusRealm statusRealm = poolSut.find(deletedId);
          assertThat(statusRealm, is(nullValue()));
          checkListItems(expectedSize, expected);
        }));
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

  private CompletableObserver getObserver(Runnable runnable) {
    return new CompletableObserver() {
      @Override
      public void onComplete() {
        runnable.run();
        rule.testComplete();
      }

      @Override
      public void onError(Throwable e) {
        rule.testComplete();
      }
      @Override
      public void onSubscribe(Disposable d) { }
    };
  }

  private void checkListItems(int expectedSize, List<Status> expectedList) {
    assertThat(sut.getItemCount(), is(expectedSize));
    for (int i = 0; i < expectedSize; i++) {
      final Status actual = sut.get(i);
      final Status expected = expectedList.get(i);
      assertThat(actual.getId(), is(expected.getId()));
    }
  }

  static abstract class RealmTestRule implements TestRule {
    private CountDownLatch countDownLatch = new CountDownLatch(1);
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
    private final Statement base;
    private final CountDownLatch countDownLatch;
    private Looper looper;
    private final CountDownLatch tearDownDone = new CountDownLatch(1);

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

      Throwable throwable = null;
      try {
        throwable = awaitQuietly(countDownLatch);
      } finally {
        looper.quit();
        Throwable t = awaitQuietly(tearDownDone);
        if (throwable == null) {
          throwable = t;
        }
        t = testTask.get();
        if (t != null) {
          throwable = t;
        }
        executorService.shutdownNow();
      }
      if (throwable != null) {
        throw throwable;
      }
    }

    private static Throwable awaitQuietly(CountDownLatch countDownLatch) {
      try {
        countDownLatch.await(3,TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        return e;
      }
      return null;
    }
  }
}