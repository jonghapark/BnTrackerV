package co.kr.bnr.bnrtrackerv.util;

import android.content.Context;

import com.google.gson.JsonObject;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import co.kr.bnr.bnrtrackerv.data.Common;
import co.kr.bnr.bnrtrackerv.data.DataReceive;
import cz.msebera.android.httpclient.Header;
import issc.data.DeviceInfo;

/**
 * Created by dingmac on 2017. 10. 14..
 */

public class Api {
    //서버에 데이터 발송
    public interface CallbackSendData {
        void callback(boolean isSuccess, String message);
    }
    public static void sendData(final Context con, DeviceInfo dataReceive, final CallbackSendData callback){
        if(!Util.checkInternetState(con)) {
            Util.myToast(con, "인터넷 연결상태를 확인해주세요");
            callback.callback(false, "인터넷 연결상태를 확인해주세요");
            return;
        }

        //final Dialog dialog = Util.dialLoading(con);
        RequestParams params = new RequestParams();

        try {
            params.put("isRegularData", "true");
            params.put("tra_datetime", dataReceive.getTime().replaceAll("\\.", ""));
            params.put("tra_temp", dataReceive.getTemperature());
            params.put("tra_humidity", dataReceive.getHumidity());
            params.put("tra_lat", dataReceive.lat);
            params.put("tra_lon", dataReceive.lon);
            params.put("de_number", "OPBT" + dataReceive.deviceName);
            params.put("tra_battery", dataReceive.getBattery());

            Util.myLog("sendData 파라미터>>"+params.toString());

            AsyncHttpClient client = new AsyncHttpClient();

            client.post(Common.SERVER_URL_API+"/saveData.php", params, new JsonHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    //Util.myToast(con, "죄송합니다. 데이터 전송에 실패 했습니다.(1)");
                    //dialog.dismiss();
                    Util.myLog("onFailure 오류메세지1:"+responseString);
                    Util.myLog("onFailure 오류메세지12:"+new Exception(throwable).getMessage());
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    //Util.myToast(con, "죄송합니다. 데이터 전송에 실패 했습니다.(2)");
                    Util.myLog("onFailure 오류메세지2:"+new Exception(throwable).getMessage());
                    Util.myLog("onFailure 오류메세지22:"+errorResponse.toString());
                    //dialog.dismiss();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    //Util.myToast(con, "죄송합니다. 데이터 전송에 실패 했습니다.(3)");
                    Util.myLog("onFailure 오류메세지3:"+new Exception(throwable).getMessage());
                    Util.myLog("onFailure 오류메세지33:"+errorResponse.toString());
                    //dialog.dismiss();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try{
                        Util.myLog("sendData 내용:"+response.toString());
                        boolean status_code = response.getBoolean("status_code");
                        String message = response.getString("message");
                        //성공이든 실패든 메세지에서 알아서 들어옴
                        //Util.myToast(con, message);

                        //요청 성공
                        if(status_code) {
                            callback.callback(true, message);
                        }
                        //실패
                        else {
                            callback.callback(false, message);
                        }

                        //dialog.dismiss();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                        //dialog.dismiss();
                    }
                    super.onSuccess(statusCode, headers, response);
                }

                @Override
                protected Object parseResponse(byte[] responseBody) throws JSONException {
                    return super.parseResponse(responseBody);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Util.myLog("onFailure 오류메세지33:"+e.toString());
            //Util.myToast(con, "죄송합니다. 데이터 전송에 실패 했습니다.(4)");
            //dialog.dismiss();
        }
    }
}