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

package com.freshdigitable.udonroad.detail;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.view.View;

import com.freshdigitable.udonroad.OnSpanClickListener;
import com.freshdigitable.udonroad.TwitterCard;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.StatusRepository;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;
import twitter4j.Status;
import twitter4j.URLEntity;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

/**
 * Created by akihit on 2018/01/27.
 */

public class StatusDetailViewModel extends AndroidViewModel {
  private static final String TAG = StatusDetailViewModel.class.getSimpleName();
  @Inject
  StatusRepository statusRepository;
  @Inject
  AppSettingStore appSetting;

  private final MutableLiveData<SpanClickEvent> spanClickEventSource;
  private final MutableLiveData<DetailItem> detailItemSource;
  private final MutableLiveData<TwitterCard> twitterCardSource;
  private Disposable itemSubs;
  private Disposable cardSubs;

  public StatusDetailViewModel(Application application) {
    super(application);
    InjectionUtil.getComponent(application).inject(this);

    statusRepository.open();
    appSetting.open();
    spanClickEventSource = new MutableLiveData<>();
    detailItemSource = new MutableLiveData<>();
    twitterCardSource = new MutableLiveData<>();
  }

  LiveData<DetailItem> observeById(long id) {
    if (detailItemSource.getValue() != null && detailItemSource.getValue().id == id) {
      return detailItemSource;
    }
    Utils.maybeDispose(itemSubs);
    itemSubs = statusRepository.observeById(id)
        .subscribe(s -> {
          detailItemSource.setValue(getDetailItem(s));
          fetchTwitterCard(s);
        });
    return detailItemSource;
  }

  LiveData<SpanClickEvent> getSpanClickEvent() {
    return spanClickEventSource;
  }

  LiveData<TwitterCard> getTwitterCard() {
    return twitterCardSource;
  }

  @NonNull
  private DetailItem getDetailItem(Status s) {
    return new DetailItem(s, getApplication(), (v, i) -> {
      spanClickEventSource.setValue(new SpanClickEvent(v, i));
      spanClickEventSource.setValue(null);
    });
  }

  private void fetchTwitterCard(Status status) {
    final Status bindingStatus = getBindingStatus(status);
    final URLEntity[] urlEntities = bindingStatus.getURLEntities();
    if (urlEntities.length < 1) {
      return;
    }

    final String expandedURL = urlEntities[0].getExpandedURL();
    if (twitterCardSource.getValue() != null && expandedURL.equals(twitterCardSource.getValue().getUrl())) {
      return;
    }
    Utils.maybeDispose(cardSubs);
    cardSubs = TwitterCard.observeFetch(expandedURL)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(twitterCardSource::setValue,
            throwable -> Timber.tag(TAG).e(throwable, "card fetch: "));
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    Utils.maybeDispose(itemSubs);
    Utils.maybeDispose(cardSubs);
    statusRepository.close();
    appSetting.close();
  }

  void createFavorite(long statusId) {
    statusRepository.createFavorite(statusId);
  }

  void destroyFavorite(long statusId) {
    statusRepository.destroyFavorite(statusId);
  }

  void retweet(long statusId) {
    statusRepository.retweetStatus(statusId);
  }

  void unretweet(long statusId) {
    statusRepository.destroyRetweet(statusId, isRetweetOfMe());
  }

  private boolean isRetweetOfMe() {
    final DetailItem item = detailItemSource.getValue();
    if (item == null || !item.retweet) {
      return false;
    }
    final long currentUserId = appSetting.getCurrentUserId();
    return item.retweetUser.getId() == currentUserId;
  }

  boolean isTweetOfMe() {
    final DetailItem item = detailItemSource.getValue();
    if (item == null) {
      return false;
    }
    final long currentUserId = appSetting.getCurrentUserId();
    return item.retweet ? item.retweetUser.getId() == currentUserId
        : item.user.getId() == currentUserId;
  }

  public static class SpanClickEvent {
    public final View view;
    public final OnSpanClickListener.SpanItem item;

    SpanClickEvent(View view, OnSpanClickListener.SpanItem item) {
      this.view = view;
      this.item = item;
    }
  }
}
