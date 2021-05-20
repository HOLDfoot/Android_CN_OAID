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

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.github.gzuliyujiang.oaid.IGetter;
import com.github.gzuliyujiang.oaid.OAIDLog;

/**
 * 绑定远程的 OAID 服务
 *
 * @author 贵州山野羡民（1032694760@qq.com）
 * @since 2021/5/20 11:59
 */
class OAIDService implements ServiceConnection {
    private final Context context;
    private final IGetter getter;
    private final RemoteRunner runner;

    public static void bind(Context context, Intent intent, IGetter getter, RemoteRunner runner) {
        new OAIDService(context, intent, getter, runner);
    }

    private OAIDService(Context context, Intent intent, IGetter getter, RemoteRunner runner) {
        if (context instanceof Application) {
            this.context = context;
        } else {
            this.context = context.getApplicationContext();
        }
        this.getter = getter;
        this.runner = runner;
        try {
            boolean ret = context.bindService(intent, this, Context.BIND_AUTO_CREATE);
            if (!ret) {
                throw new RuntimeException("Service binding failed");
            }
        } catch (Throwable e) {
            getter.onOAIDGetError(e);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        OAIDLog.print("onServiceConnected: " + name);
        try {
            String oaid = runner.runRemoteInterface(service);
            if (oaid == null || oaid.length() == 0) {
                throw new RuntimeException("OAID acquire failed");
            }
            getter.onOAIDGetComplete(oaid);
        } catch (Throwable e) {
            OAIDLog.print(e);
            getter.onOAIDGetError(e);
        } finally {
            try {
                context.unbindService(this);
            } catch (Throwable e) {
                OAIDLog.print(e);
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        OAIDLog.print("onServiceDisconnected: " + name);
    }

    public interface RemoteRunner {

        @Nullable
        String runRemoteInterface(IBinder binder) throws Throwable;

    }

}
