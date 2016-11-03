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
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.freshdigitable.udonroad.SnackBarUtil;

import rx.functions.Action0;
import rx.functions.Action1;

/**
 * FeedbackAction defines subscriber action such as StatusRequestWorker and UserRequestWorker.
 *
 * Created by akihit on 2016/09/03.
 */
public interface FeedbackAction {
  String TAG = FeedbackAction.class.getSimpleName();

  Action1<Throwable> onErrorDefault(@StringRes int msg);

  Action0 onCompleteDefault(@StringRes int msg);

  class LogFeedback implements FeedbackAction {
    @Override
    public Action1<Throwable> onErrorDefault(final @StringRes int msg) {
      return new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
          Log.e(TAG, "message: " + msg, throwable);
        }
      };
    }

    @Override
    public Action0 onCompleteDefault(final @StringRes int msg) {
      return new Action0() {
        @Override
        public void call() {
          Log.d(TAG, "call: " + msg);
        }
      };
    }
  }

  class SnackbarFeedback implements FeedbackAction {
    private final View root;

    public SnackbarFeedback(View root) {
      this.root = root;
    }

    @Override
    public Action1<Throwable> onErrorDefault(final @StringRes int msg) {
      return new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
          SnackBarUtil.show(root, msg);
          Log.e(TAG, "msg: " + msg, throwable);
        }
      };
    }

    @Override
    public Action0 onCompleteDefault(@StringRes int msg) {
      return SnackBarUtil.action(root, msg);
    }
  }

  class ToastFeedback implements FeedbackAction {
    private final Context context;
    private final int gravityFlag;
    private final int gravityXOffset;
    private final int gravityYOffset;

    public ToastFeedback(Context context) {
      this(context, 0, 0, 0);
    }

    public ToastFeedback(Context context, int gravityFlag, int gravityXOffset, int gravityYOffset) {
      this.context = context;
      this.gravityFlag = gravityFlag;
      this.gravityXOffset = gravityXOffset;
      this.gravityYOffset = gravityYOffset;
    }

    @Override
    public Action1<Throwable> onErrorDefault(final @StringRes int msg) {
      return new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
          showToast(msg);
        }
      };
    }

    @Override
    public Action0 onCompleteDefault(final @StringRes int msg) {
      return showToastAction(msg);
    }

    private Action0 showToastAction(final @StringRes int text) {
      return new Action0() {
        @Override
        public void call() {
          showToast(text);
        }
      };
    }

    private void showToast(@StringRes int text) {
      final Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
      toast.setGravity(gravityFlag, gravityXOffset, gravityYOffset);
      toast.show();
    }
  }
}
