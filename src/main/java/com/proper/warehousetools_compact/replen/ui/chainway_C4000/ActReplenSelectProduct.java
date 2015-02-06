package com.proper.warehousetools_compact.replen.ui.chainway_C4000;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.proper.data.diagnostics.LogEntry;
import com.proper.data.helpers.MyCustomNamingStrategy;
import com.proper.utils.BinQuantitySorted;
import com.proper.warehousetools_compact.R;
import com.proper.warehousetools_compact.binmove.BaseScannerActivity;
import com.proper.warehousetools_compact.replen.ui.ActReplenManager;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.*;

/**
 * Created by Lebel on 04/09/2014.
 */
public class ActReplenSelectProduct extends BaseScannerActivity {
    private BinResponse binResponse = new BinResponse();
    private BarcodeResponse bcResponse = null;
    private ProductResponse thisProduct = new ProductResponse();
    private List<ProductBinResponse> foundList =  new ArrayList<ProductBinResponse>();
    protected List<ProductResponse> responseList =  new ArrayList<ProductResponse>();
    private EditText mReception;
    private TextView txtPalate;
    private TextView txtInto;
    private Button btnScan;
    private Button btnExit;
    private Button btnEnterBinCode;
    private WebServiceProductTask wsTask;

    public String getScanInput() {
        return scanInput;
    }

    public void setScanInput(String scanInput) {
        this.scanInput = scanInput;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lyt_replen_selectproduct);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.flat_button_palebrown));
        getSupportActionBar().setTitle("Scan Product");

        Bundle extra = getIntent().getExtras();
        if (extra == null) throw new RuntimeException("onCreate: Bundled Extra cannot be null!, Line: 63");
        binResponse = (BinResponse) extra.getSerializable("RESPONSE_EXTRA");

        txtInto = (TextView) this.findViewById(R.id.txtvReplenScanProductIntro);
        txtPalate = (TextView) this.findViewById(R.id.txtvReplenScanProductPalate);
        btnScan = (Button) this.findViewById(R.id.bnReplenScanProductPerformScan);
        btnExit = (Button) this.findViewById(R.id.bnExitActReplenScanProduct);
        btnEnterBinCode = (Button) this.findViewById(R.id.bnEnterBinReplenSelectProduct);
        mReception = (EditText) this.findViewById(R.id.etxtReplenScanProductBarcode);

        mReception.addTextChangedListener(new TextChanged());
        mReception.setEnabled(false);                   ///  Disable it upon initiation
        btnScan.setOnClickListener(new ClickEvent());
        btnExit.setOnClickListener(new ClickEvent());
        btnEnterBinCode.setOnClickListener(new ClickEvent());
        txtPalate.setText(binResponse.getRequestedBinCode());

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == 1){
                    setScanInput(msg.obj.toString());   //>>>>>>>>>>>>>>>   set scanned object  <<<<<<<<<<<<<<<<
                    if (!mReception.getText().toString().equalsIgnoreCase("")) {
                        mReception.setText("");
                        mReception.setText(getScanInput());
                    }
                    else{
                        mReception.setText(getScanInput());
                    }
                    appContext.playSound(1);
                    btnScan.setEnabled(true);
                }
            }
        };

    }

    @Override
    protected void onPause() {
        threadStop = true;
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (isBarcodeOpened) {
            mInstance.close();
        }
        //soundPool.release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0) {
            mReception.setText("");  //clear the textbox
            if (wsTask != null && wsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
                wsTask.cancel(true);
            }
            refreshActivity();
        }
        if (resultCode == RESULT_OK) {
            mReception.setText("");  //clear the textbox
            if (wsTask != null && wsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
                wsTask.cancel(true);
            }
            refreshActivity();
//            Intent i  = new Intent();
//            setResult(RESULT_OK, i);
//            this.finish();
        }
    }

    class TextChanged implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            // TODO Auto-generated method stub

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s != null && !s.toString().equalsIgnoreCase("")) {
                if (inputByHand == 0) {
                    int acceptable[] = {12,13,14};
                    String eanCode = s.toString().trim();
                    if (eanCode.length() > 0 && !(Arrays.binarySearch(acceptable, eanCode.length()) == -1)) {
                        //Authenticate current user, Build Message only when all these conditions are right then proceed with asyncTask
                        currentUser = currentUser != null ? currentUser : authenticator.getCurrentUser();   //Gets currently authenticated user
                        if (currentUser != null) {
                            if (binResponse != null) {
                                if (binResponse.getMatchedProducts() > 0) {
                                    int found = 0;
                                    String newEan = barcodeHelper.formatBarcode(eanCode);
                                    foundList = new ArrayList<ProductBinResponse>();
                                    for(ProductBinResponse prod : binResponse.getProducts()) {
                                        if (prod.getEAN().equalsIgnoreCase(newEan)) {
                                            found ++;
                                            foundList.add(prod);
                                        }
                                    }
                                    if (found == 0) {
                                        appContext.playSound(2);
                                        Vibrator vib = (Vibrator) ActReplenSelectProduct.this.getSystemService(Context.VIBRATOR_SERVICE);
                                        // Vibrate for 500 milliseconds
                                        vib.vibrate(2000);
                                        String mMsg = "There is no such product in this Bin \nPlease re-scan";
                                        AlertDialog.Builder builder = new AlertDialog.Builder(ActReplenSelectProduct.this);
                                        builder.setMessage(mMsg)
                                                .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        refreshActivity();
                                                    }
                                                });
                                        builder.show();
                                    } else {
                                        //TODO - Determine if the quantity > zero
                                        if (foundList.get(0).getQtyInBin() > 0) {
                                            String msg = String.format("{\"UserId\":\"%s\", \"UserCode\":\"%s\",\"Barcode\":\"%s\"}",
                                                    currentUser.getUserId(), currentUser.getUserCode(), foundList.get(0).getBarcode());
                                            today = new java.sql.Timestamp(utilDate.getTime());
                                            thisMessage.setSource(deviceIMEI);
                                            thisMessage.setMessageType("BarcodeQuery");
                                            thisMessage.setIncomingStatus(1); //default value
                                            thisMessage.setIncomingMessage(msg);
                                            thisMessage.setOutgoingStatus(0);   //default value
                                            thisMessage.setOutgoingMessage("");
                                            thisMessage.setInsertedTimeStamp(today);
                                            thisMessage.setTTL(100);    //default value

                                            if (wsTask != null) {
                                                wsTask.cancel(true);
                                            }
                                            wsTask = new WebServiceProductTask();
                                            wsTask.execute(thisMessage);
                                        }else{
                                            // alert that qty in bi8n is less than zero
                                            String mMsg = String.format("This product's quantity of (%s) is less than 1.", foundList.get(0).getQtyInBin());
                                            AlertDialog.Builder builder = new AlertDialog.Builder(ActReplenSelectProduct.this);
                                            builder.setMessage(mMsg)
                                                    .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            refreshActivity();
                                                        }
                                                    });
                                            builder.show();
                                        }
                                    }
                                }
                            }
                        } else {
                            appContext.playSound(2);
                            Vibrator vib = (Vibrator) ActReplenSelectProduct.this.getSystemService(Context.VIBRATOR_SERVICE);
                            // Vibrate for 500 milliseconds
                            vib.vibrate(2000);
                            String mMsg = "User not Authenticated \nPlease login";
                            AlertDialog.Builder builder = new AlertDialog.Builder(ActReplenSelectProduct.this);
                            builder.setMessage(mMsg)
                                    .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //do nothing
                                        }
                                    });
                            builder.show();
                        }
                    } else {
                        new AlertDialog.Builder(ActReplenSelectProduct.this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_EAN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                refreshActivity();
                            }
                        }).show();
                    }
                }
            }
        }
    }

    class ClickEvent implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            boolean bContinuous = true;
            int iBetween = 0;
            if (v == btnExit) {
                if (readThread != null && readThread.isInterrupted() == false) {
                    readThread.interrupt();
                }
                Intent resultIntent = new Intent();
                if (backPressedParameter != null && !backPressedParameter.equalsIgnoreCase("")) {
                    setResult(1, resultIntent);
                } else {
                    setResult(RESULT_OK, resultIntent);
                }
                ActReplenSelectProduct.this.finish();
            }
            else if(v == btnScan)
            {
                btnScan.setEnabled(false);
                mReception.requestFocus();
                if (threadStop) {
                    Log.i("Reading", "My Barcode " + readerStatus);
                    readThread = new Thread(new GetBarcode(bContinuous, iBetween));
                    readThread.setName("Single Barcode ReadThread");
                    readThread.start();
                }else {
                    threadStop = true;
                }
                btnScan.setEnabled(true);
            } else if(v == btnEnterBinCode) {
                if (inputByHand == 0) {
                    turnOnInputByHand();
                    if (!mReception.isEnabled()) mReception.setEnabled(true);
                    mReception.setText("");
                    showSoftKeyboard();
                } else {
                    turnOffInputByHand();
                    if (mReception.isEnabled()) mReception.setEnabled(false);
                    //mReception.removeTextChangedListener();
                }
            }
        }
    }

    private void showSoftKeyboard() {
        InputMethodManager imm =(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null){
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
        }
    }

    private void turnOnInputByHand(){
        this.inputByHand = 1;    //Turn On Input by Hand
        this.btnScan.setEnabled(false);
        paintByHandButtons();
    }

    private void turnOffInputByHand(){
        this.inputByHand = 0;    //Turn On Input by Hand
        this.btnScan.setEnabled(true);  //
        setScanInput(mReception.getText().toString());
        if (!getScanInput().isEmpty()) {
            mReception.setText(getScanInput()); // just to trigger text changed
        }
        paintByHandButtons();
    }

    private void paintByHandButtons() {
        final String byHand = "ByHand";
        final String finish = "Finish";
        if (inputByHand == 0) {
            btnEnterBinCode.setText(byHand);
        } else {
            btnEnterBinCode.setText(finish);
        }
    }

    private class GetBarcode implements Runnable {

        private boolean isContinuous = false;
        String barCode = "";
        private long sleepTime = 1000;
        Message msg = null;

        public GetBarcode(boolean isContinuous) {
            this.isContinuous = isContinuous;
        }

        public GetBarcode(boolean isContinuous, int sleep) {
            this.isContinuous = isContinuous;
            this.sleepTime = sleep;
        }

        @Override
        public void run() {

            do {
                barCode = mInstance.scan();

                Log.i("MY", "barCode " + barCode.trim());

                msg = new Message();

                if (barCode == null || barCode.isEmpty()) {
                    msg.arg1 = 0;
                    msg.obj = "";
                } else {
                    msg.what = 1;
                    msg.obj = barCode;
                }

                handler.sendMessage(msg);

                if (isContinuous) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } while (isContinuous && !threadStop);

        }

    }

    public void refreshActivity() {
        if (!mReception.getText().toString().equalsIgnoreCase("")) mReception.setText("");
        if (!btnScan.isEnabled()) btnScan.setEnabled(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KEY_SCAN) {
            if (event.getRepeatCount() == 0) {
                boolean bContinuous = true;
                int iBetween = 0;
                mReception.requestFocus();
                if (threadStop) {
                    Log.i("Reading", "My Barcode " + readerStatus);
                    readThread = new Thread(new GetBarcode(bContinuous, iBetween));
                    readThread.setName("Single Barcode ReadThread");
                    readThread.start();
                }else {
                    threadStop = true;
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (readThread != null && readThread.isInterrupted() == false) {
            readThread.interrupt();
        }
        Intent resultIntent = new Intent();
        if (backPressedParameter != null && !backPressedParameter.equalsIgnoreCase("")) {
            setResult(1, resultIntent);
        } else {
            setResult(RESULT_OK, resultIntent);
        }
        //super.onBackPressed();
        ActReplenSelectProduct.this.finish();
    }

    private class WebServiceProductTask extends AsyncTask<com.proper.messagequeue.Message, Void, Void>{
        protected ProgressDialog xDialog;

        @Override
        protected void onPreExecute() {
            startTime = new Date().getTime(); //get start time
            xDialog = new ProgressDialog(ActReplenSelectProduct.this);
            CharSequence message = "Working hard...contacting webservice...";
            CharSequence title = "Please Wait";
            xDialog.setCancelable(true);
            xDialog.setCanceledOnTouchOutside(false);
            xDialog.setMessage(message);
            xDialog.setTitle(title);
            xDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            xDialog.show();
        }

        @Override
        protected Void doInBackground(com.proper.messagequeue.Message... msg) {
            Thread.currentThread().setName("MyGetProductsTask");
            responseList = new ArrayList<ProductResponse>();
            bcResponse = new BarcodeResponse();
            try {
                String response = resolver.resolveMessageQuery(thisMessage);
                response = responseHelper.refineResponse(response);
                if (response.contains("not recognised")) {
                    //manually error trap this error
                    String iMsg = "The Response object return null due to msg queue not recognising your improper request.";
                    today = new java.sql.Timestamp(utilDate.getTime());
                    LogEntry log = new LogEntry(1L, ApplicationID, "ActReplenSelectProduct - WebServiceTask - Line:655", deviceIMEI, RuntimeException.class.getSimpleName(), iMsg, today);
                    logger.log(log);
                    throw new RuntimeException("The bin you have scanned have not been recognised. Please check and scan again");
                }else {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.setPropertyNamingStrategy(new MyCustomNamingStrategy());
                    bcResponse = mapper.readValue(response, BarcodeResponse.class);
                    responseList = bcResponse.getProducts();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                today = new java.sql.Timestamp(utilDate.getTime());
                LogEntry log = new LogEntry(1L, ApplicationID, "ActReplenSelectProduct - doInBackground", deviceIMEI, ex.getClass().getSimpleName(), ex.getMessage(), today);
                logger.log(log);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void pResponse) {
            if (xDialog != null && xDialog.isShowing()) xDialog.dismiss();
            if (responseList != null && responseList.size() != 0) {
                //TODO - Find the primary location & pass it to the next activity
                //If more that one supplier (SuppCat) then check if 1st primary loc is full then suggest the 2nd if better
                final BinQuantitySorted sorter = new BinQuantitySorted();
                List<Bin> foundBins = new ArrayList<Bin>();
                final int prodFound = responseList.size();
                if (prodFound > 0) {
                    if (prodFound == 1) {
                        for (Bin bin : responseList.get(0).getBins()) {
                            if (bin.getBinCode().substring(0, 1).equalsIgnoreCase("1")) {
                                foundBins.add(bin);
                            }
                        }
                        Intent i = new Intent(ActReplenSelectProduct.this, ActReplenManager.class);
                        //i.putExtra("DATA_EXTRA", responseList)
                        i.putExtra("PRODUCT_EXTRA", (java.io.Serializable) foundList);
                        i.putExtra("SOURCE_EXTRA", binResponse.getRequestedBinCode());
                        i.putExtra("PRIMARY_EXTRA", (java.io.Serializable) foundBins);
                        startActivityForResult(i, RESULT_OK);
                    }else {
                        //do for multiple products (same barcode but different SuppCat)
                        for (ProductResponse prod : responseList) {
                            for (Bin bin : prod.getBins()) {
                                if (bin.getBinCode().substring(0, 1).equalsIgnoreCase("1")) {
                                    foundBins.add(bin);
                                }
                            }
                        }
                        //Sort by giving us a bin with the lowest quantity
                        Collections.sort(foundBins, sorter);

                        Intent i = new Intent(ActReplenSelectProduct.this, ActReplenManager.class);
                        i.putExtra("PRODUCT_EXTRA", (java.io.Serializable) foundList);
                        i.putExtra("SOURCE_EXTRA", binResponse.getRequestedBinCode());
                        i.putExtra("PRIMARY_EXTRA", (java.io.Serializable) foundBins);
                        startActivityForResult(i, RESULT_OK);
                    }
                } else {
                    //Yell murder, notify that product scanned has yielded no result, then clear activity
                    appContext.playSound(2);
                    Vibrator vib = (Vibrator) ActReplenSelectProduct.this.getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    vib.vibrate(2000);
                    String mMsg = "The product scanned is not suppose to exist in this bin\nPlease verify then re-scan";
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActReplenSelectProduct.this);
                    builder.setMessage(mMsg)
                            .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //do nothing
                                }
                            });
                    builder.show();
                }
            }else {
                appContext.playSound(2);
                Vibrator vib = (Vibrator) ActReplenSelectProduct.this.getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                vib.vibrate(2000);
                String mMsg = "The product query has return no result\nPlease verify then re-scan";
                AlertDialog.Builder builder = new AlertDialog.Builder(ActReplenSelectProduct.this);
                builder.setMessage(mMsg)
                        .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do nothing
                            }
                        });
                builder.show();
            }
            refreshActivity();
        }

        @Override
        protected void onCancelled() {
            mReception.setText("");
        }
    }
}