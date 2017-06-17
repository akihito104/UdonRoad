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

package com.freshdigitable.udonroad.listitem;

import android.content.Context;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.freshdigitable.udonroad.IconAttachedTextView;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.listitem.ListItem.Stat;

import java.util.List;

/**
 * Created by akihit on 2017/06/17.
 */

public class TwitterReactionContainer extends ReactionContainer {
  public TwitterReactionContainer(Context context) {
    this(context, null);
  }

  public TwitterReactionContainer(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TwitterReactionContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  void update(List<Stat> stats) {
    for (int i = reactions.size() - 1; i >= 0; i--) {
      final Stat old = stats.get(i);
      final Stat s = findStatByType(stats, old.getType());
      if (s == null) {
        removeViewAt(i);
        reactions.remove(i);
      }
    }
    for (int i = 0; i < stats.size(); i++) {
      final Stat s = stats.get(i);
      final int oldIndex = findIndexByType(reactions, s.getType());
      if (oldIndex < 0) {
        View v = createReactionIcon(s);
        updateReaction(v, s);
        addView(v, i);
        reactions.add(i, s);
      } else if (i == oldIndex) {
        final View v = getChildAt(i);
        updateReaction(v, s);
        reactions.remove(i);
        reactions.add(i, s);
      } else {
        final View v = getChildAt(oldIndex);
        updateReaction(v, s);
        removeViewAt(oldIndex);
        addView(v, i);
        reactions.remove(oldIndex);
        reactions.add(i, s);
      }
    }
  }

  private static int findIndexByType(List<Stat> reactions, int type) {
    final Stat s = findStatByType(reactions, type);
    return s != null ? reactions.indexOf(s) : -1;
  }

  private static Stat findStatByType(List<Stat> reactions, int type) {
    for (Stat s : reactions) {
      if (s.getType() == type) {
        return s;
      }
    }
    return null;
  }

  private View createReactionIcon(Stat s) {
    final int type = s.getType();
    if (type == R.drawable.ic_retweet) {
      final IconAttachedTextView view = new IconAttachedTextView(getContext());
      view.setLayoutParams(createLayoutParams(getContext()));
      view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10); // R.dimen.tweet_small_fontsize
      view.setIcon(R.drawable.ic_retweet);
      return view;
    } else if (type == R.drawable.ic_like) {
      final IconAttachedTextView view = new IconAttachedTextView(getContext());
      view.setLayoutParams(createLayoutParams(getContext()));
      view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10); // R.dimen.tweet_small_fontsize
      view.setIcon(R.drawable.ic_like);
      return view;
    } else if (type == R.drawable.ic_forum) {
      final ImageView view = new AppCompatImageView(getContext());
      view.setLayoutParams(createLayoutParams(getContext()));
      view.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_forum));
      return view;
    }
    throw new IllegalStateException();
  }

  private static void updateReaction(View v, Stat s) {
    final int type = s.getType();
    if (type == R.drawable.ic_retweet) {
      final IconAttachedTextView rtCount = (IconAttachedTextView) v;
      rtCount.setVisibility(s.getCount() > 0 || s.isMarked() ? VISIBLE : GONE);
      rtCount.tintIcon(s.isMarked()
          ? R.color.twitter_action_retweeted
          : R.color.twitter_action_normal);
      rtCount.setText(String.valueOf(s.getCount()));
    } else if (type == R.drawable.ic_like) {
      final IconAttachedTextView favCount = (IconAttachedTextView) v;
      favCount.setVisibility(s.getCount() > 0 || s.isMarked() ? VISIBLE : GONE);
      favCount.tintIcon(s.isMarked()
          ? R.color.twitter_action_faved
          : R.color.twitter_action_normal);
      favCount.setText(String.valueOf(s.getCount()));
    } else if (type == R.drawable.ic_forum) {
      final ImageView hasReplyIcon = (ImageView) v;
      hasReplyIcon.setVisibility(s.isMarked() ? VISIBLE : GONE);
    }
  }

  private static MarginLayoutParams createLayoutParams(Context context) {
    final MarginLayoutParams lp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    lp.rightMargin = getDimensionPixelSize(context, R.dimen.grid_margin);
    return lp;
  }

  private static int getDimensionPixelSize(Context context, @DimenRes int id) {
    return context.getResources().getDimensionPixelSize(id);
  }
}
