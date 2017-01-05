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

package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * LinkableTextView is a TextView which is acceptable ClickableSpan and is colored at pressed.
 *
 * Created by akihit on 2017/01/05.
 */

public class LinkableTextView extends AppCompatTextView {
  public LinkableTextView(Context context) {
    this(context, null);
  }

  public LinkableTextView(Context context, AttributeSet attrs) {
    this(context, attrs, android.R.attr.textViewStyle);
  }

  public LinkableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final ColorStateList linkStateList
        = ContextCompat.getColorStateList(getContext(), R.color.selector_link_text);
    setMovementMethod(ColorStateLinkMovementMethod.getInstance(linkStateList));
  }

  private static class ColorStateLinkMovementMethod extends LinkMovementMethod {
    private static ColorStateLinkMovementMethod movementMethod;
    private final BackgroundColorSpan pressedBackground;

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
      final boolean res = super.onTouchEvent(widget, buffer, event);
      final int action = event.getAction();
      if (action == MotionEvent.ACTION_DOWN) {
        final ClickableSpan[] clickableSpans = findClickableSpan(widget, buffer, event);
        if (clickableSpans.length > 0) {
          final int spanStart = buffer.getSpanStart(clickableSpans[0]);
          final int spanEnd = buffer.getSpanEnd(clickableSpans[0]);
          buffer.setSpan(pressedBackground, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
      } else if (action == MotionEvent.ACTION_UP) {
        buffer.removeSpan(pressedBackground);
      }
      return res;
    }

    private static ClickableSpan[] findClickableSpan(
        TextView widget, Spannable buffer, MotionEvent event) {
      int x = (int) event.getX();
      int y = (int) event.getY();

      x -= widget.getTotalPaddingLeft();
      y -= widget.getTotalPaddingTop();

      x += widget.getScrollX();
      y += widget.getScrollY();

      Layout layout = widget.getLayout();
      int line = layout.getLineForVertical(y);
      int off = layout.getOffsetForHorizontal(line, x);

      return buffer.getSpans(off, off, ClickableSpan.class);
    }

    public static MovementMethod getInstance(ColorStateList colorStateList) {
      if (movementMethod == null) {
        movementMethod = new ColorStateLinkMovementMethod(colorStateList);
      }
      return movementMethod;
    }

    private ColorStateLinkMovementMethod(ColorStateList colorStateList) {
      final int pressedColor
          = colorStateList.getColorForState(new int[]{android.R.attr.state_pressed},
          colorStateList.getDefaultColor());
      pressedBackground = new BackgroundColorSpan(pressedColor);
    }
  }
}
