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

package com.freshdigitable.udonroad.module.realm;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.MediaCache;
import com.freshdigitable.udonroad.datastore.PerspectivalStatus;
import com.freshdigitable.udonroad.datastore.StatusReaction;
import com.freshdigitable.udonroad.datastore.StatusReactionImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import rx.Observable;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;
import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_ID;

/**
 * StatusCacheRealm implements StatusCache for Realm.
 *
 * Created by akihit on 2016/07/22.
 */
public class StatusCacheRealm extends TypedCacheBaseRealm<Status> implements MediaCache {
  @SuppressWarnings("unused")
  private static final String TAG = StatusCacheRealm.class.getSimpleName();
  private final ConfigStore configStore;
  private UserCacheRealm userTypedCache;

  public StatusCacheRealm(ConfigStore configStore) {
    this.configStore = configStore;
  }

  @Override
  public void open() {
    super.open();
    this.userTypedCache = new UserCacheRealm(this);
    configStore.open();
  }

  @Override
  public void close() {
    super.close();
    configStore.close();
  }

  @Override
  public void upsert(@Nullable Status status) {
    if (status == null) {
      return;
    }
    upsert(Collections.singletonList(status));
  }

  @Override
  public void upsert(List<Status> statuses) {
    if (statuses == null || statuses.isEmpty()) {
      return;
    }
    final Collection<Status> updates = splitUpsertingStatus(statuses);
    upsertUser(updates);
    upsertStatusReaction(updates);
    userTypedCache.upsert(splitUserMentionEntity(updates));
    cache.executeTransaction(upsertTransaction(statuses));
  }

  @Override
  public Observable<Void> observeUpsert(Collection<Status> statuses) {
    if (statuses == null || statuses.isEmpty()) {
      return Observable.empty();
    }
    final Collection<Status> updates = splitUpsertingStatus(statuses);
    userTypedCache.upsert(splitUserMentionEntity(updates));
    final Collection<User> splitUser = splitUpsertingUser(updates);
    final Collection<StatusReaction> splitReaction = splitUpsertingStatusReaction(updates);
    return Observable.concat(userTypedCache.observeUpsert(splitUser),
        configStore.observeUpsert(splitReaction),
        super.observeUpsert(updates))
        .last();
  }

  @NonNull
  @Override
  public Realm.Transaction upsertTransaction(final Collection<Status> updates) {
    return realm -> {
      final ArrayList<StatusRealm> inserts = new ArrayList<>(updates.size());
      for (Status s : updates) {
        final StatusRealm update = findById(realm, s.getId(), StatusRealm.class);
        if (update == null) {
          inserts.add(new StatusRealm(s));
        } else {
          update.merge(s);
        }
      }
      realm.insertOrUpdate(inserts);
    };
  }

  @NonNull
  private Collection<Status> splitUpsertingStatus(Collection<Status> statuses) {
    final LinkedHashMap<Long, Status> updates = new LinkedHashMap<>();
    for (Status s : statuses) {
      if (!configStore.isIgnoredUser(s.getUser().getId())) {
        updates.put(s.getId(), s);
      }
      final Status quotedStatus = s.getQuotedStatus();
      if (quotedStatus != null
          && !configStore.isIgnoredUser(quotedStatus.getUser().getId())) {
        updates.put(quotedStatus.getId(), quotedStatus);
      }
      final Status retweetedStatus = s.getRetweetedStatus();
      if (retweetedStatus != null
          && !configStore.isIgnoredUser(retweetedStatus.getUser().getId())) {
        updates.put(retweetedStatus.getId(), retweetedStatus);
        final Status rtQuotedStatus = retweetedStatus.getQuotedStatus();
        if (rtQuotedStatus != null
            && !configStore.isIgnoredUser(rtQuotedStatus.getUser().getId())) {
          updates.put(rtQuotedStatus.getId(), rtQuotedStatus);
        }
      }
    }
    return updates.values();
  }

  private Collection<User> splitUpsertingUser(Collection<Status> updates) {
    Map<Long, User> res = new LinkedHashMap<>(updates.size());
    for (Status s : updates) {
      final User user = s.getUser();
      res.put(user.getId(), user);
    }
    return res.values();
  }

  private UserMentionEntity[] splitUserMentionEntity(Collection<Status> updates) {
    final Map<Long, UserMentionEntity> res = new LinkedHashMap<>();
    for (Status s : updates) {
      for (UserMentionEntity ume : s.getUserMentionEntities()) {
        res.put(ume.getId(), ume);
      }
    }
    final Collection<UserMentionEntity> values = res.values();
    return values.toArray(new UserMentionEntity[values.size()]);
  }

  private Collection<StatusReaction> splitUpsertingStatusReaction(Collection<Status> statuses) {
    Map<Long, StatusReaction> res = new LinkedHashMap<>(statuses.size());
    for (Status s : statuses) {
      if (s instanceof PerspectivalStatus) {
        res.put(s.getId(), ((PerspectivalStatus) s).getStatusReaction());
      } else {
        res.put(s.getId(), new StatusReactionImpl(s));
      }
    }
    return res.values();
  }

  @Override
  public void delete(final long statusId) {
    cache.executeTransactionAsync(realm -> realm.where(StatusRealm.class)
        .equalTo(KEY_ID, statusId)
        .findAll()
        .deleteAllFromRealm());
    configStore.delete(statusId);
  }

  /**
   * to update with response of destroyFavorite or destroy Retweet
   * @param status new data for update
   */
  @Override
  public void insert(final Status status) {
    final Collection<Status> statuses = splitUpsertingStatus(Collections.singletonList(status));
    upsertUser(statuses);
    for (StatusReaction sr : splitUpsertingStatusReaction(statuses)) {
      configStore.insert(sr);
    }

    final ArrayList<StatusRealm> entities = new ArrayList<>(statuses.size());
    for (Status s: statuses) {
      if (s instanceof StatusRealm) {
        entities.add((StatusRealm) s);
      } else {
        entities.add(new StatusRealm(s));
      }
    }
    cache.executeTransaction(realm -> realm.insertOrUpdate(entities));
  }

  @Override
  @Nullable
  public StatusRealm find(long id) {
    final StatusRealm res = getStatusInternal(id);
    if (res == null) {
      return null;
    }
    if (res.isRetweet()) {
      final StatusRealm rtStatus = getStatusInternal(res.getRetweetedStatusId());
      if (rtStatus != null && rtStatus.getQuotedStatusId() > 0) {
        rtStatus.setQuotedStatus(getStatusInternal(rtStatus.getQuotedStatusId()));
      }
      res.setRetweetedStatus(rtStatus);
      return res;
    }
    if (res.getQuotedStatusId() > 0) {
      res.setQuotedStatus(getStatusInternal(res.getQuotedStatusId()));
    }
    return res;
  }

  @NonNull
  @Override
  public Observable<Status> observeById(long statusId) {
    final StatusRealm status = find(statusId);
    if (status == null) {
      return Observable.empty();
    }
    final StatusRealm bindingStatus = getBindingStatus(status);
    final Observable<Status> statusObservable
        = statusChangesObservable(bindingStatus, status);
    final Observable<Status> quotedObservable
        = statusChangesObservable((StatusRealm) bindingStatus.getQuotedStatus(), status);
    final Observable<Status> reactionObservable
        = reactionObservable(bindingStatus.getId(), status);
    final Observable<Status> qReactionObservable
        = reactionObservable(bindingStatus.getQuotedStatusId(), status);
    return Observable.merge(
        statusObservable, quotedObservable, reactionObservable, qReactionObservable);
  }

  private Observable<Status> statusChangesObservable(@Nullable final StatusRealm bindings,
                                                     @NonNull final StatusRealm original) {
    if (bindings == null) {
      return Observable.empty();
    }
    final int favoriteCount = bindings.getFavoriteCount();
    final int retweetCount = bindings.getRetweetCount();
    final Observable<Status> statusObservable = Observable.create(
        (Observable.OnSubscribe<Status>) subscriber -> StatusRealm.addChangeListener(bindings,
            new RealmChangeListener<StatusRealm>() {
              private int prevFav = favoriteCount;
              private int prevRT = retweetCount;

              @Override
              public void onChange(StatusRealm element) {
                final Status bindings1 = getBindingStatus(element);
                if (isIgnorableChange(bindings1)) {
                  return;
                }
                subscriber.onNext(element);
                prevRT = bindings1.getRetweetCount();
                prevFav = bindings1.getFavoriteCount();
              }

              private boolean isIgnorableChange(Status bindings1) {
                return bindings1.getFavoriteCount() == prevFav
                    && bindings1.getRetweetCount() == prevRT;
              }
            })).doOnUnsubscribe(() -> StatusRealm.removeAllChangeListeners(bindings));
    if (original.getId() == bindings.getId()) {
      return statusObservable;
    }
    return statusObservable.map(status -> {
      final long statusId = status.getId();
      final StatusRealm binds = getBindingStatus(original);
      if (binds.getId() == statusId) {
        binds.setRetweetedStatus(status);
      } else if (binds.getQuotedStatusId() == statusId) {
        binds.setQuotedStatus(status);
      }
      return original;
    });
  }

  private Observable<Status> reactionObservable(final long statusId,
                                                @NonNull final StatusRealm original) {
    final StatusRealm bindings = getBindingStatus(original);
    return configStore.observeById(statusId)
        .map(reaction -> {
          final long statusId1 = reaction.getId();
          if (bindings.getId() == statusId1) {
            bindings.setStatusReaction(reaction);
          } else if (bindings.getQuotedStatusId() == statusId1) {
            ((StatusRealm) bindings.getQuotedStatus()).setStatusReaction(reaction);
          }
          return original;
        });
  }

  @Override
  public MediaEntity getMediaEntity(long mediaId) {
    return findById(cache, mediaId, MediaEntityRealm.class);
  }

  @Nullable
  private StatusRealm getStatusInternal(long id) {
    final StatusRealm status = findById(cache, id, StatusRealm.class);
    if (status == null) {
      return null;
    }
    status.setUser(userTypedCache.find(status.getUserId()));
    status.setStatusReaction(configStore.find(id));
    return status;
  }

  private void upsertUser(Collection<Status> updates) {
    final Collection<User> updateUsers = splitUpsertingUser(updates);
    userTypedCache.upsert(new ArrayList<>(updateUsers));
  }

  private void upsertStatusReaction(Collection<Status> updates) {
    final Collection<StatusReaction> statusReactions = splitUpsertingStatusReaction(updates);
    configStore.upsert(new ArrayList<>(statusReactions));
  }
}
