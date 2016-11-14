package org.bluetooth.bledemo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;  //作为中央来使用和处理数据.
import android.bluetooth.BluetoothGattCallback; //返回周边的状态
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;  //作为周边来提供数据.
//import android.bluetooth.BluetoothGattServerCallback;  //返回中央的状态和周边的数据.
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
//import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

public class BleWrapper {
	
	static final String TAG = "BleWrapper";
	
	// 定义多长时间去更新RSSI
    private static final int RSSI_UPDATE_TIME_INTERVAL = 1500; // 1.5 seconds

   
    private BleWrapperUiCallbacks mUiCallback = null;
    
    // 定义Ui 回调的空对象.
    private static final BleWrapperUiCallbacks NULL_CALLBACK = new BleWrapperUiCallbacks.Null(); 
    
    // 创建一个BleWrapper对象,设置它父Activity和回调对象(也就是BleWrapperUiCallbacks实现的接口).
    public BleWrapper(Activity parent, BleWrapperUiCallbacks callback) {
    	
    	this.mParent = parent;
    	mUiCallback = callback;
    	
    	if(mUiCallback == null) 
    		mUiCallback = NULL_CALLBACK;
    }

    // 蓝牙管理器
    public BluetoothManager getManager(){ 
    	return mBluetoothManager; 
    }
    
    //获取本地蓝牙适配器.
    public BluetoothAdapter getAdapter(){ 
    	return mBluetoothAdapter; 
    }
    
    //获取远程的蓝牙设备.
    public BluetoothDevice getDevice(){ 
    	return mBluetoothDevice; 
    }
    
    //Gatt 寻找,配置和读写Servcice端的各种attribute. 作为中央来使用和处理数据.
    public BluetoothGatt getGatt(){ 
    	return mBluetoothGatt; 
    }
    
    //作为周边来提供数据.    
    public BluetoothGattService getCachedService(){ 
    	return mBluetoothSelectedService; 
    }
    
    public List<BluetoothGattService> getCachedServices(){ 
    	return mBluetoothGattServices; 
    }
    
    public boolean isConnected(){
    	return mConnected; 
    }

	
    // 测试并检查手机的蓝牙设备蓝牙低功耗硬件是否可用
	public boolean checkBleHardwareAvailable() {
		
		// 先检查通用蓝牙硬件.
		// 然后获取到蓝牙管理器
		final BluetoothManager manager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
		if(manager == null) 
			return false;
		
		// 获取管理器的适配器.
		final BluetoothAdapter adapter = manager.getAdapter();
		if(adapter == null) 
			return false;
		
		// 检查BT LE是否可用
		boolean hasBle = mParent.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
		return hasBle;
	}    

	// 检查并使能本地蓝牙适配器
	public boolean isBtEnabled() {
		final BluetoothManager manager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
		if(manager == null) 
			return false;
		
		final BluetoothAdapter adapter = manager.getAdapter();
		if(adapter == null) 
			return false;
		
		return adapter.isEnabled();
	}
	
	//开始扫描附近的Ble设备.
	public void startScanning() {
		//startLeScan要求一个回调,这个参数一展开就看到....
        mBluetoothAdapter.startLeScan(mDeviceFoundCallback);
	}
	
	//停止扫描
	public void stopScanning() {
		mBluetoothAdapter.stopLeScan(mDeviceFoundCallback);	
	}
	
	// 初始化ble,ble管理器,本地蓝牙适配器.
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        if(mBluetoothAdapter == null) 
        	mBluetoothAdapter = mBluetoothManager.getAdapter();
        
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;    	
    }

    // 连接通过地址连接远程设备
    public boolean connect(final String deviceAddress) {
    	
        if (mBluetoothAdapter == null || deviceAddress == null) 
        	return false;
        
        mDeviceAddress = deviceAddress;
        
        if(mBluetoothGatt != null && mBluetoothGatt.getDevice().getAddress().equals(deviceAddress)) {
        	// just reconnect
        	return mBluetoothGatt.connect();
        }
        else {
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
            
            if (mBluetoothDevice == null) {
            	// 如果地址错误,则远程蓝牙设备不可用
                return false;
            }
            // 可用就直接去连接远程设备.
        	mBluetoothGatt = mBluetoothDevice.connectGatt(mParent, false, mBleCallback);
        }
        return true;
    }  
    
    // 与外设断开. 它仍然有可能过一会会重新连接这个GATT客户端
    public void diconnect() {
    	if(mBluetoothGatt != null) mBluetoothGatt.disconnect();
    	 mUiCallback.uiDeviceDisconnected(mBluetoothGatt, mBluetoothDevice);
    }

    // 完全关闭GATT客户端
    public void close() {
    	if(mBluetoothGatt != null) mBluetoothGatt.close();
    	mBluetoothGatt = null;
    }    

    //为连接提供RSSI值.
    public void readPeriodicalyRssiValue(final boolean repeat) {
    	mTimerEnabled = repeat;
    	// check if we should stop checking RSSI value
    	if(mConnected == false || mBluetoothGatt == null || mTimerEnabled == false) {
    		mTimerEnabled = false;
    		return;
    	}
    	
    	mTimerHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(mBluetoothGatt == null ||
				   mBluetoothAdapter == null ||
				   mConnected == false)
				{
					mTimerEnabled = false;
					return;
				}
				
				// request RSSI value
				mBluetoothGatt.readRemoteRssi();
				// add call it once more in the future
				readPeriodicalyRssiValue(mTimerEnabled);
			}
    	}, RSSI_UPDATE_TIME_INTERVAL);
    }    
    
    // 开始RSSI监控
    public void startMonitoringRssiValue() {
    	readPeriodicalyRssiValue(true);
    }
    
    // 停止监控RSSI
    public void stopMonitoringRssiValue() {
    	readPeriodicalyRssiValue(false);
    }
    
    // 发现所有远程设备可用的服务.
    public void startServicesDiscovery() {
    	if(mBluetoothGatt != null) 
    		mBluetoothGatt.discoverServices();
    }
    
    // 取得服务并调用ui的回调函数去处理他们.调用它之前调用getServices()确认服务是已经被发现的.
    public void getSupportedServices() {
    	
    	if(mBluetoothGattServices != null && mBluetoothGattServices.size() > 0) 
    		mBluetoothGattServices.clear();
    	
    	// 在一个本地的数组里保存它的引用.
        if(mBluetoothGatt != null) 
        	mBluetoothGattServices = mBluetoothGatt.getServices();
        
        mUiCallback.uiAvailableServices(mBluetoothGatt, mBluetoothDevice, mBluetoothGattServices);
    }

    // 获取特定服务的所有特征,并传递到它们的ui回调.
    public void getCharacteristicsForService(final BluetoothGattService service) {
    	
    	if(service == null) 
    		return;
    	
    	List<BluetoothGattCharacteristic> chars = null;
    	
    	chars = service.getCharacteristics();   //得到特征.
    	
    	mUiCallback.uiCharacteristicForService(mBluetoothGatt, mBluetoothDevice, service, chars);
    	
    	// 先保持住,最用用来选择服务
    	mBluetoothSelectedService = service;
    }

    // 为指定的特征去取存在远程设备上的新的值.
    public void requestCharacteristicValue(BluetoothGattCharacteristic ch) {
    	
        if (mBluetoothAdapter == null || mBluetoothGatt == null) 
        	return;
        
        // 在回调对象中新值将以notified传递
        mBluetoothGatt.readCharacteristic(ch);
        // new value available will be notified in Callback Object
    }

    // 获取特征的值(并为某系特征解析它)
    // 调用它之前你应该总是更新值通过调用requestCharacteristicValue().
    public void getCharacteristicValue(BluetoothGattCharacteristic ch) {
    	
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) 
        	return;
        
        byte[] rawValue = ch.getValue();
        String strValue = null;
        int intValue = 0;
        
        // 如果特征值相等.
        UUID uuid = ch.getUuid();
        
        // 以下测试用,可删可留
        if(uuid.equals(BleDefinedUUIDs.Characteristic.HEART_RATE_MEASUREMENT)) { // 心律计测试
        	
        	int index = ((rawValue[0] & 0x01) == 1) ? 2 : 1;
        	
        	int format = (index == 1) ? BluetoothGattCharacteristic.FORMAT_UINT8 : BluetoothGattCharacteristic.FORMAT_UINT16;

        	intValue = ch.getIntValue(format, index);
        	strValue = intValue + " bpm"; 
        }
        else if (uuid.equals(BleDefinedUUIDs.Characteristic.HEART_RATE_MEASUREMENT) || 
        		 uuid.equals(BleDefinedUUIDs.Characteristic.MODEL_NUMBER_STRING) || 
        		 uuid.equals(BleDefinedUUIDs.Characteristic.FIRMWARE_REVISION_STRING)) 
        {
        	strValue = ch.getStringValue(0);
        }
        else if(uuid.equals(BleDefinedUUIDs.Characteristic.APPEARANCE)) { // appearance
        	intValue  = ((int)rawValue[1]) << 8;
        	intValue += rawValue[0];
        	strValue = BleNamesResolver.resolveAppearance(intValue);
        }
        else if(uuid.equals(BleDefinedUUIDs.Characteristic.BODY_SENSOR_LOCATION)) { 
        	intValue = rawValue[0];
        	strValue = BleNamesResolver.resolveHeartRateSensorLocation(intValue);
        }
        else if(uuid.equals(BleDefinedUUIDs.Characteristic.BATTERY_LEVEL)) { 
        	intValue = rawValue[0];
        	strValue = "" + intValue + "% battery level";
        }        
        else {
        	intValue = 0;
        	if(rawValue.length > 0) 
        		intValue = (int)rawValue[0];
        	
        	if(rawValue.length > 1) 
        		intValue = intValue + ((int)rawValue[1] << 8); 
        	
        	if(rawValue.length > 2) 
        		intValue = intValue + ((int)rawValue[2] << 8); 
        	
        	if(rawValue.length > 3) 
        		intValue = intValue + ((int)rawValue[3] << 8); 
        	
            if (rawValue.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(rawValue.length);
                for(byte byteChar : rawValue) {
                    stringBuilder.append(String.format("%c", byteChar));
                }
                strValue = stringBuilder.toString();
            }
        }
        
        String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(new Date());
        mUiCallback.uiNewValueForCharacteristic(mBluetoothGatt,
                                                mBluetoothDevice,
                                                mBluetoothSelectedService,
        		                                ch,
        		                                strValue,
        		                                intValue,
        		                                rawValue,
        		                                timestamp);
    }    
    
    public int getValueFormat(BluetoothGattCharacteristic ch) {
    	int properties = ch.getProperties();
    	
    	if((BluetoothGattCharacteristic.FORMAT_FLOAT & properties) != 0) 
    		return BluetoothGattCharacteristic.FORMAT_FLOAT;
    	
    	if((BluetoothGattCharacteristic.FORMAT_SFLOAT & properties) != 0) 
    		return BluetoothGattCharacteristic.FORMAT_SFLOAT;
    	
    	if((BluetoothGattCharacteristic.FORMAT_SINT16 & properties) != 0) 
    		return BluetoothGattCharacteristic.FORMAT_SINT16;
    	
    	if((BluetoothGattCharacteristic.FORMAT_SINT32 & properties) != 0) 
    		return BluetoothGattCharacteristic.FORMAT_SINT32;
    	
    	if((BluetoothGattCharacteristic.FORMAT_SINT8 & properties) != 0) 
    		return BluetoothGattCharacteristic.FORMAT_SINT8;
    	
    	if((BluetoothGattCharacteristic.FORMAT_UINT16 & properties) != 0) 
    		return BluetoothGattCharacteristic.FORMAT_UINT16;
    	
    	if((BluetoothGattCharacteristic.FORMAT_UINT32 & properties) != 0) 
    		return BluetoothGattCharacteristic.FORMAT_UINT32;
    	
    	if((BluetoothGattCharacteristic.FORMAT_UINT8 & properties) != 0) 
    		return BluetoothGattCharacteristic.FORMAT_UINT8;
    	
    	return 0;
    }

    // 写特征值
    public void writeDataToCharacteristic(final BluetoothGattCharacteristic ch, final byte[] dataToWrite) {
    	
    	if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) 
    		return;
    	
    	ch.setValue(dataToWrite);
    	// 提交
    	mBluetoothGatt.writeCharacteristic(ch);
    }
    
    //这里设置了setCharacteristicNotification使能.
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic ch, boolean enabled){
    	
    	Log.v(TAG, "自己重写的setCharacteristicNotification函数.");
    	
    	if(mBluetoothAdapter == null || mBluetoothGatt == null)
    		return false;
    	
    	if(!mBluetoothGatt.setCharacteristicNotification(ch, enabled)){
    	
    		return false;
    	}
    	
    	BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
    	
    	if(descriptor == null)
    		return false;
    	
    	if(enabled){
    		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    	}
    	else{
    		descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    	}
    	
    	return mBluetoothGatt.writeDescriptor(descriptor);
    }
    
    // 使能/禁用 特征的notification  
    public void setNotificationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled) {
    	
        if (mBluetoothAdapter == null || mBluetoothGatt == null) 
        	return;
        
        boolean success = mBluetoothGatt.setCharacteristicNotification(ch, enabled);
        if(!success) {
        	Log.e("------", "Seting proper notification status for characteristic failed!");
        }
        
        BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if(descriptor != null) {
        	byte[] val = enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
	        descriptor.setValue(val);
	        mBluetoothGatt.writeDescriptor(descriptor);
        }
    }
    
    // 扫描外设的回调.
    private BluetoothAdapter.LeScanCallback mDeviceFoundCallback = new BluetoothAdapter.LeScanCallback() {
        @Override  //scanRecord表示来自BT Le扫描的扫描记录.也就是识别到的信息存于数组里.
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        	// 这接口啥也没干. 
        	mUiCallback.uiDeviceFound(device, rssi, scanRecord); 
        }
    };	    
    
    // 在ble远程设备上任何动作的回调.
    private final BluetoothGattCallback mBleCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        	
            if (newState == BluetoothProfile.STATE_CONNECTED) {
            	
            	mConnected = true;
            	mUiCallback.uiDeviceConnected(mBluetoothGatt, mBluetoothDevice);
            
            	mBluetoothGatt.readRemoteRssi();
 
            	startServicesDiscovery();
            	
            	startMonitoringRssiValue();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            	mConnected = false;
            	mUiCallback.uiDeviceDisconnected(mBluetoothGatt, mBluetoothDevice);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

            	getSupportedServices();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	getCharacteristicValue(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic)
        {
        	// 这里是setCharacteristicNotification的回调.
        	// 这里必须要自己重写,不能直接用官网推荐的写法.否则有notification来收不到.
        	// mUiCallback.uiGotNotification(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic); //原有的. 2016/7/16
        	Log.v(TAG, "收到notification的回调");
        	mUiCallback.uiBroadcastUpdate(this.toString(), characteristic);
        }
        
        @Override  // 刷新到Ui上
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        	String deviceName = gatt.getDevice().getName();
        	String serviceName = BleNamesResolver.resolveServiceName(characteristic.getService().getUuid().toString().toLowerCase(Locale.getDefault()));
        	String charName = BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString().toLowerCase(Locale.getDefault()));
        	String description = "设备: " + deviceName + " 服务: " + serviceName + " 特征: " + charName;
        	
        	if(status == BluetoothGatt.GATT_SUCCESS) {
        		 mUiCallback.uiSuccessfulWrite(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic, description);
        	}
        	else {
        		 mUiCallback.uiFailedWrite(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic, description + " 状态 = " + status);
        	}
        };
        
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        	if(status == BluetoothGatt.GATT_SUCCESS) {
        		 mUiCallback.uiNewRssiAvailable(mBluetoothGatt, mBluetoothDevice, rssi);
        	}
        };
    };
    
	private Activity mParent = null;    
	private boolean mConnected = false;
	private String mDeviceAddress = "";

    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice  mBluetoothDevice = null;
    private BluetoothGatt    mBluetoothGatt = null;
    private BluetoothGattService mBluetoothSelectedService = null;
    private List<BluetoothGattService> mBluetoothGattServices = null;	
    
    private Handler mTimerHandler = new Handler();
    private boolean mTimerEnabled = false;
}
