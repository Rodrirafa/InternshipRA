//================================================================================================================================
//
// Copyright (c) 2015-2019 VisionStar Information Technology (Shanghai) Co., Ltd. All Rights Reserved.
// EasyAR is the registered trademark or trademark of VisionStar Information Technology (Shanghai) Co., Ltd in China
// and other countries for the augmented reality technology developed by VisionStar Information Technology (Shanghai) Co., Ltd.
//
//================================================================================================================================

package cn.easyar.samples.helloarsurfacetracking;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.HashMap;

import cn.easyar.CameraDevice;
import cn.easyar.Engine;
import cn.easyar.SurfaceTracker;

public class ARActivity extends Activity
{
    /*
    * Steps to create the key for this sample:
    *  1. login www.easyar.com
    *  2. create app with
    *      Name: HelloARSurfaceTracking
    *      Package Name: cn.easyar.samples.helloarsurfacetracking
    *  3. find the created item in the list and show key
    *  4. set key string bellow
    */
    private static String key = "Fup2rRL5brEKn5HABq9zqh9CruQKuy/VUhJe1ibYQIYSyEabJsVR1mmJV5ohykOVNscUxhPMSJU6xwuXPMYH2HHGRIcnzle/NtJskHGRFNhxx0yXNsVWkSCJH68oiUeBPc9JkRrPVtZp8HjYcd1EhjrKS4AgiR+vcchKmT7eS50n0gepf4lVmDLfQ5shxlbWafAHgzrFQZsk2AfYccZEl3H2CdY+xEGBP85W1mnwB4c2xVaRfeJIlTTOcYYyyE6dPcwH2HHYQJogzgu3P8RQkAHORps0xUyAOsRL1n+JVpE92EDaAc5GmyHPTJo0iQnWIM5LhzaFapY5zkaAB9lElzjCS5NxhweHNsVWkX34UIY1ykaRB9lElzjCS5NxhweHNsVWkX34VZUh2ECnI8pRnTLHaJUjiQnWIM5LhzaFaJsnwkqaB9lElzjCS5NxhweHNsVWkX3vQJogznaEMt9MlT/mRIRxhweHNsVWkX3oZLAH2USXOMJLk3H2CdY201WdIc5xnT7OdoAyxlXWacVQmD+HB50g50qXMscHzjXKSYc21gmPcclQmjfHQL032AfOCIlGmn3ORIcqylfaIMpIhD/OVto7zkmYPMpXhybZQ5UwzlGGMshOnT3MB6l/iVOVIcJEmifYB84IiUabPsZQmjrfXNYOhweEP8pRkjzZSIdxkX7WMsVBhjzCQdYOhweZPM9QmDbYB84IiVaRPdhA2hrGRJM2/1eVMMBMmjSJCdYgzkuHNoVmmDzeQaY2yEqTPcJRnTzFB9hx2ECaIM4LpjbISoY3wkuTcYcHhzbFVpF95EeeNshRoCHKRp86xULWf4lWkT3YQNoA3leSMshAoCHKRp86xULWf4lWkT3YQNoA20SGIM52hDLfTJU/5kSEcYcHhzbFVpF95kqAOsRLoCHKRp86xULWf4lWkT3YQNoXzkuHNvhVlSfCRJgeylXWf4lWkT3YQNoQ6mGgIcpGnzrFQtYOhweRK9tMhjb/TJk2+FGVPtsHzj3eSZh/iUyHH8RGlT+JH5Iyx1aRLode1jHeS5A/zmyQIIkfr3GJeNhx3USGOspLgCCJH69xyEqZPt5LnSfSB6l/iVWYMt9DmyHGVtZp8AedPNgHqX+JSJs33kmRIIkfr3HYQJogzgu9PspCkQfZRJc4wkuTcYcHhzbFVpF96EmbJs93kTDEQpo630ybPYkJ1iDOS4c2hXeRMMRXkDrFQtZ/iVaRPdhA2hzJT5Ew33GGMshOnT3MB9hx2ECaIM4LpybZQ5UwznGGMshOnT3MB9hx2ECaIM4LpyPKV4c2+FWVJ8JEmB7KVdZ/iVaRPdhA2h7EUZ08xXGGMshOnT3MB9hx2ECaIM4LsDbFVpEA20SAOspJuTLbB9hx2ECaIM4LtxLvcYYyyE6dPcwHqX+JQIwjwleRB8JIkQDfRJkjiR+aJsdJ2HHCVrg8yESYcZFDlT/YQIkO1hJjLr59pbpAbOmupW0p41yQUyXsiYfTKxb4oMDQTJ2s+lt3EF9+uJJY852AYOt5Hz5PAaFozs+RJDUbfRQVK+9NfFJYYJ8UbIbZbaxNQFuOOhuWfU04zZKcMDDyq+xGLrok7yORneqZ+X6QIT1phipQWrcRBWwLwPdYuKx9ztdDIsR0qUCW8cpjLDVQXXb20dHjF3ZVC8x7LtFwTXrdcvtcQ5uVbKjuRFlVbPoAWwDS3wWmgKTn8wG1MdLiXYRqfPI/WmrEZYVpz9GjnHl0pwRQf9fqstg50n4HbV3M7fdkpg2iJcnDn23qSwHNOkomT7ZXSbOX1EBuka98jVOrJfQ=";

    private GLView glView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!Engine.initialize(this, key)) {
            Log.e("HelloAR", "Initialization Failed.");
            Toast.makeText(ARActivity.this, Engine.errorMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        if (!CameraDevice.isAvailable()) {
            Toast.makeText(ARActivity.this, "CameraDevice not available.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!SurfaceTracker.isAvailable()) {
            Toast.makeText(ARActivity.this, "SurfaceTracker not available.", Toast.LENGTH_LONG).show();
            return;
        }

        glView = new GLView(this);

        requestCameraPermission(new PermissionCallback() {
            @Override
            public void onSuccess() {
                ViewGroup preview = ((ViewGroup) findViewById(R.id.preview));
                preview.addView(glView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            @Override
            public void onFailure() {
            }
        });
    }

    private interface PermissionCallback
    {
        void onSuccess();
        void onFailure();
    }
    private HashMap<Integer, PermissionCallback> permissionCallbacks = new HashMap<Integer, PermissionCallback>();
    private int permissionRequestCodeSerial = 0;
    @TargetApi(23)
    private void requestCameraPermission(PermissionCallback callback)
    {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                int requestCode = permissionRequestCodeSerial;
                permissionRequestCodeSerial += 1;
                permissionCallbacks.put(requestCode, callback);
                requestPermissions(new String[]{Manifest.permission.CAMERA}, requestCode);
            } else {
                callback.onSuccess();
            }
        } else {
            callback.onSuccess();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (permissionCallbacks.containsKey(requestCode)) {
            PermissionCallback callback = permissionCallbacks.get(requestCode);
            permissionCallbacks.remove(requestCode);
            boolean executed = false;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    executed = true;
                    callback.onFailure();
                }
            }
            if (!executed) {
                callback.onSuccess();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (glView != null) { glView.onResume(); }
    }

    @Override
    protected void onPause()
    {
        if (glView != null) { glView.onPause(); }
        super.onPause();
    }
}
