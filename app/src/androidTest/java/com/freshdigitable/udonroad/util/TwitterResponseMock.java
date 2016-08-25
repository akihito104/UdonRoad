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

package com.freshdigitable.udonroad.util;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import twitter4j.ExtendedMediaEntity;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import twitter4j.UserStreamListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/07/01.
 */
public class TwitterResponseMock {
  @NonNull
  public static StatusDeletionNotice createDeletionNotice(final Status target) {
    return new StatusDeletionNotice() {
      @Override
      public long getStatusId() {
        return target.getId();
      }

      @Override
      public long getUserId() {
        return target.getUser().getId();
      }

      @Override
      public int compareTo(@NonNull StatusDeletionNotice statusDeletionNotice) {
        return 0;
      }
    };
  }

  public static Status createStatus(long id) {
    final Status status = mock(Status.class);
    when(status.getId()).thenReturn(id * 1000L);
    when(status.getCreatedAt()).thenReturn(new Date());
    when(status.getText()).thenReturn(createText(id));
    when(status.isRetweet()).thenReturn(false);
    when(status.getSource())
        .thenReturn("<a href=\"https://twitter.com/akihito104\">Udonroad</a>");
    when(status.getURLEntities()).thenReturn(new URLEntity[0]);
    when(status.getExtendedMediaEntities()).thenReturn(new ExtendedMediaEntity[0]);
    when(status.getUserMentionEntities()).thenReturn(new UserMentionEntity[0]);
    final User user = mock(User.class);
    when(user.getId()).thenReturn(2000L);
    when(user.getName()).thenReturn("akihito matsuda");
    when(user.getScreenName()).thenReturn("akihito104");
    when(status.getUser()).thenReturn(user);
    return status;
  }

  @NonNull
  public static String createText(long id) {
    return "tweet body " + id;
  }

  public static Status createRtStatus(long newStatusId, long rtedStatusId, boolean isFromApi) {
    final Status rtStatus = createStatus(rtedStatusId);
    when(rtStatus.isRetweeted()).thenReturn(isFromApi);
    final int retweetCount = rtStatus.getRetweetCount();
    when(rtStatus.getRetweetCount()).thenReturn(retweetCount + 1);

    final Status status = createStatus(newStatusId);
    final String rtText = rtStatus.getText();
    when(status.getText()).thenReturn(rtText);
    when(status.isRetweet()).thenReturn(true);
    when(status.isRetweeted()).thenReturn(isFromApi);
    when(status.getRetweetedStatus()).thenReturn(rtStatus);
    return status;
  }

  @NonNull
  public static ResponseList<Status> createResponseList() {
    return new ResponseList<Status>() {
      List<Status> list = new ArrayList<>();

      @Override
      public RateLimitStatus getRateLimitStatus() {
        return null;
      }

      @Override
      public void add(int location, Status object) {
        list.add(location, object);
      }

      @Override
      public boolean add(Status object) {
        return list.add(object);
      }

      @Override
      public boolean addAll(int location, @NonNull Collection<? extends Status> collection) {
        return list.addAll(location, collection);
      }

      @Override
      public boolean addAll(@NonNull Collection<? extends Status> collection) {
        return list.addAll(collection);
      }

      @Override
      public void clear() {
        list.clear();
      }

      @Override
      public boolean contains(Object object) {
        return list.contains(object);
      }

      @Override
      public boolean containsAll(@NonNull Collection<?> collection) {
        return list.containsAll(collection);
      }

      @Override
      public Status get(int location) {
        return list.get(location);
      }

      @Override
      public int indexOf(Object object) {
        return list.indexOf(object);
      }

      @Override
      public boolean isEmpty() {
        return list.isEmpty();
      }

      @NonNull
      @Override
      public Iterator<Status> iterator() {
        return list.iterator();
      }

      @Override
      public int lastIndexOf(Object object) {
        return list.lastIndexOf(object);
      }

      @Override
      public ListIterator<Status> listIterator() {
        return list.listIterator();
      }

      @NonNull
      @Override
      public ListIterator<Status> listIterator(int location) {
        return list.listIterator(location);
      }

      @Override
      public Status remove(int location) {
        return list.remove(location);
      }

      @Override
      public boolean remove(Object object) {
        return list.remove(object);
      }

      @Override
      public boolean removeAll(@NonNull Collection<?> collection) {
        return list.removeAll(collection);
      }

      @Override
      public boolean retainAll(@NonNull Collection<?> collection) {
        return list.retainAll(collection);
      }

      @Override
      public Status set(int location, Status object) {
        return list.set(location, object);
      }

      @Override
      public int size() {
        return list.size();
      }

      @NonNull
      @Override
      public List<Status> subList(int start, int end) {
        return list.subList(start, end);
      }

      @NonNull
      @Override
      public Object[] toArray() {
        return list.toArray();
      }

      @NonNull
      @Override
      public <T> T[] toArray(@NonNull T[] array) {
        return list.toArray(array);
      }

      @Override
      public int getAccessLevel() {
        return 0;
      }
    };
  }

  public static void receiveDeletionNotice(final UserStreamListener listener, Status... statuses)
      throws InterruptedException {
    Observable.just(Arrays.asList(statuses))
        .flatMapIterable(new Func1<List<Status>, Iterable<Status>>() {
          @Override
          public Iterable<Status> call(List<Status> statuses) {
            return statuses;
          }
        })
        .map(new Func1<Status, StatusDeletionNotice>() {
          @Override
          public StatusDeletionNotice call(Status status) {
            return createDeletionNotice(status);
          }
        })
        .observeOn(Schedulers.io())
        .subscribe(new Action1<StatusDeletionNotice>() {
          @Override
          public void call(StatusDeletionNotice statusDeletionNotice) {
            listener.onDeletionNotice(statusDeletionNotice);
          }
        });
    Thread.sleep(600);
  }

  public static void receiveStatuses(final UserStreamListener listener, Status... statuses)
      throws InterruptedException {
    Observable.just(Arrays.asList(statuses))
        .flatMapIterable(new Func1<List<Status>, Iterable<Status>>() {
          @Override
          public Iterable<Status> call(List<Status> statuses) {
            return statuses;
          }
        })
        .observeOn(Schedulers.io())
        .subscribe(new Action1<Status>() {
          @Override
          public void call(Status status) {
            listener.onStatus(status);
          }
        });
    Thread.sleep(600); // buffering tweets in 500ms
  }
}