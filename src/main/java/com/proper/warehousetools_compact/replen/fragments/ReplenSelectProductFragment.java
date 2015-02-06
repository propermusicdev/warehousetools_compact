package com.proper.warehousetools_compact.replen.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.proper.warehousetools_compact.R;

/**
 * Created by Lebel on 03/09/2014.
 */
public class ReplenSelectProductFragment extends Fragment {

    public ReplenSelectProductFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.lyt_replen_selectproduct, container, false);
    }
}