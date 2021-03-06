package com.example.js.myapplication;

import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
@SuppressWarnings("deprecation") // 아이스크림 샌드위치 이후 버전에서 탭 액티비티를 사용하면 경고가 나오는것을 막아주기 위한 코드


public class MainActivity extends TabActivity {

    BluetoothAdapter mBluetoothAdapter; //블루투스 어댑터
    static final int REQUEST_ENABLE_BT = 10;  //블루투스 활성 상태 식별자
    int mPairedDeviceCount = 0; //페어링 된 디바이스의 개수를 저장하는 변수

    Set<BluetoothDevice> pairedDevices; //연결할 블루투스 정보를 조회할 수 있는 클래스.
    BluetoothDevice BD;

    //페어링된 기기와 통신에 사용되는 변수
    BluetoothSocket mSocket = null;
    OutputStream mOutputStream = null;
    InputStream mInputStream = null;
    String mStrDelimiter = "\n";
    char mCharDelimiter =  '\n';

    Thread WorkerThread = null;
    int readBufferPosition;
    byte[] readBuffer;

    TextView txt,valuetxt;
    EditText mEditReceive;
    Button mButtonSend, testButton;
    measureProcess analogValues;

    List<Entry> list = new ArrayList<Entry>();// 측정 전류값 저장 리스트
    LineDataSet ls; //차트에 표시할 데이터 셋
    List<ILineDataSet> dataSets;
    LineData ldata;
    LineChart chart ;

    // 가로 막대 그래프에 사용하는 변수
    ArrayList<BarEntry> hbList = new ArrayList<BarEntry>();
    HorizontalBarChart hbChart;   //일별 전력 사용량 측정에 사용할 변수
    BarDataSet hbDataset;
    BarData hbData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //******************탭 사용 하기 코드 *************************

        TabHost tabHost = getTabHost();

        TabHost.TabSpec tabSpec1 = tabHost.newTabSpec("tab1").setIndicator("실시간 전류 측정");
        tabSpec1.setContent(R.id.tab1);
        tabHost.addTab(tabSpec1);

        TabHost.TabSpec tabSpec2 = tabHost.newTabSpec("tab2").setIndicator("일별 전력 사용량");
        tabSpec2.setContent(R.id.tab2);
        tabHost.addTab(tabSpec2);

        tabHost.setCurrentTab(0);

        //******************************************************************

        //mEditReceive = (EditText)findViewById(R.id.receiveString);
        txt = (TextView)findViewById(R.id.textView);

        mButtonSend = (Button)findViewById(R.id.button1);

        txt.setText("testing");
        analogValues = new measureProcess(100);
        chart = (LineChart) findViewById(R.id.chart); //라인차트 추가
        hbChart = (HorizontalBarChart) findViewById(R.id.chart2);

        // no description text
        chart.getDescription().setEnabled(false);
        hbChart.getDescription().setEnabled(false);

        list.add(new Entry(2,3));
        ls= new LineDataSet(list,"전류량");
        dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(ls);
        ldata = new LineData(dataSets);
        chart.setData(ldata);

        //가로막대 그래프 초기 값 설정
        for(int i =0 ; i < 8; i++){
            float val = (float) (Math.random()*100);
            hbList.add(new BarEntry(i*10f,val));
        }

        hbDataset = new BarDataSet(hbList,"전기 사용량");
        hbDataset.setDrawIcons(false);

        ArrayList<IBarDataSet> hbDataSets = new ArrayList<IBarDataSet>();
        hbDataSets.add(hbDataset);

        hbData = new BarData(hbDataSets);
        hbChart.setData(hbData);






        mButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBlueTooth();
            }
        });

    }

    //블루투스의 연결 상태를 점검하고 연결하는 메소드
    void checkBlueTooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // 장치가 블루투스 지원하지 않는 경우
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기", Toast.LENGTH_LONG).show();
            finish();
        }

        //블루투스가 비활성화 되어 있을때 활성화
        if (!mBluetoothAdapter.isEnabled()) {
            //블루투스를 활성화 상태로 바꾸기 위해 사용자 동의 요청
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else
            selectDevice();  //페어링 할 블루투스 장치를 선택 하는 메소드
    }

    void selectDevice() {
        //페어링 할 장치를 선택하는 메소드
        pairedDevices = mBluetoothAdapter.getBondedDevices();//페어링된 장치 목록 불러오기.
        mPairedDeviceCount = pairedDevices.size();

        if (mPairedDeviceCount == 0) {
            //페어링 된 장치가 없는 경우.
            Toast.makeText(getApplicationContext(), "장치 없음ㅠ", Toast.LENGTH_LONG).show();
        } else {
            //페어링 된 장치가 있는 경우
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("블루투스 장치 선택");

            List<String> listItems = new ArrayList<String>(); //페어링 된 기기들을 저장할 리스트
            for (BluetoothDevice device : pairedDevices) {
                //검색한 페어링 된 기기들의 이름을 리스트에 저장
                listItems.add(device.getName());
            }
            listItems.add("취소");
            //list형태의 자료들을 CharSequence 배열 형 태로 저장
            final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
            listItems.toArray(new CharSequence[listItems.size()]);

            builder.setItems(items, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i != mPairedDeviceCount) {
                        connectToSelectedDevice(items[i].toString());
                    }
                }
            });

            builder.setCancelable(false);//뒤로가긱 비활성화
            AlertDialog alert =builder.create();
            alert.show();

        }
    }

    void sendData(String msg){
        //msg+= mStrDelimiter; //문자열 종료(\n)
        try{
            mOutputStream.write(msg.getBytes());//문자열 전송
        }
        catch(Exception e){
            Toast.makeText(getApplicationContext(), "데이터 전송오류",
                    Toast.LENGTH_LONG).show();
        }
    }

    void beginListenForData(){
        //데이터 수신(쓰레드) 수신된 메시지를 계속 검사함.
        final Handler handler = new Handler();

        readBufferPosition =0;   //버퍼 내 수신 문자 저장 위치
        readBuffer = new byte[1024]; //수신 버퍼

        //문자열 수신 쓰레드
        WorkerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    try{
                        int byteAvailable = mInputStream.available(); //수신 데이터 확인
                        if(byteAvailable > 0){//데이터가 수신 된 경우
                            byte[] packetBytes = new byte[byteAvailable];
                            mInputStream.read(packetBytes);
                            //데이터를 읽음.
                            for(int i=0; i < byteAvailable; i++){
                                byte b = packetBytes[i];
                                if(b == mCharDelimiter/*'\n버퍼의 끝을 표시'*/){
                                    byte[] encodeBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer,0,encodeBytes,0,encodeBytes.length);

                                    final String data = new String(encodeBytes,"US-ASCII");
                                    readBufferPosition = 0 ;

                                    handler.post(new Runnable() {
                                        //수신된 문자열 데이터 처리
                                        @Override
                                        public void run() {
                                            txt.setText("Receiving");
                                            int idx = data.indexOf('\r');
                                            String aVal= data.substring(0,idx);

                                            analogValues.pushValue(aVal);
                                            list.clear();
                                            for(int i=0 ; i < analogValues.getLastPosition();i++){
                                                list.add(new Entry(i,analogValues.getValueAt(i)));
                                            }
                                            ls= new LineDataSet(list,"전류량");
                                            dataSets = new ArrayList<ILineDataSet>();
                                            dataSets.add(ls);
                                            ldata = new LineData(dataSets);
                                            chart.setData(ldata);

                                            chart.invalidate();

                                        }

                                    });
                                }
                                else{
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }catch(Exception e){
                        Toast.makeText(getApplicationContext(), "데이터 수신오류", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    void connectToSelectedDevice(String selectedDeviceName) {
        //블루투스와 연결 하는 메소드
        BD = FindDevice(selectedDeviceName);
        //선택된 장치를  BD 객체에 저장
        UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        //랜덤한 unique 키 생성 ;
        try {
            //블루투스와 스마트폰이 통신할수 있는 소켓을 생성 .
            mSocket = BD.createInsecureRfcommSocketToServiceRecord(uuid);
            mSocket.connect();


            //데이터 송수신을 위한 스트림
            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();
            txt.setText("연결 성공");
            beginListenForData();
            WorkerThread.start();
        }
        catch(Exception e){
            Toast.makeText(getApplicationContext(),
                    "연결 실패", Toast.LENGTH_LONG).show();
        }

    }

    BluetoothDevice FindDevice(String name) {
        //이름이 name인 디바이스를 찾아 리턴하는 메소드
        BluetoothDevice selectedDevice = null;

        for (BluetoothDevice device : pairedDevices) {
            //페어링 된 기기의 목록중에서 name 과 이름이 같은 객체를 selectedDevice에 저장.
            if (name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }



    @Override
    protected void onDestroy() {
        try{
            WorkerThread.interrupt(); // 데이터 수신 쓰레드 종료
            mInputStream.close();
            mSocket.close();
        }catch(Exception e){}
        super.onDestroy();
    }



}
