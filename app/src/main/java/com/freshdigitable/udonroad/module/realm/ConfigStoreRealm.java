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

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.StatusReaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import twitter4j.Relationship;
import twitter4j.User;

import static com.freshdigitable.udonroad.module.realm.CacheUtil.findById;
import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_ID;

/**
 * ConfigStoreRealm is data store for user-oriented configurations
 * such as IgnorableUser and StatusReaction.
 *
 * Created by akihit on 2016/07/30.
 */
public class ConfigStoreRealm implements ConfigStore {
  @SuppressWarnings("unused")
  private static final String TAG = ConfigStoreRealm.class.getSimpleName();
  private Realm configStore;
  private final RealmConfiguration config;

  public ConfigStoreRealm() {
    config = new RealmConfiguration.Builder()
        .name(StoreType.CONFIG.storeName)
        .deleteRealmIfMigrationNeeded()
        .build();
  }

  @Override
  public void open() {
    configStore = Realm.getInstance(config);
  }

  @Override
  public void close() {
    configStore.close();
  }

  @Override
  public void clear() {
    configStore.executeTransaction(realm -> realm.deleteAll());
  }

  @Override
  public void drop() {
    Realm.deleteRealm(config);
  }

  @Override
  public void replaceIgnoringUsers(final Collection<Long> iDs) {
    configStore.executeTransaction(realm -> {
      realm.delete(IgnoringUser.class);
      List<IgnoringUser> ignoringUsers = new ArrayList<>(iDs.size());
      for (Long id : iDs) {
        final IgnoringUser iUser = new IgnoringUser(id);
        ignoringUsers.add(iUser);
      }
      realm.insertOrUpdate(ignoringUsers);
    });
  }

  @Override
  public boolean isIgnoredUser(long userId) {
    return configStore.where(IgnoringUser.class)
        .equalTo("id", userId)
        .findFirst() != null;
  }

  @Override
  public void addIgnoringUser(User user) {
    final long userId = user.getId();
    configStore.executeTransaction(realm -> realm.insertOrUpdate(new IgnoringUser(userId)));
  }

  @Override
  public void removeIgnoringUser(User user) {
    final long userId = user.getId();
    configStore.executeTransaction(realm ->
        realm.where(IgnoringUser.class)
            .equalTo("id", userId)
            .findAll()
            .deleteAllFromRealm());
  }

  @Override
  public void upsert(StatusReaction entity) {
    if (entity == null) {
      return;
    }
    upsert(Collections.singletonList(entity));
  }

  @Override
  public void upsert(final Collection<StatusReaction> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    configStore.executeTransaction(upsertTransaction(entities));
  }

  @Override
  public Completable observeUpsert(final Collection<StatusReaction> entities) {
    if (entities == null || entities.isEmpty()) {
      return Completable.complete();
    }
    return CacheUtil.observeUpsertImpl(configStore, upsertTransaction(entities));
  }

  @NonNull
  private static Realm.Transaction upsertTransaction(final Collection<StatusReaction> entities) {
    return realm -> {
      final ArrayList<StatusReactionRealm> insertReactions = new ArrayList<>(entities.size());
      for (StatusReaction s : entities) {
        final StatusReactionRealm r = findById(realm, s.getId(), StatusReactionRealm.class);
        if (r == null) {
          insertReactions.add(new StatusReactionRealm(s));
        } else {
          r.merge(s);
        }
      }
      realm.insertOrUpdate(insertReactions);
    };
  }

  @Override
  public void insert(final StatusReaction entity) {
    configStore.executeTransaction(realm -> realm.insertOrUpdate(new StatusReactionRealm(entity)));
  }

  @Nullable
  @Override
  public StatusReactionRealm find(long id) {
    return findById(configStore, id, StatusReactionRealm.class);
  }

  @NonNull
  @Override
  public Observable<? extends StatusReaction> observeById(long id) {
    final StatusReactionRealm statusReaction = find(id);
    return statusReaction != null ?
        observe(statusReaction).cast(StatusReaction.class)
        : Observable.empty();
  }

  static Observable<? extends RealmModel> observe(StatusReactionRealm statusReaction) {
    return RealmObjectObservable.create(statusReaction)
        .filter(RealmObject::isValid);
  }

  @Override
  public void delete(final long id) {
    configStore.executeTransactionAsync(realm ->
        realm.where(StatusReactionRealm.class)
            .equalTo(KEY_ID, id)
            .findAll()
            .deleteAllFromRealm());
  }

  @Override
  public void shrink() {
    configStore.executeTransaction(r -> {
      r.where(StatusReactionRealm.class)
          .beginGroup()
          .equalTo("retweeted", false)
          .equalTo("favorited", false)
          .endGroup()
          .findAll()
          .deleteAllFromRealm();
      final RealmResults<StatusReactionRealm> all = r.where(StatusReactionRealm.class)
          .findAll();
      if (all.size() <= 1000) {
        return;
      }
      final StatusReactionRealm deadline = all
          .sort("id", Sort.DESCENDING)
          .get(1000);
      r.where(StatusReactionRealm.class)
          .lessThan("id", deadline.getId())
          .findAll()
          .deleteAllFromRealm();
    });
  }

  @Override
  public void upsertRelationship(Relationship relationship) {
    final RelationshipRealm f = findRelationshipById(relationship.getTargetUserId());
    configStore.executeTransaction(r -> {
      if (f == null) {
        r.insertOrUpdate(new RelationshipRealm(relationship));
      } else {
        f.merge(relationship);
      }
    });
  }

  private RelationshipRealm findRelationshipById(long targetUserId) {
    return configStore.where(RelationshipRealm.class)
        .equalTo("id", targetUserId)
        .findFirst();
  }

  @Override
  public void updateFriendship(long targetUserId, boolean following) {
    final RelationshipRealm relationship = findRelationshipById(targetUserId);
    configStore.executeTransaction(r -> relationship.setSourceFollowingTarget(following));
  }

  @Override
  public void updateBlocking(long targetUserId, boolean blocking) {
    final RelationshipRealm relationship = findRelationshipById(targetUserId);
    configStore.executeTransaction(r -> relationship.setSourceBlockingTarget(blocking));
  }

  @Override
  public void updateMuting(long targetUserId, boolean muting) {
    final RelationshipRealm relationship = findRelationshipById(targetUserId);
    configStore.executeTransaction(r -> relationship.setSourceMutingTarget(muting));
  }

  @Override
  public Observable<Relationship> observeRelationshipById(long targetUserId) {
    final RealmResults<RelationshipRealm> relationship = configStore.where(RelationshipRealm.class)
        .equalTo("id", targetUserId)
        .findAll();
    return relationship.isEmpty() ?
        EmptyRealmObjectObservable.create(relationship).cast(Relationship.class)
        : RealmObjectObservable.create(relationship.first()).cast(Relationship.class);
  }
}
