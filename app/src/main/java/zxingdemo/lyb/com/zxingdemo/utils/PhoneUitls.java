package zxingdemo.lyb.com.zxingdemo.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneUitls {
    /**
     * 获取版本名称
     *
     * @param c
     * @return
     */
    public static String getVerName(Context c) {
        try {
            PackageInfo packageInfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return "未知版本";
    }

    /**
     * 中国移动：46000 46002 中国联通：46001 中国电信：46003
     *
     * @param c
     * @return
     */
    public static String GetOperator(Context c) {
        TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        String operator = tm.getSimOperator();
        if (operator != null) {
            if (operator.equals("46000") || operator.equals("46002") || operator.equals("46007")) {
                return "中国移动";
            } else if (operator.equals("46001")) {
                return "中国联通";
            } else if (operator.equals("46003")) {
                return "中国电信";
            }
        }
        return "未知";
    }

    /**
     * 获取imei
     *
     * @param c
     * @return
     */
    public static String GetIMEI(Context c) {
        TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getDeviceId();
    }

    /**
     * 获取wifi信息
     *
     * @param mContext
     */
    public static void getWifiInfo(Context mContext) {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getBSSID() != null) {
            // wifi名称
            String ssid = wifiInfo.getSSID();
            // wifi信号强度
            int signalLevel = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
            // wifi速度
            int speed = wifiInfo.getLinkSpeed();
            // wifi速度单位
            String units = WifiInfo.LINK_SPEED_UNITS;
            LogCat.lyb("ssid=" + ssid + ",signalLevel=" + signalLevel + ",speed=" + speed + ",units=" + units);
        }
    }


    /**
     * 得到wifi状态下的ip 需要使用wifi中，需要添加权限 <uses-permission
     * android:name="android.permission.ACCESS_WIFI_STATE"></uses-permission>
     * <uses-permission
     * android:name="android.permission.CHANGE_WIFI_STATE"></uses-permission>
     * <uses-permission
     * android:name="android.permission.WAKE_LOCK"></uses-permission>
     *
     * @return String格式的字符串（10.111.130.233;fe80::7ad6:f0ff:fe0a:da50%wlan0;192.168
     * .1.100）
     */
    public static String getLocalIpAddress() {
        String ipaddress = "";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        ipaddress = ipaddress + ";" + inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            LogCat.lyb("WifiPreference IpAddress" + ex.toString());
        }
        return ipaddress;
    }

    /**
     * 检查后台的server是否运行，通过输入server name
     *
     * @param context
     * @param className
     * @return
     */
    public static boolean isServiceRunning(Context context, String className) {

        boolean isRunning = false;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);

        if (!(serviceList.size() > 0)) {
            return false;
        }
        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    /**
     * 获取屏幕宽高
     *
     * @param c
     * @return
     */
    public static int[] ScreenWH(Activity c) {
        int[] screenwh = new int[2];
        DisplayMetrics dm = new DisplayMetrics();
        c.getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenwh[0] = dm.widthPixels;
        screenwh[1] = dm.heightPixels;
        return screenwh;
    }

    /**
     * 获取phone型号
     *
     * @return
     */
    public static String GetPhoneType() {
        return android.os.Build.MODEL.replaceAll(" ", "_");
    }

    /**
     * 获取当前应用的versioncode
     *
     * @return
     */
    public static int GetPhoneType(Context c) {
        try {
            PackageInfo packageInfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().densityDpi;
        return (int) (dipValue * (scale / 160) + 0.5f);
    }

    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().densityDpi;
        return (int) ((pxValue * 160) / scale + 0.5f);
    }

    /**
     * 获取状态栏高度
     *
     * @param context
     * @return
     */
    public static int getStateBarHeight(Activity context) {

        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, sbar = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            sbar = context.getResources().getDimensionPixelSize(x);
            return sbar;
        } catch (Exception e1) {
            e1.printStackTrace();
            return 0;
        }
    }

    /**
     * 验证是否是手机号
     * @param mobiles
     * @return
     */
    public static boolean isMobileNO(String mobiles) {

        Pattern p = Pattern
                .compile("1[3|5|7|8]\\d{9}");

        Matcher m = p.matcher(mobiles.replace("+86",""));
        return m.matches();

    }
}
