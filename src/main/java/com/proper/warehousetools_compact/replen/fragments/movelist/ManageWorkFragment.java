package com.proper.warehousetools_compact.replen.fragments.movelist;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.*;
import android.widget.ExpandableListView;
import android.widget.ViewFlipper;
import com.proper.data.diagnostics.LogEntry;
import com.proper.data.helpers.ReplenDialogHelper;
import com.proper.data.replen.ReplenMoveListItemResponse;
import com.proper.data.replen.ReplenMoveListResponse;
import com.proper.data.replen.ReplenSelectedMoveWrapper;
import com.proper.data.replen.adapters.ReplenMoveListAdapter;
import com.proper.messagequeue.Message;
import com.proper.warehousetools_compact.R;
import com.proper.warehousetools_compact.replen.ui.chainway_C4000.ActReplenManageWork;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lebel on 01/12/2014.
 */
//public class ManageWorkFragment extends BaseReplenPlainFragment {
public class ManageWorkFragment extends Fragment {
    private String TAG = ManageWorkFragment.class.getSimpleName();
    private static final String menuItemMoveList = "Work this MoveList";
    private static final String menuItemMoveListProcess = "Process MoveList";
    private int groupPos;
    private boolean canWork = false, canComplete = false;
    private ViewFlipper flipper;
    private ExpandableListView lvWork;
    private RetrieveWorkAsync getWorkAsync;
    private ActReplenManageWork mActivity = (ActReplenManageWork) getActivity();
    private Message vMessage = null;

    public ManageWorkFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        if (mActivity == null) {
            mActivity = (ActReplenManageWork) getActivity();
        }
        View view = inflater.inflate(R.layout.lyt_replen_resume, container, false);
        flipper = (ViewFlipper) view.findViewById(R.id.vfReplenResume);
        lvWork = (ExpandableListView) view.findViewById(R.id.lvReplenResume);
        lvWork.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View view, int groupPosition, long id) {
                onListViewGroupClicked(parent, view, groupPosition, id);
                return false;
            }
        });

        lvWork.setChoiceMode(ExpandableListView.CHOICE_MODE_SINGLE);
        registerForContextMenu(lvWork);

        // retrieve any outstanding work (move list) from the database
        String msg = String.format("{\"UserId\":\"%s\", \"UserCode\":\"%s\"}", mActivity.getCurrentUser().getUserId(), mActivity.getCurrentUser().getUserCode());
        //String msg = "{\"UserId\":\"263\", \"UserCode\":\"B97C51\"}";       //test only
        mActivity.setToday(new Timestamp(mActivity.getUtilDate().getTime()));
        vMessage = new Message();
        vMessage.setSource(mActivity.getDeviceIMEI());
        vMessage.setMessageType("GetUserMovelists");
        vMessage.setIncomingStatus(1); //default value
        vMessage.setIncomingMessage(msg);
        vMessage.setOutgoingStatus(0);   //default value
        vMessage.setOutgoingMessage("");
        vMessage.setInsertedTimeStamp(mActivity.getToday());
        vMessage.setTTL(100);    //default value
        getWorkAsync = new RetrieveWorkAsync();
        getWorkAsync.execute(vMessage);  //executes both -> Send Queue Directly AND Send queue to Service
        return view;
    }

    private void removeSharedPreferences() {
        mActivity.setPrefs(getActivity().getSharedPreferences(getString(R.string.preference_replenmovelist), Context.MODE_PRIVATE));
        SharedPreferences.Editor editor = mActivity.getPrefs().edit();
        editor.remove("ApplicationID");
        editor.remove("IMEI");
        editor.remove("Device");
        editor.remove("UserToken");
        editor.remove("Movelist");
        editor.apply();
    }

    private void saveSharedPreferences() {
        SharedPreferences credPref = mActivity.getSharedPreferences(getString(R.string.preference_credentials), Context.MODE_PRIVATE);
        mActivity.setPrefs(mActivity.getSharedPreferences(getString(R.string.preference_replenmovelist), Context.MODE_PRIVATE));
        SharedPreferences.Editor editor = mActivity.getPrefs().edit();
        editor.putString("ApplicationID", mActivity.getApplicationID());
        editor.putString("IMEI", mActivity.getDeviceIMEI());
        editor.putString("Device", mActivity.getDeviceID());
        editor.putString("UserToken", credPref.getString("UserToken", ""));
        editor.putString("Movelist", mActivity.getMoveListReponseString());
        editor.commit();
    }

    private void loadSharedPreferences() {
        SharedPreferences credPref = getActivity().getSharedPreferences(getString(R.string.preference_credentials), Context.MODE_PRIVATE);
        mActivity.setPrefs(getActivity().getSharedPreferences(getString(R.string.preference_replenmovelist), Context.MODE_PRIVATE));
        SharedPreferences.Editor editor = mActivity.getPrefs().edit();
        mActivity.getPrefs().getString("ApplicationID", mActivity.getApplicationID());
        mActivity.getPrefs().getString("IMEI", mActivity.getDeviceIMEI());
        mActivity.getPrefs().getString("Device", mActivity.getDeviceID());
        mActivity.getPrefs().getString("UserToken", credPref.getString("UserToken", ""));
        mActivity.getPrefs().getString("Movelist", mActivity.getMoveListReponseString());
    }

    private void showDialog(int severity, int dialogType, String message, String title) {
        FragmentManager fm = mActivity.getSupportFragmentManager();

        //DialogHelper dialog = new DialogHelper(severity, dialogType, message, title);
        //DialogHelper dialog = new DialogHelper();
        ReplenDialogHelper dialog = new ReplenDialogHelper();
        Bundle args = new Bundle();
        args.putInt("DialogType_ARG", dialogType);
        args.putInt("Severity_ARG", severity);
        args.putString("Message_ARG", message);
        args.putString("Title_ARG", title);
        args.putString("Originated_ARG", ManageWorkFragment.class.getSimpleName());
        dialog.setArguments(args);
        dialog.show(fm, "Dialog");
    }

    private void onListViewGroupClicked(ExpandableListView parent, View view, int groupPosition, long id) {
        if (mActivity.getMoveListAdapter() != null) {
            mActivity.setSelectedMove(new ReplenSelectedMoveWrapper(mActivity.getMoveListAdapter().getGroup(groupPosition)));
            lvWork.setItemChecked(groupPosition, true);
            view.setSelected(true);
            mActivity.setCurrentSelectedIndex(groupPosition);
            groupPos = groupPosition;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.lvReplenResume) {
            ExpandableListView.ExpandableListContextMenuInfo info =
                    (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
            int type =
                    ExpandableListView.getPackedPositionType(info.packedPosition);
            int group =
                    ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int child =
                    ExpandableListView.getPackedPositionChild(info.packedPosition);

            menu.add(menuItemMoveList);
            menu.add(menuItemMoveListProcess);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        /** REF: http://stackoverflow.com/questions/14734586/oncontextitemselected-called-twice-for-fragment*/

        if (getUserVisibleHint()) {
            ExpandableListView.ExpandableListContextMenuInfo info =
                    (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
            final int type = ExpandableListView.getPackedPositionType(info.packedPosition);
            if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP && TAG.equalsIgnoreCase(ManageWorkFragment.class.getSimpleName())) {
                groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
                //mActivity.setSelectedMove(mActivity.getMoveListResponse().getMovelists().get(groupPos));
                mActivity.setSelectedMove(new ReplenSelectedMoveWrapper(mActivity.getMoveListResponse().getMovelists().get(groupPos)));
                mActivity.setCurrentSelectedIndex(groupPos);
                lvWork.setSelected(true);
                lvWork.setItemChecked(groupPos, true);
                String msg = "";
                if (item.getTitle().toString().equalsIgnoreCase(menuItemMoveList)) {
                    //TODO - check if move list line is completed if it's not then we can work it
                    msg = String.format("Are you sure you want to work this move list entry number (%s)", groupPos);
                    mActivity.setCanNavigate(true);
                    canWork = true;
                    showDialog(R.integer.MSG_TYPE_NOTIFICATION, R.integer.MSG_POSITIVE, msg, "This Line?");
                }
                canComplete = false;
                if (item.getTitle().toString().equalsIgnoreCase(menuItemMoveListProcess)) {
                    //TODO - check that all move list are completed make sure that there's no outstanding move list available
                    msg = String.format("Are you sure you want to process this entry number (%s) from move list", groupPos);
                    canComplete = true;
                    //mActivity.setCanNavigate(true);
                    showDialog(R.integer.MSG_TYPE_NOTIFICATION, R.integer.MSG_POSITIVE, msg, "This Line?");
                }
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void setUserVisibleHint(boolean visible) {

        super.setUserVisibleHint(visible);
        if (visible && isResumed()) {
            /** -> Only manually call onResume if fragment is already visible
                -> Otherwise allow natural fragment lifecycle to call onResume  */
            canComplete = false;
            onResume();
        }
    }

//    @Override
//    public void onDialogMessage(int buttonClicked) {
//        switch (buttonClicked) {
//            case R.integer.MSG_CANCEL:
//                break;
//            case R.integer.MSG_YES:
//                break;
//            case R.integer.MSG_OK:
//                if (canWork) {
//                    canWork = false;
//                    mActivity.getDisplayedFragment().switchFragment(MOVE_LINE_TAB);
//                }
////                if (mActivity.isCanNavigate()) {
////                    mActivity.setCanNavigate(false);
////                    mActivity.getDisplayedFragment().switchFragment(MOVE_LINE_TAB);
////                }
//                break;
//            case R.integer.MSG_NO:
//                break;
//        }
//    }

    private class RetrieveWorkAsync extends AsyncTask<Message, Void, ReplenMoveListResponse> {
        private ProgressDialog vDialog;

        @Override
        protected void onPreExecute() {
            vDialog = new ProgressDialog(getActivity());
            CharSequence message = "Working hard...sending queue [directly] [to webservice]...";
            CharSequence title = "Please Wait";
            vDialog.setCancelable(true);
            vDialog.setCanceledOnTouchOutside(false);
            vDialog.setMessage(message);
            vDialog.setTitle(title);
            vDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            vDialog.show();
        }

        @Override
        protected ReplenMoveListResponse doInBackground(Message... inputMsg) {
            //qryResponse = new BinResponse();
            ReplenMoveListResponse qryResponse = null;

            try {
                mActivity.setMoveListReponseString(mActivity.getResolver().resolveMessageQueue(inputMsg[0]));
                if (!mActivity.getMoveListReponseString().isEmpty()) {
                    if (mActivity.getMoveListReponseString().contains("Error")) {
                        //manually error trap this error
                        String iMsg = "The Response object return null due to msg queue not recognising your improper request.";
                        mActivity.setToday(new Timestamp(mActivity.getUtilDate().getTime()));
                        LogEntry log = new LogEntry(1L, mActivity.getApplicationID(), "ActReplenResume - queryTask - Line:231", mActivity.getDeviceIMEI(), RuntimeException.class.getSimpleName(), iMsg, mActivity.getToday());
                        mActivity.getLogger().log(log);
                        throw new RuntimeException("The barcode you have scanned have not been recognised. Please check and scan again");
                    }else {
                        //Process it manually

                        try {
                            JSONObject resp = new JSONObject(mActivity.getMoveListReponseString());
                            int requestedUserId = Integer.parseInt(resp.getString("RequestedUserId"));
                            int movelistsReturned = Integer.parseInt(resp.getString("MovelistsReturned"));
                            JSONArray moveList = resp.getJSONArray("Movelists");

                            List<ReplenMoveListItemResponse> moveListItemResponseList = new ArrayList<ReplenMoveListItemResponse>();
                            //get move list
                            for (int i = 0; i < moveList.length(); i++) {
                                JSONObject move = moveList.getJSONObject(i);
                                int movelistId = Integer.parseInt(move.getString("MovelistId"));
                                String description = move.getString("Description");
                                String notes = move.getString("Notes");
                                Timestamp insertTimeStamp = Timestamp.valueOf(move.getString("InsertTimeStamp"));
                                int assignedTo = Integer.parseInt(move.getString("AssignedTo"));
                                int status = Integer.parseInt(move.getString("Status"));
                                String statusName = move.getString("StatusName");
                                int listType = Integer.parseInt(move.getString("ListType"));
                                String listTypeName = move.getString("ListTypeName");
                                int totalLines = Integer.parseInt(move.getString("TotalLines"));
                                int totalQty = Integer.parseInt(move.getString("TotalQty"));
                                ReplenMoveListItemResponse moveListItemResponse = new ReplenMoveListItemResponse(movelistId, description, notes, insertTimeStamp,
                                        assignedTo, status, statusName, listType, listTypeName, totalLines, totalQty);
                                moveListItemResponseList.add(moveListItemResponse);
                            }
                            qryResponse =  new ReplenMoveListResponse(requestedUserId, moveListItemResponseList, movelistsReturned);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            mActivity.setToday(new Timestamp(mActivity.getUtilDate().getTime()));
                            LogEntry log = new LogEntry(1L, mActivity.getApplicationID(), "ActReplenResume - doInBackground", mActivity.getDeviceIMEI(), ex.getClass().getSimpleName(), ex.getMessage(), mActivity.getToday());
                            mActivity.getLogger().log(log);
                        }
                    }
                }
                mActivity.setMoveListResponse(qryResponse);
            } catch (Exception ex) {
                ex.printStackTrace();
                mActivity.setToday(new Timestamp(mActivity.getUtilDate().getTime()));
                LogEntry log = new LogEntry(1L, mActivity.getApplicationID(), "ActReplenResume - doInBackground", mActivity.getDeviceIMEI(), ex.getClass().getSimpleName(), ex.getMessage(), mActivity.getToday());
                mActivity.getLogger().log(log);
            }
            return qryResponse;
        }

        @Override
        protected void onPostExecute(ReplenMoveListResponse response) {
            if (vDialog != null && vDialog.isShowing()) {
                vDialog.dismiss();
            }
            if (response != null) {
                //fill the list
                mActivity.setMoveListAdapter(new ReplenMoveListAdapter(getActivity(), response.getMovelists()));
                lvWork.setAdapter(mActivity.getMoveListAdapter());
                flipper.setDisplayedChild(1);
            }
        }
    }
}
