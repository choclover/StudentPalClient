package com.studentpal.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.studentpal.R;
import com.studentpal.app.ResourceManager;
import com.studentpal.engine.ClientEngine;
import com.studentpal.util.ActivityUtil;

public class AccessDeniedNotification extends Activity {
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(ActivityUtil.getString(R.string.operation_forbidden_warning));
    builder.setCancelable(false);  //cannot be dismissed by clicking BACK button
    builder.setPositiveButton(R.string.send_request,
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          //FIXME
//          Intent i = new Intent(AccessDeniedNotification.this,
//              AccessRequestForm.class);
//          i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//          startActivity(i);

          dismiss(dialog, false);
        }
    });
    builder.setNegativeButton(R.string.cancel,
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dismiss(dialog, true);
        }
    });

    Dialog alert = builder.create();
    alert.show();
  }

  private void dismiss(DialogInterface dialog, boolean backHome) {
    dialog.cancel();
    finish();

    if (backHome) {
      ActivityUtil.returnToHomeScreen(this);
    }
  }

}
