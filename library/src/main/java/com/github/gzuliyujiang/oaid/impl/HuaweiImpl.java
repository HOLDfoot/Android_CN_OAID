/*
 * Copyright (c) 2016-present 贵州纳雍穿青人李裕江<1032694760@qq.com>
 *
 * The software is licensed under the Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *     http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package com.github.gzuliyujiang.oaid.impl;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.github.gzuliyujiang.oaid.IGetter;
import com.github.gzuliyujiang.oaid.IOAID;
import com.github.gzuliyujiang.oaid.OAIDLog;

import repeackage.com.uodis.opendevice.aidl.OpenDeviceIdentifierService;

/**
 * 参阅华为官方 HUAWEI Ads SDK。
 * <prev>
 * implementation `com.huawei.hms:ads-identifier:3.4.39.302`
 * AdvertisingIdClient.getAdvertisingIdInfo(context).getId()
 * </pre> *
 *
 * @author 大定府羡民（1032694760@qq.com）
 * @since 2020/5/30
 */
class HuaweiImpl implements IOAID {
    private final Context context;
    private String packageName;

    public HuaweiImpl(Context context) {
        this.context = context;
    }

    @Override
    public boolean supported() {
        try {
            PackageManager pm = context.getPackageManager();
            packageName = "com.huawei.hwid";
            if (pm.getPackageInfo(packageName, 0) != null) {
                return true;
            }
            packageName = "com.huawei.hwid.tv";
            if (pm.getPackageInfo(packageName, 0) != null) {
                return true;
            }
            packageName = "com.huawei.hms";
            return pm.getPackageInfo(packageName, 0) != null;
        } catch (Throwable e) {
            OAIDLog.print(e);
            return false;
        }
    }

    @Override
    public void doGet(@NonNull final IGetter getter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                String oaid = Settings.Global.getString(context.getContentResolver(), "pps_oaid");
                if (!TextUtils.isEmpty(oaid)) {
                    getter.onOAIDGetComplete(oaid);
                    return;
                }
            } catch (Throwable e) {
                OAIDLog.print(e);
            }
        }
        if (TextUtils.isEmpty(packageName) && !supported()) {
            getter.onOAIDGetError(new RuntimeException("Huawei Advertising ID not available"));
            return;
        }
        Intent intent = new Intent("com.uodis.opendevice.OPENIDS_SERVICE");
        intent.setPackage(packageName);
        OAIDService.bind(context, intent, getter, new OAIDService.RemoteRunner() {
            @Override
            public String runRemoteInterface(IBinder service) throws Throwable {
                OpenDeviceIdentifierService anInterface = OpenDeviceIdentifierService.Stub.asInterface(service);
                if (anInterface.isOaidTrackLimited()) {
                    // 实测在系统设置中关闭了广告标识符，将获取到固定的一大堆0
                    throw new RuntimeException("User has disabled advertising identifier");
                }
                return anInterface.getOaid();
            }
        });
    }

}
