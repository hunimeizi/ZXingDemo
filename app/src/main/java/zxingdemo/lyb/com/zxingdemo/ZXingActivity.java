package zxingdemo.lyb.com.zxingdemo;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.FileNotFoundException;
import java.util.Hashtable;

import zxingdemo.lyb.com.zxingdemo.utils.RGBLuminanceSource;
import zxingdemo.lyb.com.zxingdemo.utils.ReadPathFromMediaStore;
import zxingdemo.lyb.com.zxingdemo.zxing.CaptureCamraActivity;


public class ZXingActivity extends Activity {

    private EditText edit_string;
    private View mCameraMenuView;

    protected static final int REQUEST_IMAGE_CODE = 0;
    protected static final int REQUEST_CAMERA_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zxing);
        initView();
    }

    private void initView() {
        edit_string = (EditText) findViewById(R.id.edit_string);
        Button button_erweima = (Button) findViewById(R.id.button_erweima);
        button_erweima.setOnClickListener(new myOnclick());
        Button button_saomiao = (Button) findViewById(R.id.button_saomiao);
        button_saomiao.setOnClickListener(new myOnclick());

        mCameraMenuView = LayoutInflater.from(this).inflate(R.layout.camera_menu_dialog, null);
        Button btn__camera = (Button) mCameraMenuView.findViewById(R.id.btn__camera);
        btn__camera.setOnClickListener(new myOnclick());
        Button btn_native_image = (Button) mCameraMenuView.findViewById(R.id.btn_native_image);
        btn_native_image.setOnClickListener(new myOnclick());
        Button btnCancel = (Button) mCameraMenuView.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new myOnclick());
    }

    /**
     * Buton 的点击事件
     */
    private class myOnclick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_erweima:
                    if (!TextUtils.isEmpty(edit_string.getText().toString().trim())) {
                        startActivity(new Intent(ZXingActivity.this, ZXingQRCodeActivity.class).putExtra("EditString", edit_string.getText().toString().trim()));
                    } else {
                        Toast.makeText(ZXingActivity.this, "请输入要生成的二维码内容", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.button_saomiao:
                    changeHanding();
                    break;
                case R.id.btn__camera:
                    Intent i = new Intent(getApplicationContext(), CaptureCamraActivity.class);
                    startActivityForResult(i, REQUEST_CAMERA_CODE);
                    break;
                case R.id.btn_native_image:
                    // 扫描对方二维码能快速填入地址和数量
                    Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT); // "android.intent.action.GET_CONTENT"
                    innerIntent.setType("image/*");
                    Intent wrapperIntent = Intent.createChooser(innerIntent, getString(R.string.choce_erweima));
                    ZXingActivity.this.startActivityForResult(wrapperIntent, REQUEST_IMAGE_CODE);
                    break;
                case R.id.btnCancel:
                    mCameraDialog.dismiss();
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resuVTCode, Intent data) {
        super.onActivityResult(requestCode, resuVTCode, data);
        if (resuVTCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CODE:
                    // 获取选中图片的路径
                    String photo_path;
                    try {
                        photo_path = ReadPathFromMediaStore.getRealPathFromURI(this, data.getData());
                    } catch (Exception e) {
                        photo_path = data.getData().getPath();
                    }
                            final Result result = scanningImage(photo_path);
                            if (result != null) {
//                                Message m = mHandler.obtainMessage();
//                                m.what = PARSE_BARCODE_SUC;
//                                m.obj = result.getText().toString().trim();
//                                mHandler.sendMessage(m);
                                        Toast.makeText(ZXingActivity.this, result.getText().trim(), Toast.LENGTH_SHORT).show();
                            } else {
//                                Message m = mHandler.obtainMessage();
//                                m.what = PARSE_BARCODE_FAIL;
//                                m.obj = "Scan failed!";
//                                mHandler.sendMessage(m);
                                        Toast.makeText(ZXingActivity.this, "识别结果错误", Toast.LENGTH_SHORT).show();
                            }
                    break;
                case REQUEST_CAMERA_CODE:
                    final String result1 = data.getStringExtra("STEP1RESULT");
                    if (result1 != null) {
//                        Message m = mHandler.obtainMessage();
//                        m.what = PARSE_BARCODE_SUC;
//                        m.obj = result.trim();
//                        mHandler.sendMessage(m);
                                Toast.makeText(ZXingActivity.this, result1.trim(), Toast.LENGTH_SHORT).show();
                    } else {
//                        Message m = mHandler.obtainMessage();
//                        m.what = PARSE_BARCODE_FAIL;
//                        m.obj = "Scan failed!";
//                        mHandler.sendMessage(m);
                                Toast.makeText(ZXingActivity.this, "识别结果错误", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 根据图片url解析二维码图片的方法
     *
     * @param path 图片的绝对路径
     * @return Result
     */
    public Result scanningImage(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        Hashtable<DecodeHintType, String> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF8"); // 设置二维码内容的编码
        RGBLuminanceSource source;
        try {
            source = new RGBLuminanceSource(path);
            BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
            QRCodeReader reader = new QRCodeReader();
            return reader.decode(bitmap1, hints);
        } catch (NotFoundException | ChecksumException | FormatException | FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraDialog != null && mCameraDialog.isShowing()) {
            mCameraDialog.dismiss();
        }
    }

    private Dialog mCameraDialog;

    private void changeHanding() {
        if (mCameraDialog == null) {
            mCameraDialog = getMenuDialog(this, mCameraMenuView);
            mCameraDialog.show();
        } else {
            mCameraDialog.show();
        }
    }

    // 菜单dialog
    public static Dialog getMenuDialog(Activity context, View view) {

        final Dialog dialog = new Dialog(context, R.style.MenuDialogStyle);
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        // int screenH = getScreenHeight(context);
        lp.width = getScreenWidth(context);
        window.setGravity(Gravity.BOTTOM); // 此处可以设置dialog显示的位置
        window.setWindowAnimations(R.style.MenuDialogAnimation); // 添加动画
        return dialog;
    }

    public static int getScreenWidth(Activity context) {
        DisplayMetrics dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }
}
