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

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;
import com.freshdigitable.udonroad.timeline.fetcher.FetchQuery;
import com.freshdigitable.udonroad.timeline.fetcher.FetchQueryProvider;
import com.freshdigitable.udonroad.timeline.fetcher.ListFetcher;

import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.processors.PublishProcessor;

/**
 * Created by akihit on 2018/01/13.
 */

class ListItemRepositoryCreator {
  @NonNull
  static <T> ListItemRepository create(
      SortedListItemCache<T> cache, Map<StoreType, Provider<ListFetcher<T>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback, StoreType type, @StringRes int msg) {
    return create(cache, listFetchers, userFeedback, type, msg, new FetchQueryProvider());
  }

  @NonNull
  static <T> ListItemRepository create(
      SortedListItemCache<T> cache, Map<StoreType, Provider<ListFetcher<T>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback, StoreType type, @StringRes int msg,
      FetchQueryProvider fetchQueryProvider) {

    final ListFetcher<T> fetcher = listFetchers.get(type).get();
    return new ListItemRepository() {
      private long id;
      private String q;

      @Override
      public void init(long id, String query) {
        this.id = id;
        this.q = query;
        cache.open(type.nameWithSuffix(id, q));
      }

      @Override
      public void close() {
        cache.close();
      }

      @Override
      public Flowable<UpdateEvent> observeUpdateEvent() {
        return cache.observeUpdateEvent();
      }

      @Override
      public long getId(int position) {
        return cache.getId(position);
      }

      @Override
      public ListItem get(int position) {
        final T status = cache.get(position);
        return status != null ? cache.typeMapper.apply(status) : null;
      }

      @Override
      public int getItemCount() {
        return cache.getItemCount();
      }

      @Override
      public int getPositionById(long id) {
        return cache.getPositionById(id);
      }

      @NonNull
      @Override
      public Observable<? extends ListItem> observeById(long id) {
        return cache.observeById(id).map(cache.typeMapper::apply);
      }

      @NonNull
      @Override
      public Observable<? extends ListItem> observe(ListItem element) {
        return observeById(element.getId());
      }

      @Override
      public void getInitList() {
        final FetchQuery initQuery = fetchQueryProvider.getInitQuery(id, q);
        fetchToStore(fetcher.fetchInit(initQuery));
      }

      @Override
      public void getListOnEnd() {
        final long lastPageCursor = cache.getLastPageCursor();
        if (lastPageCursor < 0) {
          userFeedback.onNext(new UserFeedbackEvent(R.string.msg_no_next_page));
          return;
        }
        final FetchQuery query = fetchQueryProvider.getNextQuery(id, q, lastPageCursor);
        fetchToStore(fetcher.fetchNext(query));
      }

      @Override
      public void getListOnStart() {
        final long cursor = cache.getStartPageCursor();
        if (cursor < 0) {
          getInitList();
          return;
        }
        final FetchQuery query = new FetchQuery.Builder()
            .id(id)
            .lastPageCursor(cursor)
            .searchQuery(q)
            .build();
        fetchToStore(fetcher.fetchLatest(query));
      }

      @Override
      public void drop() {
        cache.drop();
      }

      private void fetchToStore(Single<? extends List<T>> fetchingTask) {
        fetchingTask.observeOn(AndroidSchedulers.mainThread()).subscribe(
            cache::upsert,
            th -> userFeedback.onNext(new UserFeedbackEvent(msg)));
      }

      @NonNull
      @Override
      public StoreType getStoreType() {
        return type;
      }
    };
  }

  private ListItemRepositoryCreator() {}
}
