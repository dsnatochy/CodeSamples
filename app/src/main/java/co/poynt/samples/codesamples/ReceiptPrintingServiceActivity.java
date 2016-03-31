package co.poynt.samples.codesamples;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.poynt.os.services.v1.IPoyntReceiptPrintingService;
import co.poynt.os.services.v1.IPoyntReceiptPrintingServiceListener;

public class ReceiptPrintingServiceActivity extends Activity {
    private final static String TAG = "ReceiptPrintingActivity";
    @Bind(R.id.printImageBtn) Button printImageBtn;


    private IPoyntReceiptPrintingService receiptPrintingService;
    private IPoyntReceiptPrintingServiceListener receiptPrintingServiceListener = new IPoyntReceiptPrintingServiceListener.Stub(){
        @Override
        public void printQueued() throws RemoteException {
            Log.d(TAG, "Receipt queued");
        }
        @Override
        public void printFailed() throws RemoteException {
            Log.d(TAG, "Receipt printing failed");
        }
    };
    private ServiceConnection receiptServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            receiptPrintingService = IPoyntReceiptPrintingService.Stub.asInterface(iBinder);
            Log.d(TAG, "Receiptprintingservice connection established");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            receiptPrintingService = null;
            Log.d(TAG, "Receiptprintingservice connection disconnected");
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_printing_service);
        android.app.ActionBar actionBar = getActionBar();
        if (actionBar !=null ) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(IPoyntReceiptPrintingService.class.getName()), receiptServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(receiptServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_receipt_printing, menu);
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

        if (id==android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.printImageBtn)
    public void printImage(){
        String jobId = UUID.randomUUID().toString();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.receipt);
        if (receiptPrintingService != null){
            try {
                receiptPrintingService.printBitmap(jobId, bitmap, receiptPrintingServiceListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
