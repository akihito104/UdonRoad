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
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;

import com.freshdigitable.udonroad.OnSpanClickListener;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.TwitterCard;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.repository.ImageQuery;
import com.freshdigitable.udonroad.repository.ImageRepository;

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
  TypedCache<Status> statusCache;
  @Inject
  ImageRepository imageRepository;

  private final MutableLiveData<SpanClickEvent> spanClickEventSource;

  public StatusDetailViewModel(Application application) {
    super(application);
    InjectionUtil.getComponent(application).inject(this);

    statusCache.open();
    spanClickEventSource = new MutableLiveData<>();
  }

  public LiveData<DetailItem> findById(long id) {
    return new LiveData<DetailItem>() {
      private Disposable itemSubs;

      @Override
      protected void onActive() {
        super.onActive();
        itemSubs = statusCache.observeById(id)
            .map(this::getDetailItem)
            .subscribe(this::setValue);
      }

      @NonNull
      private DetailItem getDetailItem(Status s) {
        return new DetailItem(s, getApplication(), (v, i) -> {
          spanClickEventSource.setValue(new SpanClickEvent(v, i));
          spanClickEventSource.setValue(null);
        });
      }

      @Override
      protected void onInactive() {
        super.onInactive();
        Utils.maybeDispose(itemSubs);
      }
    };
  }

  public LiveData<SpanClickEvent> getSpanClickEvent() {
    return spanClickEventSource;
  }

  public LiveData<TwitterCard> getTwitterCard(long id) {
    final Status status = statusCache.find(id);
    final Status bindingStatus = getBindingStatus(status);
    final URLEntity[] urlEntities = bindingStatus.getURLEntities();
    return new LiveData<TwitterCard>() {
      private Disposable cardSubscription;

      @Override
      protected void onActive() {
        super.onActive();
        if (urlEntities.length < 1) {
          return;
        }
        final String expandedURL = urlEntities[0].getExpandedURL();
        cardSubscription = TwitterCard.observeFetch(expandedURL)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::setValue,
                throwable -> Timber.tag(TAG).e(throwable, "card fetch: "));
      }

      @Override
      protected void onInactive() {
        super.onInactive();
        Utils.maybeDispose(cardSubscription);
      }
    };
  }

  public LiveData<Drawable> loadTwitterCardImage(String url) {
    return new LiveData<Drawable>() {
      private Disposable cardSummaryImageSubs;

      @Override
      protected void onActive() {
        super.onActive();
        if (TextUtils.isEmpty(url)) {
          return;
        }
        final ImageQuery query = new ImageQuery.Builder(url)
            .sizeForSquare(getApplication(), R.dimen.card_summary_image)
            .centerCrop()
            .build();
        cardSummaryImageSubs = imageRepository.queryImage(query)
            .subscribe(this::setValue, th -> {});
      }

      @Override
      protected void onInactive() {
        super.onInactive();
        Utils.maybeDispose(cardSummaryImageSubs);
      }
    };
  }

  @Override
  protected void onCleared() {
    super.onCleared();
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
