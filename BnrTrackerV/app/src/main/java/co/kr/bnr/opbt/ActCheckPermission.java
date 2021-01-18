package co.kr.bnr.opbt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.List;

/**
 * 앱 사용에 필요한 권한 체크 액티비티
 * */
public class ActCheckPermission extends Activity {
    String[] PERMISSTIONS_FOR_THIS_ACTIVITY = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.act_main);
        //ButterKnife.bind(this);

        context = this;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        checkPermission();
    }


    boolean isShowingDialog = false;
    public void showSettingsAlert() {
        System.out.println(this.isShowingDialog);
        if (!this.isShowingDialog) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("GPS is settings");
            alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
            alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    isShowingDialog = false;
                    startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    isShowingDialog = false;
                    dialog.cancel();
                }
            });
            alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                    if (keyCode == 4) {
                        isShowingDialog = false;
                        arg0.dismiss();
                    }
                    return true;
                }
            });
            alertDialog.show();
            this.isShowingDialog = true;
        }
    }

    void checkPermission() {
        TedPermission.with(context)
                .setPermissionListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        //showSettingsAlert();

                        startActivity(new Intent(context, ActMainBeacon.class));
                        finish();
                    }

                    @Override
                    public void onPermissionDenied(List<String> deniedPermissions) {
                        checkPermission();//허용하기 전까지 무한반복이다...
                        Log.e("test", "권한 미통과");
                    }
                })
                .setDeniedMessage("앱을 실행시키는데 필요한 권한이 거부 되었습니다. 설정에서 허용을 눌러주세요.")
                .setPermissions(PERMISSTIONS_FOR_THIS_ACTIVITY)
                .check();
    }
}
