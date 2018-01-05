package edu.cmu.cs.utils;

import android.content.Context;

/**
 * Created by junjuew on 3/4/16.
 */
public abstract class GenericEditTextAlertDialog {
    protected DialogEditTextResultListener delegate;
    protected Context mContext;

    public interface DialogEditTextResultListener {
        void onDialogEditTextResult(String... result);
    }

    public GenericEditTextAlertDialog(Context m, DialogEditTextResultListener delegate){
        this.mContext=m;
        this.delegate=delegate;
    }

//    public abstract Dialog createDialog(String title, String[] msg, String[] hints);
}

//    /**
//     * Create and return an example alert dialog with an edit text box.
//     */
//    public Dialog createDialog(String title,
//                               String msg,
//                               String hint){
//        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
//        builder.setTitle(title);
//        builder.setMessage(msg);
//
//        // Use an EditText view to get user input.
//        final EditText input = new EditText(this.mContext);
//        input.setText(hint);
//        dialogInputTextEdit = input;
//        inputDialogResult = null;
//        builder.setView(input);
//
//        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int whichButton) {
//            }
//        });
//
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                return;
//            }
//        });
//
//
//        AlertDialog alertDialog = builder.create();
//        alertDialog.show();
//        Button customOkButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
//        customOkButton.setOnClickListener(new OKReturnTextListener(alertDialog));
//        return alertDialog;
//    }
//
//    class OKReturnTextListener implements View.OnClickListener {
//        private final Dialog dialog;
//        public OKReturnTextListener(Dialog dialog) {
//            this.dialog = dialog;
//        }
//        @Override
//        public void onClick(View v) {
//            inputDialogResult = dialogInputTextEdit.getText().toString();
//            Log.d(TAG, "user input: " + inputDialogResult);
//            if (null != delegate){
//                delegate.onDialogEditTextResult(inputDialogResult);
//            }
//            this.dialog.dismiss();
//            return;
//        }
//    }

