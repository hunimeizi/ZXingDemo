package zxingdemo.lyb.com.zxingdemo.zxing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

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

import zxingdemo.lyb.com.zxingdemo.R;
import zxingdemo.lyb.com.zxingdemo.utils.RGBLuminanceSource;

/**
 * 识别本地SD卡上的图片
 * 
 * @author 猫
 * 
 */
@SuppressLint("Registered")
public class CaptureImageActivity extends Activity {

	protected static final int REQUEST_CODE = 0;
	private TextView text;
	private Button btn;
	static Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.capture_image);
		context = getBaseContext();
		InitView();
	}

	private void InitView() {
		btn = (Button) findViewById(R.id.btn_openfile);
		btn.setOnClickListener(new MyBtnOnclick());
		text = (TextView) findViewById(R.id.textcode);
	}

	private class MyBtnOnclick implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btn_openfile:
				Intent innerIntent = new Intent(Intent.ACTION_GET_CONTENT); // "android.intent.action.GET_CONTENT"
				innerIntent.setType("image/*");
				Intent wrapperIntent = Intent.createChooser(innerIntent, getString(R.string.choce_erweima));
				CaptureImageActivity.this.startActivityForResult(wrapperIntent, REQUEST_CODE);
				break;
			default:
				break;
			}
		}

	}

	ProgressDialog mProgress;
	private String photo_path;

	@Override
	protected void onActivityResult(int requestCode, int resuVTCode, Intent data) {
		super.onActivityResult(requestCode, resuVTCode, data);
		if (resuVTCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_CODE:
				// 获取选中图片的路径
				photo_path = data.getData().getPath();
				mProgress = new ProgressDialog(CaptureImageActivity.this);
				mProgress.setMessage(getString(R.string.saomiaoing) + "...");
				mProgress.setCancelable(false);
				mProgress.show();
				new Thread(new Runnable() {
					@Override
					public void run() {
						Result result = scanningImage(photo_path);
						if (result != null) {
							Message m = mHandler.obtainMessage();
							m.what = PARSE_BARCODE_SUC;
							m.obj = result.getText().toString().trim();
							mHandler.sendMessage(m);
						} else {
							Message m = mHandler.obtainMessage();
							m.what = PARSE_BARCODE_FAIL;
							m.obj = "Scan failed!";
							mHandler.sendMessage(m);
						}
					}
				}).start();
				break;
			}
		}
	}

	/**
	 * 根据图片url解析二维码图片的方法
	 * 
	 * @param path
	 *            图片的绝对路径
	 * @return
	 */
	public Result scanningImage(String path) {
		if (TextUtils.isEmpty(path)) {
			return null;
		}
		Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
		hints.put(DecodeHintType.CHARACTER_SET, "UTF8"); // 设置二维码内容的编码
		RGBLuminanceSource source;
		try {
			source = new RGBLuminanceSource(path);
			BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
			QRCodeReader reader = new QRCodeReader();
			return reader.decode(bitmap1, hints);
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (ChecksumException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private final static int PARSE_BARCODE_SUC = 1;
	private final static int PARSE_BARCODE_FAIL = 2;
	private Handler mHandler = new myHandler();

	private  class myHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case PARSE_BARCODE_SUC:
				mProgress.cancel();
				text.setText( context.getString(R.string.shibeichenggong)+ msg.obj);
				break;
			case PARSE_BARCODE_FAIL:
				mProgress.cancel();
				text.setText(" <string name=\"text_tex\">2B,你能弄张带二维码的图片？\n如果你确定你图片有二维码\n好吧，我承认是我识别系统出现问题了\n你自己看源码找问题吧</string>");
				break;

			default:
				break;
			}
		}
	}
}
