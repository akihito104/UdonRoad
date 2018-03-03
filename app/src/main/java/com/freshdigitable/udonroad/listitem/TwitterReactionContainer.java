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
import android.databinding.BindingAdapter;
import android.support.annotation.Nullable;
import android.support.v7.content.res.AppCompatResources;
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

  @BindingAdapter("stats")
  public static void setStats(TwitterReactionContainer container, List<Stat> stats) {
    container.update(stats);
  }

  @Override
  public void update(List<Stat> stats) {
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
    return ReactionIcon.fromType(s.getType()).create(getContext());
  }

  private static void updateReaction(View v, Stat s) {
    ReactionIcon.fromType(s.getType()).update(v, s);
  }

  public enum ReactionIcon {
    RETWEET(R.drawable.ic_retweet) {
      @Override
      View create(Context context) {
        return createIconAttachedTextView(context);
      }

      @Override
      void update(View v, Stat s) {
        if (s.getCount() <= 0 && !s.isMarked()) {
          v.setVisibility(GONE);
          return;
        }
        final IconAttachedTextView rtCount = (IconAttachedTextView) v;
        rtCount.setVisibility(VISIBLE);
        rtCount.tintIcon(s.isMarked() ? R.color.twitter_action_retweeted
            : R.color.twitter_action_normal);
        final String text = String.valueOf(s.getCount());
        if (!text.equals(rtCount.getText())) {
          rtCount.setText(text);
        }
      }
    },
    FAV(R.drawable.ic_like) {
      @Override
      View create(Context context) {
        return createIconAttachedTextView(context);
      }

      @Override
      void update(View v, Stat s) {
        if (s.getCount() <= 0 && !s.isMarked()) {
          v.setVisibility(GONE);
          return;
        }
        final IconAttachedTextView favCount = (IconAttachedTextView) v;
        favCount.setVisibility(VISIBLE);
        favCount.tintIcon(s.isMarked() ? R.color.twitter_action_faved
            : R.color.twitter_action_normal);
        final String text = String.valueOf(s.getCount());
        if (!text.equals(favCount.getText())) {
          favCount.setText(text);
        }
      }
    },
    IN_REPLY_TO(R.drawable.ic_forum) {
      @Override
      View create(Context context) {
        final ImageView view = new AppCompatImageView(context);
        view.setId(type);
        view.setLayoutParams(createLayoutParams(context));
        view.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_forum));
        return view;
      }

      @Override
      void update(View v, Stat s) {
        v.setVisibility(s.isMarked() ? VISIBLE : GONE);
      }
    },
    FOLLOWING(R.drawable.ic_following) {
      @Override
      View create(Context context) {
        final IconAttachedTextView view = createIconAttachedTextView(context);
        view.setVisibility(VISIBLE);
        return view;
      }

      @Override
      void update(View v, Stat s) {
        ((IconAttachedTextView) v).setText(String.valueOf(s.getCount()));
      }
    },
    FOLLOWER(R.drawable.ic_follower) {
      @Override
      View create(Context context) {
        final IconAttachedTextView view = createIconAttachedTextView(context);
        view.setVisibility(VISIBLE);
        return view;
      }

      @Override
      void update(View v, Stat s) {
        ((IconAttachedTextView) v).setText(String.valueOf(s.getCount()));
      }
    };

    public final int type;

    ReactionIcon(int type) {
      this.type = type;
    }

    abstract View create(Context context);

    abstract void update(View v, Stat s);

    IconAttachedTextView createIconAttachedTextView(Context context) {
      final IconAttachedTextView view = new IconAttachedTextView(context);
      view.setId(type);
      view.setLayoutParams(createLayoutParams(context));
      view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);  // R.dimen.tweet_small_fontsize
      view.setIcon(type);
      return view;
    }

    private static MarginLayoutParams createLayoutParams(Context context) {
      final MarginLayoutParams lp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
      lp.rightMargin = context.getResources().getDimensionPixelSize(R.dimen.grid_margin);
      return lp;
    }

    static ReactionIcon fromType(int type) {
      for (ReactionIcon i : values()) {
        if (i.type == type) {
          return i;
        }
      }
      throw new IllegalStateException();
    }
  }
}
