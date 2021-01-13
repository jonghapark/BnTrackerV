package co.kr.bnr.bnrtrackerv.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import co.kr.bnr.bnrtrackerv.R;

public class CommonUtil {

	//내 로그~
	public static void myLog(String msg) {
		Log.e("BLE", msg);
	}

	//내 토스트~
	public static void myToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	public static boolean isEmpty(final CharSequence s) {
		return s == null || s.length() == 0 || s.equals("null");
	}
	public static boolean isEmpty(final Object s) {
		return s == null || s.toString().length() == 0 || s.toString().equals("null");
	}
	public static boolean isEmpty(final EditText editText) {
		String s = editText.getText().toString();
		return s == null || s.toString().length() == 0;
	}

	//투버튼다이얼로그 띄우기
	public interface CallbackOk {
		void callback();
	}

	public static Dialog dialOneButton(Context con, String message) {
		return dialOneButton(con, "안내", message, null);
	}

	public static Dialog dialOneButton(Context con, String message, final CallbackOk callback) {
		return dialOneButton(con, "안내", message, callback);
	}

	public static Dialog dialOneButton(Context con, String title, String message, final CallbackOk callback) {
		LayoutInflater vi = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = vi.inflate(R.layout.dial_one_button, null);

		final Dialog mDialog = new Dialog(con);
		mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mDialog.setContentView(view);
		mDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);//요게 핵심
		mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		TextView tv_title = (TextView) view.findViewById(R.id.dial_one_button_tv_title);
		TextView tv_message = (TextView) view.findViewById(R.id.dial_one_button_tv_message);

		if(title == null)
			tv_title.setText("안내");
		else
			tv_title.setText(title);

		tv_message.setText(message);

		Button bt_ok = (Button) view.findViewById(R.id.dial_one_button_bt_ok);
		bt_ok.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(callback != null)
					callback.callback();
				mDialog.dismiss();
			}
		});

		mDialog.show();

		return mDialog;
	}

	public static double hexToDecimal(String hex) {
		try{
			return Integer.parseInt(hex, 16 );
		}
		catch (Exception e) {
			return Long.parseLong(hex, 16 );
		}
		//return Long.parseLong(hex, 16);
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static String getDateTimeToHexa() {
		Calendar mCalendar = Calendar.getInstance();
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		//Locale.setDefault(Locale.KOREA);
		TimeZone gmtTime =  TimeZone.getTimeZone("Asia/Seoul");
		mCalendar.setTimeZone(gmtTime);
		final Date date = mCalendar.getTime();
		return Long.toHexString(date.getTime()/1000);
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}

		byte[] reversedBytes = new byte[data.length];
		for(int i=0; i<data.length; i++) {
			reversedBytes[data.length - i - 1] = data[i];
		}

		return reversedBytes;
	}


	public static String byteArrayToHexString(byte[] bytes){

		StringBuilder sb = new StringBuilder();

		for(byte b : bytes){

			sb.append(String.format("%02X", b&0xff));
		}

		return sb.toString();
	}
}