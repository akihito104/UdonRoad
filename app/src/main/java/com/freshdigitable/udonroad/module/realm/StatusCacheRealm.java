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
import com.freshdigitable.udonroad.datastore.TypedCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmObject;
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
public class StatusCacheRealm implements TypedCache<Status>, MediaCache {
  @SuppressWarnings("unused")
  private static final String TAG = StatusCacheRealm.class.getSimpleName();
  private final PoolRealm pool;
  private final ConfigStore configStore;
  private UserCacheRealm userTypedCache;

  public StatusCacheRealm(ConfigStore configStore) {
    this.configStore = configStore;
    this.pool = new PoolRealm();
  }

  @Override
  public void open() {
    this.userTypedCache = new UserCacheRealm(pool);
    configStore.open();
    pool.open();
  }

  @Override
  public void close() {
    pool.close();
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
  public void upsert(Collection<Status> statuses) {
    if (statuses == null || statuses.isEmpty()) {
      return;
    }
    final Collection<Status> updates = splitUpsertingStatus(statuses);
    final Realm.Transaction statusTransaction = upsertTransaction(updates);
    final Realm.Transaction userTransaction = getUserUpsertTransaction(updates);
    upsertStatusReaction(updates);
    pool.executeTransaction(r -> {
      userTransaction.execute(r);
      statusTransaction.execute(r);
    });
  }

  @Override
  public Completable observeUpsert(Collection<Status> statuses) {
    if (statuses == null || statuses.isEmpty()) {
      return Completable.complete();
    }
    final Collection<Status> updates = splitUpsertingStatus(statuses);
    final Realm.Transaction statusTransaction = upsertTransaction(updates);
    final Realm.Transaction userTransaction = getUserUpsertTransaction(updates);
    final Collection<StatusReaction> splitReaction = splitUpsertingStatusReaction(updates);
    return Completable.concatArray(configStore.observeUpsert(splitReaction),
        pool.observeUpsertImpl(r -> {
          userTransaction.execute(r);
          statusTransaction.execute(r);
        }));
  }

  @NonNull
  private Realm.Transaction getUserUpsertTransaction(Collection<Status> updates) {
    final Collection<User> splitUser = splitUpsertingUser(updates);
    return userTypedCache.upsertTransaction(splitUser);
  }

  @NonNull
  private Realm.Transaction upsertTransaction(final Collection<Status> updates) {
    return realm -> {
      final ArrayList<StatusRealm> inserts = new ArrayList<>(updates.size());
      for (Status s : updates) {
        final StatusRealm update = CacheUtil.findById(realm, s.getId(), StatusRealm.class);
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
      for (UserMentionEntity userMentionEntity : s.getUserMentionEntities()) {
        res.put(userMentionEntity.getId(), new UserRealm(userMentionEntity));
      }
    }
    for (Status s : updates) {
      final User user = s.getUser();
      res.put(user.getId(), user);
    }
    return res.values();
  }

  private void upsertStatusReaction(Collection<Status> updates) {
    final Collection<StatusReaction> statusReactions = splitUpsertingStatusReaction(updates);
    configStore.upsert(statusReactions);
  }

  private Collection<StatusReaction> splitUpsertingStatusReaction(Collection<Status> statuses) {
    Map<Long, StatusReaction> res = new LinkedHashMap<>(statuses.size());
    for (Status s : statuses) {
      res.put(s.getId(), s instanceof PerspectivalStatus ?
          ((PerspectivalStatus) s).getStatusReaction()
          : new StatusReactionImpl(s));
    }
    return res.values();
  }

  @Override
  public void delete(final long statusId) {
    pool.executeTransactionAsync(r -> r.where(StatusRealm.class)
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
    for (StatusReaction sr : splitUpsertingStatusReaction(statuses)) {
      configStore.insert(sr);
    }

    final ArrayList<StatusRealm> entities = new ArrayList<>(statuses.size());
    for (Status s: statuses) {
      entities.add(s instanceof StatusRealm ?
          ((StatusRealm) s)
          : new StatusRealm(s));
    }
    final Realm.Transaction userUpsertTransaction = getUserUpsertTransaction(statuses);
    pool.executeTransaction(r -> {
      userUpsertTransaction.execute(r);
      r.insertOrUpdate(entities);
    });
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
  public Observable<? extends Status> observeById(long statusId) {
    final StatusRealm status = find(statusId);
    return status != null ?
        StatusChangeObservable.create(status)
            .filter(s -> RealmObject.isValid(s))
            .map(s -> {
              final Status quotedStatus = s.getQuotedStatus();
              if (quotedStatus != null && !RealmObject.isValid((StatusRealm) quotedStatus)) {
                s.setQuotedStatus(null);
              }
              return s;
            })
        : Observable.empty();
  }

  @Override
  public MediaEntity getMediaEntity(long mediaId) {
    return pool.findById(mediaId, MediaEntityRealm.class);
  }

  @Nullable
  private StatusRealm getStatusInternal(long id) {
    final StatusRealm status = pool.findById(id, StatusRealm.class);
    if (status == null) {
      return null;
    }
    status.setUser(userTypedCache.find(status.getUserId()));
    status.setStatusReaction(configStore.find(id));
    return status;
  }

  @Override
  public void clear() {
    pool.clear();
  }

  @Override
  public void drop() {
    pool.drop();
  }

  private static class StatusChangeObservable extends Observable<StatusRealm> {
    static StatusChangeObservable create(@NonNull StatusRealm statusRealm) {
      if (!statusRealm.isManaged()) {
        throw new IllegalStateException("status is not managed...");
      }
      return new StatusChangeObservable(statusRealm);
    }

    private final StatusRealm statusRealm;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Collection<Observable<? extends RealmModel>> observables = new ArrayList<>();

    private StatusChangeObservable(StatusRealm statusRealm) {
      this.statusRealm = statusRealm;
      final StatusRealm bindingStatus = getBindingStatus(statusRealm);
      observables.add(observeStatus(bindingStatus));
      observables.add(ConfigStoreRealm.observe((StatusReactionRealm) bindingStatus.getStatusReaction()));
      final Status quotedStatus = bindingStatus.getQuotedStatus();
      if (quotedStatus != null) {
        final StatusRealm qs = (StatusRealm) quotedStatus;
        observables.add(observeStatus(qs));
        observables.add(RealmObjectObservable.create((StatusReactionRealm) qs.getStatusReaction()));
      }
    }

    private static Observable<StatusRealm> observeStatus(StatusRealm bindingStatus) {
      return RealmObjectObservable.create(bindingStatus);
    }

    @Override
    protected void subscribeActual(Observer<? super StatusRealm> observer) {
      for (Observable<? extends RealmModel> o : observables) {
        o.subscribeWith(new StatusObserver<>(statusRealm, observer, disposables));
      }
      observer.onSubscribe(disposables);
    }

    private static class StatusObserver<T extends RealmModel> implements Observer<T> {
      private StatusRealm root;
      private Observer<? super StatusRealm> observer;
      private CompositeDisposable disposables;
      private boolean done = false;

      StatusObserver(StatusRealm root,
                     Observer<? super StatusRealm> observer,
                     CompositeDisposable disposables) {
        this.root = root;
        this.observer = observer;
        this.disposables = disposables;
      }

      @Override
      public void onSubscribe(Disposable d) {
        disposables.add(d);
      }

      @Override
      public void onNext(T t) {
        if (!done) {
          observer.onNext(root);
        }
      }

      @Override
      public void onError(Throwable e) {
        observer.onError(e);
      }

      @Override
      public void onComplete() {
        done = true;
      }
    }
  }
}
