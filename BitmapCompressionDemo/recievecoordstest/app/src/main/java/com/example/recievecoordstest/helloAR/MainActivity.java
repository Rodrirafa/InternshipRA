
package com.example.recievecoordstest.helloAR;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.recievecoordstest.R;
import com.example.recievecoordstest.rendering.CustomPointCloud;
import com.example.recievecoordstest.rendering.ObjectRenderer;
import com.example.recievecoordstest.rendering.PointCloudRenderer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.javagl.obj.Obj;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class MainActivity extends Activity implements GLSurfaceView.Renderer, View.OnTouchListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private boolean installRequested;

    //private Session session;
    //private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper displayRotationHelper;
    //private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
    //private TapHelper tapHelper;

    //private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private ServerSocket serverSocket;
    private InputStream socketInputStream;
    private ObjectInputStream objectInputStream;
    private Socket socket;
    private java.net.Socket nSocket;
    private Object lock = new Object();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
    //private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    private ImageView imageView;
    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private float[] anchorMatrix = new float[16];
    //private static final float[] DEFAULT_COLOR = new float[] {0f, 0f, 0f, 0f};

    private float[] projmtx = new float[16];
    private float[] viewmtx = new float[16];
    private float[] colorCorrectionRgba = new float[]{1.0646f, 1.0f, 0.9484f, 0.4143f};
    private float[] color = new float[]{139.0f, 195.0f, 74.0f, 255.0f};
    private float screen_width, screen_height;
    private byte[] bytes = new byte[15_000];

    private Timer timer;
    private Object fpslock = new Object();
    private int framecounter;
    private TextView textView;
    private SurfaceHolder holder;

    private SurfaceView surface;
    // Resolutions: 240p: 240x320 480p: 480x640 720p: 720x1280
    private final static int BITMAP_WIDTH = 240;
    private final static int BITMAP_HEIGHT = 320;
    private int height,width;
    private ByteArrayInputStream byteArrayInputStream;
    //private CustomPointCloud customPointCloud;
    //private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //surfaceView = findViewById(R.id.surfaceview);
        imageView = findViewById(R.id.image);
        textView = findViewById(R.id.fpscounter);
        surface = findViewById(R.id.surface);
        //surfaceView.setOnTouchListener(this);
        holder = surface.getHolder();
        //copied code
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;
        //

        //surface.setClipBounds(new Rect(0,0,width,height));
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        ChatApplication app = (ChatApplication) getApplication();
        socket = app.getSocket();

        Task task = new Task();
        task.execute(new Object());

        //float[] array = new float[]{0.11440471f,-0.16945852f,-0.08182826f,0.22479634f,0.15350401f,-0.12958063f,-0.12667859f,0.18814395f};
        //FloatBuffer floatBuffer = FloatBuffer.wrap(array);
        //customPointCloud = new CustomPointCloud(floatBuffer,0);
        //int i = 0;

        //Log.d("debug"," " + floatBuffer.remaining());
        socket.on(Socket.EVENT_CONNECT, onConnect);
        socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectTimeout);
        socket.on("coords", onCoords);
        //socket.on("lol", onHu);
        //socket.disconnect();
        //socket.connect();
        //socket.open();
        //Log.d("debug"," " + socket.connected());
        //socket.on("login", onLogin);
        socket.emit("add user", "reciever");
        // Set up tap listener.
        //tapHelper = new TapHelper(/*context=*/ this);
        //surfaceView.setOnTouchListener(tapHelper);
        // Set up renderer.
        //surfaceView.setPreserveEGLContextOnPause(true);
        //surfaceView.setEGLContextClientVersion(2);
        //surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        //surfaceView.setRenderer(this);
        //surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        //surfaceView.setWillNotDraw(false);

        installRequested = false;
        /*
        String str = "0.99241894 -1.0486935E-17 0.122900814 0.0 4.135903E-25 1.0 8.532844E-17 0.0 -0.122900814 -8.4681556E-17 " +
                "0.99241894 0.0 0.0831813 -0.1836766 -0.14182673 1.0 " +
                "3.0322385 0.0 0.0 0.0 0.0 1.4490343 0.0 0.0 -6.764026E-4 0.0024706982 " +
                "-1.002002 -1.0 0.0 0.0 -0.2002002 0.0 " +
                "0.9929271 -0.0026364997 -0.118696764 0.0 0.09704679 0.59395015 0.798627 0.0 0.06839438 -0.8044975 " +
                "0.59000504 0.0 -0.069218375 0.0015187036 -0.08443551 1.0";
        anchorMatrix = new float[]{0.99241894f,-1.0486935E-17f,0.122900814f,0.0f,4.135903E-25f,1.0f,8.532844E-17f,0.0f,-0.122900814f,-8.4681556E-17f,0.99241894f,0.0f,0.0831813f,-0.1836766f,-0.14182673f,1.0f};
        projmtx = new float[]{3.0322385f,0.0f,0.0f,0.0f,0.0f,1.4490343f,0.0f,0.0f,-6.764026E-4f,0.0024706982f,-1.002002f,-1.0f,0.0f,0.0f,-0.2002002f,0.0f};
        viewmtx = new float[]{0.9929271f,-0.0026364997f,-0.118696764f,0.0f,0.09704679f,0.59395015f,0.798627f,0.0f,0.06839438f,-0.8044975f,0.59000504f,0.0f,-0.069218375f,0.0015187036f,-0.08443551f,1.0f,};
        */
    }

    private class Task extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                serverSocket = new ServerSocket(5555);
                nSocket = serverSocket.accept();
                Log.d("debug", "Connected to: " + nSocket.getInetAddress().toString());
                //socketInputStream = nSocket.getInputStream();
                objectInputStream = new ObjectInputStream(nSocket.getInputStream());
                launchReadThread();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void launchReadThread() {
        ReadTask readTask = new ReadTask();
        readTask.execute(new Object());
    }

    private Bitmap bitmap;

    private class FpsTimerTask extends TimerTask {

        @Override
        public void run() {
            synchronized (fpslock){
                Log.d("debug", "fps: " + framecounter);
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(" " + framecounter);

                    }
                });*/

                framecounter = 0;
            }
        }
    }

    private class ReadTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            do {
                try {

                    synchronized (lock) {
                        int n_bytes;
                        int offset = 0;

                        int bread = objectInputStream.readInt();
                        //bytes = new byte[bread];
                        Log.d("debug", "bytes to read: " + bread);

                        while ((n_bytes = objectInputStream.read(bytes, offset, bread-offset)) > 0)
                        {
                            offset += n_bytes;
                            //Log.d("debug","n_bytes: " + n_bytes);
                            if(offset == bread)
                                break;
                        }

                        /*while (socketInputStream.read(bytes, offset, 1) > 0) {
                            offset++;
                            if (offset == 307200)
                                break;
                        }*/
                        //Log.d("debug", "off: " + offset);
                        //nSocket.close();
                        bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
                        //ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                        //byteBuffer.rewind();
                        //bitmap.copyPixelsFromBuffer(byteBuffer);
                        byteArrayInputStream = new ByteArrayInputStream(bytes);
                        bitmap = BitmapFactory.decodeStream(byteArrayInputStream);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //imageView.setImageResource(android.R.color.transparent);
                            /*synchronized (fpslock)
                            {
                                framecounter++;
                            }*/
                            //Drawable dr = imageView.getDrawable();
                            //imageView.
                            //dr.draw(new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888,true)));

                            //try

                            imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, width, height,false));

                            //imageView.setImageBitmap(bitmap);
                            //surface.setClipBounds(new Rect(0,0,width,height));
                            /*Canvas canvas = holder.lockCanvas();
                            if (canvas != null) {
                                //surface.draw(new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888,true)));

                                canvas.drawBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeStream(byteArrayInputStream), width, height,true),0,0,null);
                                holder.unlockCanvasAndPost(canvas);
                            }*/
                            //surface.draw(new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888,true)));

                        }
                    });
                    if (!nSocket.isConnected())
                        break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (true);
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.close();
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getApplicationContext(),
                            "Connected to socketIO", Toast.LENGTH_SHORT).show();
                }
            });

        }
    };

    private Emitter.Listener onLogin = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            int numUsers;
            try {
                numUsers = data.getInt("numUsers");
            } catch (JSONException e) {
                return;
            }

        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getApplicationContext(),
                            "Disconnected to socketIO", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    private JSONObject obj;
    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            //obj = (JSONObject) args[0];
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getApplicationContext(),
                            "Connection error", Toast.LENGTH_SHORT).show();
                   /* String str = "";
                    try {
                        str = obj.getString("msg");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.d("debug",str);*/

                }
            });
        }
    };
    private Emitter.Listener onConnectTimeout = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getApplicationContext(),
                            "Connection Timed out", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private Activity getActivity() {
        return this;
    }

    private Emitter.Listener onCoords = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject obj = (JSONObject) args[0];
            String str = "";
            //Log.d("debug","onCoords");
            try {
                str = obj.getString("matrixes");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Log.d("debug",str);
            synchronized (lock) {
                decodeString(str);
            }
        }
    };

    private void decodeString(String str) {

        StringTokenizer strtok = new StringTokenizer(str);
        if (strtok.countTokens() != 48) {
            //Log.d("debug2", "tokens: " + strtok.countTokens());
            return;
        }
        int matrixstep = 0;
        while (strtok.hasMoreTokens()) {
            if (matrixstep < 16)
                anchorMatrix[matrixstep] = Float.parseFloat(strtok.nextToken());
            if (matrixstep >= 16 && matrixstep < 32)
                projmtx[matrixstep - 16] = Float.parseFloat(strtok.nextToken());
            if (matrixstep >= 32)
                viewmtx[matrixstep - 32] = Float.parseFloat(strtok.nextToken());
            matrixstep++;
        }

        Log.d("debug", " " + anchorMatrix[0]);
        Log.d("debug", " " + anchorMatrix[15]);
        Log.d("debug", " " + projmtx[0]);
        Log.d("debug", " " + projmtx[15]);
        Log.d("debug", " " + viewmtx[0]);
        Log.d("debug", " " + viewmtx[15]);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //surfaceView.onResume();
        //displayRotationHelper.onResume();
        timer = new Timer();
        timer.schedule(new FpsTimerTask(),0,1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        timer.cancel();
        //surfaceView.onPause();
    }

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
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            //backgroundRenderer.createOnGlThread(/*context=*/ this);
            //planeRenderer.createOnGlThread(/*context=*/ this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(/*context=*/ this);

            virtualObject.createOnGlThread(/*context=*/ this, "models/andy.obj", "models/andy.png");
            virtualObject.setMaterialProperties(0.0f, 1.0f, 0.5f, 6.0f);

            virtualObjectShadow.createOnGlThread(
                    /*context=*/ this, "models/andy_shadow.obj", "models/andy_shadow.png");
            virtualObject.setBlendMode(ObjectRenderer.BlendMode.Grid);
            virtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow);
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        screen_width = width;
        screen_height = height;
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //Log.d("debug","draw frame");
        //displayRotationHelper.updateSessionIfNeeded(session);

        try {
            //session.setCameraTextureName(backgroundRenderer.getTextureId());

            float scaleFactor = 0.6f;
            // Update and draw the model and its shadow.
            synchronized (lock) {
                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);

                virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);

                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, color);

                virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, color);
            }
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            JSONObject obj = new JSONObject();

            float x, y;
            x = event.getX() / screen_width;
            y = event.getY() / screen_height;
            String str = "" + x + " " + y;

            Log.d("debug", "touch " + x + " : " + y);
            socket.emit("touch", str);
            socket.emit("new message", "touch " + x + " : " + y);
            return true;
        }
        Log.d("debug", "false");
        return false;
    }
}
