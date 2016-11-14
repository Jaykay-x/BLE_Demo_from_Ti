/**************************************************************************************************/
/* 主Activity                                                                                     */
/*                                                                                                */
/**************************************************************************************************/
package org.bluetooth.bledemo;

import org.bluetooth.bledemo.R;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class ScanningActivity extends ListActivity {
	
	private static final long SCANNING_TIMEOUT = 5 * 1000; /* 5 seconds */
	private static final int ENABLE_BT_REQUEST_ID = 1;
	
	private boolean mScanning = false;	//扫描标记
	private Handler mHandler = new Handler();	//处理线程间通信.
	private DeviceListAdapter mDevicesListAdapter = null; //设备列表容器.
	private BleWrapper mBleWrapper	= null; //
	
	
	/***********************************************************************************************/
	/* 主Activit创建的时候就生产一个空回调的BleWrapper对象.然后调用uiDeviceFound去查找外围器件                      */
	/***********************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        // 创建一个BleWrapper对象.使用一个空的回调对象. 参数里new了一个空的回调函数集合. 
        // BleWrapper各种蓝牙操作的函数集合.但不包括回调函数.
        mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null() {
        	@Override
        	
        	// App启动后首先扫描硬件.
        	public void uiDeviceFound(	final BluetoothDevice device, 
        								final int rssi, 
        								final byte[] record) {
        		
        		//扫描完去调用本类的方法,把扫描到的结果写到动态数组里面保存起来
        		handleFoundDevice(device, rssi, record);
        	}
        });
        
        // 检查是否有蓝牙并支持BLE.
        if(mBleWrapper.checkBleHardwareAvailable() == false) {
        	bleMissing();
        }
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	
       	// 检查蓝牙是否能用
    	if(mBleWrapper.isBtEnabled() == false) {
    		
    		// 如果没启动,要求打开它
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			
			// 设置子Activity返回
		    startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_ID);
		}
    	
    	
    	// 调用mBleWrapper对象.初始化.
        mBleWrapper.initialize();
    	
        // new一个动态数组.
    	mDevicesListAdapter = new DeviceListAdapter(this);
        setListAdapter(mDevicesListAdapter); // ListView用的东西.
    	
        // 自动开始扫描外围设备
    	mScanning = true;
		
    	// 设置扫描的超时时间
		addScanningTimeout();  
		
		//扫描
		mBleWrapper.startScanning();  
		
		//扫描有结果后刷新的ui.
        invalidateOptionsMenu(); 
    };
    
    @Override
    protected void onPause() {
    	super.onPause();
    	mScanning = false;    	
    	mBleWrapper.stopScanning();
    	invalidateOptionsMenu();
    	
    	mDevicesListAdapter.clearList();
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scanning, menu);

        if (mScanning) {
            menu.findItem(R.id.scanning_start).setVisible(false);
            menu.findItem(R.id.scanning_stop).setVisible(true);
            

        } else {
            menu.findItem(R.id.scanning_start).setVisible(true);
            menu.findItem(R.id.scanning_stop).setVisible(false);
           
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scanning_start:
            	mScanning = true;
            	mBleWrapper.startScanning(); //扫描
                break;
            case R.id.scanning_stop:
            	mScanning = false;
            	mBleWrapper.stopScanning();
                break;         
        }
        invalidateOptionsMenu();
        return true;
    }
    
    /* user has selected one of the device */
    // 用户在列表上点了其中一个设备.
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	// 根据用户点击的位置,取出设备
        final BluetoothDevice device = mDevicesListAdapter.getDevice(position);
        
        if (device == null) //如果没有设备
        	return;			//返回
        
        // 从Intent的参数可以看出,这里调用PeripheralActivity类,
        // 设备不为空
        final Intent intent = new Intent(this, PeripheralActivity.class); // 启动PeripheralActivity类.
        
        // 把设备的名字,地址,RSSi通过intent传递给 PeripheralActivity (已经实现了Ble的各种回调函数).
        intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_RSSI, mDevicesListAdapter.getRssi(position));
        
        if (mScanning) {
            mScanning = false;
            invalidateOptionsMenu();
            mBleWrapper.stopScanning();
        }

        startActivity(intent);  // 运行PeripheralActivity.
    }    
    
    /*  子Activity执行完后回到主界面,或者还有一些子模块的数据交给主界面处理.就要调用onActivityResult.  */
    @Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
        if (requestCode == ENABLE_BT_REQUEST_ID) {
        	
        	if(resultCode == Activity.RESULT_CANCELED) {
		    	btDisabled();
		        return;
		    }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

	private void addScanningTimeout() {
		Runnable timeout = new Runnable() {
            @Override
            public void run() {
            	if(mBleWrapper == null) 
            		return;
                mScanning = false;
                mBleWrapper.stopScanning();
                invalidateOptionsMenu();
            }
        };
        mHandler.postDelayed(timeout, SCANNING_TIMEOUT);
	}    

	// 添加远程设备到当前的设备列表里
    private void handleFoundDevice(final BluetoothDevice device,
            final int rssi,
            final byte[] scanRecord)
	{
		// 添加到Ui
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				//把搜索到的远程设备写到动态数组
				mDevicesListAdapter.addDevice(device, rssi, scanRecord);
				mDevicesListAdapter.notifyDataSetChanged();
			}
		});
	}	

    private void btDisabled() {
    	Toast.makeText(this, "Sorry, BT has to be turned ON for us to work!", Toast.LENGTH_LONG).show();
        finish();    	
    }
    
    private void bleMissing() {
    	Toast.makeText(this, "BLE Hardware is required but not available!", Toast.LENGTH_LONG).show();
        finish();    	
    }
}
