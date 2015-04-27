package zxingdemo.lyb.com.zxingdemo.utils;

public class LogCat {
    static final boolean LOG = true;
    static final String lyb = "lyb";

    public static void i(String tag, String string) {
	if (LOG)
	    android.util.Log.i(tag, string);
    }

    public static void e(String tag, String string) {
	if (LOG)
	    android.util.Log.e(tag, string);
    }

    public static void d(String tag, String string) {
	if (LOG)
	    android.util.Log.d(tag, string);
    }

    public static void v(String tag, String string) {
	if (LOG)
	    android.util.Log.v(tag, string);
    }

    public static void w(String tag, String string) {
	if (LOG)
	    android.util.Log.w(tag, string);
    }


    public static void lyb(String string) {
	if (LOG)
	    android.util.Log.e(lyb, string);
    }
}
