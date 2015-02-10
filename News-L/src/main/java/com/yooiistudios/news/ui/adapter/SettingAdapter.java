package com.yooiistudios.news.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.yooiistudios.news.R;
import com.yooiistudios.news.model.Settings;
import com.yooiistudios.news.model.language.LanguageUtils;
import com.yooiistudios.news.model.panelmatrix.PanelMatrix;
import com.yooiistudios.news.model.panelmatrix.PanelMatrixUtils;
import com.yooiistudios.news.ui.fragment.SettingFragment;

import static com.yooiistudios.news.ui.fragment.SettingFragment.SettingItem;

/**
 * Created by Dongheyon Jeong on in News-Android-L from Yooii Studios Co., LTD. on 14. 11. 3.
 *
 * SettingAdapter
 *  세팅화면에 사용될 어뎁터
 */
public class SettingAdapter extends BaseAdapter {
    private Context mContext;

    public SettingAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return SettingItem.values().length;
    }

    @Override
    public Object getItem(int position) {
        return SettingItem.values()[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return SettingItemFactory.inflate(mContext, parent, position);
    }

    private static class SettingItemFactory {
        private static View inflate(final Context context, ViewGroup parent, int position) {
            View view = null;

            SettingItem item = SettingItem.values()[position];
            switch (item) {
                case MAIN_SUB_HEADER:
                    view = LayoutInflater.from(context).inflate(R.layout.setting_item_sub_header, parent, false);
                    break;

                case LANGUAGE:
                case MAIN_PANEL_MATRIX:
                    view = LayoutInflater.from(context).inflate(R.layout.setting_item_base, parent, false);
                    initBaseItem(context, item, view);
                    break;

                case KEEP_SCREEN_ON:
                    view = LayoutInflater.from(context).inflate(R.layout.setting_item_switch, parent, false);
                    initKeepScreenOnItem(context, view);
                    break;

                case MAIN_AUTO_REFRESH_SPEED:
                case MAIN_AUTO_REFRESH_INTERVAL:
                    view = LayoutInflater.from(context).inflate(R.layout.setting_item_seekbar, parent, false);
                    initSeekBarItem(context, item, view);
                    break;

                case TUTORIAL:
                    view = LayoutInflater.from(context).inflate(R.layout.setting_item_single, parent, false);
                    break;
            }

            TextView titleTextView = (TextView) view.findViewById(R.id.setting_item_title_textview);
            titleTextView.setText(item.getTitleResId());

            // TODO 나중에 폰트의 영어 높이가 너무 높은 부분에 대해서 고민하기. 마이너스 마진을 통해서 해결해야 하지 않을까 생각
//            if (item == SettingItem.MAIN_SUB_HEADER || item == SettingItem.NEWS_FEED_SUB_HEADER) {
//                titleTextView.setTypeface(TypefaceUtils.getMediumTypeface(context));
//                titleTextView.setTypeface(TypefaceUtils.getEngRegularTypeface(context));
//            } else {
//                titleTextView.setTypeface(TypefaceUtils.getRegularTypeface(context));
//                titleTextView.setTypeface(TypefaceUtils.getEngRegularTypeface(context));
//            }

//            TextView descriptionTextView =
//                    (TextView) view.findViewById(R.id.setting_item_description_textview);
//            if (descriptionTextView != null) {
//                descriptionTextView.setTypeface(TypefaceUtils.getEngRegularTypeface(context));
//            }
            return view;
        }
    }

    private static void initBaseItem(Context context, SettingItem item, View view) {
        TextView descriptionTextView =
                (TextView) view.findViewById(R.id.setting_item_description_textview);

        if (item == SettingItem.LANGUAGE) {
            descriptionTextView.setText(
                    LanguageUtils.getCurrentLanguageType(context).getLocalNotationStringId());
        } else if (item == SettingItem.MAIN_AUTO_REFRESH_INTERVAL) {
            int autoRefreshInterval = Settings.getAutoRefreshInterval(context);
            descriptionTextView.setText(
                    context.getString(R.string.setting_item_sec_description, autoRefreshInterval));
        } else if (item == SettingItem.MAIN_PANEL_MATRIX) {
            PanelMatrix currentPanelMatrix = PanelMatrixUtils.getCurrentPanelMatrix(context);
            descriptionTextView.setText(context.getString(
                    R.string.setting_main_panel_matrix_description, currentPanelMatrix.getDisplayName()));
        }
    }

    private static void initKeepScreenOnItem(Context context, View view) {
        SwitchCompat keepScreenSwitch = (SwitchCompat) view.findViewById(R.id.setting_item_switch);
        final SharedPreferences preferences = context.getSharedPreferences(
                SettingFragment.KEEP_SCREEN_ON_PREFS, Context.MODE_PRIVATE);
        keepScreenSwitch.setChecked(preferences.getBoolean(SettingFragment.KEEP_SCREEN_ON_KEY, false));
        keepScreenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(SettingFragment.KEEP_SCREEN_ON_KEY, isChecked).apply();
            }
        });
    }

    private static void initSeekBarItem(final Context context, SettingItem item, final View view) {
        if (item == SettingItem.MAIN_AUTO_REFRESH_INTERVAL) {
            initAutoRefreshIntervalItem(context, view);
        } else {
            initAutoRefreshSpeedItem(context, view);
        }
    }

    private static void initAutoRefreshIntervalItem(final Context context, View view) {
        TextView titleTextView = (TextView) view.findViewById(R.id.setting_item_title_textview);
        final TextView statusTextView = (TextView) view.findViewById(R.id.setting_item_status_textview);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.setting_item_seekbar);

        titleTextView.setText(R.string.setting_main_auto_refresh_interval);
        int oldInterval = Settings.getAutoRefreshIntervalProgress(context);
        statusTextView.setText(context.getString(R.string.setting_item_sec_description,
                Settings.getAutoRefreshInterval(context)));
        seekBar.setProgress(oldInterval);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Settings.setAutoRefreshIntervalProgress(context, progress);
                statusTextView.setText(context.getString(R.string.setting_item_sec_description,
                        Settings.getAutoRefreshInterval(context)));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private static void initAutoRefreshSpeedItem(final Context context, View view) {
        TextView titleTextView = (TextView) view.findViewById(R.id.setting_item_title_textview);
        final TextView statusTextView = (TextView) view.findViewById(R.id.setting_item_status_textview);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.setting_item_seekbar);

        titleTextView.setText(R.string.setting_main_auto_refresh_speed);
        int oldSpeedProgress = Settings.getAutoRefreshSpeedProgress(context);
        setAutoRefreshSpeedTextView(statusTextView, -1, oldSpeedProgress);
        seekBar.setProgress(oldSpeedProgress);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int oldSpeedProgress = Settings.getAutoRefreshSpeedProgress(context);
                setAutoRefreshSpeedTextView(statusTextView, oldSpeedProgress, progress);
                Settings.setAutoRefreshSpeedProgress(context, progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    // 텍스트를 한 번만 바꿔주게 예외처리
    private static void setAutoRefreshSpeedTextView(TextView textView, int oldSpeed, int newSpeed) {
        boolean isFirstLoad = false;
        if (oldSpeed == -1) {
            isFirstLoad = true;
        }
        if (newSpeed < 20) {
            if (oldSpeed >= 20 || isFirstLoad) {
                textView.setText(R.string.setting_news_feed_auto_scroll_very_slow);
            }
        } else if (newSpeed >= 20 && newSpeed < 40) {
            if (oldSpeed < 20 || oldSpeed >= 40 || isFirstLoad) {
                textView.setText(R.string.setting_news_feed_auto_scroll_slow);
            }
        } else if (newSpeed >= 40 && newSpeed < 60) {
            if (oldSpeed < 40 || oldSpeed >= 60 || isFirstLoad) {
                textView.setText(R.string.setting_news_feed_auto_scroll_normal);
            }
        } else if (newSpeed >= 60 && newSpeed < 80) {
            if (oldSpeed < 60 || oldSpeed >= 80 || isFirstLoad) {
                textView.setText(R.string.setting_news_feed_auto_scroll_fast);
            }
        } else if (newSpeed >= 80){
            if (oldSpeed < 80 || isFirstLoad) {
                textView.setText(R.string.setting_news_feed_auto_scroll_very_fast);
            }
        }
    }
}
