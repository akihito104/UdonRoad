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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Intent;
import android.databinding.ObservableField;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;

import com.freshdigitable.udonroad.OnSpanClickListener;
import com.freshdigitable.udonroad.TwitterCard;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
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

public class StatusDetailViewModel extends ViewModel {
  private static final String TAG = StatusDetailViewModel.class.getSimpleName();
  private final StatusRepository statusRepository;
  private final AppSettingStore appSetting;

  private final MutableLiveData<SpanClickEvent> spanClickEventSource;
  private final MutableLiveData<DetailItem> detailItemSource;
  private final MutableLiveData<TwitterCard> twitterCardSource;
  public final ObservableField<DetailItem> detailItem = new ObservableField<>();
  public final ObservableField<TwitterCard> cardItem = new ObservableField<>();
  private Disposable itemSubs;
  private Disposable cardSubs;

  @Inject
  StatusDetailViewModel(StatusRepository statusRepository, AppSettingStore appSetting) {
    this.statusRepository = statusRepository;
    this.appSetting = appSetting;
    this.statusRepository.open();
    this.appSetting.open();
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
          detailItemSource.postValue(getDetailItem(s));
          detailItem.set(getDetailItem(s));
          fetchTwitterCard(s);
        });
    return detailItemSource;
  }

  LiveData<SpanClickEvent> getSpanClickEvent() {
    return spanClickEventSource;
  }

  @NonNull
  private DetailItem getDetailItem(Status s) {
    return new DetailItem(s, (v, i) -> {
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
        .subscribe(cardItem::set,
            throwable -> Timber.tag(TAG).e(throwable, "card fetch: "));
  }

  public void onTwitterCardClicked(View view) {
    final TwitterCard card = this.cardItem.get();
    if (card == null || !card.isValid()) {
      return;
    }
    final Intent intent = new Intent(Intent.ACTION_VIEW);
    final String appUrl = card.getAppUrl();
    if (!TextUtils.isEmpty(appUrl)) {
      intent.setData(Uri.parse(appUrl));
      final ComponentName componentName = intent.resolveActivity(view.getContext().getPackageManager());
      if (componentName == null) {
        intent.setData(Uri.parse(card.getUrl()));
      }
    } else {
      intent.setData(Uri.parse(card.getUrl()));
    }
    view.getContext().startActivity(intent);
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

  private boolean isTweetOfMe() {
    final DetailItem item = detailItemSource.getValue();
    if (item == null) {
      return false;
    }
    final long currentUserId = appSetting.getCurrentUserId();
    return item.retweet ? item.retweetUser.getId() == currentUserId
        : item.user.getId() == currentUserId;
  }

  public void delete(long statusId) {
    if (isTweetOfMe()) {
      statusRepository.destroyStatus(statusId);
    }
  }

  public static class SpanClickEvent {
    public final View view;
    public final OnSpanClickListener.SpanItem item;

    SpanClickEvent(View view, OnSpanClickListener.SpanItem item) {
      this.view = view;
      this.item = item;
    }
  }

  static StatusDetailViewModel getInstance(FragmentActivity activity, ViewModelProvider.Factory factory) {
    return ViewModelProviders.of(activity, factory).get(StatusDetailViewModel.class);
  }
}
