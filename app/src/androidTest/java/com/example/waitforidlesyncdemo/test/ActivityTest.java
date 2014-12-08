package com.example.waitforidlesyncdemo.test;

import android.app.Instrumentation;
import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;

import com.example.waitforidlesyncdemo.MainActivity;
import com.example.waitforidlesyncdemo.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class ActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public static final Summary sSummary = new Summary();
    public static final String TAG = "WaitForIdleSyncTest";
    public static final int STARTUP_DELAY = 5000;
    public static final int LOOP_TIMES = 50;
    public static final long MS_TO_NS = (long) Math.pow(10, 6);
    public static final long DELAY_BETWEEN_WAITS = 50; // ms

    public ActivityTest() {
        super(MainActivity.class);
    }


    private final Handler mHandler = new Handler();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fail("Test took longer than 1 minute to run.");
            }
        }, 60000);
    }

    @Override
    protected void tearDown() throws Exception {
        mHandler.removeCallbacksAndMessages(null);

        super.tearDown();
    }

    public void testWaitForIdleSync_withProgress() {
        getActivity();

        Log.d(TAG, "Showing progress spinner…");
        startupWait();

        double variance = waitFiftyTimes(true);

        if (sSummary != null /* !!!! */) {
            sSummary.testWithSpinnerVariance = variance;
        }
    }

    public void testWaitForIdleSync_withoutProgress() {
        final MainActivity activity = getActivity();

        Log.d(TAG, "Hiding progress spinner…");
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.findViewById(R.id.progress).setVisibility(View.GONE);
            }
        });

        startupWait();

        double variance = waitFiftyTimes(true);

        if (sSummary != null /* !!!! */) {
            sSummary.testWithoutSpinnerVariance = variance;
        }
    }

    public void testControl_withProgress() {
        getActivity();

        Log.d(TAG, "CONTROL Showing progress spinner…");
        startupWait();
        double variance = waitFiftyTimes(false);

        if (sSummary != null /* !!!! */) {
            sSummary.controlWithSpinnerVariance = variance;
        }

    }

    public void testControl_withoutProgress() {
        final MainActivity activity = getActivity();

        Log.d(TAG, "CONTROL Hiding progress spinner…");
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.findViewById(R.id.progress).setVisibility(View.GONE);
            }
        });

        startupWait();

        double variance = waitFiftyTimes(false);

        if (sSummary != null /* !!!! */) {
            sSummary.controlWithoutSpinnerVariance = variance;
        }

    }

    public void testZzPrintSummary() {
        if (sSummary == null) {
            Log.d(TAG, "this platform is broken, so a summary could not be made");
        } else {
            Log.d(TAG, sSummary.toString());
        }
    }

    private void startupWait() {
        Log.d(TAG, "waiting 5s for the system to settle…");
        try {
            Thread.sleep(STARTUP_DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private double waitFiftyTimes(boolean idleSync) {
        if (idleSync) {
            Log.d(TAG, "Calling waitForIdleSync 50×…");
        } else {
            Log.d(TAG, "Waiting and looping 50×…");
        }

        ArrayList<Long> timing = new ArrayList<Long>(LOOP_TIMES);

        Instrumentation instrumentation = getInstrumentation();

        for (int i = 0; i < LOOP_TIMES; i++) {
            timing.add(System.nanoTime());

            try {
                Thread.sleep(DELAY_BETWEEN_WAITS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (idleSync) {
                instrumentation.waitForIdleSync();
            }
        }

        ArrayList<Double> differences = new ArrayList<Double>(LOOP_TIMES - 1);

        StringBuilder sb = new StringBuilder();
        sb.append("Timing: ");
        for (int i = 1; i < LOOP_TIMES; i++) {
            double difference =
                    ((timing.get(i) - timing.get(i - 1)) - DELAY_BETWEEN_WAITS * MS_TO_NS)
                            / (double) MS_TO_NS;
            differences.add(difference);

            sb.append(String.format(Locale.US, "%2.2f, ", difference));
        }

        Log.d(TAG, sb.toString());

        Log.d(TAG, String.format(Locale.US, "Minimum: %2.2f", Collections.min(differences)));
        Log.d(TAG, String.format(Locale.US, "Maximum: %2.2f", Collections.max(differences)));
        double variance = Collections.max(differences) - Collections.min(differences);
        Log.d(TAG, String.format(Locale.US, "Variance: %2.2f", variance));

        return variance;
    }

    public static class Summary {
        public double controlWithSpinnerVariance;
        public double controlWithoutSpinnerVariance;
        public double testWithSpinnerVariance;
        public double testWithoutSpinnerVariance;

        public String toString() {
            double test = (testWithSpinnerVariance - testWithoutSpinnerVariance);
            double control = (controlWithSpinnerVariance - controlWithoutSpinnerVariance);
            return String
                    .format(Locale.US,
                            "variance due to waitForIdleSync when showing spinner (test, control): %2.2f %2.2f",
                            test, control);

        }
    }
}
