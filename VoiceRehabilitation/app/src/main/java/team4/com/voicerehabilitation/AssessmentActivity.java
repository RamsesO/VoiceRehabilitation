package team4.com.voicerehabilitation;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Process;
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
import android.widget.VideoView;

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

import org.jtransforms.fft.FloatFFT_1D;

import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.List;

public class AssessmentActivity extends AppCompatActivity implements OnChartValueSelectedListener{

    private LineChart vChart;
    private long items;

    // graph fields
    ArrayList<ILineDataSet> lines;
    ILineDataSet correctVoiceGraph;
    // video fields
    private VideoView playVideo;
    private Uri uri;
    private int id;

    // audio fields
    private static final int SAMPLE_RATE = 44100; // Hz
    private static final int ENCODING = AudioFormat.ENCODING_PCM_FLOAT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    private static final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
    private static final int RECORD_TIME = 1;

    private boolean isStopButtonPressed = false;
    private boolean continueParsing;
    private boolean state;


    private AudioRecord audioRecord = null;

    public float[] audioData;

    public float[] magnitudes;
    public ArrayList<Integer> peakIndexes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment);

        id = getIntent().getIntExtra("buttonId", -1);
        continueParsing = true;
        state = true;
        String uriParse = "";

        switch(id)
        {
            case R.id.eeBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.ae;
                setTitle("Assessing a");

                break;
            case R.id.iBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.a;
                setTitle("Assessing i");
                break;
            default:
                System.out.println("went to default");
                break;
        }
        playVideo = findViewById(R.id.videoView1);
        uri = Uri.parse(uriParse);
        playVideo.setVideoURI(uri);

        Button playButton = findViewById(R.id.assistanceBTN);
        playButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                playVideo.start();
            }
        });

        //chart initialization

        items = 0;

        vChart = (LineChart) findViewById(R.id.vChart);
        vChart.setOnChartValueSelectedListener(this);

        vChart.getDescription().setEnabled(false);

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

            case R.id.actionSave: {
                vChart.saveToPath("title" + System.currentTimeMillis(), "");
                break;
            }
        }
        return true;
    }

    public void buttonAddEntry(View view) {
        //addEntry();

        if(state) {
            lines = new ArrayList<>();
            correctVoiceGraph = initializeCorrectGraph();

//            new Thread(new Runnable() {
//                @Override
//                public void run() {
                    while (state) {

                        recordAudio();
                        //while loop for parsing data and getting chunks
                        while (continueParsing && state) ;

                        ILineDataSet currentVoiceGraph = voiceGraph();

                        lines.add(correctVoiceGraph);
                        lines.add(currentVoiceGraph);

                        ((LineDataSet) lines.get(1)).enableDashedLine(10, 15, 0);
                        vChart.setData(new LineData(lines));
                        vChart.invalidate();
                        lines.clear();
                        continueParsing = true;
                    }
//                }
//            }).start();
        }
        else
            state = false;



    }


    private LineDataSet initializeCorrectGraph(){
        ArrayList<Entry> correctList = new ArrayList<Entry>();

        for(int i = 0; i < 22500; i++){
            correctList.add(new Entry(i, (float)(Math.random() * 22500)));
        }

        LineDataSet correctSet = createSet("correct sound", Color.GREEN, correctList);
        return correctSet;
    }

    private LineDataSet voiceGraph(){

        ArrayList<Entry> voiceList = new ArrayList<Entry>();

        for(int i = 0; i < this.magnitudes.length; i++){
            voiceList.add(new Entry(i, this.magnitudes[i]));
        }

        LineDataSet voiceSet = createSet("voice", Color.BLACK, voiceList);
        return voiceSet;
    }


    private void addEntry(){

        LineData data = vChart.getData();

        ILineDataSet correctSet = data.getDataSetByIndex(0);


        if(correctSet == null){
            correctSet = createSet("correct sounds", ColorTemplate.getHoloBlue(), null);
            data.addDataSet(correctSet);
        }


        float yVal = (float)(Math.random() * 700) - 300f;
        float xVal = (float) items++;
        data.addEntry(new Entry(xVal, yVal), 0);

        data.notifyDataChanged();
        vChart.notifyDataSetChanged();


        vChart.setVisibleXRangeMaximum(60);

        vChart.moveViewToX(data.getEntryCount());

    }

    private LineDataSet createSet(String label, int rgb, ArrayList<Entry> entries) {

        LineDataSet set = new LineDataSet(entries, label);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(rgb);
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(5f);
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


    public void recordAudio() {
        this.audioData = new float[SAMPLE_RATE * RECORD_TIME];

        this.audioRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(BUFFER_SIZE)
                .build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                audioRecord.startRecording();
                for( int i =0; i < Integer.MAX_VALUE && !isStopButtonPressed; i++) {
                    int shortsRead = 0;
                    while (shortsRead < audioData.length) {
                        int numberOfIndexs = audioRecord.read(audioData, 0, audioData.length, AudioRecord.READ_NON_BLOCKING);
                        shortsRead += numberOfIndexs;
                    }
                    generateGraphData(audioData);
                }
                float max = Float.MIN_VALUE;
                for (int i = 0; i < audioData.length; i++){
                    if (audioData[i] > max){
                        max = audioData[i];
                    }
                }


                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;

            }
        }).start();

    }

    public void generateGraphData(final float[] audioData){
        new Thread(new Runnable() {
            @Override
            public void run() {

                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

                float[] magnitude = calculateFFT(audioData);
                ArrayList<Integer> peakIndex = calculatePeaks(magnitude, 500);
                magnitudes = magnitude;
                peakIndexes = peakIndex;
                continueParsing = false;
            }
        }).start();
    }

    public float[] calculateFFT(float[] audioData){

        FloatFFT_1D fft = new FloatFFT_1D(audioData.length);
        fft.realForward(audioData);

        float[] magnitudes = new float[audioData.length/2];
        for (int frequencyBin = 0; frequencyBin < audioData.length/2; frequencyBin++){
            float real = audioData[frequencyBin*2];
            float imaginary = audioData[2*frequencyBin+1];
            float magnitude = (float)Math.sqrt(real * real + imaginary * imaginary);
            magnitudes[frequencyBin] = magnitude;
        }
        return magnitudes;
    }

    public ArrayList<Integer> calculatePeaks(float[] magnitudes, int minimumDistance){
        ArrayList<Integer> peakIndexes = new ArrayList<>();
        int frequencyBin = 0;
        float max = magnitudes[0];
        int lastPeakIndex = 0;
        while(frequencyBin < magnitudes.length - 1){
            while(frequencyBin < magnitudes.length - 1 && magnitudes[frequencyBin + 1] >= max){
                frequencyBin++;
                max = magnitudes[frequencyBin];
            }
            if(!peakIndexes.isEmpty() && frequencyBin - lastPeakIndex < minimumDistance){
                if(magnitudes[lastPeakIndex] > magnitudes[frequencyBin]){
                    //Do not do anything, old peak is better
                } else {
                    //new peak is higher so replace the old close by one
                    peakIndexes.remove(peakIndexes.size() - 1);
                    peakIndexes.add(frequencyBin);
                    lastPeakIndex = frequencyBin;
                    //do not change peakIndex
                }
            } else {
                //Add a new peak not near any others
                peakIndexes.add(frequencyBin);
                lastPeakIndex = frequencyBin;
            }

            while(frequencyBin < magnitudes.length - 1 && magnitudes[frequencyBin + 1] <= max){
                frequencyBin++;
                max = magnitudes[frequencyBin];
            }
        }
        return peakIndexes;
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

