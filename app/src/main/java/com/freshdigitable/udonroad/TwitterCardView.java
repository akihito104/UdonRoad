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

package com.freshdigitable.udonroad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * TwitterCardView shows twitter card.
 *
 * Created by akihit on 2016/09/08.
 */
public class TwitterCardView extends RelativeLayout {

  private ImageView image;
  private TextView title;
  private TextView url;

  public TwitterCardView(Context context) {
    this(context, null);
  }

  public TwitterCardView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TwitterCardView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final View v = View.inflate(context, R.layout.view_twitter_card, this);
    image = (ImageView) v.findViewById(R.id.card_image);
    title = (TextView) v.findViewById(R.id.card_title);
    url = (TextView) v.findViewById(R.id.card_url);
  }

  public void bindData(TwitterCard data) {
    title.setText(data.getTitle());
    url.setText(data.getDisplayUrl());
  }

  public ImageView getImage() {
    return image;
  }
}
