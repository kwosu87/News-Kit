package com.yooiistudios.newskit.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.yooiistudios.newskit.core.util.ConnectivityUtils;
import com.yooiistudios.newskit.model.BackgroundCacheUtils;
import com.yooiistudios.newskit.model.BackgroundServiceUtils;

import java.util.LinkedList;

import static com.yooiistudios.newskit.model.BackgroundServiceUtils.CacheTime;

/**
 * Created by Dongheyon Jeong on in ServiceWithTaskTest from Yooii Studios Co., LTD. on 14. 11. 6.
 *
 * BackgroundCacheJobService
 *  롤리팝 이전 버전용 백그라운드 캐시 서비스
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BackgroundCacheJobService extends JobService {

    private LinkedList<JobParameters> mJobParamsMap;

    public BackgroundCacheJobService() {
        mJobParamsMap = new LinkedList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        mJobParamsMap.add(params);

        Context context = getApplicationContext();

        if (!ConnectivityUtils.isWifiAvailable(getApplicationContext())) {
            BackgroundServiceUtils.saveMessageAndPrintLogDebug(context, "Wifi unavailable.");
            jobFinished(params, false);
            return true;
        }

        int cacheTimeKey = BackgroundServiceUtils.getCacheTimeKeyIfNecessary(getApplicationContext());
        if (CacheTime.isValidKey(cacheTimeKey)) {
            CacheTime cacheTime = CacheTime.getByUniqueKey(cacheTimeKey);
            BackgroundServiceUtils.saveMessageAndPrintLogDebug(getApplicationContext(), "Start caching.");
            BackgroundCacheUtils.getInstance().cache(getApplicationContext(), cacheTime,
                    new BackgroundCacheUtils.OnCacheDoneListener() {
                        @Override
                        public void onDone() {
                            jobFinished(params, false);
                            BackgroundServiceUtils.saveMessageAndPrintLogDebug(getApplicationContext(), "Cache done.");
                        }
                    });
        } else {
            jobFinished(params, false);
        }


        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        mJobParamsMap.remove(params);

        return true;
    }
}