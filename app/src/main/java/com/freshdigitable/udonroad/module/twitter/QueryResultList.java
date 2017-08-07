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

package com.freshdigitable.udonroad.module.twitter;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.RateLimitStatus;
import twitter4j.Status;

/**
 * Created by akihit on 2017/08/06.
 */

public class QueryResultList implements List<Status>, QueryResult {
  private final QueryResult queryResult;

  public QueryResultList(QueryResult queryResult) {
    this.queryResult = queryResult;
  }

  @Override
  public long getSinceId() {
    return queryResult.getSinceId();
  }

  @Override
  public long getMaxId() {
    return queryResult.getMaxId();
  }

  @Override
  public String getRefreshURL() {
    return queryResult.getRefreshURL();
  }

  @Override
  public int getCount() {
    return queryResult.getCount();
  }

  @Override
  public double getCompletedIn() {
    return queryResult.getCompletedIn();
  }

  @Override
  public String getQuery() {
    return queryResult.getQuery();
  }

  @Override
  public List<Status> getTweets() {
    return queryResult.getTweets();
  }

  @Override
  public Query nextQuery() {
    return queryResult.nextQuery();
  }

  @Override
  public boolean hasNext() {
    return queryResult.hasNext();
  }

  @Override
  public int size() {
    return queryResult.getTweets().size();
  }

  @Override
  public boolean isEmpty() {
    return queryResult.getTweets().isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return queryResult.getTweets().contains(o);
  }

  @NonNull
  @Override
  public Iterator<Status> iterator() {
    return queryResult.getTweets().iterator();
  }

  @NonNull
  @Override
  public Object[] toArray() {
    return queryResult.getTweets().toArray();
  }

  @Override
  public Status get(int i) {
    return queryResult.getTweets().get(i);
  }

  @Override
  public int indexOf(Object o) {
    return queryResult.getTweets().indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return queryResult.getTweets().lastIndexOf(o);
  }

  @NonNull
  @Override
  public ListIterator<Status> listIterator() {
    return queryResult.getTweets().listIterator();
  }

  @NonNull
  @Override
  public ListIterator<Status> listIterator(int i) {
    return queryResult.getTweets().listIterator(i);
  }

  @NonNull
  @Override
  public List<Status> subList(int i, int i1) {
    return queryResult.getTweets().subList(i, i1);
  }

  @Override
  public boolean containsAll(@NonNull Collection collection) {
    return queryResult.getTweets().containsAll(collection);
  }

  @NonNull
  @Override
  public <T> T[] toArray(@NonNull T[] ts) {
    return queryResult.getTweets().toArray(ts);
  }

  @Override
  public RateLimitStatus getRateLimitStatus() {
    return queryResult.getRateLimitStatus();
  }

  @Override
  public int getAccessLevel() {
    return queryResult.getAccessLevel();
  }

  @Override
  public Status set(int i, Status status) {
    throw new IllegalStateException();
  }

  @Override
  public void add(int i, Status status) {
    throw new IllegalStateException();
  }

  @Override
  public boolean add(Status s) {
    throw new IllegalStateException();
  }

  @Override
  public boolean remove(Object o) {
    throw new IllegalStateException();
  }

  @Override
  public boolean addAll(@NonNull Collection collection) {
    throw new IllegalStateException();
  }

  @Override
  public boolean addAll(int i, @NonNull Collection collection) {
    throw new IllegalStateException();
  }

  @Override
  public void clear() {
    throw new IllegalStateException();
  }

  @Override
  public Status remove(int i) {
    throw new IllegalStateException();
  }

  @Override
  public boolean retainAll(@NonNull Collection collection) {
    throw new IllegalStateException();
  }

  @Override
  public boolean removeAll(@NonNull Collection collection) {
    throw new IllegalStateException();
  }
}
