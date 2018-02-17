package team4.com.voicerehabilitation;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class AssessmentActivity extends AppCompatActivity implements OnChartValueSelectedListener{

    private LineChart vChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment);


        //chart initialization

        vChart = (LineChart) findViewById(R.id.vChart);
        vChart.setOnChartValueSelectedListener(this);

        vChart.getDescription().setEnabled(true);

        // enable scaling and dragging
        vChart.setDragEnabled(true);
        vChart.setScaleEnabled(true);
        vChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        vChart.setPinchZoom(true);

        // set an alternative background color
        vChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        vChart.setData(data);

        Legend l = vChart.getLegend();

        l.setForm(Legend.LegendForm.LINE);
        l.setTypeface(Typeface.SANS_SERIF);
        l.setTextColor(Color.WHITE);

        XAxis xl = vChart.getXAxis();
        xl.setTypeface(Typeface.SANS_SERIF);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = vChart.getAxisLeft();
        leftAxis.setTypeface(Typeface.SANS_SERIF);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(1000f);
        leftAxis.setAxisMinimum(-1000f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = vChart.getAxisRight();
        rightAxis.setEnabled(false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.realtime, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.actionAdd: {
                addEntry();
                break;
            }
            case R.id.actionClear: {
                vChart.clearValues();
                Toast.makeText(this, "Chart cleared!", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.actionFeedMultiple: {
                feedMultiple();
                break;
            }
        }
        return true;
    }

    public void buttonAddEntry(View view) {
        addEntry();
    }


    private void addEntry(){
        LineData data = vChart.getData();

        ILineDataSet correctSet = data.getDataSetByIndex(0);


        if(correctSet == null){
            correctSet = createSet("correct sounds", ColorTemplate.getHoloBlue());
            data.addDataSet(correctSet);
        }


        float yVal = (float)(Math.random() * 40) + 30f;
        data.addEntry(new Entry(correctSet.getEntryCount(), yVal), 0);
        data.notifyDataChanged();
        vChart.notifyDataSetChanged();


        vChart.setVisibleXRangeMaximum(120);

        vChart.moveViewToX(data.getEntryCount());

    }

    private LineDataSet createSet(String label, int rgb) {

        LineDataSet set = new LineDataSet(null, label);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(rgb);
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(1f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;

    }

    private Thread thread;

    private void feedMultiple(){
        if (thread != null)
            thread.interrupt();

        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                addEntry();
            }
        };

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {

                    // Don't generate garbage runnables inside the loop.
                    runOnUiThread(runnable);

                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("Entry selected", e.toString());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(thread != null){
            thread.interrupt();
        }
    }
}

