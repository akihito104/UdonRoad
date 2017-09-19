package com.freshdigitable.udonroad.util;

import android.support.test.InstrumentationRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by akihit on 2017/04/20.
 */

public class StorageUtil {
  public static void initStorage() {
    for (File d : listDir()) {
      for (String l : listStorage(d)) {
        final RealmConfiguration config = new RealmConfiguration.Builder()
            .directory(d)
            .name(l)
            .deleteRealmIfMigrationNeeded()
            .build();
        final int localInstanceCount = Realm.getLocalInstanceCount(config);
        final int globalInstanceCount = Realm.getGlobalInstanceCount(config);
        if (globalInstanceCount > 0 || localInstanceCount > 0) {
          final Realm realm = Realm.getInstance(config);
          realm.executeTransaction(r -> r.deleteAll());
          realm.close();
        } else {
          final boolean b = Realm.deleteRealm(config);
          assertTrue(b);
        }
      }
    }
  }

  private static String[] listStorage(File dir) {
    final String[] list = dir.list((file, s) -> s.matches("^.*\\.management$"));
    for (int i = 0; i < list.length; i++) {
      list[i] = list[i].replace(".management", "");
    }
    return list;
  }

  private static File[] listDir() {
    final File realmDirectory = new RealmConfiguration.Builder().build().getRealmDirectory();
    final File[] dirs = realmDirectory.listFiles(f -> f.isDirectory() && f.getName().startsWith("user_"));
    final File[] res = new File[dirs.length + 1];
    System.arraycopy(dirs, 0, res, 0, dirs.length);
    res[dirs.length] = realmDirectory;
    return res;
  }

  public static void checkAllRealmInstanceClosed() {  // XXX
    final ArrayList<StorageUtil.InstanceCount> realmCounts = new ArrayList<>();
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
        StorageUtil.getAllRealmInstanceCount(realmCounts));
    StorageUtil.checkAllRealmInstanceClosed(realmCounts);
  }

  private static void getAllRealmInstanceCount(List<InstanceCount> realms) {
    for (File d : listDir()) {
      for (String l : listStorage(d)) {
        final InstanceCount ic = checkRealmInstanceCount(d, l);
        realms.add(ic);
      }
    }
  }

  private static void checkAllRealmInstanceClosed(List<InstanceCount> realms) {
    final String countResult = parseCountResult(realms);
    for (InstanceCount r : realms) {
      assertThat(countResult, r.localCount, is(0));
      assertThat(countResult, r.globalCount, is(0));
    }
  }

  private static String parseCountResult(List<InstanceCount> instanceCounts) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (InstanceCount ic : instanceCounts) {
      stringBuilder.append(ic.toString()).append("\n");
    }
    return stringBuilder.toString();
  }

  private static InstanceCount checkRealmInstanceCount(File dir, String name) {  // XXX
    final RealmConfiguration conf = new RealmConfiguration.Builder()
        .directory(dir)
        .name(name)
        .build();
    return new InstanceCount(dir.getName() + ")" + name,
        Realm.getLocalInstanceCount(conf), Realm.getGlobalInstanceCount(conf));
  }

  private static class InstanceCount {
    private final String name;
    private final int localCount;
    private final int globalCount;

    InstanceCount(String name, int localCount, int globalCount) {
      this.name = name;
      this.localCount = localCount;
      this.globalCount = globalCount;
    }

    @Override
    public String toString() {
      return "name: " + name + ", local: " + localCount + ", global: " + globalCount;
    }
  }

  private StorageUtil() {}
}
