package il.co.anyway.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DatePreference extends DialogPreference {

    public static String MINIMUM_DATE = "01/01/2005";
    public static String MAXIMUM_DATE = "01/01/2015";
    private final String LOG_TAG = DatePreference.class.getSimpleName();
    private int lastDate = 0;
    private int lastMonth = 0;
    private int lastYear = 0;
    private String dateValue;
    private CharSequence mSummary;
    private DatePicker picker = null;

    public DatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText("ok");
        setNegativeButtonText("cancel");
    }

    public static int getYear(String dateval) {
        String[] pieces = dateval.split("/");
        return (Integer.parseInt(pieces[2]));
    }

    public static int getMonth(String dateval) {
        String[] pieces = dateval.split("/");
        return (Integer.parseInt(pieces[1]));
    }

    public static int getDate(String dateval) {
        String[] pieces = dateval.split("/");
        return (Integer.parseInt(pieces[0]));
    }

    @Override
    protected View onCreateDialogView() {
        picker = new DatePicker(getContext());
        picker.setCalendarViewShown(false);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date minDate, maxDate;
        try {
            minDate = sdf.parse(MINIMUM_DATE);
            maxDate = sdf.parse(MAXIMUM_DATE);

            picker.setMinDate(minDate.getTime());
            picker.setMaxDate(maxDate.getTime());

        } catch (ParseException e) {
            Log.e(LOG_TAG, "Error: " + e.getMessage());
        }

        return (picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        // (-1) - month is stored as a number between 1-12, the picker save the month in range of 0-11
        picker.updateDate(lastYear, lastMonth - 1, lastDate);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            lastYear = picker.getYear();
            // (+1) - month is stored as a number between 1-12, the picker save the month in range of 0-11
            lastMonth = picker.getMonth() + 1;
            lastDate = picker.getDayOfMonth();

            String dateValue = String.valueOf(lastDate) + "/"
                    + String.valueOf(lastMonth) + "/"
                    + String.valueOf(lastYear);

            if (callChangeListener(dateValue)) {
                persistString(dateValue);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        dateValue = null;

        if (restoreValue) {
            if (defaultValue == null) {
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yyyy");
                String formatted = format1.format(cal.getTime());
                dateValue = getPersistedString(formatted);
            } else {
                dateValue = getPersistedString(defaultValue.toString());
            }
        } else {
            dateValue = defaultValue.toString();
        }
        lastYear = getYear(dateValue);
        lastMonth = getMonth(dateValue);
        lastDate = getDate(dateValue);
    }

    public String getText() {
        return dateValue;
    }

    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        dateValue = text;

        persistString(text);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    public CharSequence getSummary() {
        return mSummary;
    }

    public void setSummary(CharSequence summary) {
        if (summary == null && mSummary != null || summary != null
                && !summary.equals(mSummary)) {
            mSummary = summary;
            notifyChanged();
        }
    }
}
