package co.kr.bnr.bnrtrackerv.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Util {
	//앱 구동되는 동안 필요한 변수들 초기화(앱이 오랫동안 백그라운드에있다가 오면 죽는 일이있어서 얘네중에 하나가 널이면 다시 세팅하려고!!)
	public static void initData(Context context) {
	}

	//내 로그~
	public static void myLog(String msg) {
		Log.e("Witch", msg);
	}

	//내 토스트~
	public static void myToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	//캘린더 받아서 스트링만들기
	public static String makeStringFromCalendar(Calendar cal, String format) {
		String str_title = new SimpleDateFormat(format, Locale.KOREA).format(cal.getTime());
		return str_title;
	}

	//인터넷 연결되어있는지 확인
	public static boolean checkInternetState(Context con) {
		ConnectivityManager connectivityManager = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}
}