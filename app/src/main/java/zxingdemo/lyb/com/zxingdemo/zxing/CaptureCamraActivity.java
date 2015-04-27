/*
 * Copyright (C) 2008 ZXing authors
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

package zxingdemo.lyb.com.zxingdemo.zxing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.result.ResultParser;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;

import zxingdemo.lyb.com.zxingdemo.R;
import zxingdemo.lyb.com.zxingdemo.android.BeepManager;
import zxingdemo.lyb.com.zxingdemo.android.CaptureActivityHandler;
import zxingdemo.lyb.com.zxingdemo.android.ViewfinderView;
import zxingdemo.lyb.com.zxingdemo.camera.CameraManager;
import zxingdemo.lyb.com.zxingdemo.result.URIResultHandler;
import zxingdemo.lyb.com.zxingdemo.utils.LogCat;

/**
 * 摄像头识别二维码
 *
 * @author 猫
 */
public final class CaptureCamraActivity extends Activity implements
        SurfaceHolder.Callback {

    private static final String TAG = CaptureCamraActivity.class
            .getSimpleName();

    public static final int HISTORY_REQUEST_CODE = 0x0000bacc;

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private Result savedResultToShow;
    private ViewfinderView viewfinderView;
    private TextView statusView;
    private Result lastResult;
    private boolean hasSurface;
    private String returnUrlTemplate;
    private Collection<BarcodeFormat> decodeFormats;
    private String characterSet;
    private BeepManager beepManager;
    AlertDialog.Builder builder;
    private Toast toastStart;
    private boolean inToast = false;

    private Source source;

    private enum Source {
        NATIVE_APP_INTENT, PRODUCT_SEARCH_LINK, ZXING_LINK, NONE
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.capture_camra);

        hasSurface = false;
        beepManager = new BeepManager(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        inToast = true;
        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);

        statusView = (TextView) findViewById(R.id.status_view);

        handler = null;
        lastResult = null;

        resetStatusView();

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        beepManager.updatePrefs(); // 设置震动

        source = Source.NONE;
        decodeFormats = null;
        characterSet = null;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    protected void onPause() {
        inToast = false;
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        cancelToast();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (lastResult != null) {
                // restartPreviewAfterDelay(0L);
                if (source == Source.NATIVE_APP_INTENT) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                } else if ((source == Source.NONE || source == Source.ZXING_LINK)
                        && lastResult != null) {
                    // findViewById(R.id.result_view).setVisibility(View.GONE);
                    resetStatusView();
                    if (handler != null) {
                        handler.sendEmptyMessage(R.id.restart_preview);
                    }
                    return true;
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_FOCUS
                || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // Handle these events so they don't launch the Camera app
            // resetStatusView();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler,
                        R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            LogCat.e(TAG,
                    "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult The contents of the barcode.
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode) {
        lastResult = rawResult;

        URIResultHandler resultHandler = new URIResultHandler(this,
                ResultParser.parseResult(rawResult));

        if (barcode == null) {
            // This is from history -- no saved barcode
            handleDecodeInternally(rawResult, resultHandler, null);
        } else {
            beepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, rawResult);
            // if (PreferenceConfig.KEY_BULK_MODE_ENABLE) {
            // Toast.makeText(this, R.string.msg_bulk_mode_scanned,
            // Toast.LENGTH_SHORT).show();
            // // Wait a moment or else it will scan the same barcode
            // continuously about 3 times
            // restartPreviewAfterDelay(BULK_MODE_SCAN_DELAY_MS);
            // } else {
            switch (source) {
                case NATIVE_APP_INTENT:
                case PRODUCT_SEARCH_LINK:
                    // handleDecodeExternally(rawResult, barcode);
                    break;
                case ZXING_LINK:
                    if (returnUrlTemplate == null) {
                        handleDecodeInternally(rawResult, resultHandler, barcode);
                    } else {
                        // handleDecodeExternally(rawResult, barcode);
                    }
                    break;
                case NONE: {
                    handleDecodeInternally(rawResult, resultHandler, barcode);
                }
                break;
            }
            // handleDecodeInternally(rawResult, resultHandler, barcode);
            // }
        }
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of
     * the barcode.
     *
     * @param barcode   A bitmap of the captured image.
     * @param rawResult The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_image_border));
            paint.setStrokeWidth(3.0f);
            paint.setStyle(Paint.Style.STROKE);
            Rect border = new Rect(2, 2, barcode.getWidth() - 2,
                    barcode.getHeight() - 2);
            canvas.drawRect(border, paint);

            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1]);
            } else if (points.length == 4
                    && (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A || rawResult
                    .getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and
                // metadata
                drawLine(canvas, paint, points[0], points[1]);
                drawLine(canvas, paint, points[2], points[3]);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    canvas.drawPoint(point.getX(), point.getY(), paint);
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a,
                                 ResultPoint b) {
        canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
    }

    /**
     * 识别结果以及后续操作
     *
     * @param rawResult
     * @param resultHandler
     * @param barcode
     */
    private void showResult(Result rawResult, URIResultHandler resultHandler,
                            Bitmap barcode) {
        final CharSequence displayContents = resultHandler.getDisplayContents();
        // 识别结果
        if (displayContents != null && !displayContents.equals("")) {
            // initToast("文字内容：\n" + displayContents.toString(),
            // R.color.alpha_0f, Gravity.CENTER, 0, 200, 1000, 18, Color.RED);
            Intent it = new Intent();
            it.putExtra("STEP1RESULT", displayContents.toString());
            setResult(Activity.RESULT_OK, it);
            finish();
        } else {
            initToast(getString(R.string.jinggao) + displayContents.toString(),
                    R.color.alpha_0f, Gravity.CENTER, 0, 200, 1000, 18,
                    Color.RED);
            // 识别结果不正确 初始化识别程序重新识别
            if (source == Source.NATIVE_APP_INTENT) {
                setResult(RESULT_CANCELED);
                finish();
            } else if ((source == Source.NONE || source == Source.ZXING_LINK)
                    && lastResult != null) {
                resetStatusView();
                if (handler != null) {
                    handler.sendEmptyMessage(R.id.restart_preview);
                }
            }
        }

    }

    /**
     * Put up our own UI for how to handle the decoded contents. 处理扫描结果
     *
     * @param rawResult
     * @param resultHandler
     * @param barcode       条码截图
     */
    private void handleDecodeInternally(Result rawResult,
                                        URIResultHandler resultHandler, Bitmap barcode) {
        statusView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        showResult(rawResult, resultHandler, barcode);// dialog展示简要信息

        // 二维码格式 例：QR_CODE
        String format = rawResult.getBarcodeFormat().toString();

        // 二维码扫描结果类型 例： URL
        String type = resultHandler.getType().toString();

        // time
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                DateFormat.SHORT);
        String formattedTime = formatter.format(new Date(rawResult
                .getTimestamp()));

        // url
        CharSequence displayContents = resultHandler.getDisplayContents();

        LogCat.d(TAG, String.format(
                "formatText=%s typeText=%s timeText=%s displayContents=%s",
                format, type, formattedTime, displayContents));

    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats,
                        characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            LogCat.e(TAG, ioe.toString());
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            LogCat.e(TAG, "Unexpected error initializing camera" + e);
            displayFrameworkBugMessageAndExit();
        }
    }

    /**
     * 相机不能使用时 弹出提示
     */
    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.msg_load_ing));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();
    }

    private void resetStatusView() {
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(View.VISIBLE);
        viewfinderView.setVisibility(View.VISIBLE);
        lastResult = null;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    /**
     * 自定义Toast
     *
     * @param str      显示内容
     * @param bg       背景图片
     * @param gravity  屏幕位置
     * @param xoffset  X轴偏移量
     * @param yoffset  Y轴偏移量
     * @param time     显示时间
     * @param textsize 字体大小
     * @param textsize 字体颜色
     */
    private void initToast(String str, int bg, int gravity, int xoffset,
                           int yoffset, int time, int textsize, int color) {
        if (inToast) {
            View toastRoot = getLayoutInflater().inflate(R.layout.toast, null);
            TextView message = (TextView) toastRoot.findViewById(R.id.message);
            message.setTextColor(color);
            message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textsize);
            message.setBackgroundResource(bg);
            message.setGravity(Gravity.CENTER);
            message.setText(str);
            if (toastStart == null) {
                toastStart = new Toast(this);
            }
            toastStart.setGravity(gravity, xoffset, yoffset);
            toastStart.setDuration(time);
            toastStart.setView(toastRoot);
            toastStart.show();
        }

    }

    private void cancelToast() {
        if (toastStart != null) {
            toastStart.cancel();
        }
    }
}
