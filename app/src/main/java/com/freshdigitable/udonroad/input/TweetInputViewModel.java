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

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.repository.ImageQuery;
import com.freshdigitable.udonroad.repository.ImageRepository;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;

import static android.app.Activity.RESULT_OK;

/**
 * Created by akihit on 2017/12/02.
 */

class TweetInputViewModel implements LifecycleObserver {
  private final StatusRequestWorker statusRequestWorker;
  private final AppSettingStore appSettings;
  private final TypedCache<Status> statusCache;
  private final ImageRepository imageRepository;

  private ReplyEntity replyEntity;
  private final List<Uri> media = new ArrayList<>(4);
  private final List<Long> quoteStatusIds = new ArrayList<>(4);
  private PublishSubject<List<Uri>> mediaEmitter;
  private Uri cameraPicUri;

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  public void onStart() {
    appSettings.open();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  public void onStop() {
    appSettings.close();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  public void onDestroy() {
    if (mediaEmitter != null && mediaEmitter.hasObservers()) {
      mediaEmitter.onComplete();
    }
  }

  @Inject
  TweetInputViewModel(AppSettingStore appSettings, TypedCache<Status> statusCache,
                      ImageRepository imageRepository, StatusRequestWorker statusRequestWorker) {
    this.appSettings = appSettings;
    this.statusCache = statusCache;
    this.imageRepository = imageRepository;
    this.statusRequestWorker = statusRequestWorker;
    mediaEmitter = PublishSubject.create();
  }

  void setReplyToStatusId(long inReplyToStatusId) {
    statusCache.open();
    final Status inReplyTo = statusCache.find(inReplyToStatusId);
    if (inReplyTo != null) {
      replyEntity = ReplyEntity.create(inReplyTo, appSettings.getCurrentUserId());
    }
    statusCache.close();
  }

  int getUrlLength() {
    final TwitterAPIConfiguration twitterAPIConfig = appSettings.getTwitterAPIConfig();
    return twitterAPIConfig != null ? twitterAPIConfig.getShortURLLengthHttps() : 0;
  }

  void addQuoteId(long quoteId) {
    quoteStatusIds.add(quoteId);
  }

  Observable<User> observeCurrentUser() {
    return appSettings.observeCurrentUser();
  }

  Observable<Drawable> queryImage(ImageQuery query) {
    return imageRepository.queryImage(query);
  }

  Observable<Drawable> queryImage(Single<ImageQuery> query) {
    return imageRepository.queryImage(query);
  }

  Single<Status> createSendObservable(Context context, String sendingText) {
    if (!isStatusUpdateNeeded()) {
      return statusRequestWorker.observeUpdateStatus(sendingText);
    }
    final StatusUpdate statusUpdate = createStatusUpdate(sendingText);
    return media.size() > 0 ? statusRequestWorker.observeUpdateStatus(context, statusUpdate, media)
        : statusRequestWorker.observeUpdateStatus(statusUpdate);
  }

  private boolean isStatusUpdateNeeded() {
    return replyEntity != null
        || quoteStatusIds.size() > 0
        || media.size() > 0;
  }

  private StatusUpdate createStatusUpdate(@NonNull String sendingText) {
    final StringBuilder s = new StringBuilder(sendingText);
    statusCache.open();
    for (long q : quoteStatusIds) {
      final Status status = statusCache.find(q);
      if (status == null) {
        continue;
      }
      s.append(" https://twitter.com/")
          .append(status.getUser().getScreenName()).append("/status/").append(q);
    }
    statusCache.close();
    final StatusUpdate statusUpdate = new StatusUpdate(s.toString());
    if (replyEntity != null) {
      statusUpdate.setInReplyToStatusId(replyEntity.getInReplyToStatusId());
    }
    return statusUpdate;
  }

  Observable<List<Uri>> observeMedia() {
    return mediaEmitter.publish();
  }

  Intent getMediaChooserIntent(Context context) {
    final Intent pickMediaIntent = getPickMediaIntent();
    final Intent cameraIntent = getCameraIntent(context);
    cameraPicUri = cameraIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
    final Intent chooser = Intent.createChooser(pickMediaIntent, context.getString(R.string.media_chooser_title));
    chooser.putExtra(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
            Intent.EXTRA_ALTERNATE_INTENTS : Intent.EXTRA_INITIAL_INTENTS,
        new Intent[]{cameraIntent});
    return chooser;
  }

  @NonNull
  private static Intent getPickMediaIntent() {
    final Intent intent;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
    } else {
      intent = new Intent(Intent.ACTION_GET_CONTENT);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    }
    intent.setType("image/*");
    return intent;
  }

  @NonNull
  private static Intent getCameraIntent(Context context) {
    final ContentValues contentValues = new ContentValues();
    contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
    final long timeStamp = System.currentTimeMillis();
    contentValues.put(MediaStore.Images.Media.TITLE, timeStamp + ".jpg");
    contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, timeStamp + ".jpg");

    final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    final Uri cameraPicUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPicUri);
    return intent;
  }

  void onMediaChooserResult(Context context, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
      final List<Uri> uris = parseMediaData(data);
      if (uris.isEmpty()) {
        addMedia(cameraPicUri);
      } else {
        addAllMedia(uris);
        removeFromContentResolver(context, cameraPicUri);
      }
    } else {
      removeFromContentResolver(context, cameraPicUri);
    }
    cameraPicUri = null;
  }

  private List<Uri> parseMediaData(Intent data) {
    if (data != null && data.getData() != null) {
      return Collections.singletonList(data.getData());
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      return parseClipData(data);
    }
    return Collections.emptyList();
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
  private static List<Uri> parseClipData(Intent data) {
    if (data == null || data.getClipData() == null) {
      return Collections.emptyList();
    }
    final ClipData clipData = data.getClipData();
    final int itemCount = clipData.getItemCount();
    final ArrayList<Uri> res = new ArrayList<>(itemCount);
    for (int i = 0; i < itemCount; i++) {
      final ClipData.Item item = clipData.getItemAt(i);
      res.add(item.getUri());
    }
    return res;
  }

  private static void removeFromContentResolver(@NonNull Context context, Uri cameraPicUri) {
    if (cameraPicUri == null) {
      return;
    }
    context.getContentResolver().delete(cameraPicUri, null, null);
  }

  private void addMedia(Uri uri) {
    media.add(uri);
    mediaEmitter.onNext(media);
  }

  private void addAllMedia(Collection<Uri> uris) {
    media.addAll(uris);
    mediaEmitter.onNext(media);
  }

  void removeMedia(Uri uri) {
    media.remove(uri);
    mediaEmitter.onNext(media);
  }

  void clear() {
    replyEntity = null;
    quoteStatusIds.clear();
    clearMedia();
  }

  private void clearMedia() {
    media.clear();
    mediaEmitter.onNext(media);
  }

  boolean isCleared() {
    return replyEntity == null && quoteStatusIds.isEmpty() && media.isEmpty();
  }

  boolean hasQuoteStatus() {
    return !quoteStatusIds.isEmpty();
  }

  boolean hasReplyEntity() {
    return replyEntity != null;
  }

  String createReplyString() {
    return replyEntity != null ? replyEntity.createReplyString() : "";
  }

  private static final String SS_MEDIA = "ss_media";
  private static final String SS_QUOTED_STATUS_IDS = "ss_quotedStatusIds";
  private static final String SS_CAMERA_PIC_URI = "ss_cameraPicUri";
  private static final String SS_REPLY_ENTITY = "ss_replyEntity";

  void onSaveInstanceState(Bundle outState) {
    outState.putParcelableArray(SS_MEDIA, media.toArray(new Uri[media.size()]));
    final long[] qIds = new long[quoteStatusIds.size()];
    for (int i = 0; i < quoteStatusIds.size(); i++) {
      qIds[i] = quoteStatusIds.get(i);
    }
    outState.putLongArray(SS_QUOTED_STATUS_IDS, qIds);
    outState.putParcelable(SS_CAMERA_PIC_URI, cameraPicUri);
    if (replyEntity != null) {
      outState.putParcelable(SS_REPLY_ENTITY, replyEntity);
    }
  }

  void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      return;
    }
    final Parcelable[] uris = savedInstanceState.getParcelableArray(SS_MEDIA);
    if (uris != null && uris.length > 0) {
      for (Parcelable p : uris) {
        media.add((Uri) p);
      }
    }

    final long[] qIds = savedInstanceState.getLongArray(SS_QUOTED_STATUS_IDS);
    if (qIds != null) {
      for (long qId : qIds) {
        quoteStatusIds.add(qId);
      }
    }

    cameraPicUri = savedInstanceState.getParcelable(SS_CAMERA_PIC_URI);
    replyEntity = savedInstanceState.getParcelable(SS_REPLY_ENTITY);
  }
}
