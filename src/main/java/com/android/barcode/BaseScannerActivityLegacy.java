package com.android.barcode;


import android.app.AlertDialog;
import android.content.DialogInterface;
//import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
//import android.support.v4.view.MenuItemCompat;
//import android.support.v7.widget.ShareActionProvider;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
import android.view.View;
import com.proper.data.diagnostics.LogEntry;
import com.proper.data.helpers.BarcodeHelper;
import com.proper.data.helpers.ResponseHelper;
import com.proper.logger.LogHelper;
import com.proper.messagequeue.HttpMessageResolver;
import com.proper.security.UserAuthenticator;
import com.proper.security.UserLoginResponse;
import com.proper.utils.DeviceUtils;
import com.proper.warehousetools_compact.AppContext;
import com.proper.warehousetools_compact.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Lebel on 26/08/2014.
 */
public class BaseScannerActivityLegacy extends ActionBarActivity {
    protected AppContext appContext;
    public static final int KEY_SCAN = 111;
    public static final int KEY_F1 = 112;
    public static final int KEY_F2 = 113;
    public static final int KEY_F3 = 114;
    public static final int KEY_YELLOW = 115;
    protected int KEY_POSITION = 0;
    protected int NAV_INSTRUCTION = 0;
    protected int NAV_TURN = 0;
    protected int fullTurnCount = 0;
    protected int inputByHand = 0;
    protected String deviceIMEI = ""; //**OK
    protected static final String ApplicationID = "BinMove";
    protected java.util.Date utilDate = java.util.Calendar.getInstance().getTime();
    protected java.sql.Timestamp today = null;
    protected com.android.barcode.DeviceControl DevCtrl;
    protected com.android.barcode.SerialPort mSerialPort;
    public int fd;
    protected Thread readThread = null;
    protected static final String TAG = "SerialPort";
    protected static final String myMessageType = "";
    protected boolean key_start = true;
    protected boolean Powered = false;
    protected boolean Opened = false;
    protected Timer timer = new Timer();
    protected Timer retrig_timer = new Timer();
    protected Handler handler = null;
    protected Handler t_handler = null;
    protected Handler n_handler = null;
    protected boolean ops = false;
    protected String scanInput;
    protected ResponseHelper responseHelper = new ResponseHelper();
    protected UserLoginResponse currentUser = new UserLoginResponse();
    protected HttpMessageResolver resolver = null;
    protected LogHelper logger = null;
    protected com.proper.messagequeue.Message thisMessage = null;
    protected UserAuthenticator authenticator = null;
    protected DeviceUtils utils = null;
    protected BarcodeHelper barcodeHelper = null;
    //protected ShareActionProvider actionProvider = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext = (AppContext) getApplication();
        responseHelper = new ResponseHelper();
        currentUser = new UserLoginResponse();
        resolver = new HttpMessageResolver(appContext);
        logger = new LogHelper();
        thisMessage = new com.proper.messagequeue.Message();
        authenticator = new UserAuthenticator(appContext);
        utils = new DeviceUtils(appContext);
        deviceIMEI = utils.getIMEI();
        currentUser = currentUser != null ? currentUser : authenticator.getCurrentUser();
        barcodeHelper = new BarcodeHelper();
        //setContentView(R.layout.lyt_baselegacy);

        try {
            DevCtrl = new DeviceControl("/proc/driver/scan");

        } catch (SecurityException e) {
            e.printStackTrace();
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "ActBinMain - onCreate", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
            logger.log(log);
        } catch (IOException e) {
            Log.d(TAG, "AAA");
            today = new java.sql.Timestamp(utilDate.getTime());
            LogEntry log = new LogEntry(1L, ApplicationID, "ActBinMain - onCreate", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
            logger.log(log);
            new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).show();
            return;
        }
        ops = true;

        KEY_POSITION = 0; //Set for Yellow button to btnScanSource


        t_handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == 1) {
                    try {
                        DevCtrl.PowerOffDevice();
                    } catch (IOException e) {
                        Log.d(TAG, "BBB");
                        e.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActBinMain - onCreate", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                        logger.log(log);
                    }//powersave
                    Powered = false;
                }
            }
        };

        n_handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == 1) {
                    try {
                        if(key_start == false)
                        {
                            DevCtrl.TriggerOffDevice();
                            timer = new Timer();				//start a timer, when machine is idle for some time, cut off power to save energy.
                            timer.schedule(new MyTask(), 60000);
                            key_start = true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        today = new java.sql.Timestamp(utilDate.getTime());
                        LogEntry log = new LogEntry(1L, ApplicationID, "ActBinMain - onCreate", deviceIMEI, e.getClass().getSimpleName(), e.getMessage(), today);
                        logger.log(log);
                    }
                }
            }
        };

    }

    public class MyTask extends TimerTask {

        @Override
        public void run() {
            Message message = new Message();
            message.what = 1;
            t_handler.sendMessage(message);
        }
    }

    public class RetrigTask extends TimerTask {
        @Override
        public void run() {
            //startTime = System.currentTimeMillis(); // begin long process time elapse count
            Message message = new Message();
            message.what = 1;
            n_handler.sendMessage(message);
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu
//        getMenuInflater().inflate(R.menu.mnu_main, menu);
//
//        // Find the share item
//        MenuItem shareItem = menu.findItem(R.id.menu_share);
//
//        // Need to use MenuItemCompat to retrieve the Action Provider
//        actionProvider = (ShareActionProvider)
//                MenuItemCompat.getActionProvider(shareItem);
//
//        return super.onCreateOptionsMenu(menu);
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_share:
//                // populate the share intent with data
//                Intent iShare = new Intent(Intent.ACTION_SEND);
//                iShare.setType("text/plain");
//                iShare.putExtra(Intent.EXTRA_TEXT, "This is a message for you");
//                actionProvider.setShareIntent(iShare);
//            default:
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    /**
     * The ScrollView highly targeted based on internal controls in the end
     *
     * @param scroll
     * @param inner
     */
    public void scrollToBottom(final View scroll, final View inner) {

        Handler mHandler = new Handler();

        mHandler.post(new Runnable() {
            public void run() {
                if (scroll == null || inner == null) {
                    return;
                }
                int offset = inner.getMeasuredHeight() - scroll.getHeight();
                if (offset < 0) {
                    offset = 0;
                }

                scroll.scrollTo(0, offset);
            }
        });
    }
}