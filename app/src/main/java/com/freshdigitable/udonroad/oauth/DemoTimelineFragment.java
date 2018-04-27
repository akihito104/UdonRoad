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

package com.freshdigitable.udonroad.oauth;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.view.MenuItem;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.TimelineFragment;
import com.freshdigitable.udonroad.listitem.OnItemViewClickListener;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.reactivex.processors.PublishProcessor;

/**
 * Created by akihit on 2018/04/01.
 */
public class DemoTimelineFragment extends TimelineFragment {
  @Inject
  PublishProcessor<UserFeedbackEvent> userFeedback;

  @Override
  public void onAttach(Context context) {
    AndroidSupportInjection.inject(this);
    super.onAttach(context);
  }

  @Override
  public void onStart() {
    super.onStart();
    ((DemoTimelineAdapter) tlAdapter).loginClickListener = v ->
        ((OAuthActivity) getActivity()).startAuthorization();
    ((DemoTimelineAdapter) tlAdapter).sendPinClickListener = (v, pin) ->
        ((OAuthActivity) getActivity()).startAuthentication(pin);
  }

  @Override
  protected Observer<MenuItem> getMenuItemObserver() {
    return item -> {
      if (item == null) {
        return;
      }
      final int itemId = item.getItemId();
      if (itemId == R.id.iffabMenu_main_fav) {
        userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_fav));
      } else if (itemId == R.id.iffabMenu_main_rt) {
        userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_rt));
      } else if (itemId == R.id.iffabMenu_main_favRt) {
        userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_favRt));
      } else if (itemId == R.id.iffabMenu_main_detail) {
        userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_detail));
      } else if (itemId == R.id.iffabMenu_main_conv) {
        userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_conv));
      } else if (itemId == R.id.iffabMenu_main_reply) {
        userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_reply));
      } else if (itemId == R.id.iffabMenu_main_quote) {
        userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_quote));
      }
    };
  }

  @Override
  public void onStop() {
    super.onStop();
    ((DemoTimelineAdapter) tlAdapter).loginClickListener = null;
    ((DemoTimelineAdapter) tlAdapter).sendPinClickListener = null;
  }

  @Override
  protected OnItemViewClickListener createOnItemViewClickListener() {
    return null;
  }
}
