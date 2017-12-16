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

package com.freshdigitable.udonroad.input;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by akihit on 2017/12/10.
 */

class TweetInputModel implements Parcelable {
  private ReplyEntity replyEntity;
  private final List<Uri> media;
  private final List<Long> quoteStatusIds;
  private String text;

  void setText(String text) {
    this.text = text;
  }

  String getText() {
    return text;
  }

  void setReplyEntity(ReplyEntity replyEntity) {
    this.replyEntity = replyEntity;
  }

  ReplyEntity getReplyEntity() {
    return replyEntity;
  }

  void addQuoteId(long quoteId) {
    quoteStatusIds.add(quoteId);
  }

  boolean isStatusUpdateNeeded() {
    return replyEntity != null
        || quoteStatusIds.size() > 0
        || media.size() > 0;
  }

  boolean hasReplyEntity() {
    return replyEntity != null;
  }

  List<Long> getQuoteIds() {
    return quoteStatusIds;
  }

  boolean hasQuoteId() {
    return !quoteStatusIds.isEmpty();
  }

  public List<Uri> getMedia() {
    return media;
  }

  void addMediaAll(Collection<Uri> uris) {
    media.addAll(uris);
  }

  void removeMedia(Uri uri) {
    media.remove(uri);
  }

  boolean hasMedia() {
    return !media.isEmpty();
  }

  boolean isCleared() {
    return TextUtils.isEmpty(text) && replyEntity == null
        && quoteStatusIds.isEmpty() && media.isEmpty();
  }

  void clear() {
    replyEntity = null;
    quoteStatusIds.clear();
    media.clear();
    text = "";
  }

  @Override
  public int describeContents() {
    return 0;
  }

  TweetInputModel() {
    media = new ArrayList<>(4);
    quoteStatusIds = new ArrayList<>(4);
  }

  private TweetInputModel(Parcel in) {
    replyEntity = in.readParcelable(ReplyEntity.class.getClassLoader());
    media = in.createTypedArrayList(Uri.CREATOR);
    quoteStatusIds = new ArrayList<>();
    in.readList(quoteStatusIds, this.getClass().getClassLoader());
    text = in.readString();
    state = (State) in.readSerializable();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(replyEntity, flags);
    dest.writeTypedList(media);
    dest.writeList(quoteStatusIds);
    dest.writeString(text);
    dest.writeSerializable(state);
  }

  public static final Creator<TweetInputModel> CREATOR = new Creator<TweetInputModel>() {
    @Override
    public TweetInputModel createFromParcel(Parcel in) {
      return new TweetInputModel(in);
    }

    @Override
    public TweetInputModel[] newArray(int size) {
      return new TweetInputModel[size];
    }
  };

  private State state = State.DEFAULT;

  State getState() {
    return state;
  }

  void setState(State state) {
    this.state = state;
  }

  enum State {
    DEFAULT, WRITING, SENDING, SENT, RESUMED
  }
}
