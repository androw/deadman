package com.androw.deadman;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.view.*;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import com.androw.utils.LicenseManager;
import com.androw.utils.LocationManager;

public class DeadMan extends Activity implements LicenseManager.OnCompleteListener {
    private FragmentManager fragmentManager;
    private boolean counting = false;

    private EditText number;
    private EditText duration;
    private CheckBox location;
    private SharedPreferences settings;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(new LocationManager(), "lm");
        ft.add(new LicenseManager(), "licensing");
        ft.commit();

        settings = getPreferences(Context.MODE_PRIVATE);

        location = (CheckBox) findViewById(R.id.location);
        duration = (EditText) findViewById(R.id.time);
        number = (EditText) findViewById(R.id.phone);

        location.setChecked(settings.getBoolean("location", false));
        //location.setEnabled(false);

        duration.setText(settings.getString("duration", "30"));
        number.setText(settings.getString("number", "0123456789"));

        final Button button = (Button) findViewById(R.id.button);
        final ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);

        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    String dur;
                    try {
                        dur = duration.getText().toString();
                    } catch (NullPointerException e) {
                        alertMessage(getString(R.string.invalid_time));
                        return true;
                    }
                    if (dur.equals("")) {
                        alertMessage(getString(R.string.invalid_time));
                        return true;
                    } else {
                        pb.setMax(Integer.parseInt(duration.getText().toString()));
                        counting = true;
                        new CountDownTimer(pb.getMax() * 1000, 1000) {

                            public void onTick(long millisUntilFinished) {
                                pb.setProgress((int) millisUntilFinished / 1000);
                            }

                            public void onFinish() {
                                pb.setProgress(0);
                                counting = false;
                            }
                        }.start();
                        return false;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (counting) {
                        sendSMS();
                    }
                    return false;
                }
                return false;
            }

        });

    }

    public void sendSMS() {
        final EditText number = (EditText) findViewById(R.id.phone);
        String phoneNumber = number.getText().toString();
        String message = settings.getString("message", getString(R.string.default_message));
        LocationManager lm = ((LocationManager)fragmentManager.findFragmentByTag("lm"));

        if (location.isChecked() && lm.servicesConnected()) {
            message = message + " Lat: " + lm.getLastLocation().getLatitude()+ " Lon: " +lm.getLastLocation().getLongitude();
        }

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }

    public void exitMessage(String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(DeadMan.this);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.setMessage(text);
        builder.create().show();
    }

    public void alertMessage(String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(DeadMan.this);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        builder.setMessage(text);
        builder.create().show();
    }

    public void setMessagePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DeadMan.this);
        LayoutInflater inflater = DeadMan.this.getLayoutInflater();
        builder.setTitle(R.string.set_message);

        View view = inflater.inflate(R.layout.message, null);
        final EditText et = (EditText) view.findViewById(R.id.edit_message);
        et.setText(settings.getString("message", getString(R.string.default_message)));

        builder.setView(view);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SharedPreferences.Editor editor = settings.edit();
                try {
                    editor.putString("message", et.getText().toString());
                } catch (NullPointerException e) {}

                // Commit the edits!
                editor.commit();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        builder.create().show();
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.setMessage:
                setMessagePopup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("location", location.isChecked());
        try {
            editor.putString("number", number.getText().toString());
        } catch (NullPointerException e) {}
        try {
            editor.putString("duration", duration.getText().toString());
        } catch (NullPointerException e) {}

        // Commit the edits!
        editor.commit();
    }

    public void onComplete() {
        LicenseManager licm = ((LicenseManager) fragmentManager.findFragmentByTag("licensing"));
        licm.doCheck();
    }
}
