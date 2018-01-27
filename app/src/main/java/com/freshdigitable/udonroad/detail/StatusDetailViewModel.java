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
import android.view.View;

import com.freshdigitable.udonroad.OnSpanClickListener;
import com.freshdigitable.udonroad.TwitterCard;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.InjectionUtil;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;
import twitter4j.Status;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

/**
 * Created by akihit on 2018/01/27.
 */

public class StatusDetailViewModel extends AndroidViewModel {
  private static final String TAG = StatusDetailViewModel.class.getSimpleName();
  @Inject
  TypedCache<Status> statusCache;
  private final MutableLiveData<SpanClickEvent> spanClickEventSource;
  private final MutableLiveData<DetailItem> detailItemSource;
  private final MutableLiveData<TwitterCard> twitterCardSource;
  private Disposable cardSubscription;
  private Disposable itemSubs;

  public StatusDetailViewModel(Application application) {
    super(application);
    InjectionUtil.getComponent(application).inject(this);

    statusCache.open();
    spanClickEventSource = new MutableLiveData<>();
    detailItemSource = new MutableLiveData<>();
    twitterCardSource = new MutableLiveData<>();
  }

  public LiveData<DetailItem> findById(long id) {
    final Status status = statusCache.find(id);
    final DetailItem detailItem = new DetailItem(status, getApplication(), (v, i) -> {
      spanClickEventSource.setValue(new SpanClickEvent(v, i));
      spanClickEventSource.setValue(null);
    });
    itemSubs = statusCache.observeById(status)
        .map(s -> new DetailItem(s, getApplication(), (v, i) -> {
          spanClickEventSource.setValue(new SpanClickEvent(v, i));
          spanClickEventSource.setValue(null);
        }))
        .subscribe(detailItemSource::setValue);
    detailItemSource.setValue(detailItem);
    return detailItemSource;
  }

  public LiveData<SpanClickEvent> getSpanClickEvent() {
    return spanClickEventSource;
  }

  public LiveData<TwitterCard> getTwitterCard(long id) {
    final Status status = statusCache.find(id);
    final Status bindingStatus = getBindingStatus(status);
    if (bindingStatus.getURLEntities().length < 1) {
      return twitterCardSource;
    }
    final String expandedURL = bindingStatus.getURLEntities()[0].getExpandedURL();
    cardSubscription = TwitterCard.observeFetch(expandedURL)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(twitterCardSource::setValue,
            throwable -> Timber.tag(TAG).e(throwable, "card fetch: "));
    return twitterCardSource;
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    Utils.maybeDispose(itemSubs);
    Utils.maybeDispose(cardSubscription);
    statusCache.close();
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
