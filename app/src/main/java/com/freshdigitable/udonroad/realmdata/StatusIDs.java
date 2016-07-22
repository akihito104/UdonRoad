/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import twitter4j.Status;

/**
 * Created by akihit on 2016/07/22.
 */
@RealmClass
public class StatusIDs implements RealmModel {
  @PrimaryKey
  private long id;
  private long retweetedStatusId;
  private long quotedStatusId;

  public StatusIDs() {
  }

  public StatusIDs(Status status) {
    this.id = status.getId();
    final Status retweetedStatus = status.getRetweetedStatus();
    this.retweetedStatusId = retweetedStatus != null
        ? retweetedStatus.getId()
        : -1;
    this.quotedStatusId = status.getQuotedStatusId();
  }

  public long getId() {
    return id;
  }

  public long getRetweetStatusId() {
    return retweetedStatusId;
  }

  public long getQuotedStatusId() {
    return quotedStatusId;
  }
}
