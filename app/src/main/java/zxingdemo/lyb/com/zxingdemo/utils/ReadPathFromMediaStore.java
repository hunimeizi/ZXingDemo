package zxingdemo.lyb.com.zxingdemo.utils;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class ReadPathFromMediaStore {
    /**
     * 通过Uri获取文件在本地存储的真实路径
     * 
     * @param act
     * @param contentUri
     * @return
     */
    public static String getRealPathFromURI(Activity act, Uri contentUri) {
	// can post image
	String[] proj = { MediaStore.Images.Media.DATA };
	@SuppressWarnings("deprecation")
	Cursor cursor = act.managedQuery(contentUri, proj, // Which columns to
		null, // WHERE clause; which rows to return (all rows)
		null, // WHERE clause selection arguments (none)
		null); // Order-by clause (ascending by name)
	int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	cursor.moveToFirst();
	return cursor.getString(column_index);
    }
}
