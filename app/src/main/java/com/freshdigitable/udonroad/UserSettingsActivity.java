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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.freshdigitable.udonroad.datastore.AppSettingStore;

import java.util.List;

import javax.inject.Inject;

import dagger.Module;
import dagger.android.AndroidInjector;
import dagger.android.ContributesAndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.HasSupportFragmentInjector;
import timber.log.Timber;
import twitter4j.User;

/**
 * Created by akihit on 2017/09/20.
 */

public class UserSettingsActivity extends AppCompatActivity implements HasSupportFragmentInjector {
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getSupportFragmentManager().beginTransaction()
        .replace(android.R.id.content, new SettingsFragment())
        .commit();
  }

  @Inject
  DispatchingAndroidInjector<Fragment> androidInjector;

  @Override
  public AndroidInjector<Fragment> supportFragmentInjector() {
    return androidInjector;
  }

  public static class SettingsFragment extends PreferenceFragmentCompat
      implements OnSharedPreferenceChangeListener {
    private String loginUserKey;
    @Inject
    AppSettingStore appSettings;

    @Override
    public void onAttach(Context context) {
      AndroidSupportInjection.inject(this);
      super.onAttach(context);
      loginUserKey = getString(R.string.settings_key_loginUser);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.user_settings, rootKey);
      final ListPreference p = (ListPreference) findPreference(loginUserKey);

      final CharSequence[] defaultEntries = p.getEntries();
      final CharSequence[] defaultEntryValues = p.getEntryValues();

      appSettings.open();
      final List<? extends User> users = appSettings.getAllAuthenticatedUsers();
      final CharSequence[] userEntries = new CharSequence[users.size()];
      final CharSequence[] userEntryValues = new CharSequence[users.size()];
      for (int i = 0; i < users.size(); i++) {
        final User user = users.get(i);
        userEntries[i] = "@" + user.getScreenName();
        userEntryValues[i] = Long.toString(user.getId());
      }
      appSettings.close();

      final CharSequence[] entries = new CharSequence[defaultEntries.length + userEntries.length];
      final CharSequence[] entryValues = new CharSequence[defaultEntryValues.length + userEntryValues.length];
      System.arraycopy(defaultEntries, 0, entries, 0, defaultEntries.length);
      System.arraycopy(userEntries, 0, entries, defaultEntries.length, userEntries.length);
      System.arraycopy(defaultEntryValues, 0, entryValues, 0, defaultEntryValues.length);
      System.arraycopy(userEntryValues, 0, entryValues, defaultEntryValues.length, userEntryValues.length);

      p.setEntries(entries);
      p.setEntryValues(entryValues);
      p.setSummary(p.getEntry());

      try {
        final String packageName = getActivity().getPackageName();
        final PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
        final Preference version = findPreference(getString(R.string.settings_key_version));
        version.setSummary(packageInfo.versionName);
      } catch (PackageManager.NameNotFoundException e) {
        Timber.tag(getClass().getSimpleName()).e("onCreatePreferences: ");
      }
    }

    @Override
    public void onResume() {
      super.onResume();
      getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
      super.onPause();
      getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      if (key.equals(loginUserKey)) {
        final ListPreference preference = (ListPreference) findPreference(key);
        preference.setSummary(preference.getEntry());
      }
    }
  }

  @Module
  public interface UserSettingsActivityModule {
    @ContributesAndroidInjector
    SettingsFragment contributeSettingsFragment();
  }
}
