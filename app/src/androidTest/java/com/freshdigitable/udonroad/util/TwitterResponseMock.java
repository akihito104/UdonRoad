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
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import twitter4j.MediaEntity;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import twitter4j.UserStreamListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TwitterResponseMock is utility to create Twitter response mock and entity mock.
 *
 * Created by akihit on 2016/07/01.
 */
public class TwitterResponseMock {
  @NonNull
  public static StatusDeletionNotice createDeletionNotice(final Status target) {
    final StatusDeletionNotice mock = mock(StatusDeletionNotice.class);
    final long statusId = target.getId();
    when(mock.getStatusId()).thenReturn(statusId);
    final long userId = target.getUser().getId();
    when(mock.getUserId()).thenReturn(userId);
    return mock;
  }

  public static Status createStatus(long id) {
    final User user = UserUtil.createUserA();
    return createStatus(id, user);
  }

  public static Status createStatus(long id, User user) {
    final Status status = mock(Status.class);
    when(status.getId()).thenReturn(id);
    when(status.getCreatedAt()).thenReturn(new Date());
    when(status.getText()).thenReturn(createText(id));
    when(status.isRetweet()).thenReturn(false);
    when(status.getSource())
        .thenReturn("<a href=\"https://twitter.com/akihito104\">Udonroad</a>");
    when(status.getURLEntities()).thenReturn(new URLEntity[0]);
    when(status.getMediaEntities()).thenReturn(new MediaEntity[0]);
    when(status.getUserMentionEntities()).thenReturn(new UserMentionEntity[0]);
    when(status.getUser()).thenReturn(user);
    return status;
  }

  @NonNull
  private static String createText(long id) {
    return "tweet body " + id;
  }

  public static Status createRtStatus(Status rtedStatus, long newStatusId, boolean isFromRest) {
    return createRtStatus(rtedStatus, newStatusId, 1, 0, isFromRest);
  }

  public static Status createRtStatus(Status rtedStatus, long newStatusId,
                                      int rtCount, int favCount, boolean isFromRest) {
    final Status rtStatus = createStatus(rtedStatus.getId(), rtedStatus.getUser());
    if (isFromRest) {
      when(rtStatus.isRetweeted()).thenReturn(true);
      when(rtStatus.getRetweetCount()).thenReturn(rtCount);
      when(rtStatus.getFavoriteCount()).thenReturn(favCount);
    } else {
      when(rtStatus.isRetweeted()).thenReturn(false);
      when(rtStatus.getRetweetCount()).thenReturn(-1);
      when(rtStatus.getFavoriteCount()).thenReturn(-1);
    }

    final Status status = createStatus(newStatusId);
    final String rtText = rtStatus.getText();
    when(status.getText()).thenReturn(rtText);
    when(status.isRetweet()).thenReturn(true);
    when(status.isRetweeted()).thenReturn(isFromRest);
    when(status.getRetweetedStatus()).thenReturn(rtStatus);
    return status;
  }

  @NonNull
  public static ResponseList<Status> createResponseList() {
    return createResponseList(new ArrayList<>());
  }

  public static ResponseList<Status> createResponseList(final List<Status> list) {
    return new ResponseList<Status>() {
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

  public static void receiveDeletionNotice(final UserStreamListener listener,
                                           Status... statuses) throws Exception {
    Observable.just(Arrays.asList(statuses))
        .flatMapIterable(new Func1<List<Status>, Iterable<Status>>() {
          @Override
          public Iterable<Status> call(List<Status> statuses) {
            return statuses;
          }
        })
        .map(TwitterResponseMock::createDeletionNotice)
        .observeOn(Schedulers.io())
        .subscribe(listener::onDeletionNotice);
  }

  public static void receiveStatuses(final UserStreamListener listener,
                                     Status... statuses) throws Exception {
    Observable.just(Arrays.asList(statuses))
        .flatMapIterable(new Func1<List<Status>, Iterable<Status>>() {
          @Override
          public Iterable<Status> call(List<Status> statusList) {
            return statusList;
          }
        })
        .observeOn(Schedulers.io())
        .subscribe(listener::onStatus);
  }

  public static TwitterAPIConfiguration createTwitterAPIConfigMock() {
    final TwitterAPIConfiguration mock = mock(TwitterAPIConfiguration.class);
    when(mock.getShortURLLength()).thenReturn(23);
    when(mock.getShortURLLengthHttps()).thenReturn(23);
    return mock;
  }
}