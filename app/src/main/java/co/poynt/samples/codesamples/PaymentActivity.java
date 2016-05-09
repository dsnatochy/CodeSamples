package co.poynt.samples.codesamples;

import android.app.Activity;
import android.app.backup.FullBackupDataOutput;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.poynt.api.model.Card;
import co.poynt.api.model.CardType;
import co.poynt.api.model.Customer;
import co.poynt.api.model.FundingSource;
import co.poynt.api.model.FundingSourceAccountType;
import co.poynt.api.model.FundingSourceType;
import co.poynt.api.model.Order;
import co.poynt.api.model.OrderAmounts;
import co.poynt.api.model.OrderItem;
import co.poynt.api.model.Product;
import co.poynt.api.model.Transaction;
import co.poynt.api.model.TransactionAction;
import co.poynt.api.model.TransactionAmounts;
import co.poynt.api.model.TransactionStatus;
import co.poynt.os.model.Intents;
import co.poynt.os.model.Payment;
import co.poynt.os.model.PaymentStatus;
import co.poynt.os.model.PoyntError;
import co.poynt.os.services.v1.IPoyntBusinessCustomersListListener;
import co.poynt.os.services.v1.IPoyntCustomerReadListener;
import co.poynt.os.services.v1.IPoyntCustomerService;
import co.poynt.os.services.v1.IPoyntOrderService;
import co.poynt.os.services.v1.IPoyntOrderServiceListener;
import co.poynt.os.services.v1.IPoyntTransactionService;
import co.poynt.os.services.v1.IPoyntTransactionServiceListener;
import co.poynt.samples.codesamples.utils.Util;

public class PaymentActivity extends Activity {

    private static final int AUTHORIZATION_CODE = 1993;
    // request code for payment service activity
    private static final int COLLECT_PAYMENT_REQUEST = 13132;
    private static final int DISPLAY_PAYMENT_REQUEST = 13133;
    private static final String TAG = "SampleActivity";
    private Gson gson;
    private IPoyntTransactionService mTransactionService;

    private IPoyntOrderService mOrderService;

    private Transaction transaction;

    Button chargeBtn;
    Button payOrderBtn;
    Button launchRegisterBtn;


    /*
     * Class for interacting with the OrderService
     */
    private ServiceConnection mOrderServiceConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "PoyntOrderService is now connected");
            // this gets an instance of the IRemoteInterface, which we can use to call on the service
            mOrderService = IPoyntOrderService.Stub.asInterface(service);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "PoyntOrderService has unexpectedly disconnected");
            mOrderService = null;
        }
    };
    private IPoyntOrderServiceListener saveOrderCallback = new IPoyntOrderServiceListener.Stub() {
        public void orderResponse(Order order, String s, PoyntError poyntError) throws RemoteException {
            Log.d("orderListener", "poyntError: " + (poyntError == null ? "" : poyntError.toString()));
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        android.app.ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        gson = new GsonBuilder().setPrettyPrinting().create();
        chargeBtn = (Button) findViewById(R.id.chargeBtn);
        chargeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPoyntPayment(100l, null);
                // txn 812fdd2e-5dfe-43fb-bbef-faed8ba514e7
            }
        });

        chargeBtn.setEnabled(true);

        payOrderBtn = (Button) findViewById(R.id.payOrderBtn);
        payOrderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Order order = Util.generateOrder();
                launchPoyntPayment(order.getAmounts().getNetTotal(), order);
            }
        });

        launchRegisterBtn = (Button) findViewById(R.id.launchRegisterBtn);
        // Only works if Poynt Register does not have an active order in progress
        launchRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Product product = Util.createProduct();
                Intent intent = new Intent();
                intent.setAction(Intents.ACTION_ADD_PRODUCT_TO_CART);
                intent.putExtra(Intents.INTENT_EXTRA_PRODUCT, product);
                intent.putExtra(Intents.INTENT_EXTRA_QUANTITY, 2.0f);
                startActivity(intent);
            }
        });


        bindService(new Intent(IPoyntOrderService.class.getName()), mOrderServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private class SaveOrderTask extends AsyncTask<Order, Void, Void> {
        protected Void doInBackground(Order... params) {
            Order order = params[0];
            String requestId = UUID.randomUUID().toString();
            if (mOrderService != null) {
                try {
                    mOrderService.createOrder(order, requestId, saveOrderCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mOrderServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_payment, menu);
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

    private IPoyntTransactionServiceListener mTransactionServiceListener = new IPoyntTransactionServiceListener.Stub() {
        public void onResponse(Transaction _transaction, String s, PoyntError poyntError) throws RemoteException {
            Gson gson = new Gson();
            Type transactionType = new TypeToken<Transaction>(){}.getType();
            String transactionJson = gson.toJson(transaction, transactionType);
            Log.d(TAG, "onResponse: " + transactionJson);

//            transaction = _transaction;
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    chargeBtn.setEnabled(true);
//                }
//            });
        }

        //@Override
        public void onLaunchActivity(Intent intent, String s) throws RemoteException {
            //do nothing
        }

        public void onLoginRequired() throws RemoteException {
            Log.d(TAG, "onLoginRequired called");
        }

//        @Override
//        public void onLaunchActivity(Intent intent, String s) throws RemoteException {
//
//        }

    };
    public void getTransaction(String txnId){
        try {

            mTransactionService.getTransaction(txnId, UUID.randomUUID().toString(),
                    mTransactionServiceListener);
//                    mTransactionService.getTransaction("7f1629ae-c7f0-4bb2-9693-233236b191f3", UUID.randomUUID().toString(),
//                            mTransactionServiceListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "binding to services...");
//        bindService(new Intent(IPoyntBusinessService.class.getName()),
//                mBusinessServiceConnection, Context.BIND_AUTO_CREATE);
//        bindService(new Intent(IPoyntSessionService.class.getName()),
//                mSessionConnection, Context.BIND_AUTO_CREATE);
//        bindService(new Intent(IPoyntSecondScreenService.class.getName()),
//                mSecondScreenConnection, Context.BIND_AUTO_CREATE);
//        bindService(new Intent(IPoyntCapabilityManager.class.getName()),
//                mCapabilityManagerConnection, Context.BIND_AUTO_CREATE);
//        bindService(new Intent(IPoyntTokenService.class.getName()),
//                mTokenServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(IPoyntTransactionService.class.getName()),
                mTransactionServiceConnection, Context.BIND_AUTO_CREATE);

    }
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "unbinding from services...");
//        unbindService(mBusinessServiceConnection);
//        unbindService(mSessionConnection);
//        unbindService(mSecondScreenConnection);
//        unbindService(mCapabilityManagerConnection);
//        unbindService(mTokenServiceConnection);
        unbindService(mTransactionServiceConnection);
    }
    private ServiceConnection mTransactionServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mTransactionService = IPoyntTransactionService.Stub.asInterface(iBinder);
        }
        public void onServiceDisconnected(ComponentName componentName) {
            mTransactionService = null;
        }
    };

    private void launchPoyntPayment(Long amount, Order order) {
        String currencyCode = NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode();
//        Transaction transaction = new Transaction();
//        transaction.setId(UUID.fromString("812fdd2e-5dfe-43fb-bbef-faed8ba514e7"));
//        FundingSource fs = new FundingSource();
//        Card card = new Card();
//        card.setType(CardType.VISA);
//        card.setNumberLast4("0010");
//        card.setCardHolderFirstName("John");
//        card.setCardHolderLastName("Doe");
//        card.setCardHolderFullName("John Doe");
//        fs.setCard(card);
//        fs.setType(FundingSourceType.CREDIT_DEBIT);
//        transaction.setFundingSource(fs);
//        //transaction.setAction(TransactionAction.AUTHORIZE);
//        TransactionAmounts amounts = new TransactionAmounts();
//        amounts.setCurrency("USD");
//        amounts.setTransactionAmount(3000l);
//        transaction.setAmounts(amounts);
//        transaction.setStatus(TransactionStatus.CAPTURED);
//        Log.d(TAG, "launchPoyntPayment " + transaction.getStatus().toString());;
        Payment payment = new Payment();
        String referenceId = UUID.randomUUID().toString();
        payment.setReferenceId(referenceId);

        payment.setCurrency(currencyCode);
       // payment.setTransactions(Arrays.asList(transaction));

        if (order != null){
            payment.setOrder(order);
            payment.setOrderId(order.getId().toString());
            payment.setTipAmount(1000l);
            payment.setAmount(order.getAmounts().getNetTotal());
        }else{
            // some random amount
            payment.setAmount(1200l);
            payment.setDisableTip(true);
            //payment.setTipAmount(500l);
        }


        //payment.setMultiTender(true);

        // start Payment activity for result
        try {
            Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
            collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, payment);
            startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REQUEST);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Poynt Payment Activity not found - did you install PoyntServices?", ex);
        }

        co.poynt.os.model.PrintedReceipt receipt;


//        String currencyCode = NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode();
//
//        Payment payment = new Payment();
//        String referenceId = UUID.randomUUID().toString();
//        payment.setReferenceId(referenceId);
//        payment.setAmount(amount);
//        payment.setCurrency(currencyCode);
//        payment.setMultiTender(true);
//
//        // start Payment activity for result
//        try {
//            Intent collectPaymentIntent = new Intent(Intents.ACTION_COLLECT_PAYMENT);
//            collectPaymentIntent.putExtra(Intents.INTENT_EXTRAS_PAYMENT, payment);
//            startActivityForResult(collectPaymentIntent, COLLECT_PAYMENT_REQUEST);
//        } catch (ActivityNotFoundException ex) {
//            Log.e(TAG, "Poynt Payment Activity not found - did you install PoyntServices?", ex);
//        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Received onActivityResult (" + requestCode + ")");
        // Check which request we're responding to
        if (requestCode == COLLECT_PAYMENT_REQUEST) {
            logData("Received onActivityResult from Payment Action");
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Payment payment = data.getParcelableExtra(Intents.INTENT_EXTRAS_PAYMENT);

                    //save order
                    if (payment.getOrder() != null) {
                        new SaveOrderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, payment.getOrder());
                    }

//                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    Gson gson = new Gson();
                    Type paymentType = new TypeToken<Payment>(){}.getType();
                    Log.d(TAG, gson.toJson(payment, paymentType));
                    for (Transaction t : payment.getTransactions()) {


                        getTransaction(t.getId().toString());
                        //Log.d(TAG, "Card token: " + t.getProcessorResponse().getCardToken());
                        FundingSourceAccountType fsAccountType = t.getFundingSource().getAccountType();
                        if (t.getFundingSource().getCard() != null){
                            Card c  = t.getFundingSource().getCard();
                            String numberMasked = c.getNumberMasked();
                            String approvalCode = t.getApprovalCode();
                            CardType cardType = c.getType();
                            switch (cardType){
                                case AMERICAN_EXPRESS:
                                    // amex
                                    break;
                                case VISA:
                                    // visa
                                    break;
                                case MASTERCARD:
                                    // MC
                                    break;
                                case DISCOVER:
                                    // discover
                                    break;
                            }
                        }

                    }

                    Log.d(TAG, "Received onPaymentAction from PaymentFragment w/ Status("
                            + payment.getStatus() + ")");
                    if (payment.getStatus().equals(PaymentStatus.COMPLETED)) {
                        logData("Payment Completed");
                    } else if (payment.getStatus().equals(PaymentStatus.AUTHORIZED)) {
                        logData("Payment Authorized");
                    } else if (payment.getStatus().equals(PaymentStatus.CANCELED)) {
                        logData("Payment Canceled");
                    } else if (payment.getStatus().equals(PaymentStatus.FAILED)) {
                        logData("Payment Failed");
                    } else if (payment.getStatus().equals(PaymentStatus.REFUNDED)) {
                        logData("Payment Refunded");
                    } else if (payment.getStatus().equals(PaymentStatus.VOIDED)) {
                        logData("Payment Voided");
                    } else {
                        logData("Payment Completed");
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                logData("Payment Canceled");
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            logData("Failed to validate discount");
        }
    }



    private Order generateOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        List<OrderItem> items = new ArrayList<OrderItem>();
        // create some dummy items to display in second screen
        items = new ArrayList<OrderItem>();
        OrderItem item1 = new OrderItem();
        // these are the only required fields for second screen display
        item1.setName("Item1");
        item1.setUnitPrice(100l);
        item1.setQuantity(1.0f);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        // these are the only required fields for second screen display
        item2.setName("Item2");
        item2.setUnitPrice(100l);
        item2.setQuantity(1.0f);
        items.add(item2);

        OrderItem item3 = new OrderItem();
        // these are the only required fields for second screen display
        item3.setName("Item3");
        item3.setUnitPrice(100l);
        item3.setQuantity(2.0f);
        items.add(item3);
        order.setItems(items);

        BigDecimal subTotal = new BigDecimal(0);
        for (OrderItem item : items) {
            BigDecimal price = new BigDecimal(item.getUnitPrice());
            price.setScale(2, RoundingMode.HALF_UP);
            price = price.multiply(new BigDecimal(item.getQuantity()));
            subTotal = subTotal.add(price);
        }

        OrderAmounts amounts = new OrderAmounts();
        amounts.setCurrency("USD");
        amounts.setSubTotal(subTotal.longValue());
        order.setAmounts(amounts);

        return order;
    }

    public void logData(final String data) {
        Log.d(TAG, data);
    }

    public void clearLog() {
        // do nothing
    }
}
