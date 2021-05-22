package com.urrecliner.blackbox.utility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class Permission extends AppCompatActivity {

    private static final int MULTIPLE_PERMISSION = 10235;

    public static void ask(Activity activity, Context context, PackageInfo info) {
        String [] permissions = info.requestedPermissions;
        if (!hasPermissions(context, permissions)) {
            ActivityCompat.requestPermissions(activity, permissions, MULTIPLE_PERMISSION);
        }
    }

    static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String []permissions, int[] grantResults) {
        if (requestCode == MULTIPLE_PERMISSION) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // 하나라도 거부한다면.
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setTitle("앱 권한");
                alertDialog.setMessage("해당 앱의 원할한 기능을 이용하시려면 애플리케이션 정보>권한> 에서 모든 권한을 허용해 주십시오");
                // 권한설정 클릭시 이벤트 발생
                alertDialog.setPositiveButton("권한설정",
                        (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                            startActivity(intent);
                            dialog.cancel();
                        });
                //취소
                alertDialog.setNegativeButton("취소",
                        (dialog, which) -> dialog.cancel());
                alertDialog.show();
            }
        }
    }
}
