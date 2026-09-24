/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gothwad.manager;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.Uri;
import android.os.RemoteException;
import androidx.collection.ArrayMap;
import androidx.appcompat.app.AppCompatDelegate;
import android.text.format.DateUtils;

import com.cloudrail.si.CloudRail;

import com.gothwad.manager.misc.AnalyticsManager;
import com.gothwad.manager.misc.ContentProviderClientCompat;
import com.gothwad.manager.misc.CrashReportingManager;
import com.gothwad.manager.misc.RootsCache;
import com.gothwad.manager.misc.SAFManager;
import com.gothwad.manager.misc.ThumbnailCache;
import com.gothwad.manager.misc.Utils;
import com.gothwad.manager.setting.SettingsActivity;

public class DocumentsApplication extends AppFlavour {
	private static final long PROVIDER_ANR_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;
    private static DocumentsApplication sInstance;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private RootsCache mRoots;
    private ArrayMap<Integer, Long> mSizes = new ArrayMap<Integer, Long>();
    private SAFManager mSAFManager;
    private Point mThumbnailsSize;
    private ThumbnailCache mThumbnailCache;
    private static boolean isTelevision;
    private static boolean isWatch;

    public static RootsCache getRootsCache(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mRoots;
    }

    public static ArrayMap<Integer, Long> getFolderSizes() {
        return getInstance().mSizes;
    }

    public static SAFManager getSAFManager(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mSAFManager;
    }

    public static ThumbnailCache getThumbnailCache(Context context) {
        final DocumentsApplication app = (DocumentsApplication) context.getApplicationContext();
        return app.mThumbnailCache;
    }

    public static ThumbnailCache getThumbnailsCache(Context context, Point size) {
        return getThumbnailCache(context);
    }

    public static ContentProviderClient acquireUnstableProviderOrThrow(
            ContentResolver resolver, String authority) throws RemoteException {
    	final ContentProviderClient client = ContentProviderClientCompat.acquireUnstableContentProviderClient(resolver, authority);
        if (client == null) {
            throw new RemoteException("Failed to acquire provider for " + authority);
        }
        ContentProviderClientCompat.setDetectNotResponding(client, PROVIDER_ANR_TIMEOUT);
        return client;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(!BuildConfig.DEBUG) {
            AnalyticsManager.intialize(getApplicationContext());
        }
        sInstance = this;
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final int memoryClassBytes = am != null ? am.getMemoryClass() * 1024 * 1024 : 64 * 1024 * 1024;
        if (!android.text.TextUtils.isEmpty(BuildConfig.LICENSE_KEY)) {
            try {
                CloudRail.setAppKey(BuildConfig.LICENSE_KEY);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        try {
            CrashReportingManager.enable(getApplicationContext(), true);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        mRoots = new RootsCache(this);
        mRoots.updateAsync();

        mSAFManager = new SAFManager(this);

        mThumbnailCache = new ThumbnailCache(memoryClassBytes / 4);

        final IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        packageFilter.addDataScheme("package");
        Utils.registerReceiver(this, mCacheReceiver, packageFilter, false);

        final IntentFilter localeFilter = new IntentFilter();
        localeFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
        Utils.registerReceiver(this, mCacheReceiver, localeFilter, true);

        isTelevision = Utils.isTelevision(this);
        isWatch = Utils.isWatch(this);
        try {
            if(isTelevision && Integer.valueOf(SettingsActivity.getThemeStyle()) != AppCompatDelegate.MODE_NIGHT_YES){
                SettingsActivity.setThemeStyle(AppCompatDelegate.MODE_NIGHT_YES);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static synchronized DocumentsApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        mThumbnailCache.onTrimMemory(level);
    }

    private BroadcastReceiver mCacheReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Uri data = intent.getData();
            if (data != null) {
                final String authority = data.getAuthority();
                mRoots.updateAuthorityAsync(authority);
            } else {
                mRoots.updateAsync();
            }
        }
    };

    public static boolean isSpecialDevice() {
        return isTelevision() || isWatch();
    }

    public static boolean isTelevision() {
        return isTelevision;
    }

    public static boolean isWatch() {
        return isWatch;
    }
}
