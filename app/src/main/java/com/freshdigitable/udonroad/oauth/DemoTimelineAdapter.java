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

import android.graphics.drawable.Drawable;
import android.support.v7.content.res.AppCompatResources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.databinding.ViewStatusBinding;
import com.freshdigitable.udonroad.listitem.ItemViewHolder;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.StatusViewHolder;
import com.freshdigitable.udonroad.listitem.TwitterListItem;
import com.freshdigitable.udonroad.timeline.TimelineAdapter;
import com.freshdigitable.udonroad.timeline.TimelineViewModel;

/**
 * Created by akihit on 2018/04/01.
 */
public class DemoTimelineAdapter extends TimelineAdapter {
  private static final int TYPE_AUTH = 0;
  private static final int TYPE_TWEET = 1;

  public DemoTimelineAdapter(TimelineViewModel viewModel) {
    super(viewModel);
  }

  @Override
  public int getItemViewType(int position) {
    return viewModel.get(position) instanceof TwitterListItem ? TYPE_TWEET : TYPE_AUTH;
  }

  @Override
  public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return viewType == TYPE_TWEET ?
        new StatusViewHolder(ViewStatusBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), viewModel)
        : new DemoViewHolder(View.inflate(parent.getContext(), R.layout.view_pin_auth, null));
  }

  @Override
  public void onBindViewHolder(ItemViewHolder holder, int position) {
    if (holder instanceof StatusViewHolder) {
      holder.bind(viewModel.get(position));
      final ImageView userIcon = holder.getUserIcon();
      final Drawable icon = AppCompatResources.getDrawable(userIcon.getContext(), R.mipmap.ic_launcher);
      userIcon.setImageDrawable(icon);
      final StatusViewHolder statusViewHolder = (StatusViewHolder) holder;
      if (statusViewHolder.hasQuotedView()) {
        statusViewHolder.getQuotedStatusView().qIcon.setImageDrawable(icon);
      }
    }
  }

  interface OnSendPinClickListener {
    void onClick(View v, String pin);
  }

  View.OnClickListener loginClickListener;
  OnSendPinClickListener sendPinClickListener;

  @Override
  public void onViewAttachedToWindow(ItemViewHolder holder) {
    if (holder instanceof DemoViewHolder) {
      ((DemoViewHolder) holder).oauthButton.setOnClickListener(loginClickListener);
      ((DemoViewHolder) holder).sendPin.setOnClickListener(v ->
          sendPinClickListener.onClick(v, ((DemoViewHolder) holder).pin.getText().toString()));
    } else {
      super.onViewAttachedToWindow(holder);
    }
  }

  @Override
  public void onViewDetachedFromWindow(ItemViewHolder holder) {
    if (holder instanceof DemoViewHolder) {
      ((DemoViewHolder) holder).oauthButton.setOnClickListener(null);
      ((DemoViewHolder) holder).sendPin.setOnClickListener(null);
    } else {
      super.onViewDetachedFromWindow(holder);
    }
  }

  private static class DemoViewHolder extends ItemViewHolder {
    private View oauthButton;
    private EditText pin;
    private Button sendPin;

    DemoViewHolder(View view) {
      super(view);
      oauthButton = view.findViewById(R.id.oauth_start);
      pin = view.findViewById(R.id.oauth_pin);
      sendPin = view.findViewById(R.id.oauth_send_pin);
    }

    @Override
    public ImageView getUserIcon() {
      return null;
    }

    @Override
    public void onUpdate(ListItem item) {}
  }
}
