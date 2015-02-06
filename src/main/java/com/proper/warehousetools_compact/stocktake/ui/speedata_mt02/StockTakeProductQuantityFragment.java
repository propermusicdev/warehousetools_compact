package com.proper.warehousetools_compact.stocktake.ui.speedata_mt02;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import com.proper.data.core.IStockTakeQtyCommunicator;
import com.proper.warehousetools_compact.R;

/**
 * Created by Lebel on 29/01/2015.
 */
public class StockTakeProductQuantityFragment extends DialogFragment {
    private ActStockTakeWorkLines mActivity = null;
    private IStockTakeQtyCommunicator iStockTakeQtyCommunicator;
    private EditText txtQty;
    private ImageButton btnPlus, btnMinus;
    private Button btnByHand, btnFinish;
    private int OriginalQty, modQty = 1, inputByHand, passedIndex = -1;
    private String parent = "";

    public int getModQty() {
        return modQty;
    }

    public void setModQty(int modQty) {
        this.modQty = modQty;
    }

    public StockTakeProductQuantityFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        iStockTakeQtyCommunicator = (IStockTakeQtyCommunicator) activity;
        if (mActivity == null) {
            mActivity = (ActStockTakeWorkLines) getActivity();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //return super.onCreateDialog(savedInstanceState);
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (mActivity == null) {
            mActivity = (ActStockTakeWorkLines) getActivity();
        }
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        getDialog().setCanceledOnTouchOutside(false);
        mActivity = (ActStockTakeWorkLines) getActivity();
        //Determine parent activity
        parent = getActivity().getClass().getSimpleName();

        getDialog().setTitle(getString(R.string.app_name));
        Bundle extras = getArguments();
        passedIndex = extras.getInt("INDEX_EXTRA");
        if (passedIndex < 0) {throw new ArithmeticException("INDEX_EXTRA cannot be less than zero (0)");} //forced crash
        View view = inflater.inflate(R.layout.dialog_stocktake_productquantity, container);
        TextView txtTitle = (TextView) view.findViewById(R.id.txtvSTDTitle);
        txtQty = (EditText) view.findViewById(R.id.etxtSTDQty);
        btnPlus = (ImageButton) view.findViewById(R.id.bnSTDPlus);
        btnMinus = (ImageButton) view.findViewById(R.id.bnSTDMinus);
        btnByHand = (Button) view.findViewById(R.id.bnSTDByHand);
        btnFinish = (Button) view.findViewById(R.id.bnSTDFinish);
        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        btnByHand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClicked(v);
            }
        });
        txtQty.addTextChangedListener(new TextChanged(txtQty));
        return view;
    }

    private void updateControls() {
        txtQty.setText(String.format("%s", modQty));
        if (modQty == 0) {
            if (btnFinish.isEnabled()) btnFinish.setEnabled(false);
            txtQty.setTextColor(Color.RED);
        }
        if (modQty > 0) {
            if (!btnFinish.isEnabled()) btnFinish.setEnabled(true);
            if (modQty > 1) {
                txtQty.setTextColor(Color.GREEN);
            }else {
                txtQty.setTextColor(Color.WHITE);
            }
        }
    }

    private void showSoftKeyboard() {
        InputMethodManager imm =(InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null){
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
        }
    }

    private void manageInputByHand(Button button) {
        if (button == btnByHand) {
            if (btnByHand.isEnabled()) {
                btnByHand.setEnabled(false);
                btnByHand.setPaintFlags(btnByHand.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
            if (inputByHand == 0) {
                turnOnInputByHand();
                if (!txtQty.isEnabled()) {
                    txtQty.setEnabled(true);
                }
                paintByHandButtons(btnByHand);
                if (btnFinish.isEnabled()) btnFinish.setEnabled(false);
                btnFinish.setPaintFlags(btnFinish.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                if (btnPlus.isEnabled()) btnPlus.setEnabled(false);
                if (btnMinus.isEnabled()) btnMinus.setEnabled(false);
                showSoftKeyboard();
                txtQty.requestFocus();
                selectAll(txtQty);
            } else {
                turnOffInputByHand();
                paintByHandButtons(btnByHand);
                setModQty(Integer.parseInt(txtQty.getText().toString()));
                if (getModQty() != 0) {
                    txtQty.setText(String.format("%s", getModQty()));     // just to trigger text changed
                    paintByHandButtons(btnByHand);
                    if (!btnFinish.isEnabled()) btnFinish.setEnabled(true);
                    btnFinish.setPaintFlags(btnFinish.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    if (!btnPlus.isEnabled()) btnPlus.setEnabled(true);
                    if (!btnMinus.isEnabled()) btnMinus.setEnabled(true);
                    if (txtQty.isEnabled()) txtQty.setEnabled(false);
                    updateControls();
                }
            }
            if (!btnByHand.isEnabled()) {
                btnByHand.setEnabled(true);
                btnByHand.setPaintFlags(btnByHand.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }
    }

    private void turnOnInputByHand(){
        this.inputByHand = 1;    //Turn On Input by Hand
    }

    private void turnOffInputByHand(){
        this.inputByHand = 0;    //Turn On Input by Hand
    }

    private void paintByHandButtons(Button button) {
        final String byHand = "Enter";
        final String finish = "Finish";
        final String enter = "EDIT";
        if (button == btnByHand) {
            if (inputByHand == 0) {
                btnByHand.setText(byHand);
            } else {
                btnByHand.setText(finish);
            }
//            if (modQty == 0) {
//                btnFinish.setText("Confirm");
//            } else {
//                btnFinish.setText("Done");
//            }
        }
    }

    private void selectAll(EditText editText){
        editText.selectAll();
    }

    private void buttonClicked(View v) {
        if (v == btnByHand) {
            if (!txtQty.isEnabled()) {
                txtQty.setEnabled(true);
                txtQty.requestFocus();
            }
            manageInputByHand(btnByHand);
        }
        if (v == btnMinus) {
            if (modQty > 0) {
                modQty --;
            }
            updateControls();
        }
        if (v == btnPlus) {
            modQty ++;
            updateControls();
        }
        if (v == btnFinish) {
            if (modQty <= 0) {
                //Alert that move cannot be zero
                String mMsg = "StockTake Quantity (txtQty) MUST not be zero";
                AlertDialog.Builder builder = new AlertDialog.Builder(StockTakeProductQuantityFragment.this.getActivity());
                builder.setMessage(mMsg)
                        .setPositiveButton(R.string.but_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do nothing for now...
                            }
                        });
                builder.show();
            } else {
                mActivity.setEnteredQty(modQty);    //set value to bre imported
                iStockTakeQtyCommunicator.onDialogMessage_IStockTakeQtyCommunicator(R.integer.MSG_OK, passedIndex);
                dismiss();
            }
        }
    }

    private class TextChanged implements TextWatcher {
        private View view = null;
        private TextChanged(View v) {
            this.view = v;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {

            if (!editable.toString().isEmpty()) {
                int value = Integer.parseInt(editable.toString());
                if (view == txtQty) {
                    setModQty(value);
                    if (txtQty.isEnabled()) {
                        txtQty.setEnabled(false);
                    }
                }
            }
        }
    }
}
