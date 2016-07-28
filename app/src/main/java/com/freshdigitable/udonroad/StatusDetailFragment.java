/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.freshdigitable.udonroad.databinding.FragmentStatusDetailBinding;
import com.freshdigitable.udonroad.datastore.StatusCache;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import twitter4j.Status;

public class StatusDetailFragment extends Fragment {
  private static final String TAG = StatusDetailFragment.class.getSimpleName();
  private FragmentStatusDetailBinding binding;
  private Status status;
  @Inject
  StatusCache statusCache;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return  inflater.inflate(R.layout.fragment_status_detail, container, false);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    binding = DataBindingUtil.bind(getView());

    long id = (long) getArguments().get("statusId");
    statusCache.open(getContext());
    this.status = statusCache.getStatus(id);
    binding.statusView.bindStatus(this.status);

    final ArrayAdapter<DetailMenu> arrayAdapter
        = new ArrayAdapter<DetailMenu>(getContext(), android.R.layout.simple_list_item_1){
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "ArrayAdapter.getView: " + getCount());
        View v = super.getView(position, convertView, parent);
        final DetailMenu item = getItem(position);
        final int strres = item.getMenuText();
        ((TextView) v).setText(strres);
        return v;
      }
    };
    arrayAdapter.add(this.status.isRetweetedByMe() ? DetailMenu.RT_DELETE : DetailMenu.RT_CREATE);
    arrayAdapter.add(this.status.isFavorited() ? DetailMenu.FAV_DELETE : DetailMenu.FAV_CREATE);
    arrayAdapter.add(DetailMenu.REPLY);
    arrayAdapter.add(DetailMenu.QUOTE);

    binding.detailMenu.setAdapter(arrayAdapter);
    binding.detailMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final DetailMenu menu = (DetailMenu) parent.getItemAtPosition(position);
        menu.action(twitterApi, StatusDetailFragment.this.status, getContext());
      }
    });
  }

  @Override
  public void onDestroyView() {
    twitterApi = null;
    statusCache.close();
    status = null;
    super.onDestroyView();
  }

  @Inject TwitterApi twitterApi;

  public static StatusDetailFragment getInstance(final long statusId) {
    Bundle args = new Bundle();
    args.putLong("statusId", statusId);
    final StatusDetailFragment statusDetailFragment = new StatusDetailFragment();
    statusDetailFragment.setArguments(args);
    return statusDetailFragment;
  }

  enum DetailMenu implements DetailMenuInterface {
    RT_CREATE(R.string.detail_rt_create,
        R.string.detail_rt_create_success, R.string.detail_rt_create_failed) {
      @Override
      public void action(TwitterApi api, Status status, Context context) {
        doAction(api.retweetStatus(status.getId()), context);
      }
    }, RT_DELETE(R.string.detail_rt_delete,
        R.string.detail_rt_delete_success, R.string.detail_rt_delete_failed) {
      @Override
      public void action(TwitterApi api, Status status, Context context) {
        doAction(api.destroyStatus(status.getId()), context);

      }
    }, FAV_CREATE(R.string.detail_fav_create,
        R.string.detail_fav_create_success, R.string.detail_fav_create_failed) {
      @Override
      public void action(TwitterApi api, Status status, Context context) {
        doAction(api.createFavorite(status.getId()), context);
      }
    }, FAV_DELETE(R.string.detail_fav_delete,
        R.string.detail_fav_delete_success, R.string.detail_fav_delete_failed) {
      @Override
      public void action(TwitterApi api, Status status, Context context) {
        doAction(api.destroyFavorite(status.getId()), context);
      }
    }, REPLY(R.string.detail_reply, 0, 0) {
      @Override
      public void action(TwitterApi api, Status status, Context context) {
        //TODO
      }
    }, QUOTE(R.string.detail_quote, 0, 0) {
      @Override
      public void action(TwitterApi api, Status status, Context context) {
        //TODO
      }
    },;

    final private int menuText;
    final private int messageOnSuccess;
    final private int messageOnFailed;

    DetailMenu(@StringRes int menu,
               @StringRes int messageOnSuccess,
               @StringRes int messageOnFailed) {
      this.menuText = menu;
      this.messageOnSuccess = messageOnSuccess;
      this.messageOnFailed = messageOnFailed;
    }

    @StringRes
    public int getMenuText() {
      return menuText;
    }

    protected void doAction(Observable<Status> observable, Context context) {
      observable.observeOn(AndroidSchedulers.mainThread())
          .subscribe(onNext(),
              onErrorToast(context),
              onCompleteToast(context));
    }

    private Action0 onCompleteToast(final Context context) {
      return new Action0() {
        @Override
        public void call() {
          Toast.makeText(context, messageOnSuccess, Toast.LENGTH_SHORT).show();
        }
      };
    }

    private Action1<Throwable> onErrorToast(final Context context) {
      return new Action1<Throwable>() {
        @Override
        public void call(Throwable o) {
          Toast.makeText(context, messageOnFailed, Toast.LENGTH_SHORT).show();
        }
      };
    }

    private Action1<Status> onNext() {
      return new Action1<Status>() {
        @Override
        public void call(Status o) {
        }
      };
    }
  }

  interface DetailMenuInterface {
    void action(TwitterApi api, Status status, Context context);
  }
}
