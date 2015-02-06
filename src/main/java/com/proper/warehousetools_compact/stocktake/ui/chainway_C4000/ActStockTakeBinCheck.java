package com.proper.warehousetools_compact.stocktake.ui.chainway_C4000;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.*;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import com.proper.data.diagnostics.LogEntry;
import com.proper.data.stocktake.StockTakeBinResponse;
import com.proper.utils.BinQuantitySorted;
import com.proper.warehousetools_compact.R;
import com.proper.warehousetools_compact.binmove.BaseFragmentActivity;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Created by Lebel on 20/01/2015.
 * Scan a bin code to return some Stock take properties
 */
public class ActStockTakeBinCheck extends BaseFragmentActivity {
    private Button btnEnterBin, btnScan, btnExit;
    private EditText txtBin;
    private String scanInput;
    private StockTakeBinQueryTask binQryTask;
    private StockTakeBinResponse qryResponse = null;
    private String currentBin = "";

    public String getScanInput() {
        return scanInput;
    }

    public void setScanInput(String scanInput) {
        this.scanInput = scanInput;
    }

    public String getCurrentBin() {
        return currentBin;
    }

    public void setCurrentBin(String currentBin) {
        this.currentBin = currentBin;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lyt_stocktake_bincheck);
        btnScan = (Button) this.findViewById(R.id.bnSTBScan);
        btnEnterBin = (Button) this.findViewById(R.id.bnSTBEnterBin);
        btnExit = (Button) this.findViewById(R.id.bnExitActStockTakeBinCheck);
        txtBin = (EditText) this.findViewById(R.id.etxtSTBBin);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        btnEnterBin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        txtBin.addTextChangedListener(new TextChanged());

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == 1){
                    setScanInput(msg.obj.toString());   //>>>>>>>>>>>>>>>   set scanned object  <<<<<<<<<<<<<<<<
                    if (!txtBin.getText().toString().equalsIgnoreCase("")) {
                        txtBin.setText("");
                        txtBin.setText(getScanInput());
                    }
                    else{
                        txtBin.setText(getScanInput());
                    }
                    appContext.playSound(1);
                    btnScan.setEnabled(true);
                }
            }
        };
    }

    private void buttonClicked(View v) {
        boolean isContinuous = false;   //continuous scan feature?
        int iBetween = 0;
        if (v == btnEnterBin) {
            if (inputByHand == 0) {
                turnOnInputByHand();
                if (!txtBin.isEnabled()) txtBin.setEnabled(true);
                txtBin.setText("");
                showSoftKeyboard();
            } else {
                turnOffInputByHand();
            }
        }

        if (v == btnScan) {
            btnScan.setEnabled(false);
            txtBin.requestFocus();
            if (threadStop) {
                Log.i("Reading", "My Barcode " + readerStatus);
                readThread = new Thread(new GetBarcode(isContinuous, iBetween));
                readThread.setName("Single Barcode ReadThread");
                readThread.start();
            }else {
                threadStop = true;
            }
            btnScan.setEnabled(true);
        }

        if (v == btnExit) {
            Intent i = new Intent();
            setResult(RESULT_OK, i);
            finish();
        }
    }

    class TextChanged implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s != null && !s.toString().equalsIgnoreCase("")) {
                if (inputByHand == 0) {
                    String binCode = s.toString().trim();
                    if (binCode.length() == 5) {
                        setCurrentBin(binCode);
                        //Authenticate current user, Build Message only when all these conditions are right then proceed with asyncTask
                        currentUser = currentUser != null ? currentUser : authenticator.getCurrentUser();  //Gets currently authenticated user
                        if (currentUser != null) {
                            today = new java.sql.Timestamp(utilDate.getTime());
                            String msg = String.format("{\"UserId\":\"%s\", \"UserCode\":\"%s\", \"BinCode\":\"%s\"}",
                                    currentUser.getUserId(), currentUser.getUserCode(), binCode);

                            thisMessage.setSource(deviceIMEI);
                            thisMessage.setMessageType("GetStockTakeBin");
                            thisMessage.setIncomingStatus(1); //default value
                            thisMessage.setIncomingMessage(msg);
                            thisMessage.setOutgoingStatus(0);   //default value
                            thisMessage.setOutgoingMessage("");
                            thisMessage.setInsertedTimeStamp(today);
                            thisMessage.setTTL(100);    //default value
                            binQryTask = new StockTakeBinQueryTask();
                            binQryTask.execute(thisMessage);  //executes both -> Send Queue Directly AND Send queue to Service
                        } else {
                            appContext.playSound(2);
                            Vibrator vib = (Vibrator) ActStockTakeBinCheck.this.getSystemService(Context.VIBRATOR_SERVICE);
                            // Vibrate for 500 milliseconds
                            vib.vibrate(2000);
                            String mMsg = "User not Authenticated \nPlease login";
                            AlertDialog.Builder builder = new AlertDialog.Builder(ActStockTakeBinCheck.this);
                            builder.setMessage(mMsg)
                                    .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //do nothing
                                        }
                                    });
                            builder.show();
                        }
                    }
                }
            }
        }
    }

    private void showSoftKeyboard() {
        InputMethodManager imm =(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //inputMethodManager.toggleSoftInputFromWindow(linearLayout.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
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
        setScanInput(txtBin.getText().toString());
        if (!getScanInput().isEmpty()) {
            txtBin.setText(getScanInput()); // just to trigger text changed
        }
        paintByHandButtons();
    }

    private void paintByHandButtons() {
        final String byHand = "ByHand";
        final String finish = "Finish";
        if (inputByHand == 0) {
            btnEnterBin.setText(byHand);
        } else {
            btnEnterBin.setText(finish);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KEY_SCAN) {
            if (event.getRepeatCount() == 0) {
                boolean bContinuous = true;
                int iBetween = 0;
                txtBin.requestFocus();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (binQryTask != null) {
            binQryTask.cancel(true);
            binQryTask = null;
        }
        if (!btnScan.isEnabled()) btnScan.setEnabled(true);
    }

    private class StockTakeBinQueryTask extends AsyncTask<com.proper.messagequeue.Message, Void, StockTakeBinResponse> {
        protected ProgressDialog wsDialog;

        @Override
        protected void onPreExecute() {
            txtBin.setText("");     //empty control
            wsDialog = new ProgressDialog(ActStockTakeBinCheck.this);
            CharSequence message = "Working hard...sending queue [directly] [to webservice]...";
            CharSequence title = "Please Wait";
            wsDialog.setCancelable(true);
            wsDialog.setCanceledOnTouchOutside(false);
            wsDialog.setMessage(message);
            wsDialog.setTitle(title);
            wsDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            wsDialog.show();
        }

        @Override
        protected StockTakeBinResponse doInBackground(com.proper.messagequeue.Message... msg) {
            Thread.currentThread().setName("StockTakeBinResponseAsyncTask");
            qryResponse = new StockTakeBinResponse();
            try {
                String response = resolver.resolveMessageQuery(msg[0]);
                //String response = "{\"RequestedBinCode\" : \"1HCH5\",\"MatchedProducts\" : \"17\",\"Products\" : [{\"ProductId\" : \"25168976\",\"SupplierCat\" : \"SNAP273CD\",\"Artist\" : \"FUNKADELIC\",\"Title\" : \"UNCLE JAM WANTS YOU\",\"Barcode\" : \"803415127320\",\"Format\" : \"CD\",\"EAN\" : \"0803415127320\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"82\",\"DeletionType\" : \"0\",\"Full_Artist_Value1\" : \"Funkadelic\",\"Full_Title_Value1\" : \"Uncle Jam Wants You\",\"QtyInBin\" : \"10\"},{\"ProductId\" : \"153609737\",\"SupplierCat\" : \"093624948247PMI\",\"Artist\" : \"GREEN DAY\",\"Title\" : \"UNO!\",\"Barcode\" : \"093624948247\",\"Format\" : \"CD\",\"EAN\" : \"0093624948247\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"2\",\"DeletionType\" : \"0\",\"QtyInBin\" : \"2\"},{\"ProductId\" : \"143271891\",\"SupplierCat\" : \"3325480644160PMI\",\"Artist\" : \"FRERES PITIGOI & TARA OASULU\",\"Title\" : \"MUSIQUE DE MARIAGE ET FETES RO\",\"Barcode\" : \"3325480644160\",\"Format\" : \"CD\",\"EAN\" : \"3325480644160\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"2\",\"DeletionType\" : \"0\",\"QtyInBin\" : \"2\"},{\"ProductId\" : \"138150831\",\"SupplierCat\" : \"651249076723PMI\",\"Artist\" : \"DIGWEED,JOHN\",\"Title\" : \"VOL. 2-RENAISSANCE PRESENTS TR\",\"Barcode\" : \"651249076723\",\"Format\" : \"CD\",\"EAN\" : \"0651249076723\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"1\",\"DeletionType\" : \"0\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"138150918\",\"SupplierCat\" : \"824363000121PMI\",\"Artist\" : \"CRANK YANKERS\",\"Title\" : \"VOL. 1-BEST CRANK CALLS\",\"Barcode\" : \"824363000121\",\"Format\" : \"CD\",\"EAN\" : \"0824363000121\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"1\",\"DeletionType\" : \"0\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"4993383\",\"SupplierCat\" : \"ANG646662PMI\",\"Artist\" : \"FOLLIES / O.B.C.\",\"Title\" : \"FOLLIES / O.B.C.\",\"Barcode\" : \"077776466620\",\"Format\" : \"CD\",\"EAN\" : \"0077776466620\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"2\",\"DeletionType\" : \"0\",\"Full_Artist_Value1\" : \"Broadway Cast\",\"Full_Title_Value1\" : \"Follies / O.B.C.\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"5469159\",\"SupplierCat\" : \"BAPO39026B2PMI\",\"Artist\" : \"MCNALLY,SHANNON\",\"Title\" : \"NORTH AMERICAN GHOST MUSIC\",\"Barcode\" : \"094633902626\",\"Format\" : \"CD\",\"EAN\" : \"0094633902626\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"1\",\"DeletionType\" : \"0\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"158859083\",\"SupplierCat\" : \"CDB56379480762PMI\",\"Artist\" : \"VALLDENEU,MAX\",\"Title\" : \"IT'S ABOUT LOVE (CDRP)\",\"Barcode\" : \"885767060326\",\"Format\" : \"CD\",\"EAN\" : \"0885767060326\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"1\",\"DeletionType\" : \"0\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"129403511\",\"SupplierCat\" : \"FAPO803412PMI\",\"Artist\" : \"BURNSIDE,R.L.\",\"Title\" : \"MISSISSIPPI HILL COUNTRY BLUES\",\"Barcode\" : \"045778034123\",\"Format\" : \"CD\",\"EAN\" : \"0045778034123\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"1\",\"DeletionType\" : \"0\",\"Full_Artist_Value1\" : \"R.L. Burnside\",\"Full_Title_Value1\" : \"Mississippi Country Blues\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"133803673\",\"SupplierCat\" : \"KSCOPE216\",\"Artist\" : \"ULVER\",\"Title\" : \"CHILDHOOD'S END\",\"Barcode\" : \"802644821627\",\"Format\" : \"CD\",\"EAN\" : \"0802644821627\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"46\",\"DeletionType\" : \"0\",\"Full_Artist_Value1\" : \"Ulver\",\"Full_Title_Value1\" : \"Childhood's End\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"173678827\",\"SupplierCat\" : \"RCA1961202PMI\",\"Artist\" : \"WALKER,HEZEKIAH\",\"Title\" : \"AZUSA THE NEXT GENERATION\",\"Barcode\" : \"886919612028\",\"Format\" : \"CD\",\"EAN\" : \"0886919612028\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"1\",\"DeletionType\" : \"0\",\"Full_Artist_Value1\" : \"Hezekiah Walker\",\"Full_Title_Value1\" : \"Azusa The Next Generation\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"4974588\",\"SupplierCat\" : \"RCA763792PMI\",\"Artist\" : \"ATKINS,CHET / PAUL,LES\",\"Title\" : \"CHESTER & LESTER (BONUS TRACKS\",\"Barcode\" : \"828767637921\",\"Format\" : \"CD\",\"EAN\" : \"0828767637921\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"2\",\"DeletionType\" : \"0\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"2497707\",\"SupplierCat\" : \"ROUCD9009\",\"Artist\" : \"BRAVE COMBO\",\"Title\" : \"DELETED - POLKATHARSIS\",\"Barcode\" : \"011661900929\",\"Format\" : \"CD\",\"EAN\" : \"0011661900929\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"1\",\"DeletionType\" : \"4\",\"Full_Artist_Value1\" : \"Brave Combo\",\"Full_Title_Value1\" : \"Polkatharsis\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"86158133\",\"SupplierCat\" : \"WMCD1294\",\"Artist\" : \"MENON,JAY\",\"Title\" : \"24/01THROUGH MY EYES\",\"Barcode\" : \"5016700129427\",\"Format\" : \"CD\",\"EAN\" : \"5016700129427\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"26\",\"DeletionType\" : \"0\",\"Full_Artist_Value1\" : \"Jay Menon\",\"Full_Title_Value1\" : \"Through My Eyes\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"67978413\",\"SupplierCat\" : \"FATCD62PMI\",\"Artist\" : \"BROSSEAU,TOM\",\"Title\" : \"CAVALIER\",\"Barcode\" : \"000030251304\",\"Format\" : \"CD\",\"EAN\" : \"0000030251304\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"1\",\"DeletionType\" : \"0\",\"QtyInBin\" : \"1\"},{\"ProductId\" : \"27369877\",\"SupplierCat\" : \"FV12\",\"Artist\" : \"MARTIN,JUAN\",\"Title\" : \"SOLO\",\"Barcode\" : \"5023100081224\",\"Format\" : \"CD\",\"EAN\" : \"5023100081224\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"9\",\"DeletionType\" : \"0\",\"Full_Artist_Value1\" : \"Juan Martin\",\"Full_Title_Value1\" : \"Solo\",\"QtyInBin\" : \"9\"},{\"ProductId\" : \"2498218\",\"SupplierCat\" : \"SHCD3813\",\"Artist\" : \"RANCH ROMANCE\",\"Title\" : \"FLIP CITY\",\"Barcode\" : \"015891381329\",\"Format\" : \"CD\",\"EAN\" : \"0015891381329\",\"SuppCode\" : \"PROP\",\"StockAmount\" : \"3\",\"DeletionType\" : \"0\",\"Full_Artist_Value1\" : \"Ranch Romance\",\"Full_Title_Value1\" : \"Flip City\",\"QtyInBin\" : \"1\"}]}";
                //response = responseHelper.refineResponse(response);
                if (response != null && !response.equalsIgnoreCase("")) {
                    if (response.contains("Error") || response.contains("not recognised")) {
                        String iMsg = "The Response object return null due to msg queue not recognising your improper request.";
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActBinMain - queryTask - Line:1192", deviceIMEI, RuntimeException.class.getSimpleName(), iMsg, today);
                        logger.log(log);
                        if (wsDialog != null && wsDialog.isShowing()) wsDialog.dismiss();
                        throw new NullPointerException("The barcode you have scanned have not been recognised. Please check and scan again");
                    }else {
                        ObjectMapper mapper = new ObjectMapper();
                        qryResponse = mapper.readValue(response, StockTakeBinResponse.class);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                today = new java.sql.Timestamp(utilDate.getTime());
                LogEntry log = new LogEntry(1L, ApplicationID, "StockTakeBinResponse - doInBackground", deviceIMEI, ex.getClass().getSimpleName(), ex.getMessage(), today);
                logger.log(log);
                if (wsDialog != null && wsDialog.isShowing()) wsDialog.dismiss();
                throw new NullPointerException(ex.getMessage());
            }
            return qryResponse;
        }

        @Override
        protected void onPostExecute(StockTakeBinResponse binResponse) {
            //super.onPostExecute(binResponse);
            if (wsDialog != null && wsDialog.isShowing()) wsDialog.dismiss();
            if (binResponse != null) {
                if (binResponse.getStockTakeLines() == null || binResponse.getStockTakeLines().isEmpty()) {
                    //Response is null the disable Yes button:
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActStockTakeBinCheck.this);
                    String msg = "Failed: Bin Search did NOT return any stockTake line. Please check if bin is empty or process found stock";
                    builder.setMessage(msg)
                            .setNegativeButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // Attempt to reload Activity
                                    //if (btnContinue.isEnabled()) btnContinue.setEnabled(false);
                                    Intent i = new Intent();
                                    setResult(RESULT_OK, i);
                                    ActStockTakeBinCheck.this.finish();
                                }
                            });
                } else {
                    //************************************* REPORT - SUCCESS ! ****************************************
                    //if (btnContinue.isEnabled()) btnContinue.setEnabled(false);
                    Intent i = new Intent(ActStockTakeBinCheck.this, ActStockTakeWorkLines.class);
                    i.putExtra("DATA_EXTRA", binResponse);
                    startActivityForResult(i, RESULT_FIRST_USER);
                }
            } else {
                //Response is null the disable Yes button:
                AlertDialog.Builder builder = new AlertDialog.Builder(ActStockTakeBinCheck.this);
                String msg = "Failed: Bin Search did NOT Completed because of network error, if this continues then please contact IT for help";
                builder.setMessage(msg)
                        .setNegativeButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Attempt to reload Activity
                                //if (btnContinue.isEnabled()) btnContinue.setEnabled(false);
                                Intent i = new Intent();
                                setResult(RESULT_OK, i);
                                ActStockTakeBinCheck.this.finish();
                            }
                        });
            }
        }
    }
}