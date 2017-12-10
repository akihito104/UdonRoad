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

package com.freshdigitable.udonroad.input;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.freshdigitable.udonroad.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * Created by akihit on 2017/12/10.
 */

class MediaChooserController implements Parcelable {
  private static final int REQUEST_CODE_MEDIA_CHOOSER = 40;
  private static final int REQUEST_CODE_WRITE_EXTERNAL_PERMISSION = 50;

  void switchSoftKeyboardToMediaChooser(View view, Activity activity) {
    hideSoftKeyboard(view);
    showMediaChooserIfPermitted(activity);
  }

  void switchSoftKeyboardToMediaChooser(View view, Fragment fragment) {
    hideSoftKeyboard(view);
    showMediaChooserIfPermitted(fragment);
  }

  private void hideSoftKeyboard(View v) {
    final Context context = v.getContext();
    final InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
  }

  private void showMediaChooserIfPermitted(Activity activity) {
    if (isPermissionNeed(activity)) {
      ActivityCompat.requestPermissions(activity,
          new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_PERMISSION);
    } else {
      showMediaChooser(activity);
    }
  }

  private void showMediaChooserIfPermitted(Fragment fragment) {
    if (isPermissionNeed(fragment.getContext())) {
      fragment.requestPermissions(
          new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_PERMISSION);
    } else {
      showMediaChooser(fragment);
    }
  }

  void onRequestWriteExternalStoragePermissionResult(Activity activity, int requestCode) {
    if (requestCode != REQUEST_CODE_WRITE_EXTERNAL_PERMISSION) {
      showMediaChooser(activity);
    }
  }

  void onRequestWriteExternalStoragePermissionResult(Fragment fragment, int requestCode) {
    if (requestCode != REQUEST_CODE_WRITE_EXTERNAL_PERMISSION) {
      showMediaChooser(fragment);
    }
  }

  private void showMediaChooser(Activity activity) {
    final Intent chooser = getMediaChooserIntent(activity);
    activity.startActivityForResult(chooser, REQUEST_CODE_MEDIA_CHOOSER);
  }

  private void showMediaChooser(Fragment fragment) {
    final Intent chooser = getMediaChooserIntent(fragment.getContext());
    fragment.startActivityForResult(chooser, REQUEST_CODE_MEDIA_CHOOSER);
  }

  private static boolean isPermissionNeed(Context context) {
    return ActivityCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED;
  }

  private Uri cameraPicUri;

  private Intent getMediaChooserIntent(Context context) {
    final Intent pickMediaIntent = getPickMediaIntent();
    final Intent cameraIntent = getCameraIntent(context);
    cameraPicUri = cameraIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
    final Intent chooser = Intent.createChooser(pickMediaIntent, context.getString(R.string.media_chooser_title));
    chooser.putExtra(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
            Intent.EXTRA_ALTERNATE_INTENTS : Intent.EXTRA_INITIAL_INTENTS,
        new Intent[]{cameraIntent});
    return chooser;
  }

  @NonNull
  private static Intent getPickMediaIntent() {
    final Intent intent;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
    } else {
      intent = new Intent(Intent.ACTION_GET_CONTENT);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    }
    intent.setType("image/*");
    return intent;
  }

  @NonNull
  private static Intent getCameraIntent(Context context) {
    final ContentValues contentValues = new ContentValues();
    contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
    final long timeStamp = System.currentTimeMillis();
    contentValues.put(MediaStore.Images.Media.TITLE, timeStamp + ".jpg");
    contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, timeStamp + ".jpg");

    final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    final Uri cameraPicUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPicUri);
    return intent;
  }

  Collection<Uri> onMediaChooserResult(Context context, int requestCode, int resultCode, Intent data) {
    if (requestCode != REQUEST_CODE_MEDIA_CHOOSER) {
      return Collections.emptyList();
    }
    final Uri picUri = cameraPicUri;
    cameraPicUri = null;
    if (resultCode == RESULT_OK) {
      final List<Uri> uris = parseMediaData(data);
      if (uris.isEmpty()) {
        return Collections.singleton(picUri);
      } else {
        removeFromContentResolver(context, picUri);
        return uris;
      }
    } else {
      removeFromContentResolver(context, picUri);
      return Collections.emptyList();
    }
  }

  private static List<Uri> parseMediaData(Intent data) {
    if (data != null && data.getData() != null) {
      return Collections.singletonList(data.getData());
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      return parseClipData(data);
    }
    return Collections.emptyList();
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
  private static List<Uri> parseClipData(Intent data) {
    if (data == null || data.getClipData() == null) {
      return Collections.emptyList();
    }
    final ClipData clipData = data.getClipData();
    final int itemCount = clipData.getItemCount();
    final ArrayList<Uri> res = new ArrayList<>(itemCount);
    for (int i = 0; i < itemCount; i++) {
      final ClipData.Item item = clipData.getItemAt(i);
      res.add(item.getUri());
    }
    return res;
  }

  private static void removeFromContentResolver(@NonNull Context context, Uri cameraPicUri) {
    if (cameraPicUri == null) {
      return;
    }
    context.getContentResolver().delete(cameraPicUri, null, null);
  }

  MediaChooserController() {
    cameraPicUri = null;
  }

  private MediaChooserController(Parcel in) {
    cameraPicUri = in.readParcelable(Uri.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(cameraPicUri, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<MediaChooserController> CREATOR = new Creator<MediaChooserController>() {
    @Override
    public MediaChooserController createFromParcel(Parcel in) {
      return new MediaChooserController(in);
    }

    @Override
    public MediaChooserController[] newArray(int size) {
      return new MediaChooserController[size];
    }
  };
}
