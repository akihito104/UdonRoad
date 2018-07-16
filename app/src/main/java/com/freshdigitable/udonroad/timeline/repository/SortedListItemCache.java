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

package com.freshdigitable.udonroad.timeline.repository;

import android.arch.core.util.Function;
import android.support.annotation.NonNull;

import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.listitem.ListItem;

import java.util.Collection;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 * Created by akihit on 2018/01/13.
 */

class SortedListItemCache<T> implements SortedCache<T>, WritableSortedCache<T> {

  private final SortedCache<T> readCache;
  private final WritableSortedCache<T> writableCache;
  final Function<T, ListItem> typeMapper;

  @Inject
  SortedListItemCache(SortedCache<T> readCache, WritableSortedCache<T> writableCache,
                      Function<T, ListItem> typeMapper) {
    this.readCache = readCache;
    this.writableCache = writableCache;
    this.typeMapper = typeMapper;
  }

  @Override
  public long getLastPageCursor() {
    return writableCache.getLastPageCursor();
  }

  @Override
  public long getStartPageCursor() {
    return writableCache.getStartPageCursor();
  }

  @Override
  public boolean hasNextPage() {
    return writableCache.hasNextPage();
  }

  @Override
  public Completable observeUpsert(Collection<T> entities) {
    return writableCache.observeUpsert(entities);
  }

  @Override
  public void upsert(T entity) {
    writableCache.upsert(entity);
  }

  @Override
  public void upsert(Collection<T> entities) {
    writableCache.upsert(entities);
  }

  @Override
  public void insert(T entity) {
    writableCache.insert(entity);
  }

  @Override
  public void delete(long id) {
    writableCache.delete(id);
  }

  @Override
  public void open(String name) {
    readCache.open(name);
    writableCache.open(name);
  }

  @Override
  public void clear() {
    readCache.clear();
    writableCache.clear();
  }

  @Override
  public void close() {
    readCache.close();
    writableCache.close();
  }

  @Override
  public void drop() {
    readCache.drop();
  }

  @Override
  public Flowable<UpdateEvent> observeUpdateEvent() {
    return readCache.observeUpdateEvent();
  }

  @Override
  public long getId(int position) {
    return readCache.getId(position);
  }

  @Override
  public T get(int position) {
    return readCache.get(position);
  }

  @Override
  public int getItemCount() {
    return readCache.getItemCount();
  }

  @Override
  public int getPositionById(long id) {
    return readCache.getPositionById(id);
  }

  @NonNull
  @Override
  public Observable<? extends T> observeById(long id) {
    return readCache.observeById(id);
  }

  @NonNull
  @Override
  public Observable<? extends T> observeById(T element) {
    return readCache.observeById(element);
  }
}
