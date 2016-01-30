package co.poynt.samples.codesamples;

import android.database.Cursor;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import co.poynt.os.contentproviders.orders.transactions.TransactionsColumns;

public class TransactionListActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "TxnListActivity";
    private ListView transactionListview;
    private static final int URL_LOADER = 0;
    private String sortOrder = TransactionsColumns.CREATEDAT + " DESC";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        transactionListview = (ListView) findViewById(R.id.transaction_list_view);

        getSupportLoaderManager().initLoader(URL_LOADER, null, this);

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_transaction_list, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id==android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {

        switch (loaderId) {
            case URL_LOADER:
                return new CursorLoader(
                        this,
                        TransactionsColumns.CONTENT_URI_TRXN_WITH_AMOUNT_AND_CARD,
                        null,
                        null,
                        null,
                        sortOrder
                );
        }
        return null;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        Log.d(TAG, "onLoadFinished ");
        TransactionFilterCursorWrapper cursorWrapper = new TransactionFilterCursorWrapper(data, true);
        TransactionCursorAdapter adapter = new TransactionCursorAdapter(this, cursorWrapper, false);
        transactionListview.setAdapter(adapter);
    }

    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
