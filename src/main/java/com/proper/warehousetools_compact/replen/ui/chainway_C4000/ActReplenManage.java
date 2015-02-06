package com.proper.warehousetools_compact.replen.ui.chainway_C4000;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import com.proper.warehousetools_compact.R;
import com.proper.warehousetools_compact.binmove.BaseScannerActivity;
import com.proper.warehousetools_compact.replen.fragments.ReplenSelectBinFragment;
import com.proper.warehousetools_compact.replen.fragments.ReplenSelectDestinationFragment;
import com.proper.warehousetools_compact.replen.fragments.ReplenSelectProductFragment;

/**
 * Created by Lebel on 02/09/2014.
 */
public class ActReplenManage extends BaseScannerActivity {
    public ActReplenManage() {
    }

    private ViewPager viewPager;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lyt_replenmanage);
        //viewPager = (ViewPager) this.findViewById(R.id.vp_replenManage);
        viewPager.setAdapter(new NavAdapter(getSupportFragmentManager()));
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
                Log.d("ActReplenManage", String.format("onTabReselected at position: %s from: %s with number of pixels:", i, v, i2));
            }

            @Override
            public void onPageSelected(int i) {
                getSupportActionBar().setSelectedNavigationItem(i);     //sets selected page
                Log.d("ActReplenManage", "onPageSelected at position: " + i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
                Log.d("ActReplenManage", "onPageSelected at position: " + i);
            }
        });
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS); // prepare for tabs
        ActionBar.Tab selBinTab = getSupportActionBar().newTab();
        selBinTab.setText("Bin");
        selBinTab.setTabListener(new MyTabsListener());
//        selBinTab.setTabListener(new ActionBar.TabListener() {
//            @Override
//            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                tabReselected(tab, ft);
//            }
//
//            @Override
//            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                tabUnselected(tab, ft);
//            }
//
//            @Override
//            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                tabRSelected(tab, ft);
//            }
//        });
        ActionBar.Tab selProdTab = getSupportActionBar().newTab();
        selProdTab.setText("Product");
        selProdTab.setTabListener(new MyTabsListener());
        ActionBar.Tab selDstTab = getSupportActionBar().newTab();
        selDstTab.setText("Destination");
        selDstTab.setTabListener(new MyTabsListener());
        getSupportActionBar().addTab(selBinTab);
        getSupportActionBar().addTab(selProdTab);
        getSupportActionBar().addTab(selDstTab);
    }

    class MyTabsListener implements ActionBar.TabListener {

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            viewPager.setCurrentItem(tab.getPosition());
            Log.d("ActReplenManage", String.format("onTabReselected at position: %s, with name: %s ", tab.getPosition(), tab.getText()));
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Log.d("ActReplenManage", String.format("onTabReselected at position: %s, with name: %s ", tab.getPosition(), tab.getText()));
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Log.d("ActReplenManage", String.format("onTabReselected at position: %s, with name: %s ", tab.getPosition(), tab.getText()));
        }
    }

    class NavAdapter extends FragmentPagerAdapter {

        NavAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int pos) {
            Fragment fragment = null;
            if (pos == 0) {
                fragment = new ReplenSelectBinFragment();
            }
            if (pos == 1) {
                fragment = new ReplenSelectProductFragment();
            }
            if (pos == 2) {
                fragment = new ReplenSelectDestinationFragment();
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

//    public void tabReselected (ActionBar.Tab tab, FragmentTransaction ft) {
//        Log.d("ActReplenManage", String.format("onTabReselected at position: %s, with name: %s ", tab.getPosition(), tab.getText()));
//    }
//
//    public void tabUnselected (ActionBar.Tab tab, FragmentTransaction ft) {
//        Log.d("ActReplenManage", String.format("onTabReselected at position: %s, with name: %s ", tab.getPosition(), tab.getText()));
//    }
//
//    public void tabRSelected (ActionBar.Tab tab, FragmentTransaction ft) {
//        Log.d("ActReplenManage", String.format("onTabReselected at position: %s, with name: %s ", tab.getPosition(), tab.getText()));
//    }
}