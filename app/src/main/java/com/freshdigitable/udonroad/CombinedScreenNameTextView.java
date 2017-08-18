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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;

/**
 * CombinedScreenNameTextView combines screen name and account with specified format.
 *
 * Created by akihit on 2016/07/09.
 */
public class CombinedScreenNameTextView extends AppCompatTextView {
  private final ImageSpan verifiedIcon;
  private final ImageSpan protectedIcon;

  public CombinedScreenNameTextView(Context context) {
    this(context, null);
  }

  public CombinedScreenNameTextView(Context context, AttributeSet attrs) {
    this(context, attrs, android.R.attr.textViewStyle);
  }

  public CombinedScreenNameTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    verifiedIcon = createIcon(R.drawable.ic_check_circle);
    protectedIcon = createIcon(R.drawable.ic_lock);
    // SpannableString + ImageSpan don't work in Android API 21 & 22
    // refs: http://stackoverflow.com/questions/3176033/spannablestring-with-image-example
    setTransformationMethod(null);
  }

  private CombinedName oldName;

  public void setNames(@NonNull CombinedName combinedName) {
    if (combinedName.equals(oldName)) {
      return;
    }
    final SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(combinedName.getName());
    spannableStringBuilder.setSpan(STYLE_BOLD, 0, combinedName.getName().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    if (!TextUtils.isEmpty(combinedName.getScreenName())) {
      spannableStringBuilder.append(TextViewCompat.getMaxLines(this) == 2 ? "\n" : " ")
          .append("@").append(combinedName.getScreenName());
    }
    if (combinedName.isVerified()) {
      appendIconToEnd(spannableStringBuilder, verifiedIcon);
    }
    if (combinedName.isPrivate()) {
      appendIconToEnd(spannableStringBuilder, protectedIcon);
    }
    setText(spannableStringBuilder);
    oldName = combinedName;
  }

  private void appendIconToEnd(SpannableStringBuilder ssb, ImageSpan iconSpan) {
    DrawableCompat.setTint(iconSpan.getDrawable(), getCurrentTextColor());
    final int start = ssb.length();
    // SpannableStringBuilder.append(CharSequence,Object,int) is available in API 21+
    ssb.append(" ");
    ssb.setSpan(iconSpan, start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  @NonNull
  private ImageSpan createIcon(@DrawableRes int icon) {
    // drawable is cached and if it is tinted, all icons would be tinted. so it must be mutate().
    final Drawable iconDrawable = ContextCompat.getDrawable(getContext(), icon).mutate();
    final int width
        = iconDrawable.getIntrinsicWidth() * getLineHeight() / iconDrawable.getIntrinsicHeight();
    iconDrawable.setBounds(0, 0, width, getLineHeight());
    final int margin = getContext().getResources().getDimensionPixelSize(R.dimen.grid_margin);
    return new RefinedImageSpan(iconDrawable, DynamicDrawableSpan.ALIGN_BOTTOM, margin, 0);
  }

  private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

  public interface CombinedName {
    String getName();

    String getScreenName();

    boolean isPrivate();

    boolean isVerified();
  }
}