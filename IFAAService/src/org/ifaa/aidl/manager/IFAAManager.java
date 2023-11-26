/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ifaa.aidl.manager;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.function.ToIntFunction;

import vendor.somc.hardware.ifaa.V1_0.IIFAAManagerService;

final class IFAAManager extends IfaaManagerService.Stub {
    private static final String LOG_TAG = IFAAManager.class.getSimpleName();

    private static final int AUTH_TYPE_NOT_SUPPORT = 0;
    private static final int AUTH_TYPE_FINGERPRINT = 1;
    private static final int AUTH_TYPE_IRIS = 1<<1;
    private static final int AUTH_TYPE_OPTICAL_FINGERPRINT = 1<<4;

    private static final int ACTIVITY_START_SUCCESS = 0;
    private static final int ACTIVITY_START_FAILED = -1;

    private static String deviceIdModel = null;
    private static ArrayList<Integer> fpListArray;

    private IIFAAManagerService mIFAAMangerService = null;

    Context mContext;

    IFAAManager(Context context, IfaaService ifaaService) {
        mContext = context;
        new WeakReference(ifaaService);
    }

    @Override
    public int getSupportBIOTypes() {
        return AUTH_TYPE_FINGERPRINT;
    }

    @Override
    public String getDeviceModel() {
        try {
            IIFAAManagerService ifaaManagerService = getIFAAManagerService();
            if (ifaaManagerService == null) {
                Log.e(LOG_TAG, "getDeviceModel: Failed to open SOMC IFAA HAL");
                return "";
            }

            mIFAAMangerService.get_device_id_model((retval, text) -> {
                if (retval == 0) {
                    deviceIdModel = "";
                } else {
                    deviceIdModel = text;
                }
            });
        } catch (Exception ex) {
            Log.e(LOG_TAG, "getDeviceModel: Exception: " + ex);
        }

        return deviceIdModel;
    }

    @Override
    public int getVersion() {
        return 4;
    }

    @Override
    public int startBIOManager(int authType) {
        Log.d(LOG_TAG, "startBIOManager: authType = " + authType);
        if (authType != 1) {
            return -1;
        }

        try {
            Intent intent = new Intent("android.settings.SECURITY_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            return ACTIVITY_START_SUCCESS;
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(LOG_TAG, "startBIOManager: " + ex);
            return -1;
        }
    }

    @Override
    public byte[] processCmd(byte[] bArr) {
        return processCmd(mContext, bArr);
    }


    @Override
    public String getExtInfo(int authType, String keyExtInfo) {
        return "";
    }

    @Override
    public void setExtInfo(int authType, String keyExtInfo, String valExtInfo) {
        // nothing
    }

    @Override
    public int getEnabled(int bioType) {
        return 1 == bioType ? 1000 : 1003;
    }

    @Override
    public int[] getIDList(int bioType) {
        if (1 == bioType) {
            try {
                IIFAAManagerService ifaaManagerService = getIFAAManagerService();
                if (ifaaManagerService == null) {
                    Log.e(LOG_TAG, "getIDList: Failed to open SOMC IFAA HAL");
                    return null;
                }
                ifaaManagerService.ifaa_get_idlist_cmd((retval, text) -> {
                    if (retval != 0) {
                        return;
                    }

                    fpListArray = text;
                });
            } catch (Exception ex) {
                Log.e(LOG_TAG, "getIDList: Exception: " + ex);
            }
        }

        ArrayList<Integer> arrayList = fpListArray;
        if (arrayList == null) {
            Log.d(LOG_TAG, "getIDList: fplistArray = null");
            return null;
        }

        int[] retFpList = arrayList.stream().mapToInt(new ToIntFunction() {
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                int intValue;
                intValue = (Integer) obj;
                return intValue;
            }
        }).toArray();
        Log.d(LOG_TAG, "getIDList: retFplist.length " + retFpList.length);
        return retFpList;
    }

    private native byte[] processCmd(Context context, byte[] bAddr);

    private IIFAAManagerService getIFAAManagerService() {
        if (mIFAAMangerService == null) {
            try {
                mIFAAMangerService = IIFAAManagerService.getService();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "getIFAAManagerService: Failed to get SOMC IFAA service");
            }
        }

        return mIFAAMangerService;
    }

    static {
        System.loadLibrary("ifaateeclientjni.sony");
    }
};
