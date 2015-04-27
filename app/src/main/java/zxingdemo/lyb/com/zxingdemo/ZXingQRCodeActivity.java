package zxingdemo.lyb.com.zxingdemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.UnsupportedEncodingException;

import zxingdemo.lyb.com.zxingdemo.utils.LogCat;
import zxingdemo.lyb.com.zxingdemo.utils.PhoneUitls;

/**
 * Created by MyPC on 2015/4/27.
 */
public class ZXingQRCodeActivity extends Activity {

    private ImageButton imgbtn_delete;
    private ImageView iv_erweima;
    private Intent it;
    private String editString;
    private int eqcorewith;
    private long mLastTime, mCurTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate2d);
        //获取屏幕的宽高
        if (PhoneUitls.ScreenWH(ZXingQRCodeActivity.this)[0] < PhoneUitls.ScreenWH(ZXingQRCodeActivity.this)[1]) {
            eqcorewith = PhoneUitls.ScreenWH(ZXingQRCodeActivity.this)[0] - 100;
        } else {
            eqcorewith = PhoneUitls.ScreenWH(ZXingQRCodeActivity.this)[1] - 100;
        }
        initView();
    }

    private void initView() {
        imgbtn_delete = (ImageButton) findViewById(R.id.imgbtn_delete);
        imgbtn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZXingQRCodeActivity.this.finish();
            }
        });
        iv_erweima = (ImageView) findViewById(R.id.iv_erweima);
        iv_erweima.setOnTouchListener(new onDoubleClick());
        it = getIntent();
        if (it != null) {
            editString = it.getStringExtra("EditString");
        } else {
            editString = "0";
        }
        LogCat.lyb("editString====" + editString);
        SetImageView(editString);
    }

    /**
     * onTouch事件 双击退出该页面
     */
    private class onDoubleClick implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (MotionEvent.ACTION_DOWN == event.getAction()) {
                mLastTime = mCurTime;
                mCurTime = System.currentTimeMillis();
                if (mCurTime - mLastTime < 1000) {
                    ZXingQRCodeActivity.this.finish();
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 将得到的二维码bitmap加载到imageview
     */
    Bitmap bmp = null;

    private void SetImageView(String content) {
        try {
            bmp = createBitmap(Create2DCode(content));
            iv_erweima.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成固定大小的图片
     *
     * @param src
     * @return
     */
    private Bitmap createBitmap(Bitmap src) {
        if (src == null) {
            return null;
        }
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        int w = eqcorewith;
        int h = eqcorewith;
        Bitmap newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newb);
        cv.drawColor(Color.WHITE);
        cv.drawBitmap(src, 0, 0, null);
        cv.save(Canvas.ALL_SAVE_FLAG);
        cv.restore();// 定型图片
        return newb;
    }

    /**
     * 生成二维矩阵,编码时指定大小
     *
     * @param str
     * @return bitmap
     * @throws WriterException
     * @throws UnsupportedEncodingException
     */
    public Bitmap Create2DCode(String str) throws WriterException, UnsupportedEncodingException {
        // 生成二维矩阵,编码时指定大小,不要生成了图片以后再进行缩放,这样会模糊导致识别失败
        BitMatrix matrix = new MultiFormatWriter().encode(new String(str.getBytes("utf-8"), "ISO-8859-1"), BarcodeFormat.QR_CODE,
                eqcorewith, eqcorewith);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        // 二维矩阵转为一维像素数组,也就是一直横着排了
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    pixels[y * width + x] = 0xff000000;
                }

            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 通过像素数组生成bitmap,具体参考api
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
