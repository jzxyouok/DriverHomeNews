package com.leavessilent.driverhomenews.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.leavessilent.driverhomenews.R;
import com.leavessilent.driverhomenews.activities.DetailActivity;
import com.leavessilent.driverhomenews.adapters.AdNewsAdapter;
import com.leavessilent.driverhomenews.adapters.NewsAdapter;
import com.leavessilent.driverhomenews.common.Constants;
import com.leavessilent.driverhomenews.db.NewsDatabaseDB;
import com.leavessilent.driverhomenews.entity.News;
import com.leavessilent.driverhomenews.utils.JSONUtils;
import com.leavessilent.mylibrary.ImageLoader;
import com.leavessilent.mylibrary.utils.HttpUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class NewsItemFragment extends Fragment implements Handler.Callback, ViewPager.OnPageChangeListener, AdapterView.OnItemClickListener, View.OnTouchListener, PullToRefreshBase.OnRefreshListener2 {

    private NewsDatabaseDB mDatabaseDB;

    private static final int LOAD_VIEWPAGER = 22;
    private static final int LOAD_LISTVIEW = 23;
    private static final String TAG = NewsItemFragment.class.getSimpleName();
    // 我们要返回的view
    private View mView;
    // 实例化组件
    private PullToRefreshListView mRefreshListView;
    private ListView mListView;


    // 展示广告的viewpager
    private ViewPager mAdNewsViewPager;
    //使用viewstub来决定是否显示viewpager指示点
    private ViewStub mViewStub;

    // 三个指示点
    private List<RadioButton> mRadioButtonList;


    private RadioButton mFirstDot;
    private RadioButton mSecondDot;
    private RadioButton mThirdDot;
    // viewpager下面的文字
    private TextView mAdTitle;

    // viewpager数据源
    private List<News> mAdNewsList;

    // listView数据源
    private List<News> mNewsList;

    // viewpager 适配器
    private AdNewsAdapter mAdNewsAdapter;

    // viewpager 适配器数据源
    private List<Fragment> mAdData;

    // 异步处理
    private Handler mHandler = new Handler(this);

    private String mUrlString;

    // 判断是哪种类型的新闻
    private int type;


    private NewsAdapter mNewsAdapter;

    // ListView的header
    private View mHeader;

    // viewpager的新闻地址
    private String mAdUrlString;
    private int mAdType;

    public void setAdUrlStringAndType(String adUrlString, int type) {
        mAdUrlString = adUrlString;
        mAdType = type;
    }

    public void setUrlString(String urlString) {
        mUrlString = urlString;
    }


    public NewsItemFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_news_item, container, false);
        initView();
        return mView;

    }

    private void initView() {
        mAdNewsList = new ArrayList<>();
        mNewsList = new ArrayList<>();
        mAdData = new ArrayList<>();
        mDatabaseDB = NewsDatabaseDB.getInstance(getContext());

        mRefreshListView = (PullToRefreshListView) mView.findViewById(R.id.pull_to_refresh_list_view);
        mRefreshListView.setMode(PullToRefreshBase.Mode.BOTH);
        mListView = mRefreshListView.getRefreshableView();

        mNewsAdapter = new NewsAdapter(getContext(), mNewsList, R.layout.news_item);
        mListView.setAdapter(mNewsAdapter);


        mHeader = LayoutInflater.from(getContext()).inflate(R.layout.list_header_layout, mListView, false);
        mAdTitle = (TextView) mHeader.findViewById(R.id.viewpager_ad_title);


        // 初始化viewpager指示器
        // 初始化广告页viewpager
        initAdViewPager();
        mListView.addHeaderView(mHeader);
        initListView();
        initRadioButtonList();

        mRefreshListView.setOnRefreshListener(this);
        mListView.setOnItemClickListener(this);
        mAdNewsViewPager.setOnTouchListener(this);
    }

    private void initRadioButtonList() {
        mViewStub = (ViewStub) mHeader.findViewById(R.id.viewstub_dot);
        mViewStub.inflate();
        mViewStub.setVisibility(View.GONE);

        mRadioButtonList = new ArrayList<>();
        mFirstDot = (RadioButton) mHeader.findViewById(R.id.rb_dot_first);
        mSecondDot = (RadioButton) mHeader.findViewById(R.id.rb_dot_second);
        mThirdDot = (RadioButton) mHeader.findViewById(R.id.rb_dot_third);
        mAdTitle = ((TextView) mHeader.findViewById(R.id.viewpager_ad_title));

        mRadioButtonList.add(mFirstDot);
        mRadioButtonList.add(mSecondDot);
        mRadioButtonList.add(mThirdDot);
    }

    private void initAdViewPager() {
        mAdNewsViewPager = (ViewPager) mHeader.findViewById(R.id.vp_ad);
        mAdNewsAdapter = new AdNewsAdapter(getChildFragmentManager(), mAdData);
        mAdNewsViewPager.setAdapter(mAdNewsAdapter);

        // 首先从数据库获取,没有则网络请求
        List<News> newsList = mDatabaseDB.getNewsList(mAdType);
        if (newsList.size() > 0) {
            mAdNewsList.addAll(newsList);
            mHandler.sendEmptyMessage(LOAD_VIEWPAGER);
        } else {
            getPictureNewsFromInternet(State.UP);
        }


        mAdNewsViewPager.addOnPageChangeListener(this);
        mAdNewsViewPager.setCurrentItem(0);
    }

    private void getPictureNewsFromInternet(final State state) {
        HttpUtils.getStringAsync(mAdUrlString, new HttpUtils.RequestCallback() {
            @Override
            public void onFailure() {

            }

            @Override
            public void onSuccess(String result) {
                if (result != null) {
                    List<News> newsList = JSONUtils.getNewsFromJson(result, mAdType);
                    switch (state) {
                        case UP:
                            break;
                        case DOWN:
                            mDatabaseDB.deleteNewsList(mAdType);
                            mDatabaseDB.saveNewsList(newsList);
                            mAdNewsList.clear();
                            mAdNewsList.addAll(newsList);
                            break;
                    }
                    mHandler.sendEmptyMessage(LOAD_VIEWPAGER);
                }
            }

            @Override
            public void onFinish() {

            }
        });
    }

    public void initListView() {
        // 先从数据库中获取，如果没有则网络请求
        List<News> newsList = mDatabaseDB.getNewsList(type);
        if (newsList.size() > 0) {
            mNewsList.addAll(newsList);
            mNewsAdapter.notifyDataSetChanged();
        } else {
            getNewsDataFromInternet(State.UP);
        }
    }

    private void getNewsDataFromInternet(final State state) {
        getPictureNewsFromInternet(state);
        HttpUtils.getStringAsync(mUrlString, new HttpUtils.RequestCallback() {
            @Override
            public void onFailure() {

            }

            @Override
            public void onSuccess(String result) {
                if (result != null) {
                    List<News> newsFromJson = JSONUtils.getNewsFromJson(result, type);
                    switch (state) {
                        case UP:
                            mDatabaseDB.saveNewsList(newsFromJson);
                            mNewsAdapter.addData(newsFromJson);
                            break;
                        case DOWN:
                            mDatabaseDB.deleteNewsList(type);
                            mNewsAdapter.updateData(newsFromJson);
                            mDatabaseDB.saveNewsList(newsFromJson);
                            break;
                    }
                }
                mHandler.sendEmptyMessage(LOAD_LISTVIEW);
            }

            @Override
            public void onFinish() {
                mRefreshListView.onRefreshComplete();
            }
        });


    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case LOAD_VIEWPAGER:
                // 如果viewpager
                if (mAdNewsList.size() > 1)
                    mViewStub.setVisibility(View.VISIBLE);
                for (int i = 0; i < mAdNewsList.size(); i++) {
                    News news = mAdNewsList.get(i);
                    AdNewsFragment fragment = new AdNewsFragment();
                    Bundle args = new Bundle();
                    args.putParcelable("news", news);
                    fragment.setArguments(args);
                    mAdData.add(fragment);
                }
                mAdNewsAdapter.update(mAdData);
                break;
            case LOAD_LISTVIEW:
                mNewsAdapter.notifyDataSetChanged();
                break;
        }
        return false;
    }


    public void setType(int type) {
        this.type = type;
    }


    //--------------------图片新闻ViewPager的滑动监听------------------------
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mAdTitle.setText(mAdNewsList.get(position).getTitle());
        if (mAdNewsList.size() > 1)
            mRadioButtonList.get(position).setChecked(true);
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }


    // ---------------------------------listview的item监听回调----------------------------------------------------
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), DetailActivity.class);
        intent.putExtra("id", mNewsList.get(position - 2).getId());
        intent.putExtra("type", type);
        startActivity(intent);
    }

    private int flag = 1;

    //---------------------viewpager的onTouch回调-------------------------------------------
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.vp_ad:
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        flag = 0;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        flag = 1;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (flag == 0) {
                            Intent intent = new Intent(getActivity(), DetailActivity.class);
                            int currentItem = mAdNewsViewPager.getCurrentItem();
                            intent.putExtra("id", mAdNewsList.get(currentItem).getId());
                            intent.putExtra("type", mAdType);
                            startActivity(intent);
                        }

                }
                break;
        }
        return false;
    }


    private enum State {
        DOWN, UP
    }

    //---------------------PullToRefreshListView的上拉和下拉回调-------------------------------------
    @Override
    public void onPullDownToRefresh(PullToRefreshBase refreshView) {
        getNewsDataFromInternet(State.DOWN);
    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase refreshView) {
        getNewsDataFromInternet(State.UP);
    }
}
