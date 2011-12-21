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
    builder.setMessage(ResourceManager.RES_STR_OPERATION_DENIED);
    builder.setCancelable(false);  //cannot be dismissed by clicking BACK button
    builder.setPositiveButton(ResourceManager.RES_STR_SENDREQUEST,
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          //TODO  -- send request to admin
//          Intent i = new Intent(AccessDeniedNotification.this,
//              AccessRequestForm.class);
//          i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//          startActivity(i);

          dismiss(dialog, false);
        }
    });
    builder.setNegativeButton(ResourceManager.RES_STR_CANCEL,
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
