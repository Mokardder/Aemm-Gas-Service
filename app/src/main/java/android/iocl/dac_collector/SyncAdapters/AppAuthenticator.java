package android.iocl.dac_collector.SyncAdapters;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.iocl.dac_collector.Services.FixOppoAutoKill;
import android.os.Bundle;
import android.text.TextUtils;

public class AppAuthenticator extends AbstractAccountAuthenticator {
    private final Context mContext;

    public AppAuthenticator(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures, Bundle options) {
        final Intent intent = new Intent(mContext, FixOppoAutoKill.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) {
        Bundle result = new Bundle();
        AccountManager am = AccountManager.get(mContext);
        String authToken = am.peekAuthToken(account, authTokenType);

        if (!TextUtils.isEmpty(authToken)) {
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
        } else {
            result.putParcelable(AccountManager.KEY_INTENT,
                    new Intent(mContext, FixOppoAutoKill.class));
        }
        return result;
    }

    // Other required methods
    @Override public Bundle editProperties(AccountAuthenticatorResponse r, String s) { return null; }
    @Override public Bundle confirmCredentials(AccountAuthenticatorResponse r, Account a, Bundle b) { return null; }
    @Override public String getAuthTokenLabel(String s) { return null; }
    @Override public Bundle updateCredentials(AccountAuthenticatorResponse r, Account a, String s, Bundle b) { return null; }
    @Override public Bundle hasFeatures(AccountAuthenticatorResponse r, Account a, String[] f) { return null; }
}