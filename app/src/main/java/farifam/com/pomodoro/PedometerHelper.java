package farifam.com.pomodoro;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.motion.Smotion;
import com.samsung.android.sdk.motion.SmotionPedometer;

import java.util.Timer;
import java.util.TimerTask;

public class PedometerHelper {

    final static int MODE_PEDOMETER_REALTIME = 0;
    final static int MODE_PEDOMETER_PERIODIC = 1;

    private int mMode = MODE_PEDOMETER_REALTIME;

    private Smotion mMotion;
    private SmotionPedometer mPedometer;
    private Context context;
    private boolean started = false;
    private boolean isPedometerUpDownAvailable;

    PedometerCallback pedometerCallback;

    private String TAG = "PEDOMETER";

    private Timer mTimer;
    private long mInterval = 10000;
    SmotionPedometer.Info mInfo;



    public PedometerHelper(Context context) {

        this.context = context;

    }

    public void initialize() throws IllegalArgumentException,SsdkUnsupportedException {
        mMotion = new Smotion();

        mMotion.initialize(context);
        mPedometer = new SmotionPedometer(Looper.getMainLooper(), mMotion);

        isPedometerUpDownAvailable = mMotion.isFeatureEnabled(Smotion.TYPE_PEDOMETER_WITH_UPDOWN_STEP);
    }

    public void setPedometerCallback(PedometerCallback pedometerCallback) {
        this.pedometerCallback = pedometerCallback;
    }

    public void setModePedometer(int mode){

        this.mMode = mode;

    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public void start(){
        if (!started) {
            started = true;
            mPedometer.start(changeListener);
//            mPedometer.updateInfo();

            if (mMode == MODE_PEDOMETER_PERIODIC) {
                startTimer();
            }

            pedometerCallback.motionStarted();
        }
    };

    public void stop(){
        if (started == true) {
            started = false;
            mPedometer.stop();
            if (mMode == MODE_PEDOMETER_PERIODIC) {
                stopTimer();
            }

            pedometerCallback.motionStopped();

        }
    };

    private void startTimer() {
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new MyTimer(), 0, mInterval);
        }
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    final SmotionPedometer.ChangeListener changeListener = new
            SmotionPedometer.ChangeListener() {
                @Override
                public void onChanged(SmotionPedometer.Info info) {
                    // TODO Auto-generated method stub
                    if (mMode == MODE_PEDOMETER_REALTIME) {

                        pedometerCallback.updateInfo(info);
                    }
                }
            };



    class MyTimer extends TimerTask {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            mInfo = mPedometer.getInfo();

            handler.sendEmptyMessage(0);
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            // TODO Auto-generated method stub
            if (mInfo != null) {
                pedometerCallback.updateInfo(mInfo);
            }
        }
    };

}

interface PedometerCallback{
    void motionStarted();
    void motionStopped();
    void updateInfo(SmotionPedometer.Info info);

}