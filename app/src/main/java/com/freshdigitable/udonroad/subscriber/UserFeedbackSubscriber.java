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

package com.freshdigitable.udonroad.subscriber;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import com.freshdigitable.udonroad.SnackBarUtil;

import java.lang.ref.WeakReference;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;
import timber.log.Timber;

/**
 * UserFeedbackSubscriber subscribes request job such as RT and fav.
 *
 * Created by akihit on 2016/11/02.
 */
public class UserFeedbackSubscriber {
  @SuppressWarnings("unused")
  private static final String TAG = UserFeedbackSubscriber.class.getSimpleName();
  private Disposable feedbackSubscription;
  private final PublishProcessor<UserFeedbackEvent> feedbackSubject;
  private final Context context;

  public UserFeedbackSubscriber(@NonNull Context context,
                                PublishProcessor<UserFeedbackEvent> feedbackSubject) {
    this.context = context;
    this.feedbackSubject = feedbackSubject;
    subscribe();
  }

  private void subscribe() {
    if (feedbackSubscription != null && !feedbackSubscription.isDisposed()) {
      return;
    }
    this.feedbackSubscription = this.feedbackSubject.onBackpressureBuffer()
        .observeOn(AndroidSchedulers.mainThread())
        .map(this::createFeedbackAction)
        .subscribe(Runnable::run, e -> Timber.tag(TAG).e(e, "feedback: "));
  }

  private Runnable createFeedbackAction(final UserFeedbackEvent msg) {
    final View view = rootView.get();
    final CharSequence message = msg.createMessage(context);
    if (view != null) {
      return SnackBarUtil.action(view, message);
    }
    final Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
    toast.setGravity(gravityFlag, gravityXOffset, gravityYOffset);
    return toast::show;
  }

  private WeakReference<View> rootView;

  public void registerRootView(@NonNull View rootView) {
    if (isRegisteredView(rootView)) {
      return;
    }
    this.rootView = new WeakReference<>(rootView);
    subscribe();
  }

  private boolean isRegisteredView(@NonNull View rootView) {
    if (this.rootView == null) {
      return false;
    }
    final View v = this.rootView.get();
    return rootView.equals(v);
  }

  public void unregisterRootView(View view) {
    if (isRegisteredView(view)) {
      rootView.clear();
    }
  }

  private int gravityFlag;
  private int gravityXOffset;
  private int gravityYOffset;

  public void setToastOption(int gravityFlag, int gravityXOffset, int gravityYOffset) {
    this.gravityFlag = gravityFlag;
    this.gravityXOffset = gravityXOffset;
    this.gravityYOffset = gravityYOffset;
  }

  public void unsubscribe() {
    if (feedbackSubscription != null && !feedbackSubscription.isDisposed()) {
      feedbackSubscription.dispose();
    }
    if (rootView != null) {
      rootView.clear();
    }
  }
}
