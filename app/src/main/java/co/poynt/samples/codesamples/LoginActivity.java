package co.poynt.samples.codesamples;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import co.poynt.os.Constants;

public class LoginActivity extends AppCompatActivity {

    private AccountManager accountManager;
    private static final String TAG = LoginActivity.class.getName();
    private TextView userView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        accountManager = AccountManager.get(this);
        userView = (TextView) findViewById(R.id.userTextView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public void onLoginClicked(View view) {
        Bundle options = new Bundle();
        options.putBoolean("OPTION_VERIFY_ONLY", true);
        options.putString("LOCKPIN", "1234");
        Account account = new Account("Sam", "co.poynt");
        accountManager.getAuthToken(account,
                Constants.Accounts.POYNT_AUTH_TOKEN, options, LoginActivity.this,
                new OnUserLoginAttempt(), null);
    }

    public class OnUserLoginAttempt implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
            try {
                Bundle bundle = accountManagerFuture.getResult();
                String user = (String) bundle.get(AccountManager.KEY_ACCOUNT_NAME);
                userView.setText(user);
                Toast.makeText(LoginActivity.this, "User " + user + " successfully logged in", Toast.LENGTH_LONG).show();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                Toast.makeText(LoginActivity.this, "Login canceled", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            }
        }
    }
}
