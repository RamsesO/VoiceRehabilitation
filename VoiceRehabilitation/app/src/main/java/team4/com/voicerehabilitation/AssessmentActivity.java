package team4.com.voicerehabilitation;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
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
import android.widget.TextView;
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

public class AssessmentActivity extends AppCompatActivity implements OnChartValueSelectedListener {

    private LineChart vChart;
    private long items;

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
    private boolean isAudioStarted;
    private boolean isFFTComplete = false;


    private AudioRecord audioRecord = null;

    public float[] audioData;
    private int count = 0;
    public float[] magnitudes;
    public ArrayList<Integer> peakIndexes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment);

        id = getIntent().getIntExtra("buttonId", -1);
        continueParsing = true;
        isAudioStarted = false;
        String uriParse = "";

        switch (id) {
            case R.id.eeBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.ee;
                setTitle("Assessing: /i/ - (ee)");
                setExpectedF("270", "2290");
                break;

            case R.id.iBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.i;
                setTitle("Assessing: /l/-(i)");
                setExpectedF("390", "1990");
                break;

            case R.id.eBtn:
                //uriParse = "android.resource://" + getPackageName() + "/" + R.raw.e;
                setTitle("Assessing: / /-(e)");
                setExpectedF("530", "1840");
                break;

            case R.id.aeBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.ae;
                setTitle("Assessing: /ae/-(ae)");
                setExpectedF("660", "1720");
                break;

            case R.id.ahBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.ah;
                setTitle("Assessing: /a/-(ah)");
                setExpectedF("730", "1090");
                break;

            case R.id.awBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.aw;
                setTitle("Assessing: /ə/-(aw)");
                setExpectedF("570", "840");
                break;

            case R.id.ûBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.omega;
                setTitle("Assessing: /ʊ/-(û)");
                setExpectedF("440", "1020");
                break;

            case R.id.ooBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.oo;
                setTitle("Assessing: /u/-(oo)");
                setExpectedF("300", "870");
                break;

            case R.id.uBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.u;
                setTitle("Assessing: /ʌ/-(u)");
                setExpectedF("640", "1190");
                break;

            case R.id.erBtn:
                uriParse = "android.resource://" + getPackageName() + "/" + R.raw.er;
                setTitle("Assessing: /ɛ/-(er)");
                setExpectedF("490", "1350");
                break;

            default:
                System.out.println("went to default");
                break;
        }
        playVideo = findViewById(R.id.videoView1);
        uri = Uri.parse(uriParse);
        playVideo.setVideoURI(uri);

        Button playButton = findViewById(R.id.assistanceBTN);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
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

            case R.id.actionSave: {
                vChart.saveToPath("title" + System.currentTimeMillis(), "");
                break;
            }
        }
        return true;
    }

    private void setExpectedF(String f1, String f2){
        TextView f1ExpView = (TextView) findViewById(R.id.f1ValExp);
        TextView f2ExpView = (TextView) findViewById(R.id.f2ValExp);
        TextView f1View = (TextView) findViewById(R.id.f1Val);
        TextView f2View = (TextView) findViewById(R.id.f2Val);

        f1ExpView.setText("F1 Exp: " + f1);
        f2ExpView.setText("F2 Exp: " + f2);
        f1View.setText("");
        f2View.setText("");
    }

    private void setCurrentF(ArrayList<Integer> list){
        TextView f1View = (TextView) findViewById(R.id.f1Val);
        TextView f2View = (TextView) findViewById(R.id.f2Val);

        f1View.setText("F1 Curr: " + (list.get(0) * 2) + "");
        f2View.setText("F2 Curr: " + (list.get(1) * 2) + "");
    }


    public void buttonAddEntry(View view) {
        //addEntry();

        if (isAudioStarted) {
            Toast.makeText(this, "Turning Audio off!", Toast.LENGTH_LONG).show();
            isStopButtonPressed = true;
            isAudioStarted = false;
        } else {
            recordAudio();
            isAudioStarted = true;
        }
    }

    private void m(float[] magnitude, ArrayList<Integer> peaks) {
        ArrayList<ILineDataSet> lines = new ArrayList<>();

        //ILineDataSet correctVoiceGraph = initializeCorrectGraph();
        ILineDataSet currentVoiceGraph = voiceGraph(magnitude);

        //lines.add(correctVoiceGraph);
        lines.add(currentVoiceGraph);

        //((LineDataSet) lines.get(1)).enableDashedLine(10, 15, 0);
        vChart.setData(new LineData(lines));
        vChart.notifyDataSetChanged();
        vChart.invalidate();

        setCurrentF(peaks);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                continueParsing = false;
            }
        }, 1000);

    }


    private LineDataSet initializeCorrectGraph() {
        ArrayList<Entry> correctList = new ArrayList<Entry>();

        for (int i = 0; i < 22500 / 50; i++) {
            correctList.add(new Entry(i, (float) (Math.random() * 22500 / 50)));
        }

        LineDataSet correctSet = createSet("correct sound", Color.GREEN, correctList);
        return correctSet;
    }

    private LineDataSet voiceGraph(float[] magnitude) {

        ArrayList<Entry> voiceList = new ArrayList<Entry>();

//        double value = 0;
//        for (int i = 0; i < magnitudes.length; i++) {
//            magnitudes[i] = (float) (100 * Math.sin(2 * Math.PI * 120 * value));
//            value += 0.0001;
//        }
        int arrayIndex = 0;
        for (int i = 0; i < 100; i++) {
            float value = 0;
            for(int o = 0; o < 73; o++){
                value += magnitude[o + arrayIndex];
        }
            arrayIndex += 73;
            value = value / 73;
            voiceList.add(new Entry(i, value));
        }

        LineDataSet voiceSet = createSet("voice", Color.BLACK, voiceList);
        return voiceSet;
    }


    private void addEntry() {

        LineData data = vChart.getData();

        ILineDataSet correctSet = data.getDataSetByIndex(0);


        if (correctSet == null) {
            correctSet = createSet("correct sounds", ColorTemplate.getHoloBlue(), null);
            data.addDataSet(correctSet);
        }


        float yVal = (float) (Math.random() * 26000);
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

    private void feedMultiple() {
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
                for (int i = 0; i < Integer.MAX_VALUE && !isStopButtonPressed; i++) {
                    int shortsRead = 0;
                    audioData = new float[SAMPLE_RATE * RECORD_TIME];
                    while (shortsRead < audioData.length) {
                        int numberOfIndexs = audioRecord.read(audioData, 0, audioData.length, AudioRecord.READ_NON_BLOCKING);
                        shortsRead += numberOfIndexs;
                    }
                    generateGraphData(audioData.clone());
                    while(!isFFTComplete);
                    while (continueParsing);
                    isFFTComplete = false;
                    continueParsing = true;
                }
                float max = Float.MIN_VALUE;
                for (int i = 0; i < audioData.length; i++) {
                    if (audioData[i] > max) {
                        max = audioData[i];
                    }
                }


                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;

            }
        }).start();

    }

    public void generateGraphData(final float[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

                final float[] magnitude = calculateFFT(data);
                final ArrayList<Integer> peakIndex = calculatePeaks(magnitude, 500);
                magnitudes = magnitude;
                peakIndexes = peakIndex;

                System.out.println("Iteration " + count);
                for (int i = 0; i < magnitude.length; i++) {
                    System.out.println("Index: " + i + "Value: " + magnitude[i]);

                }
                count++;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        m(magnitude.clone(), (ArrayList<Integer>) peakIndex.clone());
                    }
                });
                isAudioStarted = true;
                isFFTComplete = true;
            }
        }).start();
    }

    public float[] calculateFFT(float[] audioData) {

        FloatFFT_1D fft = new FloatFFT_1D(audioData.length);
        fft.realForward(audioData);

        float[] magnitudes = new float[audioData.length / 2];
        for (int frequencyBin = 0; frequencyBin < audioData.length / 2; frequencyBin++) {
            float real = audioData[frequencyBin * 2];
            float imaginary = audioData[2 * frequencyBin + 1];
            float magnitude = (float) Math.sqrt(real * real + imaginary * imaginary);
            magnitudes[frequencyBin] = magnitude;
        }
        return magnitudes;
    }

    public ArrayList<Integer> calculatePeaks(float[] magnitudes, int minimumDistance) {
        ArrayList<Integer> peakIndexes = new ArrayList<>();
        int frequencyBin = 0;
        float max = magnitudes[0];
        int lastPeakIndex = 0;
        while (frequencyBin < magnitudes.length - 1) {
            while (frequencyBin < magnitudes.length - 1 && magnitudes[frequencyBin + 1] >= max) {
                frequencyBin++;
                max = magnitudes[frequencyBin];
            }
            if (!peakIndexes.isEmpty() && frequencyBin - lastPeakIndex < minimumDistance) {
                if (magnitudes[lastPeakIndex] > magnitudes[frequencyBin]) {
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

            while (frequencyBin < magnitudes.length - 1 && magnitudes[frequencyBin + 1] <= max) {
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
    protected void onPause() {
        super.onPause();

        if (thread != null) {
            thread.interrupt();
        }
    }
}

