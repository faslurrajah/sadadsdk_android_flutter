package com.sadadsdk.cardform;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.sufalamtech.sadad.sdk.R;

import java.lang.reflect.Method;

import com.sadadsdk.utils.DateValidator;
import com.sadadsdk.utils.ErrorEditText;
import com.sadadsdk.utils.SlashSpan;

/**
 * Created by Hitesh Sarsava on 11/3/19.
 */
public class ExpirationDateEditText extends ErrorEditText implements TextWatcher, View.OnClickListener {

    private boolean mChangeWasAddition;
    private View.OnClickListener mClickListener;
    private boolean mUseExpirationDateDialog = false;
    private ExpirationDateDialog mExpirationDateDialog;

    public ExpirationDateEditText(Context context) {
        super(context);
        init();
    }

    public ExpirationDateEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExpirationDateEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] filters = {new InputFilter.LengthFilter(7)};
        setFilters(filters);
        addTextChangedListener(this);
        setShowKeyboardOnFocus(!mUseExpirationDateDialog);
        setCursorVisible(!mUseExpirationDateDialog);
        super.setOnClickListener(this);
    }

    /**
     * Used to enable or disable entry of the expiration date using {@link ExpirationDateDialog}.
     * Defaults to false.
     *
     * @param activity  used as the parent activity for the dialog
     * @param useDialog {@code false} to use a numeric keyboard to enter the expiration date,
     *                  {@code true} to use a custom dialog to enter the expiration date. Defaults to {@code true}.
     */
    public void useDialogForExpirationDateEntry(AppCompatActivity activity, boolean useDialog) {
        mExpirationDateDialog = ExpirationDateDialog.create(activity, this);
        mUseExpirationDateDialog = useDialog;
        setShowKeyboardOnFocus(!mUseExpirationDateDialog);
        setCursorVisible(!mUseExpirationDateDialog);
    }

    @Override
    public void setOnClickListener(View.OnClickListener l) {
        mClickListener = l;
    }

    @Override
    public void onClick(View v) {
        if (mUseExpirationDateDialog) {
            closeSoftKeyboard();
            mExpirationDateDialog.show();
        }

        if (mClickListener != null) {
            mClickListener.onClick(v);
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (mExpirationDateDialog == null) {
            return;
        }

        if (focused && mUseExpirationDateDialog) {
            closeSoftKeyboard();
            mExpirationDateDialog.show();
        } else if (mUseExpirationDateDialog) {
            mExpirationDateDialog.dismiss();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mExpirationDateDialog != null && mExpirationDateDialog.isShowing()) {
            mExpirationDateDialog.dismiss();
        }
    }

    /**
     * @return the 2-digit month, formatted with a leading zero if necessary. If no month has been
     * specified, an empty string is returned.
     */
    public String getMonth() {
        String string = getString();
        if (string.length() < 2) {
            return "";
        }
        return getString().substring(0, 2);
    }

    /**
     * @return the 2- or 4-digit year depending on user input.
     * If no year has been specified, an empty string is returned.
     */
    public String getYear() {
        String string = getString();
        if (string.length() == 4 || string.length() == 6) {
            return getString().substring(2);
        }else if (string.length() == 7){
            return getString().substring(3);
        }
        return "";
    }

    /**
     * @return whether or not the input is a valid card expiration date.
     */
    @Override
    public boolean isValid() {
        return isOptional() || DateValidator.isValid(getMonth(), getYear());
    }

    @Override
    public String getErrorMessage() {
        if (TextUtils.isEmpty(getText())) {
            return getContext().getString(R.string.bt_expiration_required);
        } else {
            return getContext().getString(R.string.bt_expiration_invalid);
        }
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        mChangeWasAddition = lengthAfter > lengthBefore;
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (mChangeWasAddition) {
            if (editable.length() == 1 && Character.getNumericValue(editable.charAt(0)) >= 2) {
                prependLeadingZero(editable);
            }
        }

        Object[] paddingSpans = editable.getSpans(0, editable.length(), SlashSpan.class);
        for (Object span : paddingSpans) {
            editable.removeSpan(span);
        }

        if (editable.length() == 6) {
            addDateSlash(editable);
        }

        if (((getSelectionStart() == 4 && !editable.toString().endsWith("20")) || getSelectionStart() == 6)
                && isValid()) {
            focusNextView();
        }
    }

    private void setShowKeyboardOnFocus(boolean showKeyboardOnFocus) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setShowSoftInputOnFocus(showKeyboardOnFocus);
        } else {
            try {
                // API 16-21
                final Method method = EditText.class.getMethod("setShowSoftInputOnFocus", boolean.class);
                method.setAccessible(true);
                method.invoke(this, showKeyboardOnFocus);
            } catch (Exception e) {
                try {
                    // API 15
                    final Method method = EditText.class.getMethod("setSoftInputShownOnFocus", boolean.class);
                    method.setAccessible(true);
                    method.invoke(this, showKeyboardOnFocus);
                } catch (Exception e1) {
                    mUseExpirationDateDialog = false;
                }
            }
        }
    }

    private void prependLeadingZero(Editable editable) {
        char firstChar = editable.charAt(0);
        editable.replace(0, 1, "0").append(firstChar);
    }

    public void addDateSlash(Editable editable) {
        final int index = 2;
        final int length = editable.length();
        if (index <= length) {
            editable.setSpan(new SlashSpan(), index - 1, index,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Convenience method to get the input text as a {@link String}.
     */
    private String getString() {
        Editable editable = getText();
        return editable != null ? editable.toString() : "";
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void removeTextListener() {
        removeTextChangedListener(this);
    }

    public void addTextListener() {
        addTextChangedListener(this);
    }
}
