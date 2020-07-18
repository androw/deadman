package com.androw.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.androw.deadman.*;
import com.google.android.gms.location.LocationClient;
import com.google.android.vending.licensing.*;
import com.google.android.vending.licensing.R;

/**
 * Created by Androw on 24/02/2014.
 */
public class LicenseManager extends Fragment {
    private static final byte[] SALT = new byte[]{
            56, 89, 16, 66, 102, 102, 112, 67, 58, 19, 116, 102, 1, 27, 99, 24, 75, 4, 7, 96
    };
    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgukvt7XCqTHS9s7Lms4PFH2zFy0dG/Ju++np22aPo1iqFSz9CIfOtxstiiz2naok6Xpfj+6l7dVcwR3V3s5uuaLTQhubu7twcB0mw+a8VYnQc1fWsqGjuwbUquAxuz/ozAO6ERwiLFZW3Fv08CZa2pB37ifWyv9p0/osO8+9cYnlXpwxeV1cZSwCKOuqaK+J7sTGRV8ouinBSeHqgyW2q9g1gMqZY221VyVpiEGJ1/zw5Ngy92Th01V8D9tF1yMHkCxn6CyGXntsyjuFh3Cx2FTi+DU0kobn6YVwd+XGNB9X9R0buOG3HZJIoc7zc8Qq2iPxPTdR6QnDlOoOlfSzywIDAQAB";

    private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        String deviceId = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
        mChecker = new LicenseChecker(
                getActivity(), new ServerManagedPolicy(getActivity(),
                new AESObfuscator(SALT, getActivity().getPackageName(), deviceId)),
                BASE64_PUBLIC_KEY
        );
        mListener.onComplete();
    }

    private class MyLicenseCheckerCallback implements LicenseCheckerCallback {
        public void allow(int reason) {
        }

        public void dontAllow(int reason) {
            if (getActivity().isFinishing()) {
                return;
            }

            if (reason == Policy.RETRY) {
                String result = String.format(getString(com.androw.deadman.R.string.fail_licensed), reason);
                exitMessage(result);
            } else {
                String result = String.format(getString(com.androw.deadman.R.string.not_licensed), reason);
                exitMessage(result);
            }
        }

        public void applicationError(int errorCode) {
            if (getActivity().isFinishing()) {
                return;
            }
            String result = String.format(getString(com.androw.deadman.R.string.fail_licensed), errorCode);
            exitMessage(result);
        }
    }

    public void exitMessage(String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        builder.setPositiveButton(com.androw.deadman.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                getActivity().finish();
            }
        });
        builder.setMessage(text);
        builder.create().show();
    }

    public void doCheck() {
        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mChecker.onDestroy();
    }

    public static interface OnCompleteListener {
        public abstract void onComplete();
    }

    private OnCompleteListener mListener;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnCompleteListener) activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCompleteListener");
        }
    }
}