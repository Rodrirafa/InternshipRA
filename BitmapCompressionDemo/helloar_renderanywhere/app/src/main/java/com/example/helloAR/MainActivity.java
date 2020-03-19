/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.example.helloAR;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.camera2.params.SessionConfiguration;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import com.google.ar.core.examples.java.helloar.R;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.example.helpers.*;
import com.example.rendering.*;


import org.json.JSONException;
import org.json.JSONObject;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.client.*;


import io.socket.engineio.client.EngineIOException;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class MainActivity extends FragmentActivity implements GLSurfaceView.Renderer {
  private static final String TAG = MainActivity.class.getSimpleName();

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private TapHelper tapHelper;

  private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
  private final ObjectRenderer virtualObject = new ObjectRenderer();
  private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
  private final PlaneRenderer planeRenderer = new PlaneRenderer();
  private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
  private boolean writeonce = true;

  private Handler senderHandler;

  private Object lock = new Object();

  private Object fpslock = new Object();
  private int framecounter;
  private TextView textView;

  private Bitmap bmp;
  //private ByteBuffer byteBuffer;
  private InputStream inputStream;

  private String touchCoords;
  private StringTokenizer stritok;
  private java.net.Socket netSocket;

  private static final String roomName = "asdjasodjasdoj";

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private float[] anchorMatrix = new float[16];
  private static final float[] DEFAULT_COLOR = new float[] {0f, 0f, 0f, 0f};
  private final JSONObject jsonObject = new JSONObject();
  private float screen_width;
  private float screen_height;

  private boolean aBoolean = false;
  private boolean flip = true;
  private boolean isConnected = false;

  private ByteBuffer byteBuffer;
  private OutputStream outputStream;
  private ObjectOutputStream objectOutputStream;

  private byte[] bitmapdata;
  private ImageView imageView;
  private int last_size=0;

  //private JitsiMeetView jitsiMeetView;

  private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";
  private Socket socket;
  private Timer timer;

  //Constants
  // Resolutions: 240p: 240x320 480p: 480x640 720p: 720x1280
  private static final int BITMAP_WIDTH = 240;
  private static final int BITMAP_HEIGHT = 320;
  private static final boolean THROTTLE_FPS = true;
  private static final int COMPRESS_QUALITY = 50;

  // Image Formats: JPEG | WEBP | PNG


  // Anchors created from taps used for object placing with a given color.
  private static class ColoredAnchor {
    public final Anchor anchor;
    public final float[] color;

    public ColoredAnchor(Anchor a, float[] color4f) {
      this.anchor = a;
      this.color = color4f;
    }
  }

  private final ArrayList<ColoredAnchor> anchors = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    imageView = findViewById(R.id.image);
    textView = findViewById(R.id.fpscounter);
    textView.setText("0");
    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    // Set up tap listener.
    tapHelper = new TapHelper(/*context=*/ this);
    surfaceView.setOnTouchListener(tapHelper);

    try {
      socket = IO.socket("http://10.0.1.106:3000");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    /*URL serverURL;
    try {
      serverURL = new URL("https://meet.jit.si");
    } catch (MalformedURLException e) {
      e.printStackTrace();
      throw new RuntimeException("Invalid server URL!");
    }
    JitsiMeetConferenceOptions options
            = new JitsiMeetConferenceOptions.Builder()
            .setServerURL(serverURL)
            .setWelcomePageEnabled(false)
            .setAudioMuted(true)
            .setVideoMuted(false)
            .setRoom(roomName)
            .build();

    JitsiMeetView view = new JitsiMeetView(this);

    view.join(options);*/

    Button bt = new Button(this);
    Button bt2 = new Button(this);
    bt2.setBackgroundColor(Color.RED);

    //view.addView(bt2,300,300);
    //view.addView(bt,200,200);
    touchCoords = "";
    stritok = new StringTokenizer(touchCoords);


    bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View vieww) {
                Log.d("debug","touched button " + aBoolean);
                if(aBoolean)
                {
                    session.pause();
                    //setContentView(view);
                    //surfaceView.onPause();
                    //setContentView(view);
                    //view.removeView(surfaceView);
                    aBoolean = false;
                }
                else
                {
                  try {
                    session.resume();
                    //setContentView(R.layout.activity_main);
                  } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                  }
                  //surfaceView.onResume();
                    //setContentView(surfaceView);
                    //view.addView(glView,1,new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    //        LinearLayout.LayoutParams.MATCH_PARENT));
                    aBoolean = true;
                }

            }
        });

    bt2.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view2) {
          //view.removeView(surfaceView);
          RelativeLayout relativeLayout = findViewById(R.id.relativelayout);
          relativeLayout.addView(surfaceView);
          setContentView(relativeLayout);
      }
    });

    socket.on(Socket.EVENT_CONNECT, onConnect);
    socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
    socket.on(Socket.EVENT_CONNECT_ERROR,onConnectError);
    socket.on("touch",onTouch);
    //socket.on("lol", onHu);
    //socket.disconnect();
    //socket.connect();

    //socket.open();

    // Set up renderer.
    surfaceView.setPreserveEGLContextOnPause(true);
    surfaceView.setEGLContextClientVersion(2);
    surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
    surfaceView.setRenderer(this);
    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    surfaceView.setWillNotDraw(false);
    Task task = new Task();
    task.execute(new Object());

    //RelativeLayout linearLayout = findViewById(R.id.relativelayout);
    //linearLayout.removeView(surfaceView);

    //view.addView(surfaceView,0,new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
    //    LinearLayout.LayoutParams.MATCH_PARENT));

    //setContentView(view);
    HandlerThread thread = new HandlerThread("ArSendThread");
    thread.start();
    senderHandler = new Handler(thread.getLooper());

    installRequested = false;
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // Create the session.
        //session = new Session(/* context= */ this);


        //session = new Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA));

        session = new Session(this);

        //Config config = new Config(session);
        //config.setUpdateMode(Config.UpdateMode.BLOCKING);

        //session.configure(config);

      } catch (UnavailableArcoreNotInstalledException
          | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
        exception = e;
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }


    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
     try {
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }

    surfaceView.onResume();
    displayRotationHelper.onResume();

    timer = new Timer();
    timer.schedule(new FpsTimerTask(),0,1000);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
      timer.cancel();

    }
  }
  private class Task extends AsyncTask {
    @Override
    protected Object doInBackground(Object[] objects) {

      try {
        netSocket = new java.net.Socket("10.0.1.102", 5555);
        if(netSocket.isConnected()){
          Log.d("debug","Connected to: " + netSocket.getInetAddress().toString());
          isConnected = true;
        }
        //outputStream = netSocket.getOutputStream();
        //objectOutputStream = netSocket.getOutputStream();
        objectOutputStream = new ObjectOutputStream(netSocket.getOutputStream());
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  private Emitter.Listener onConnect = new Emitter.Listener() {
    @Override
    public void call(Object... args) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(getApplicationContext(),
                  "Connected to socketIO", Toast.LENGTH_LONG).show();
          socket.emit("add user", "jojo");
        }
      });


    }
  };

  private Emitter.Listener onDisconnect = new Emitter.Listener() {
    @Override
    public void call(Object... args) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(getApplicationContext(),
                  "Disconnected to socketIO", Toast.LENGTH_LONG).show();
        }
      });

    }
  };

  private String str;

  private Emitter.Listener onConnectError = new Emitter.Listener() {
    @Override
    public void call(Object... args) {
      /*if (args[0] instanceof EngineIOException){
        EngineIOException e = (EngineIOException) args[0];
        e.printStackTrace();
        return;
      }*/
      //JSONObject obj = (JSONObject) args[0];
      /*try {
        str = obj.getString("message");
      } catch (JSONException e) {
        e.printStackTrace();
      }*/
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(getApplicationContext(),
                  "Error connecting to socket: " +  str, Toast.LENGTH_LONG).show();
        }
      });

    }
  };

  private Emitter.Listener onTouch = new Emitter.Listener() {
    @Override
    public void call(Object... args) {
      String str1 = "", str2 = "";
      float ratio_x = 0,ratio_y = 0;
      JSONObject obj = (JSONObject) args[0];
      try {
       // ratio_x = Float.parseFloat(obj.getString("ratio_x"));
       // ratio_y = Float.parseFloat(obj.getString("ratio_y"));
        touchCoords = obj.getString("ratios");
      } catch (JSONException e) {
        e.printStackTrace();
      }
      stritok = new StringTokenizer(touchCoords);
      Log.d("debug","touch " + touchCoords);
      if(stritok.countTokens() == 2){
        str1 = stritok.nextToken();
        str2 = stritok.nextToken();
      }
      ratio_x = Float.parseFloat(str1);
      ratio_y = Float.parseFloat(str2);
      Log.d("debug","touch " + ratio_x + " : " + ratio_y);

      ratio_x *= screen_width;
      ratio_y *= screen_height;
      //tapHelper.onTouch(surfaceView,MotionEvent.obtain(0,0,
      //        MotionEvent.ACTION_POINTER_DOWN,ratio_x,ratio_y,0));
      tapHelper.addTap(MotionEvent.obtain(1,1,
              MotionEvent.ACTION_POINTER_DOWN,ratio_x,ratio_y,0));
    }
  };

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
          .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onPointerCaptureChanged(boolean hasCapture) {

  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
    // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
    try {
      // Create the texture and pass it to ARCore session to be filled during update().
      backgroundRenderer.createOnGlThread(/*context=*/ this);
      planeRenderer.createOnGlThread(/*context=*/ this, "models/trigrid.png");
      pointCloudRenderer.createOnGlThread(/*context=*/ this);

      virtualObject.createOnGlThread(/*context=*/ this, "models/model.obj", "models/andy.png");
      virtualObject.setMaterialProperties(0.0f, 1.0f, 0.5f, 3.0f);

      virtualObjectShadow.createOnGlThread(
          /*context=*/ this, "models/andy_shadow.obj", "models/andy_shadow.png");
      //virtualObject.setBlendMode(BlendMode.Grid);
      virtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow);
      virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

    } catch (IOException e) {
      Log.e(TAG, "Failed to read an asset file", e);
    }
    //timer = new Timer();
    //timer.schedule(new fpsTimerTask(),0,1000);
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    GLES20.glViewport(0, 0, width, height);
    screen_height = height;
    screen_width = width;
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    // Clear screen to notify driver it should not load any pixels from previous frame.

    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    //Log.d("debug","Draw Frame");


    if (session == null) {
      return;
    }
    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(session);

    try {
      session.setCameraTextureName(backgroundRenderer.getTextureId());

      Frame frame = session.update();

      Camera camera = frame.getCamera();

      // Handle one tap per frame.
      //handleTap(frame, camera);
      // If frame is ready, render camera preview image to the GL surface.
      backgroundRenderer.draw(frame);
      // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
      trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());
      // If not tracking, don't draw 3D objects, show tracking failure reason instead.
      if (camera.getTrackingState() == TrackingState.PAUSED) {
        messageSnackbarHelper.showMessage(
            this, TrackingStateHelper.getTrackingFailureReasonString(camera));
        return;
      }

      // Get projection matrix.
      float[] projmtx = new float[16];
      camera.getProjectionMatrix(projmtx, 0, 0.01f, 100.0f);

      // Get camera matrix and draw.
      float[] viewmtx = new float[16];
      camera.getViewMatrix(viewmtx,0);

      handleTap(frame, camera,projmtx,viewmtx);

      // Compute lighting from average intensity of the image.
      // The first three components are color scaling factors.
      // The last one is the average pixel intensity in gamma space.
      final float[] colorCorrectionRgba = new float[4];
      frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

      // Visualize tracked points.
      // Use try-with-resources to automatically release the point cloud.
      try (PointCloud pointCloud = frame.acquirePointCloud()) {
        //Log.d("debug - pointCloud", " " + pointCloud.getPoints().remaining());
        //writePointCloud(pointCloud);
        pointCloudRenderer.update(pointCloud);
        pointCloudRenderer.draw(viewmtx, projmtx);
      }

      // No tracking error at this point. If we detected any plane, then hide the
      // message UI, otherwise show searchingPlane message.
      if (hasTrackingPlane()) {
        messageSnackbarHelper.hide(this);
      } else {
        messageSnackbarHelper.showMessage(this, SEARCHING_PLANE_MESSAGE);
      }

      // Visualize planes.
      planeRenderer.drawPlanes(
          session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

      // Visualize anchors created by touch.
      float scaleFactor = 0.4f;
      for (ColoredAnchor coloredAnchor : anchors) {
        if (coloredAnchor.anchor.getTrackingState() != TrackingState.TRACKING) {
          continue;
        }
        // Get the current pose of an Anchor in world space. The Anchor pose is updated
        // during calls to session.update() as ARCore refines its estimate of the world.
        coloredAnchor.anchor.getPose().toMatrix(anchorMatrix,0);

        // Update and draw the model and its shadow.
        virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);

        //virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);
        virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
        //virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
        //if(writeonce) {
          //writeonce = false;


          /*for(int i=0;i<colorCorrectionRgba.length;i++){
            Log.d("debug"," " + colorCorrectionRgba[i]);
          }
          for(int i=0;i<coloredAnchor.color.length;i++){
            Log.d("debug"," " + coloredAnchor.color[i]);
          }*/
        //}
      }
      //if(!anchors.isEmpty())
        //sendCoords(anchorMatrix, projmtx, viewmtx);  // Send coordinates to SocketIO
    //if(writeonce) {
    //  writeonce = false;



    //}
    } catch (Throwable t) {
      // Avoid crashing the application due to unhandled exceptions.
      Log.e(TAG, "Exception on the OpenGL thread", t);
    }
    //if(writeonce) {
     // ByteArrayOutputStream bos = new ByteArrayOutputStream();
      //Bitmap bm = surfaceView.getDrawingCache();
      //bm.compress(Bitmap.CompressFormat.JPEG,100,bos);
      //byte[] bitmapdata = bos.toByteArray();
      //ByteArrayInputStream fis = new ByteArrayInputStream(bitmapdata);
    synchronized (fpslock){
          framecounter++;
    }
    if(!THROTTLE_FPS)
    {
      if(isConnected)
      {
        WriteTask writeTask = new WriteTask();
        writeTask.execute(new Object());
      }
      return;
    }
    if(!flip)
    {
      flip = true;
    }
    else {
      if(isConnected) {
        WriteTask writeTask = new WriteTask();
        writeTask.execute(new Object());
      }
      flip = false;
    }

  }
  private class FpsTimerTask extends TimerTask{

    @Override
    public void run() {
      synchronized (fpslock){
        //Log.d("debug", "fps: " + framecounter);
        textView.setText("" + framecounter);
        framecounter = 0;
      }
    }
  }
  private void writePointCloud(PointCloud cloud) throws JSONException {
    jsonObject.put("numpoints",cloud.getPoints().remaining());
    String keyname = "";
    String key = "";
    int step = 4;
    FloatBuffer floatBuffer = cloud.getPoints();
    //float f = floatBuffer.get(0);
    int i = 0;
    while(floatBuffer.hasRemaining()){
      //Log.d("debug - get 0 ", i + ": " + floatBuffer.get());
      i++;
    }

    //Log.d("debug - get 0 ", " " + f);

    /*if(floatBuffer.hasArray()){
      Log.d("debug - cloud length", " " + floatBuffer.array().length);
    }
    else
      Log.d("debug - else error", "no array");*/



    /*for(int i = 0;i < cloud.getPoints().remaining();i++){
      keyname = "point" + i;
      jsonObject.put(keyname,"a");
    }*/
  }

  private void writeSurface(){
    //Log.d("debug","writeSurface");
    Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH,BITMAP_HEIGHT,Bitmap.Config.ARGB_8888);

    PixelCopy.request(surfaceView,bitmap, new PixelCopy.OnPixelCopyFinishedListener() {
      @Override
      public void onPixelCopyFinished(int i) {
        if(i == PixelCopy.SUCCESS){
          sendARView(bitmap);
        }
        else
          Log.d("debug","not successful");
      }
    },senderHandler);
  }

  private void sendARView(Bitmap bitmap){
    if(bitmap == null) return;
    //Log.d("debug","#1");

    int size =  bitmap.getRowBytes() * bitmap.getHeight();

    //Log.d("debug","bytes: " + size);
    //if(size != last_size) {

    //synchronized (lock) {
    //  byteBuffer = ByteBuffer.allocate(size);
    //  bitmap.copyPixelsToBuffer(byteBuffer);
    //}


    //  last_size = size;
    //}

    // !! works !! - really cpu intensive

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    boolean works = bitmap.compress(Bitmap.CompressFormat.JPEG,COMPRESS_QUALITY, stream);

    //Log.d("debug","works: " + works);
    //byte[] tempbyte = new byte[stream.size()];
    //stream.write(tempbyte,0, stream.size());
    //byte[] byteArray = stream.toByteArray();
    //Log.d("debug","byteArraysize: " + stream.toByteArray().length );


    WriteTask writeTask = new WriteTask();
    writeTask.execute(stream);

    //bitmapdata = byteBuffer.array();
    //Log.d("debug","" + byteBuffer.array().length);
    //byteBuffer.clear();
    //JSONObject obj = new JSONObject();
    /*try {
      obj.put("bitmap",byteBuffer.array());
    } catch (JSONException e) {
      e.printStackTrace();
    }*/
    /*new Thread(new Runnable() {
      @Override
      public void run() {
        socket.emit("bitmap",obj);
      }
    }).run();*/


    //byteBuffer.clear();
    //bitmapdata = null;
    //byteBuffer = null;

    //inputStream = new ByteArrayInputStream(bitmapdata);
    //ByteBuffer buff = ByteBuffer.wrap(bitmapdata);

    //bmp = Bitmap.createBitmap(480, 640,Bitmap.Config.ARGB_8888);
    //bmp.copyPixelsFromBuffer(buff);

    //BitmapFactory.Options options = new BitmapFactory.Options();
    //options.inMutable = true;
    //Bitmap bmp = BitmapFactory.decodeByteArray(bitmapdata,0,bitmapdata.length,options);
    //bmp = BitmapFactory.decodeStream(inputStream);
    /*runOnUiThread(new Runnable() {
      @Override
      public void run() {
        imageView.setImageBitmap(bmp);
        //imageView.setImageBitmap(bmp);
      }
    });*/

  }

  private class WriteTask extends AsyncTask{
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected Object doInBackground(Object[] objects) {
        //Log.d("debug","write task");
        //Bitmap bitmap = (Bitmap) objects[0];
        Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH,BITMAP_HEIGHT,Bitmap.Config.ARGB_8888);

        PixelCopy.request(surfaceView,bitmap, new PixelCopy.OnPixelCopyFinishedListener() {
          @Override
          public void onPixelCopyFinished(int i) {
            if(i == PixelCopy.SUCCESS){
              try{
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                boolean works = bitmap.compress(Bitmap.CompressFormat.JPEG,COMPRESS_QUALITY, stream);

                synchronized (lock) {
                  Log.d("debug", "bytes to write: " + stream.toByteArray().length);
                  //byte b = (byte) baOutput.toByteArray().length;
                  //Log.d("debug",);
                  //Log.d("debug","bytes to write: " + (byte)baOutput.toByteArray().length);
                  if (netSocket.isConnected()) {
                    objectOutputStream.writeInt(stream.toByteArray().length);
                    objectOutputStream.write(stream.toByteArray(), 0, stream.toByteArray().length);
                    objectOutputStream.flush();
                  }
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
                //bitmap.compress(Bitmap.CompressFormat.JPEG,20,outputStream);
            }
            else
              Log.d("debug","not successful");
          }
        },senderHandler);

      return null;
    }
  }

  private void sendCoords(float[] anchorMatrix,float[] projmtx, float[] viewmtx){
    String string = "";
    string += makeString(anchorMatrix) + makeString(projmtx) + makeString(viewmtx) + "\n\n";
    //Log.d("debug",string);
    socket.emit("coords",string);
  }

  private String makeString(float[] matrix){
    String str = "";
    for(int i = 0;i<matrix.length;i++){
      str += matrix[i] + " ";
    }
    //Log.d("debug",str);
    return str;
  }
  // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
  private void handleTap(Frame frame, Camera camera,float[] viewmtx,float[] projmtx) {
    MotionEvent tap = tapHelper.poll();
    if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
      Log.d("debug","Tracking");
      //List<HitResult> hr = frame.hitTest(tap);

      List<HitResult> hits = frame.hitTest(tap);
      //Log.d("debug"," " + hits.isEmpty());
      if(!hits.isEmpty()) {
        for (HitResult hit : hits) {
          //Log.d("debug","Hit result");
          // Check if any plane was hit, and if it was hit inside the plane polygon
          Trackable trackable = hit.getTrackable();
          // Creates an anchor if a plane or an oriented point was hit.
          Log.d("debug", " " + (trackable instanceof Plane));
          Log.d("debug", " " + (trackable instanceof Point));
          /*if ((trackable instanceof Plane
                   && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                   && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
               || (trackable instanceof Point
                   && ((Point) trackable).getOrientationMode()
                       == OrientationMode.ESTIMATED_SURFACE_NORMAL)) {*/
          // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
          // Cap the number of objects created. This avoids overloading both the
          // rendering system and ARCore.
          if (anchors.size() >= 1) {
            anchors.get(0).anchor.detach();
            anchors.remove(0);
          }
          // Assign a color to the object for rendering based on the trackable type
          // this anchor attached to. For AR_TRACKABLE_POINT, it's blue color, and
          // for AR_TRACKABLE_PLANE, it's green color.
          float[] objColor;
          if (trackable instanceof Point) {
            objColor = new float[]{66.0f, 133.0f, 244.0f, 255.0f};
          } else if (trackable instanceof Plane) {
            objColor = new float[]{139.0f, 195.0f, 74.0f, 255.0f};
          } else {
            objColor = DEFAULT_COLOR;
          }
          // Adding an Anchor tells ARCore that it should track this position in
          // space. This anchor is created on the Plane to place the 3D model
          // in the correct position relative both to the world and to the plane.
          Anchor anc = hit.createAnchor();

          anchors.add(new ColoredAnchor(hit.createAnchor(), objColor));
          break;
          //}
        }
      }
      else
      {
        //float [] objColor = new float[]{128f, 0f, 0f, 255.0f};
        /*if (anchors.size() >= 1) {
          anchors.get(0).anchor.detach();
          anchors.remove(0);
        }*/

        //Anchor anchor = coordsFromTouch();
        //anchors.add(new ColoredAnchor(anchor,objColor));

      }
      /*if(hits.isEmpty()) {
        Vector3f vec = coordsFromTouch(tap, viewmtx, projmtx);

        //List<HitResult> hits = frame.hitTest(new float[]{0f,0f,0f},0,new float[]{vec.x,vec.y,vec.z},0);
        //Log.d("debug"," " + hits.isEmpty());
        if (anchors.size() >= 1) {
          anchors.get(0).anchor.detach();
          anchors.remove(0);
        }

        Anchor anchor = session.createAnchor(camera.getPose().compose(Pose.makeTranslation( vec.x,  vec.y, vec.z).extractTranslation()));
        //Anchor anchor = session.createAnchor(Pose.makeTranslation(vec.x,vec.y,vec.z).extractTranslation());
        anchors.add(new ColoredAnchor(anchor, new float[]{128f, 0f, 0f, 255.0f}));
      }*/
    }
  }

  private Vector3f coordsFromTouch(MotionEvent tap,float[] viewmtx, float[] projmtx){
    float x,y;
    x = tap.getX();
    y = tap.getY();

    Vector3f coords = GetWorldCoords(new Vector2f(x,y),screen_width,screen_height,projmtx,viewmtx);

    Log.d("debug","x: " + coords.x + " y: " + coords.y + " z: " + coords.z);
    return coords;
  }

  public static Vector3f GetWorldCoords(Vector2f touchPoint, float screenWidth, float screenHeight, float[] projectionMatrix, float[] viewMatrix) {
    Ray touchRay = projectRay(touchPoint, screenWidth, screenHeight, projectionMatrix, viewMatrix);
    touchRay.direction.scale(1.0f); // STATIC !!!!!!!
    touchRay.origin.add(touchRay.direction);
    return touchRay.origin;
  }

    private static Ray projectRay(Vector2f touchPoint, float screenWidth, float screenHeight, float[] projectionMatrix, float[] viewMatrix) {
        float[] viewProjMtx = new float[16];
        Matrix.multiplyMM(viewProjMtx, 0, projectionMatrix, 0, viewMatrix, 0);
        return screenPointToRay(touchPoint, new Vector2f(screenWidth, screenHeight), viewProjMtx);
    }

    private static Ray screenPointToRay(Vector2f point, Vector2f viewportSize, float[] viewProjMtx) {
        point.y = viewportSize.y - point.y;
        float x = point.x * 2.0F / viewportSize.x - 1.0F;
        float y = point.y * 2.0F / viewportSize.y - 1.0F;
        float[] farScreenPoint = new float[]{x, y, 1.0F, 1.0F};
        float[] nearScreenPoint = new float[]{x, y, -1.0F, 1.0F};
        float[] nearPlanePoint = new float[4];
        float[] farPlanePoint = new float[4];
        float[] invertedProjectionMatrix = new float[16];
        Matrix.setIdentityM(invertedProjectionMatrix, 0);
        Matrix.invertM(invertedProjectionMatrix, 0, viewProjMtx, 0);
        Matrix.multiplyMV(nearPlanePoint, 0, invertedProjectionMatrix, 0, nearScreenPoint, 0);
        Matrix.multiplyMV(farPlanePoint, 0, invertedProjectionMatrix, 0, farScreenPoint, 0);
        Vector3f direction = new Vector3f(farPlanePoint[0] / farPlanePoint[3], farPlanePoint[1] / farPlanePoint[3], farPlanePoint[2] / farPlanePoint[3]);
        Vector3f origin = new Vector3f(new Vector3f(nearPlanePoint[0] / nearPlanePoint[3], nearPlanePoint[1] / nearPlanePoint[3], nearPlanePoint[2] / nearPlanePoint[3]));
        direction.sub(origin);
        direction.normalize();
        return new Ray(origin, direction);
    }

    /** Checks if we detected at least one plane. */
  private boolean hasTrackingPlane() {
    for (Plane plane : session.getAllTrackables(Plane.class)) {
      if (plane.getTrackingState() == TrackingState.TRACKING) {
        return true;
      }
    }
    return false;
  }
}
