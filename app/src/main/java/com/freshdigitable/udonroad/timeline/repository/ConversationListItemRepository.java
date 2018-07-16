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

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;

import java.util.Collections;

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Status;

/**
 * Created by akihit on 2018/01/14.
 */

class ConversationListItemRepository extends ListItemRepository.Adapter {
  private final TwitterApi twitterApi;
  private final SortedListItemCache<Status> cache;
  private final PublishProcessor<UserFeedbackEvent> userFeedback;
  private long id;

  @Inject
  ConversationListItemRepository(TwitterApi twitterApi,
                                 SortedListItemCache<Status> cache,
                                 PublishProcessor<UserFeedbackEvent> userFeedback) {
    this.twitterApi = twitterApi;
    this.cache = cache;
    this.userFeedback = userFeedback;
  }

  @Override
  public void init(long id, String query) {
    this.id = id;
    cache.open(StoreType.CONVERSATION.nameWithSuffix(id, query));
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
    return cache.typeMapper.apply(cache.get(position));
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
    twitterApi.fetchConversations(id)
        .observeOn(AndroidSchedulers.mainThread())
        .map(Collections::singleton)
        .flatMapCompletable(cache::observeUpsert)
        .subscribe(() -> {},
            e -> userFeedback.onNext(new UserFeedbackEvent(R.string.msg_tweet_not_download)));
  }

  @Override
  public void drop() {
    cache.drop();
  }

  @NonNull
  @Override
  public StoreType getStoreType() {
    return StoreType.CONVERSATION;
  }

  @Override
  public void getListOnEnd() {}
}
