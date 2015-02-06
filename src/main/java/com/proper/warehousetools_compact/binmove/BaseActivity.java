package com.proper.warehousetools_compact.binmove;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import com.proper.logger.LogHelper;
import com.proper.messagequeue.HttpMessageResolver;
import com.proper.security.UserAuthenticator;
import com.proper.security.UserLoginResponse;
import com.proper.utils.DeviceUtils;
import com.proper.warehousetools_compact.AppContext;
import com.proper.warehousetools_compact.AppManager;
import com.proper.warehousetools_compact.R;
import com.proper.warehousetools_compact.MockClass;

/**
 * Created by Lebel on 27/08/2014.
 */
public class BaseActivity extends ActionBarActivity {
    protected final String ApplicationID = "BinMove"; //this.getPackageName(); //"BinMove";
    protected AppContext appContext;
    protected AppManager appManager;
    protected int screenSize;
    protected String deviceID = "";
    protected String deviceIMEI = "";
    protected java.util.Date utilDate = java.util.Calendar.getInstance().getTime();
    protected java.sql.Timestamp today = null;
    protected DeviceUtils device = null;
    protected HttpMessageResolver resolver = null;
    protected UserAuthenticator authenticator = null;
    protected UserLoginResponse currentUser = null;
    protected LogHelper logger = new LogHelper();
    protected com.proper.messagequeue.Message thisMessage = null;
    protected ShareActionProvider actionProvider = null;
    protected SearchView mSearchView;
    protected MockClass testResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext = (AppContext) getApplication();
        screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        authenticator = new UserAuthenticator(this);
        device = new DeviceUtils(this);
        logger = new LogHelper();
        thisMessage = new com.proper.messagequeue.Message();
        deviceID = device.getDeviceID();
        deviceIMEI = device.getIMEI();
        currentUser = authenticator.getCurrentUser();
        resolver = new HttpMessageResolver(appContext);
        testResolver = new MockClass();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.mnu_main, menu);

//        MenuItem searchItem = menu.findItem(R.id.action_search);
//        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        // Find the share item
        MenuItem shareItem = menu.findItem(R.id.menu_share);
        // Need to use MenuItemCompat to retrieve the Action Provider
        actionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case android.R.id.home:
//                Intent intent = new Intent();
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                //startActivity(intent);
//                setResult(RESULT_OK, intent);
//                finish();
//                break;
            case R.id.menu_share:
                // populate the share intent with data
                Intent iShare = new Intent(Intent.ACTION_SEND);
                iShare.setType("text/plain");
                iShare.putExtra(Intent.EXTRA_TEXT, "This is a message for you");
                actionProvider.setShareIntent(iShare);
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}