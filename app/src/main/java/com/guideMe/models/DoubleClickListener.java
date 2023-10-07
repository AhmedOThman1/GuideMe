package com.guideMe.models;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * A simple double-click listener
 * Usage:
 * // Scenario 1: Setting double click listener for myView
 * myView.setOnClickListener(new DoubleClickListener() {
 *
 * @author Srikanth Venkatesh
 * @version 1.0
 * @Override public void onDoubleClick() {
 * // double-click code that is executed if the user double-taps
 * // within a span of 200ms (default).
 * }
 * });
 * <p>
 * // Scenario 2: Setting double click listener for myView, specifying a custom double-click span time
 * myView.setOnClickListener(new DoubleClickListener(500) {
 * @Override public void onDoubleClick() {
 * // double-click code that is executed if the user double-taps
 * // within a span of 500ms (default).
 * }
 * });
 * @since 2014-09-15
 */
public abstract class DoubleClickListener implements OnClickListener {

    // The time in which the second tap should be done in order to qualify as
    // a double click
    private static final long DEFAULT_QUALIFICATION_SPAN = 200;
    private long doubleClickQualificationSpanInMillis;
    private long timestampLastClick;

    public DoubleClickListener() {
        doubleClickQualificationSpanInMillis = DEFAULT_QUALIFICATION_SPAN;
        timestampLastClick = 0;
    }

    public DoubleClickListener(long doubleClickQualificationSpanInMillis) {
        this.doubleClickQualificationSpanInMillis = doubleClickQualificationSpanInMillis;
        timestampLastClick = 0;
    }

    boolean doubleClick = false;

    @Override
    public void onClick(View v) {
        Log.w("CCCC", "" + (SystemClock.elapsedRealtime() - timestampLastClick) + "," + doubleClickQualificationSpanInMillis);
        if ((SystemClock.elapsedRealtime() - timestampLastClick) < doubleClickQualificationSpanInMillis) {
            Log.w("DO", "HERE");
            doubleClick = true;
            onDoubleClick();
        }

        new Handler().postDelayed(() -> {
            if (!doubleClick) {
                Log.w("CL", "HERE");
                onClick();
            }
        }, doubleClickQualificationSpanInMillis + 50);

        timestampLastClick = SystemClock.elapsedRealtime();
    }

    public abstract void onClick();

    public abstract void onDoubleClick();

}