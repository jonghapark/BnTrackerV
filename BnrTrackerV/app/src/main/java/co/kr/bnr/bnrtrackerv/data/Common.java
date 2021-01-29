package co.kr.bnr.bnrtrackerv.data;

import android.os.Environment;

import co.kr.bnr.bnrtrackerv.util.CommonUtil;

public class Common {

    //실시간 데이터 전송 시작 명령
    public static String commandStartSendRTData = "7E7E0100000000007D7D";
    public static byte[] commandStartSendRTDataToByte = new java.math.BigInteger(commandStartSendRTData, 16).toByteArray();

    //실시간 데이터 전송 종료 명령
    public static String commandFinishSendRTData = "7E7E0200000000007D7D";
    public static byte[] commandFinishSendRTDataToByte = new java.math.BigInteger(commandFinishSendRTData, 16).toByteArray();

    //실시간 데이터 전송 요청 성공 응답
    public static String responseSuccessReceiveRTData = "7E7E0180000000007D7D";
    public static byte[] responseSuccessReceiveRTDataToByte = new java.math.BigInteger(responseSuccessReceiveRTData, 16).toByteArray();

    //실시간 데이터 전송 요청 실패 응답
    public static String responseFailReceiveRTData = "7E7E0190000000007D7D";
    public static byte[] responseFailReceiveRTDataToByte = new java.math.BigInteger(responseFailReceiveRTData, 16).toByteArray();

    //시간 세팅 5deddfe //TODO 이 4는 뭐지? 4원래있는건가봐 ... ㄷㄷ 04000000는 길이 페이로드
    public static String commandSetCurrentTime = "7E7E210004000000" + CommonUtil.byteArrayToHexString(CommonUtil.hexStringToByteArray(CommonUtil.getDateTimeToHexa())) + "7D7D";
    public static byte[] commandSetCurrentTimeToByte = new java.math.BigInteger(commandSetCurrentTime, 16).toByteArray();

    //비콘/저장 모드로 변경
    public static String commandSetSaveAndBeaconMode = "7E7E260001000000" + "01" + "7D7D";
    public static byte[] commandSetSaveAndBeaconModeToByte = new java.math.BigInteger(commandSetSaveAndBeaconMode, 16).toByteArray();

    //로깅 모드로 변경
    public static String commandSetLoggingMode = "7E7E260001000000" + "01" + "7D7D";
    public static byte[] commandSetLoggingModeToByte = new java.math.BigInteger(commandSetLoggingMode, 16).toByteArray();

    //들어오는 데이터 예시
    //7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D
    public static String L = "tracker";

    public static final int REQUEST_CODE_TRACKING_ALARAM = 1000;

    //public static final String SERVER_URL_API ="http://bnrtracker.dreammug.com/_API/";
    public static final String SERVER_URL_API ="http:175.126.232.236/_API/";
    public static final int MILLISECOND_FOR_UPDATE_MINUTE_FOR_TEST = 1 * 60 * 1000;
    public static final int MILLISECOND_FOR_UPDATE_MINUTE = 10 * 60 * 1000;

    public static final String INTENT_KEY_FROM_NOTIFICATION = "INTENT_KEY_FROM_NOTIFICATION"; //노티눌러서 앱들어올때 구분하는값. 얘 담당 밸류가 true면 리사이클러뷰의 첫번째 아이템이 반짝임
    public static final int NOTI_ID_STATE_CHANGE = 0;
    public static final int NOTI_ID_ALWAYS_ON = 1;
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_FORMAT_LOG_FILE = "yyyy-MM-dd_HH:mm:ss";

    public static final String DIR_LOG_FILE = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Tracker";
    public static final String FILE_NAME_TEMP_DATA = "tmpDataWhenLostNetwork.txt";//인터넷 끊겼을때 전송할데이터 담고있는 텍스트파일명

    public static int lastShownNotificationId = 1;
}
