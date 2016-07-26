/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.datastore;

import android.support.annotation.Nullable;

import twitter4j.Status;
import twitter4j.User;

/**
 * Created by akihit on 2016/07/25.
 */
public interface StatusCache {
  void upsertStatus(@Nullable final Status rtStatus);

  void deleteStatus(long statusId);

  Status getStatus(long statusId);

  User getUser(long userId);

  void clear();

  void close();
}
