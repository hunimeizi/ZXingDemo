package zxingdemo.lyb.com.zxingdemo.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

public class CoinToJiFenDialog extends AlertDialog {
	@SuppressWarnings("unused")
	private View view;

	protected CoinToJiFenDialog(Context context) {
		super(context);

	}

	public CoinToJiFenDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);

	}

	public CoinToJiFenDialog(Context context, int theme, View view) {
		super(context, theme);
		this.view = view;
		setContentView(view);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
/*		 requestWindowFeature(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
		 requestWindowFeature(Window.FEATURE_NO_TITLE);
		 super.onCreate(savedInstanceState);
		 getWindow().setBackgroundDrawableResource(android.R.color.transparent);*/
		
	}

}
