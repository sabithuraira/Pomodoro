package farifam.com.pomodoro;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.motion.SmotionPedometer;

import org.w3c.dom.Text;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements PedometerCallback {

    PedometerHelper pedometerHelper;
    TextView txt_timer, txt_jobs, txt_title_rest, txt_timer_rest, txt_step_rest, txt_start;
    Button btnStart, btn_reset;
    LinearLayout linear_rest, linear_jobs;
    CountDownTimer countDown;
    ImageButton bar_info;


    public static final String MyPREFERENCES = "pref_name" ;
    public static final String pref_job = "pref_job";

    public static final String pref_old_step = "pref_old_step";

    private long time_jobs=25*60000;
    private long time_short_break=5*60000;
    private long time_long_break=15*60000;
    private long step_short_break=100;
    private long step_long_break=200;


    SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pedometerHelper = new PedometerHelper(this);

        try {
            pedometerHelper.initialize();
            pedometerHelper.setPedometerCallback(this);

        }catch (IllegalArgumentException e){
            showErrorDialog("Something went wrong",e.getMessage());
            return;
        }catch (SsdkUnsupportedException e){
            showErrorDialog("SDK Not Supported",e.getMessage());
            return;
        }

        pedometerHelper.setModePedometer(PedometerHelper.MODE_PEDOMETER_REALTIME);

        txt_timer=(TextView)findViewById(R.id.txt_timer);
        txt_jobs=(TextView)findViewById(R.id.txt_jobs);

        txt_title_rest=(TextView)findViewById(R.id.txt_title_rest);
        txt_timer_rest=(TextView)findViewById(R.id.txt_timer_rest);
        txt_step_rest=(TextView)findViewById(R.id.txt_step_rest);

        txt_start = (TextView) findViewById(R.id.txt_start);
        btnStart = (Button)findViewById(R.id.btn_start);
        btn_reset = (Button)findViewById(R.id.btn_reset);
        linear_jobs = (LinearLayout) findViewById(R.id.linear_jobs);
        linear_rest = (LinearLayout) findViewById(R.id.linear_rest);
        linear_rest.setVisibility(View.GONE);
        bar_info = (ImageButton) findViewById(R.id.bar_info);

        preferences = this.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        editor = preferences.edit();


        bar_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Pomodoro Info");
                alertDialog.setMessage(getResources().getString(R.string.pomodoro));
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newJobs();

//                if (pedometerHelper.isStarted() == false){
//                    pedometerHelper.start();
//                }
//                else {
//                    pedometerHelper.stop();
//                }
            }
        });


        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt(pref_job, 0);
//                editor.putLong(pref_step, 0);
                editor.apply();

                btnStart.setVisibility(View.VISIBLE);
                linear_rest.setVisibility(View.GONE);
                linear_jobs.setVisibility(View.VISIBLE);

                txt_start.setVisibility(View.VISIBLE);

                txt_start.setText("Click START to begin job "+ Integer.toString(preferences.getInt(pref_job,0)+1));
                txt_jobs.setText("Waiting for next jobs");
                txt_timer.setText("00 : 00");

                if(countDown!=null)
                    countDown.cancel();
            }
        });

//        Refresh();
    }

    private void newJobs(){
        btnStart.setVisibility(View.GONE);
        txt_start.setVisibility(View.GONE);

        int current_jobs= preferences.getInt(pref_job,0);
        editor.putInt(pref_job, current_jobs+1);
        editor.putLong(pref_old_step, 0);
        editor.apply();
        editor.apply();


        countDown = new CountDownTimer(time_jobs, 1000) {
            public void onTick(long millisUntilFinished) {
                txt_jobs.setText("Jobs "+Integer.toString(preferences.getInt(pref_job,0)));
                txt_timer.setText(timeToText(millisUntilFinished));
            }

            public void onFinish() {
                Refresh();

                linear_jobs.setVisibility(View.GONE);
                linear_rest.setVisibility(View.VISIBLE);

                if(preferences.getInt(pref_job,0)+1<=4){
                    ShortBreak();
                }
                else{
                    LongBreak();
                }
            }
        }.start();
    }

    private void afterBreak(){
        if(countDown!=null)
            countDown.cancel();

        linear_rest.setVisibility(View.GONE);
        linear_jobs.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.VISIBLE);
        txt_start.setVisibility(View.VISIBLE);

        txt_start.setText("Click START to begin job "+ Integer.toString(preferences.getInt(pref_job,0)+1));
        txt_jobs.setText("Waiting for next jobs");
        txt_timer.setText("00 : 00");
    }

    String timeToText(long times){
        Long minutes = TimeUnit.MILLISECONDS.toMinutes(times);
        Long seconds = TimeUnit.MILLISECONDS.toSeconds(times) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(times));

        String minutes_label = (minutes >=10) ? Long.toString(minutes) : "0"+Long.toString(minutes);
        String seconds_label = (seconds >=10) ? Long.toString(seconds) : "0"+Long.toString(seconds);

        return minutes_label+" : "+seconds_label;
    }

    private void ShortBreak(){
        pedometerHelper.start();
        countDown = new CountDownTimer(time_short_break, 1000) {
            public void onTick(long millisUntilFinished) {
                txt_title_rest.setText("Take a short break 5 minutes or walk 100 steps");
                txt_timer_rest.setText(timeToText(millisUntilFinished));
            }

            public void onFinish() {
                afterBreak();
                pedometerHelper.stop();
            }
        }.start();
    }

    private void LongBreak(){

        pedometerHelper.start();
        countDown = new CountDownTimer(time_long_break, 1000) {
            public void onTick(long millisUntilFinished) {
                txt_title_rest.setText("Take a short break 15 minutes or walk 200 steps");
                txt_timer_rest.setText(timeToText(millisUntilFinished));
            }

            public void onFinish() {
                afterBreak();
                pedometerHelper.stop();
            }
        }.start();
    }

    private void Refresh(){
//        editor.putLong(pref_step, 0);
//        editor.apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pedometerHelper.stop();
    }

    private String getStatus(int status) {
        String str = null;
        switch (status) {
            case SmotionPedometer.Info.STATUS_WALK_UP:
                str = "Walk Up";
                break;
            case SmotionPedometer.Info.STATUS_WALK_DOWN:
                str = "Walk Down";
                break;
            case SmotionPedometer.Info.STATUS_WALK_FLAT:
                str = "Walk";
                break;
            case SmotionPedometer.Info.STATUS_RUN_DOWN:
                str = "Run Down";
                break;
            case SmotionPedometer.Info.STATUS_RUN_UP:
                str = "Run Up";
                break;
            case SmotionPedometer.Info.STATUS_RUN_FLAT:
                str = "Run";
                break;
            case SmotionPedometer.Info.STATUS_STOP:
                str = "Stop";
                break;
            case SmotionPedometer.Info.STATUS_UNKNOWN:
                str = "Unknown";
                break;
            default:
                break;
        }
        return str;
    }

    @Override
    public void motionStarted() {
//        btnStart.setText(R.string.stop);
    }

    @Override
    public void motionStopped() {

//        btnStart.setText(R.string.start);
    }

    @Override
    public void updateInfo(SmotionPedometer.Info info) {
        SmotionPedometer.Info pedometerInfo = info;

        if(preferences.getLong(pref_old_step,0) == 0){
            editor.putLong(pref_old_step, info.getCount(SmotionPedometer.Info.COUNT_TOTAL));
            editor.apply();
        }

        long old_step=preferences.getLong(pref_old_step,0);
        long step = info.getCount(SmotionPedometer.Info.COUNT_TOTAL) - old_step;
        txt_step_rest.setText(Long.toString(step));

        if(preferences.getInt(pref_job,0)<4 && step>=step_short_break) {
            editor.putLong(pref_old_step, info.getCount(SmotionPedometer.Info.COUNT_TOTAL));
            editor.apply();
            afterBreak();
        }
        else if(preferences.getInt(pref_job,0)>=4 && step>=step_long_break){
            editor.putLong(pref_old_step, info.getCount(SmotionPedometer.Info.COUNT_TOTAL));
            editor.apply();
            afterBreak();
        }


//        editor.putLong(pref_step, step);
//        editor.apply();

//        System.out.println("HelloMotion PedometerHelper");
//        double calorie = info.getCalorie();
//        double distance = info.getDistance();
//        double speed = info.getSpeed();
//        long count = info.getCount(SmotionPedometer.Info.COUNT_TOTAL);
//        int status = info.getStatus();
    }

    void showErrorDialog(String title,String message){
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
