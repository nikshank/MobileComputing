package com.example.kunalsutradhar.heartratemonitor;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.Date;


/**We have used MPChart library for generating the graph.
 * We included the library as a jar file in our project and used APIs to draw the graph.**/

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.formatter.XAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class MainActivity extends AppCompatActivity {
    Timestamp currentTime = null;
    String name = "";
    String age1 = "";
    String sex1 = "";
    String id = "";
    String tableName = "";
    int fieldCheck = 0;

    static boolean stopGraph = false;

    private TextView patientID;
    private TextView age;
    private TextView patientName;
    private TextView sex;

    private EditText patientIDText;
    private EditText ageText;
    private EditText patientNameText;

    private RadioGroup type;

    private Button run;
    private Button stop;
    private Button submit;
    private Button upload;
    private Button download;
    private LinearLayout linearLayout;
    private LineChart lineChart;

    SQLiteDatabase myDatabase;
    SQLiteDatabase myDownloadDatabase;
    MyBroadcastReceiver mybroadcastRecvr;
    int intentCheck = 1;

    int serverResponseCode = 0;

    String upLoadServerUri = "http://impact.asu.edu/CSE535Spring18Folder/UploadToServer.php";

    final String uploadFilePath = "/mnt/sdcard/android/data/CSE535_ASSIGNMENT2/";
    final String uploadFileName = "Group32.db";

    private ProgressDialog processDialog;

    @Override
    protected void onStart() {
        super.onStart();

    }

    private void setUIElementsForMainActivity() {
        patientID = (TextView) findViewById(R.id.textView);
        age = (TextView) findViewById(R.id.textView2);
        patientName = (TextView) findViewById(R.id.textView3);
        sex = (TextView) findViewById(R.id.textView4);

        patientIDText = (EditText) findViewById(R.id.editText);
        ageText = (EditText) findViewById(R.id.editText2);
        patientNameText = (EditText) findViewById(R.id.editText3);
        mybroadcastRecvr = new MyBroadcastReceiver();
        type = (RadioGroup) findViewById(R.id.radioGroup);
        submit = (Button) findViewById(R.id.submit);
        run = (Button) findViewById(R.id.button);
        stop = (Button) findViewById(R.id.button2);
        upload = (Button) findViewById(R.id.upload);
        download = (Button) findViewById(R.id.download);
        stop.setEnabled(false);
        run.setEnabled(false);
        upload.setEnabled(false);
        download.setEnabled(false);
    }

    private void setRequestPermissions() {

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                100);
    }

    private void createDBAndSetViewsEnabled(File database) {
        myDatabase = SQLiteDatabase.openOrCreateDatabase(database, null);

        fieldCheck = 0;
        name = patientNameText.getText().toString();
        age1 = ageText.getText().toString();
        id = patientIDText.getText().toString();
        if (type.getCheckedRadioButtonId() != -1) {
            sex1 = ((RadioButton) findViewById(type.getCheckedRadioButtonId())).getText().toString();
        }
        if (name.equals("") || age1.equals("") || id.equals("") || sex1.equals("")) {
            Toast.makeText(MainActivity.this, "Some of the fields are empty", Toast.LENGTH_LONG).show();
            fieldCheck = 1;
        } else if (name.contains(" ") || age1.contains(" ") || id.contains(" ")) {
            Toast.makeText(MainActivity.this, "Spaces are not allowed", Toast.LENGTH_LONG).show();
            fieldCheck = 1;
        } else {
            tableName = name + "_" + id + "_" + age1 + "_" + sex1;
            Log.i("Table Name", tableName);
        }
        if (fieldCheck == 0) {
            try {
                myDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + "(timeStamp VARCHAR, xValues FLOAT(5), yValues FLOAT(5), zValues FLOAT(5))");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setViewForSubmit() {
        if (intentCheck == 1) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Accelerometer.MY_ACTION);
            registerReceiver(mybroadcastRecvr, intentFilter);
            intentCheck = 0;
        }
        run.setEnabled(true);
        download.setEnabled(true);
        submit.setEnabled(false);
        upload.setEnabled(false);
    }

    private void setViewForRun() {

        if (intentCheck == 1) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Accelerometer.MY_ACTION);
            registerReceiver(mybroadcastRecvr, intentFilter);
            intentCheck = 0;
        }

        GraphThread graphThread = new GraphThread(MainActivity.this);
        stopGraph = false;
        Thread t = new Thread(graphThread);
        t.start();

        upload.setEnabled(false);
        stop.setEnabled(true);
        run.setEnabled(false);
        download.setEnabled(false);

    }

    public void setViewForStop(Intent serviceIntent) {
        if (intentCheck == 0) {
            unregisterReceiver(mybroadcastRecvr);
            intentCheck = 1;
        }
        stopService(serviceIntent);
        upload.setEnabled(true);
        run.setEnabled(true);
        stop.setEnabled(false);
        download.setEnabled(true);
        submit.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setting request permissions for the app to read/write external storage.
        setRequestPermissions();

        //creating service intent
        final Intent serviceIntent = new Intent(this, Accelerometer.class);

        //setting all the views with view defined in activity_main
        setUIElementsForMainActivity();


        //defining action for submit - perform submit on click listener.
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                startService(serviceIntent);
                clearGraph();


                //create a db path in SD card for the DB
                File sdCard = Environment.getExternalStorageDirectory();
                File dbpath = new File(sdCard.getAbsolutePath() + "/android/data/CSE535_ASSIGNMENT2");

                if (!dbpath.exists())
                    dbpath.mkdirs();

                File database = new File(dbpath, uploadFileName);
                if (!database.exists()) {
                    try {
                        database.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //create a Db if it doesnt exist and also set views.
                createDBAndSetViewsEnabled(database);

                setViewForSubmit();
            }
        });

        //Onclicklistener for run button
        run.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                generateGraph();
                setViewForRun();
            }
        });

        //Onclicklistener for stop button
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearGraph();
                setViewForStop(serviceIntent);
            }
        });

        //Onclick Listener for upload button
        upload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                processUploading();
            }
        });

        //Onclick Listener for download button
        download.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                processDownloading();
                run.setEnabled(false);
//                isFirst=false;
            }
        });

        linearLayout = (LinearLayout) findViewById(R.id.glayout);
        lineChart = new LineChart(this);

        //Adding lineChart view to Linear Layout
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        linearLayout.addView(lineChart, params);
    }

    //Method for initializing the graph
    private void generateGraph() {
        //Setting description
        lineChart.setDescription("");

        lineChart.setDrawGridBackground(false);

        //Setting Background color
        lineChart.setBackgroundColor(Color.LTGRAY);

        //Working on Data
        LineData lineData = new LineData();
        lineData.setValueTextColor(Color.WHITE);

        //Set the data to Line Chart
        lineChart.setData(lineData);

        //Disabling zoom
        lineChart.setScaleEnabled(false);

        //Fetching legend object
        Legend l = lineChart.getLegend();

        //Customizing Legend
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        //Configuring x-axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(Color.BLACK);
        xAxis.setEnabled(true);
        xAxis.setDrawGridLines(true);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setDrawLabels(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setSpaceBetweenLabels(1);
        xAxis.setValueFormatter(new XAxisValueFormatter() {
            int i = -1;
            String s = null;

            @Override
            public String getXValue(String s, int i, ViewPortHandler viewPortHandler) {
                i++;
                s = String.valueOf(i);
                return s;
            }
        });

        //Configuring y-axis
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setTextColor(Color.BLACK);
        yAxis.setAxisMaxValue(15f);
        yAxis.setAxisMinValue(-15f);
        yAxis.setDrawGridLines(true);
        yAxis.setEnabled(true);
        yAxis.setDrawLabels(true);
        yAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);

        YAxis y2 = lineChart.getAxisRight();
        y2.setEnabled(false);
    }

    //Creating method to add entry to the the graph
    public void addEntry() {
        Float x = 0f;
        Float y = 0f;
        Float z = 0f;
        LineData data = lineChart.getData();
        if (data != null) {
            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
            LineDataSet set1 = (LineDataSet) data.getDataSetByIndex(1);
            LineDataSet set2 = (LineDataSet) data.getDataSetByIndex(2);

            if (set == null) {
                set = createLineSet();
                set.setLabel("X-Value");
                set.setColor(Color.BLUE);
                data.addDataSet(set);
            }
            if (set1 == null) {
                set1 = createLineSet();
                set1.setLabel("Y-Value");
                set1.setColor(Color.GREEN);
                data.addDataSet(set1);
            }
            if (set2 == null) {
                set2 = createLineSet();
                set2.setLabel("Z-Value");
                set2.setColor(Color.RED);
                data.addDataSet(set2);
            }
            try {
                Cursor c = myDatabase.rawQuery("SELECT * FROM " + tableName, null);
                c.moveToLast();
                x = c.getFloat(c.getColumnIndex("xValues"));
                y = c.getFloat(c.getColumnIndex("yValues"));
                z = c.getFloat(c.getColumnIndex("zValues"));
                Log.i("x=", String.valueOf(x));
                Log.i("y=", String.valueOf(y));
                Log.i("z=", String.valueOf(z));
                data.addXValue("");
                data.addEntry(new Entry(x, set.getEntryCount()), 0);
                data.addEntry(new Entry(y, set1.getEntryCount()), 1);
                data.addEntry(new Entry(z, set2.getEntryCount()), 2);
                lineChart.notifyDataSetChanged();
                lineChart.setVisibleXRange(0f, 9f);
                lineChart.moveViewToX(data.getXValCount() - 7);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //Creates initial data point on Graph
    private LineDataSet createLineSet() {
        LineDataSet set = new LineDataSet(null, "Line Graph");
        set.setDrawCubic(true);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 177));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);

        return set;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    //Executes when back button is pressed
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            clearGraph();
//            isFirst = true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //Stop thread for deleting graph
    private void clearGraph() {
        lineChart.clear();
        stopGraph = true;
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            int flag = 0;
            long elapsedTime = 0;
            float xVal, yVal, zVal;
            if (currentTime == null) {
                currentTime = new Timestamp(new Date().getTime());
                flag = 1;
            } else {
                Timestamp ts = new Timestamp(new Date().getTime());
                if (ts.getTime() - currentTime.getTime() > 999) {
                    Log.d("Time", String.valueOf(ts.getTime() - currentTime.getTime()));
                    currentTime = ts;
                    flag = 1;
                }
            }
            if (flag == 1) {
                 xVal = arg1.getFloatExtra("X", 0.0f);
                 yVal = arg1.getFloatExtra("Y", 0.0f);
                 zVal = arg1.getFloatExtra("Z", 0.0f);

                try {
                    long currentTime = System.currentTimeMillis();
                    ContentValues values = new ContentValues();
                    values.put("xValues", xVal);
                    values.put("yValues", yVal);
                    values.put("zValues", zVal);
                    values.put("timeStamp", currentTime);
                    myDatabase.insert(tableName, null, values);
                } catch (SQLiteException e) {
                }
            }
        }

    }

    private void processUploading() {
        final TaskUpload taskUploadForExecution = new TaskUpload(MainActivity.this);
        taskUploadForExecution.execute(uploadFilePath + "" + uploadFileName);
    }

    private void processDownloading() {
        final TaskDownload taskDownloadForExecution = new TaskDownload(MainActivity.this);
        taskDownloadForExecution.execute("http://impact.asu.edu/CSE535Spring18Folder/" + uploadFileName);
    }

    private class TaskUpload extends AsyncTask<String, Void, String> {
        private Context context;

        public TaskUpload(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            String fileName = uploadFilePath + "" + uploadFileName;

            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String endLine = "\r\n";
            String twoHyphens = "--";
            String starBorder = "*****";
            int bytesRead, bytesAvailable, sizeOfBuffer;
            byte[] myBuffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File sourceFile = new File(fileName);

            if (!sourceFile.isFile()) {

                Log.e("uploadFile", "Source File does not exist :"
                        + uploadFilePath + "" + uploadFileName);

                runOnUiThread(new Runnable() {
                    public void run() {
                    }
                });
            } else {
                try {

                   //opening URL connection
                    FileInputStream fiStream = new FileInputStream(sourceFile);
                    URL url = new URL(upLoadServerUri);

                    // Opening HTTP  connection
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + starBorder);
                    conn.setRequestProperty("uploaded_file", fileName);

                    dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + starBorder + endLine);
                    dos.writeBytes("Content-Disposition: form-data; name=uploaded_file ;filename=" + fileName + "" + endLine);
                    dos.writeBytes(endLine);

                    // buffer creation
                    bytesAvailable = fiStream.available();
                    sizeOfBuffer = Math.min(bytesAvailable, maxBufferSize);
                    myBuffer = new byte[sizeOfBuffer];

                    bytesRead = fiStream.read(myBuffer, 0, sizeOfBuffer);
                    while (bytesRead > 0) {
                        //writing to the data output stream
                        dos.write(myBuffer, 0, sizeOfBuffer);
                        bytesAvailable = fiStream.available();
                        sizeOfBuffer = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fiStream.read(myBuffer, 0, sizeOfBuffer);

                    }
                    dos.writeBytes(endLine);

                    //after file data, writing the multi-part form data
                    dos.writeBytes(twoHyphens + starBorder + twoHyphens + endLine);

                    serverResponseCode = conn.getResponseCode();
                    String msgServer = conn.getResponseMessage();

                    //logging server response message
                    Log.i("uploadFile", "HTTP Response is : "
                            + msgServer + ": " + serverResponseCode);

                    if (serverResponseCode == 200) {

                        runOnUiThread(new Runnable() {
                            public void run() {

                                Toast.makeText(MainActivity.this, "File Upload Completed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    fiStream.close();
                    dos.flush();
                    dos.close();

                }
                catch (MalformedURLException ex)
                {
                    ex.printStackTrace();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "MalformedURLException",
                                    Toast.LENGTH_SHORT).show();
                        }});

                    Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "Exception Occurred : see logcat ",
                                    Toast.LENGTH_SHORT).show();
                        }});
                    Log.e("Upload the Exception", "Exception Message:" + e.getMessage(), e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println("Uploaded");
            processDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            System.out.println("Starting upload");

            processDialog = new ProgressDialog(MainActivity.this);
            processDialog.setMessage("Loading... Please wait...");
            processDialog.setIndeterminate(false);
            processDialog.setCancelable(false);
            processDialog.show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class TaskDownload extends AsyncTask<String, Void, String> {

        private Context context;

        public TaskDownload(Context context) {
            this.context = context;
        }

        /**
         * Below method performs the task of downloading file in the background thread
         */
        @Override
        protected String doInBackground(String... f_url) {
            int writeCount;
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dbpath = new File(sdCard.getAbsolutePath() + "/android/data/CSE535_ASSIGNMENT2_Extra");

                //Check for dbpath. If doesn't exist, then create it.
                if (!dbpath.exists())
                    dbpath.mkdirs();

                String rootDir = Environment.getExternalStorageDirectory().toString();

                System.out.println("Downloading");

                // Open a new connection and input stream to read file with buffer of size 8k
                URL url = new URL(f_url[0]);
                URLConnection conn = url.openConnection();
                conn.connect();

                InputStream inStream = new BufferedInputStream(url.openStream(), 8192);
                OutputStream outStream = new FileOutputStream(rootDir + "/android/data/CSE535_ASSIGNMENT2_Extra/" + uploadFileName);
                byte data[] = new byte[1024];

                while ((writeCount = inStream.read(data)) != -1) {
                    outStream.write(data, 0, writeCount);
                }
                outStream.flush();
                outStream.close();
                inStream.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
                runOnUiThread(new Runnable() {
                    public void run() {

                        Toast.makeText(MainActivity.this, "File Download not Completed.",
                                Toast.LENGTH_SHORT).show();
                    }});
            }
            return null;
        }

        /**
         * Pre execution process to the background thread
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            System.out.println("Starting download");

            processDialog = new ProgressDialog(MainActivity.this);
            processDialog.setMessage("Loading... Please wait...");
            processDialog.setIndeterminate(false);
            processDialog.setCancelable(false);
            processDialog.show();
        }

        /**
         * Post execution process to the background task
         **/
        @Override
        protected void onPostExecute(String file_url) {
            System.out.println("Downloaded");
            processDialog.dismiss();

            generateGraph();

            File sdCard = Environment.getExternalStorageDirectory();
            String dbpath = sdCard.getAbsolutePath() + "/android/data/CSE535_ASSIGNMENT2_Extra/" + uploadFileName;

            myDownloadDatabase = SQLiteDatabase.openDatabase(dbpath, null, 0);

            Float xVal[] = new Float[10];
            Float yVal[] = new Float[10];
            Float zVal[] = new Float[10];
            int count;
            Cursor curr;

            //Data processing
            LineData lineData = lineChart.getData();

            if (lineData != null) {

                try
                {
                    //extract the datasets
                    LineDataSet x_data = (LineDataSet) lineData.getDataSetByIndex(0);
                    LineDataSet y_data = (LineDataSet) lineData.getDataSetByIndex(1);
                    LineDataSet z_data = (LineDataSet) lineData.getDataSetByIndex(2);

                    //Check for availability of each co-ordinate dataset for the monitor

                    if (x_data == null)
                    {
                        x_data = createLineSet();
                        x_data.setLabel("X-Value");
                        x_data.setColor(Color.BLUE);
                        lineData.addDataSet(x_data);
                    }
                    if (y_data == null) {
                        y_data = createLineSet();
                        y_data.setLabel("Y-Value");
                        y_data.setColor(Color.GREEN);
                        lineData.addDataSet(y_data);
                    }
                    if (z_data == null) {
                        z_data = createLineSet();
                        z_data.setLabel("Z-Value");
                        z_data.setColor(Color.RED);
                        lineData.addDataSet(z_data);
                    }

                    curr = myDownloadDatabase.rawQuery("SELECT * FROM " + tableName, null);
                    count = curr.getCount();

                    for (int i = 10, j = 0; i > 0; i--, j++)
                    {
                        curr.moveToPosition(count - i);
                        xVal[j] = curr.getFloat(curr.getColumnIndex("xValues"));
                        yVal[j] = curr.getFloat(curr.getColumnIndex("yValues"));
                        zVal[j] = curr.getFloat(curr.getColumnIndex("zValues"));

                        Log.i("x=", String.valueOf(xVal[j]));
                        Log.i("y=", String.valueOf(yVal[j]));
                        Log.i("z=", String.valueOf(zVal[j]));
                        lineData.addXValue("");
                        lineData.addEntry(new Entry(xVal[j], x_data.getEntryCount()), 0);
                        lineData.addEntry(new Entry(yVal[j], y_data.getEntryCount()), 1);
                        lineData.addEntry(new Entry(zVal[j], z_data.getEntryCount()), 2);
                    }

                    lineChart.notifyDataSetChanged();
                    lineChart.setVisibleXRange(0f, 9f);
                    lineChart.moveViewToX(lineData.getXValCount() - 7);

                    runOnUiThread(new Runnable() {
                        public void run() {

                            Toast.makeText(MainActivity.this, "File Download Complete.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        public void run() {

                            Toast.makeText(MainActivity.this, "User data not found. Run for 10 seconds",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            submit.setEnabled(true);
        }

    }
}

class GraphThread extends Thread {
    MainActivity mainActivity = new MainActivity();

    public GraphThread(MainActivity activity) {
        this.mainActivity = activity;
    }

    @Override
    public void run() {
        //Infinite Loop. Iterates until Stop button is pressed
        for (int i = 0; ; i++) {
            if (mainActivity.stopGraph == false) {
                mainActivity.addEntry();
            } else {
                Log.i("check", "exited loop");
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
