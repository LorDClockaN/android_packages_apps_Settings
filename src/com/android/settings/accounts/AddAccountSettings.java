/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.accounts;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

/**
 * Entry point Actiivty for account setup. Works as follows
 *
 * 1) When the other Activities launch this Activity, it launches {@link ChooseAccountActivity}
 *    without showing anything.
 * 2) After receiving an account type from ChooseAccountActivity, this Activity launches the
 *    account setup specified by AccountManager.
 * 3) After the account setup, this Activity finishes without showing anything.
 *
 * Note:
 * Previously this Activity did what {@link ChooseAccountActivity} does right now, but we
 * currently delegate the work to the other Activity. When we let this Activity do that work, users
 * would see the list of account types when leaving this Activity, since the UI is already ready
 * when returning from each account setup, which doesn't look good.
 */
public class AddAccountSettings extends Activity {
    /**
     * 
     */
    private static final String KEY_ADD_CALLED = "AddAccountCalled";

    /**
     * Extra parameter to identify the caller. Applications may display a
     * different UI if the calls is made from Settings or from a specific
     * application.
     */
    private static final String KEY_CALLER_IDENTITY = "pendingIntent";

    private static final String TAG = "AccountSettings";

    /* package */ static final String EXTRA_SELECTED_ACCOUNT = "selected_account";

    private static final int CHOOSE_ACCOUNT_REQUEST = 1;

    private PendingIntent mPendingIntent;

    private AccountManagerCallback<Bundle> mCallback = new AccountManagerCallback<Bundle>() {
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                Bundle bundle = future.getResult();
                bundle.keySet();
                setResult(RESULT_OK);

                if (mPendingIntent != null) {
                    mPendingIntent.cancel();
                }

                if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "account added: " + bundle);
            } catch (OperationCanceledException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "addAccount was canceled");
            } catch (IOException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "addAccount failed: " + e);
            } catch (AuthenticatorException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "addAccount failed: " + e);
            } finally {
                finish();
            }
        }
    };

    private boolean mAddAccountCalled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mAddAccountCalled = savedInstanceState.getBoolean(KEY_ADD_CALLED);
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "restored");
        }

        if (mAddAccountCalled) {
            // We already called add account - maybe the callback was lost.
            finish();
            return;
        }
        final String[] authorities =
                getIntent().getStringArrayExtra(AccountPreferenceBase.AUTHORITIES_FILTER_KEY);
        final String[] accountTypes =
                getIntent().getStringArrayExtra(AccountPreferenceBase.ACCOUNT_TYPES_FILTER_KEY);
        final Intent intent = new Intent(this, ChooseAccountActivity.class);
        if (authorities != null) {
            intent.putExtra(AccountPreferenceBase.AUTHORITIES_FILTER_KEY, authorities);
        }
        if (accountTypes != null) {
            intent.putExtra(AccountPreferenceBase.ACCOUNT_TYPES_FILTER_KEY, accountTypes);
        }
        startActivityForResult(intent, CHOOSE_ACCOUNT_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case CHOOSE_ACCOUNT_REQUEST:
            if (resultCode == RESULT_CANCELED) {
                setResult(resultCode);
                finish();
                return;
            }
            // Go to account setup screen. finish() is called inside mCallback.
            addAccount(data.getStringExtra(EXTRA_SELECTED_ACCOUNT));
            break;
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_ADD_CALLED, mAddAccountCalled);
        if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "saved");
    }

    private void addAccount(String accountType) {
        Bundle addAccountOptions = new Bundle();
        mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(), 0);
        addAccountOptions.putParcelable(KEY_CALLER_IDENTITY, mPendingIntent);
        AccountManager.get(this).addAccount(
                accountType,
                null, /* authTokenType */
                null, /* requiredFeatures */
                addAccountOptions,
                this,
                mCallback,
                null /* handler */);
        mAddAccountCalled  = true;
    }
}
