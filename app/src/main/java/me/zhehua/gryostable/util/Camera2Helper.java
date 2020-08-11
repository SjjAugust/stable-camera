package me.zhehua.gryostable.util;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.zhehua.gryostable.ImageUtil;
import me.zhehua.gryostable.MainCameraActivity;
import me.zhehua.gryostable.R;
import me.zhehua.gryostable.StableProcessor;
import me.zhehua.gryostable.ThetaHelper;
import me.zhehua.gryostable.widget.GlRenderView;
public class Camera2Helper {
    private Activity mContext;
    private final String TAG = "Camera2Helper";
    private String mCameraId;
    private OnPreviewSizeListener onPreviewSizeListener;
    private ImageReader imageReader;
    private OnPreviewListener onPreviewListener;
    private SensorManager mSensorManager;
    private Sensor mGyroSensor;
    private ThetaHelper mThetaHelper;
    public  long timeDelay = 12000000;
    public StableProcessor stableProcessor;
    public boolean isCrop = true;
    private TextView textView;
    private Button cropButton;
    private SeekBar seekBar;
    private GlRenderView glRenderView;
    private Handler mainHandler;

    private long lastImageTime = 0;

    public Camera2Helper(Activity mContext, GlRenderView glRenderView) {
        this.mContext = mContext;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mThetaHelper = new ThetaHelper();
        stableProcessor = new StableProcessor();
        glRenderView.startDisplayThread();


//        textView = mContext.findViewById(R.id.tv_timedelay);
//        textView.setText(String.valueOf((int)(timeDelay/1000/1000)));
//        cropButton = mContext.findViewById(R.id.bt_crop);
//        cropButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                isCrop = !isCrop;
//                if (isCrop) {
//                    ((Button) v).setText("Crop");
//                } else {
//                    ((Button) v).setText("Not Crop");
//                }
//                stableProcessor.setCrop(isCrop);
//            }
//        });
//        seekBar = mContext.findViewById(R.id.sb_time);
//        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
//                timeDelay = progress * 1000 * 1000;
//                Log.i(TAG, "progress " + progress);
//                textView.setText(String.valueOf((int)(timeDelay/1000/1000)));
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
        this.glRenderView = glRenderView;
        glRenderView.stableProcessor = stableProcessor;

    }

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;


    private SurfaceTexture mSurfaceTexture;

    private CameraDevice mCameraDevice;


    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;
    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;
    /**
     * Opens the camera specified .
     */


    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;


    public void openCamera(int width, int height, SurfaceTexture mSurfaceTexture) {
        this.mSurfaceTexture = mSurfaceTexture;

        startBackgroundThread();

        //设置预览图像的大小，surfaceview的大小。
        setUpCameraOutputs(width, height);
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        mSensorManager.registerListener(mThetaHelper, mGyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mContext.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    Activity#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for Activity#requestPermissions for more details.
                    return;
                }
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public int getCameraId() {
        return Integer.valueOf(mCameraId);
    }


    /**
     * Closes the current {@link CameraDevice}.
     */
    public void closeCamera() {

        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

//        if (mSurfaceTexture != null) {
//            mSurfaceTexture.release();
//            mSurfaceTexture = null;
//        }
        stopBackgroundThread();

    }


    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        imageReaderThread = new HandlerThread("imageReader handlerThread");
        imageReaderThread.start();
        imageReaderHandler = new Handler(imageReaderThread.getLooper());
    }


    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private HandlerThread imageReaderThread;
    private Handler imageReaderHandler;
    private Range<Integer>[] fpsRange;
    private void setUpCameraOutputs(int width, int height) {

        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Point displaySize = new Point();
                mContext.getWindowManager().getDefaultDisplay().getSize(displaySize);

                fpsRange = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)),
                        new CompareSizesByArea());
                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
//                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
//                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
//                        maxPreviewHeight, largest);
                mPreviewSize = new Size(1920, 1080);

//                if (onPreviewSizeListener != null) {
//                    onPreviewSizeListener.onSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//                }

                imageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
                imageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                stableProcessor.init(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                mCameraId = cameraId;


                glRenderView.postDisplayThread();
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {

        }
    }


    public Size getSize() {
        return mPreviewSize;
    }

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {


        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }

    };


    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {

            // This is the output Surface we need to start preview.

            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

//            Surface surface = new Surface(mSurfaceTexture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(imageReader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, 0);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange[fpsRange.length-1]);
                                // Flash is automatically enabled when necessary.

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "onConfigureFailed: ");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
        }

    };


    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("Camera2Helper", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public void setPreviewSizeListener(OnPreviewSizeListener onPreviewSizeListener) {
        this.onPreviewSizeListener = onPreviewSizeListener;
    }


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        Mat curFrame;
        byte[] byteBuffer;
        byte[] byteBuffer2;
        Mat lastFrame;
        long lastTimestamp;
        int c = 0;
        Mat R;
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "Camera2Helper: cur_thread:"+Thread.currentThread().getId());
            Image image = reader.acquireNextImage();
            if (image == null) {
                return;
            }
//            int imageWidth = image.getWidth();
//            int imageHeight = image.getHeight();
//            byte[] data68 = ImageUtil.getBytesFromImageAsType(image,2);
//            int rgb[] = ImageUtil.decodeYUV420SP(data68, imageWidth, imageHeight);
//            Bitmap bitmap2 = Bitmap.createBitmap(rgb, 0, imageWidth,
//                    imageWidth, imageHeight,
//                    android.graphics.Bitmap.Config.ARGB_8888);
//            try {
//                File newFile = new File("/data/data/me.zhehua.gryostable/data/pic.png");
//                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile));
//                bitmap2.compress(Bitmap.CompressFormat.PNG, 100, bos);
//                bos.flush();
//                bos.close();
//                bitmap2.recycle();
//            } catch (Exception e) {
//
//            }

            final long timeStamp = image.getTimestamp() + timeDelay;

            long curTimeStamp = image.getTimestamp();
            Log.d(TAG, "fps1111: "+ 1/((float)(curTimeStamp - lastImageTime)/1000/1000/1000));
            lastImageTime = curTimeStamp;
            Log.d(TAG, "onImageAvailable: timestamp"+image.getTimestamp());
//            glRenderView.glRender.getTimeStamp(timeStamp);
            if (curFrame == null) {
                curFrame = new Mat(image.getHeight() / 2 * 3, image.getWidth(), CvType.CV_8U); // TODO
                byteBuffer = new byte[image.getWidth() * image.getHeight()];
                byteBuffer2 = new byte[image.getWidth() * image.getHeight() / 2];
                lastFrame = new Mat(image.getHeight() / 2 * 3, image.getWidth(), CvType.CV_8U);
                R = new Mat(3, 3, CvType.CV_64F);
            }

            Image.Plane[] planes = image.getPlanes();
            planes[0].getBuffer().get(byteBuffer);
            curFrame.put(0, 0, byteBuffer);
            planes[1].getBuffer().get(byteBuffer2, 0, byteBuffer2.length - 1); // buffer is 1 shorter
            curFrame.put(image.getHeight(), 0, byteBuffer2);
            image.close();
//            planes[2].getBuffer().get(byteBuffer2, 0, byteBuffer2.length - 1); // buffer is 1 shorter TODO
//            curFrame.put(image.getHeight() / 2 * 3, 0, byteBuffer2); // TODO

            if (c == 0) {
                curFrame.copyTo(lastFrame);
                lastTimestamp = timeStamp;
                c++;

            } else {
                int idx = stableProcessor.dequeueInputBuffer();
                mThetaHelper.n_getR(lastTimestamp, R.nativeObj, isCrop);
                Mat rs_out_mat = new Mat(0, 0, CvType.CV_64F);
                mThetaHelper.n_RsChangeVectorToMat(rs_out_mat.nativeObj);
                stableProcessor.enqueueInputBuffer(idx, lastFrame, R, rs_out_mat);
                Log.d(TAG, "onImageAvailable: "+lastFrame.get(0, 0)[0]);
//                if (onPreviewListener != null) {
//                    onPreviewListener.onPreviewFrame(lastFrame, lastFrame.length);
//                }
                synchronized (glRenderView.syncObj){
                    glRenderView.syncObj.notify();
                }
                curFrame.copyTo(lastFrame);
                lastTimestamp = timeStamp;
            }




//            Image.Plane[] planes = image.getPlanes();
//            int width = image.getWidth();
//            int height = image.getHeight();
//
//            byte[] yBytes = new byte[width * height];
//            byte[] uBytes = new byte[width * height / 4];
//            byte[] vBytes = new byte[width * height / 4];
//            byte[] i420 = new byte[width * height * 3 / 2];
//
//
//
//            for (int i = 0; i < planes.length; i++) {
//                int dstIndex = 0;
//                int uIndex = 0;
//                int vIndex = 0;
//                int pixelStride = planes[i].getPixelStride();
//                int rowStride = planes[i].getRowStride();
//
//                ByteBuffer buffer = planes[i].getBuffer();
//
//                byte[] bytes = new byte[buffer.capacity()];
//
//                buffer.get(bytes);
//                int srcIndex = 0;
//                if (i == 0) {
//                    for (int j = 0; j < height; j++) {
//                        System.arraycopy(bytes, srcIndex, yBytes, dstIndex, width);
//                        srcIndex += rowStride;
//                        dstIndex += width;
//                    }
//                } else if (i == 1) {
//                    for (int j = 0; j < height / 2; j++) {
//                        for (int k = 0; k < width / 2; k++) {
//                            uBytes[dstIndex++] = bytes[srcIndex];
//                            srcIndex += pixelStride;
//                        }
//
//                        if (pixelStride == 2) {
//                            srcIndex += rowStride - width;
//                        } else if (pixelStride == 1) {
//                            srcIndex += rowStride - width / 2;
//                        }
//                    }
//                } else if (i == 2) {
//                    for (int j = 0; j < height / 2; j++) {
//                        for (int k = 0; k < width / 2; k++) {
//                            vBytes[dstIndex++] = bytes[srcIndex];
//                            srcIndex += pixelStride;
//                        }
//
//                        if (pixelStride == 2) {
//                            srcIndex += rowStride - width;
//                        } else if (pixelStride == 1) {
//                            srcIndex += rowStride - width / 2;
//                        }
//                    }
//                }
//                System.arraycopy(yBytes, 0, i420, 0, yBytes.length);
//                System.arraycopy(uBytes, 0, i420, yBytes.length, uBytes.length);
//                System.arraycopy(vBytes, 0, i420, yBytes.length + uBytes.length, vBytes.length);




//            }
            image.close();
        }
    };

    public void setOnPreviewListener(OnPreviewListener onPreviewListener) {
        this.onPreviewListener = onPreviewListener;
    }

    public interface OnPreviewSizeListener {
        void onSize(int width, int height);
    }

    public interface OnPreviewListener {
        void onPreviewFrame(byte[] data, int len);
    }
}