                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  package com.example.yoga.discover;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.yoga.Model.GlobalVariable;
import com.example.yoga.bluetooth.ChatActivity;
import com.example.yoga.R;

import java.util.ArrayList;
import java.util.Objects;

public class BluetoothActivity extends AppCompatActivity {
    private ListView listView;
    private DevicesAdapter adapter;
    private ArrayList<BluetoothDevice> list;
    private ScanDevices mScanDevices;

    private GlobalVariable global = GlobalVariable.Companion.getInstance();

    public static final String[] permissions = { // 權限清單
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_PRIVILEGED",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_ADVERTISE",
            "android.permission.ACCESS_COARSE_LOCATION"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_bluetooth);
        Objects.requireNonNull(getSupportActionBar()).hide();
        requestPermissions(permissions, 1);


        // 初始化 python 環境
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }

        init();
    }

    private void init() {
        listView = (ListView) findViewById(R.id.listview);
        list = new ArrayList<>();
        adapter = new DevicesAdapter(this, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(icl);
        mScanDevices = new ScanDevices();
        initReceiver();
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    public void onClick(View view){ // onClick 搜尋設備
        list.clear();
        list.addAll(mScanDevices.getBondedDevices()); // 先添加已綁定過的設備
        adapter.notifyDataSetChanged();
        mScanDevices.startDiscovery(); // 搜尋附近的設備
    }

    public void onClickPass(View view){ // 沒有瑜珈墊
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("remoteAddress", "0");
        startActivity(intent);
        finish();
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.requireNonNull(intent.getAction()).equals(BluetoothDevice.ACTION_FOUND)) { //接收附近的裝置
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                assert device != null;
                String address = device.getAddress();
                for (int i = 0; i < list.size(); i++) { //避免有重複的裝置出現
                    if (address == null || address.equals(list.get(i).getAddress())) {
                        return;
                    }
                }
                list.add(device);
                adapter.notifyDataSetChanged();
            }
        }
    };

    AdapterView.OnItemClickListener icl = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String address = list.get(position).getAddress();
            startChat(address);
        }
    };

    private void startChat(String remoteAddress) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("remoteAddress", remoteAddress);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        global.backgroundMusic.pause();
        super.onDestroy();
    }
    @Override
    protected void onStart(){
        super.onStart();
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                global.backgroundMusic.play();
            }
        }, 800);
    }
    @Override
    protected void onPause() {
        super.onPause();
        global.backgroundMusic.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        global.backgroundMusic.play();
    }
}
