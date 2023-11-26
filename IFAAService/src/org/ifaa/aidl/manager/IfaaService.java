/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ifaa.aidl.manager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class IfaaService extends Service {
    private static final String LOG_TAG = IfaaService.class.getSimpleName();

    private final IBinder mIFAABinder = new IFAAManager(this, this);

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return this.mIFAABinder;
    }
}
