package org.bluetooth.bledemo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;  //��Ϊ������ʹ�úʹ�������.
import android.bluetooth.BluetoothGattCallback; //�����ܱߵ�״̬
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;  //��Ϊ�ܱ����ṩ����.
//import android.bluetooth.BluetoothGattServerCallback;  //���������״̬���ܱߵ�����.
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
//import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

public class BleWrapper {
	
	static final String TAG = "BleWrapper";
	
	// ����೤ʱ��ȥ����RSSI
    private static final int RSSI_UPDATE_TIME_INTERVAL = 1500; // 1.5 seconds

   
    private BleWrapperUiCallbacks mUiCallback = null;
    
    // ����Ui �ص��Ŀն���.
    private static final BleWrapperUiCallbacks NULL_CALLBACK = new BleWrapperUiCallbacks.Null(); 
    
    // ����һ��BleWrapper����,��������Activity�ͻص�����(Ҳ����BleWrapperUiCallbacksʵ�ֵĽӿ�).
    public BleWrapper(Activity parent, BleWrapperUiCallbacks callback) {
    	
    	this.mParent = parent;
    	mUiCallback = callback;
    	
    	if(mUiCallback == null) 
    		mUiCallback = NULL_CALLBACK;
    }

    // ����������
    public BluetoothManager getManager(){ 
    	return mBluetoothManager; 
    }
    
    //��ȡ��������������.
    public BluetoothAdapter getAdapter(){ 
    	return mBluetoothAdapter; 
    }
    
    //��ȡԶ�̵������豸.
    public BluetoothDevice getDevice(){ 
    	return mBluetoothDevice; 
    }
    
    //Gatt Ѱ��,���úͶ�дServcice�˵ĸ���attribute. ��Ϊ������ʹ�úʹ�������.
    public BluetoothGatt getGatt(){ 
    	return mBluetoothGatt; 
    }
    
    //��Ϊ�ܱ����ṩ����.    
    public BluetoothGattService getCachedService(){ 
    	return mBluetoothSelectedService; 
    }
    
    public List<BluetoothGattService> getCachedServices(){ 
    	return mBluetoothGattServices; 
    }
    
    public boolean isConnected(){
    	return mConnected; 
    }

	
    // ���Բ�����ֻ��������豸�����͹���Ӳ���Ƿ����
	public boolean checkBleHardwareAvailable() {
		
		// �ȼ��ͨ������Ӳ��.
		// Ȼ���ȡ������������
		final BluetoothManager manager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
		if(manager == null) 
			return false;
		
		// ��ȡ��������������.
		final BluetoothAdapter adapter = manager.getAdapter();
		if(adapter == null) 
			return false;
		
		// ���BT LE�Ƿ����
		boolean hasBle = mParent.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
		return hasBle;
	}    

	// ��鲢ʹ�ܱ�������������
	public boolean isBtEnabled() {
		final BluetoothManager manager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
		if(manager == null) 
			return false;
		
		final BluetoothAdapter adapter = manager.getAdapter();
		if(adapter == null) 
			return false;
		
		return adapter.isEnabled();
	}
	
	//��ʼɨ�踽����Ble�豸.
	public void startScanning() {
		//startLeScanҪ��һ���ص�,�������һչ���Ϳ���....
        mBluetoothAdapter.startLeScan(mDeviceFoundCallback);
	}
	
	//ֹͣɨ��
	public void stopScanning() {
		mBluetoothAdapter.stopLeScan(mDeviceFoundCallback);	
	}
	
	// ��ʼ��ble,ble������,��������������.
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

    // ����ͨ����ַ����Զ���豸
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
            	// �����ַ����,��Զ�������豸������
                return false;
            }
            // ���þ�ֱ��ȥ����Զ���豸.
        	mBluetoothGatt = mBluetoothDevice.connectGatt(mParent, false, mBleCallback);
        }
        return true;
    }  
    
    // ������Ͽ�. ����Ȼ�п��ܹ�һ��������������GATT�ͻ���
    public void diconnect() {
    	if(mBluetoothGatt != null) mBluetoothGatt.disconnect();
    	 mUiCallback.uiDeviceDisconnected(mBluetoothGatt, mBluetoothDevice);
    }

    // ��ȫ�ر�GATT�ͻ���
    public void close() {
    	if(mBluetoothGatt != null) mBluetoothGatt.close();
    	mBluetoothGatt = null;
    }    

    //Ϊ�����ṩRSSIֵ.
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
    
    // ��ʼRSSI���
    public void startMonitoringRssiValue() {
    	readPeriodicalyRssiValue(true);
    }
    
    // ֹͣ���RSSI
    public void stopMonitoringRssiValue() {
    	readPeriodicalyRssiValue(false);
    }
    
    // ��������Զ���豸���õķ���.
    public void startServicesDiscovery() {
    	if(mBluetoothGatt != null) 
    		mBluetoothGatt.discoverServices();
    }
    
    // ȡ�÷��񲢵���ui�Ļص�����ȥ��������.������֮ǰ����getServices()ȷ�Ϸ������Ѿ������ֵ�.
    public void getSupportedServices() {
    	
    	if(mBluetoothGattServices != null && mBluetoothGattServices.size() > 0) 
    		mBluetoothGattServices.clear();
    	
    	// ��һ�����ص������ﱣ����������.
        if(mBluetoothGatt != null) 
        	mBluetoothGattServices = mBluetoothGatt.getServices();
        
        mUiCallback.uiAvailableServices(mBluetoothGatt, mBluetoothDevice, mBluetoothGattServices);
    }

    // ��ȡ�ض��������������,�����ݵ����ǵ�ui�ص�.
    public void getCharacteristicsForService(final BluetoothGattService service) {
    	
    	if(service == null) 
    		return;
    	
    	List<BluetoothGattCharacteristic> chars = null;
    	
    	chars = service.getCharacteristics();   //�õ�����.
    	
    	mUiCallback.uiCharacteristicForService(mBluetoothGatt, mBluetoothDevice, service, chars);
    	
    	// �ȱ���ס,��������ѡ�����
    	mBluetoothSelectedService = service;
    }

    // Ϊָ��������ȥȡ����Զ���豸�ϵ��µ�ֵ.
    public void requestCharacteristicValue(BluetoothGattCharacteristic ch) {
    	
        if (mBluetoothAdapter == null || mBluetoothGatt == null) 
        	return;
        
        // �ڻص���������ֵ����notified����
        mBluetoothGatt.readCharacteristic(ch);
        // new value available will be notified in Callback Object
    }

    // ��ȡ������ֵ(��Ϊĳϵ����������)
    // ������֮ǰ��Ӧ�����Ǹ���ֵͨ������requestCharacteristicValue().
    public void getCharacteristicValue(BluetoothGattCharacteristic ch) {
    	
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) 
        	return;
        
        byte[] rawValue = ch.getValue();
        String strValue = null;
        int intValue = 0;
        
        // �������ֵ���.
        UUID uuid = ch.getUuid();
        
        // ���²�����,��ɾ����
        if(uuid.equals(BleDefinedUUIDs.Characteristic.HEART_RATE_MEASUREMENT)) { // ���ɼƲ���
        	
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

    // д����ֵ
    public void writeDataToCharacteristic(final BluetoothGattCharacteristic ch, final byte[] dataToWrite) {
    	
    	if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) 
    		return;
    	
    	ch.setValue(dataToWrite);
    	// �ύ
    	mBluetoothGatt.writeCharacteristic(ch);
    }
    
    //����������setCharacteristicNotificationʹ��.
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic ch, boolean enabled){
    	
    	Log.v(TAG, "�Լ���д��setCharacteristicNotification����.");
    	
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
    
    // ʹ��/���� ������notification  
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
    
    // ɨ������Ļص�.
    private BluetoothAdapter.LeScanCallback mDeviceFoundCallback = new BluetoothAdapter.LeScanCallback() {
        @Override  //scanRecord��ʾ����BT Leɨ���ɨ���¼.Ҳ����ʶ�𵽵���Ϣ����������.
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        	// ��ӿ�ɶҲû��. 
        	mUiCallback.uiDeviceFound(device, rssi, scanRecord); 
        }
    };	    
    
    // ��bleԶ���豸���κζ����Ļص�.
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
        	// ������setCharacteristicNotification�Ļص�.
        	// �������Ҫ�Լ���д,����ֱ���ù����Ƽ���д��.������notification���ղ���.
        	// mUiCallback.uiGotNotification(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic); //ԭ�е�. 2016/7/16
        	Log.v(TAG, "�յ�notification�Ļص�");
        	mUiCallback.uiBroadcastUpdate(this.toString(), characteristic);
        }
        
        @Override  // ˢ�µ�Ui��
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        	String deviceName = gatt.getDevice().getName();
        	String serviceName = BleNamesResolver.resolveServiceName(characteristic.getService().getUuid().toString().toLowerCase(Locale.getDefault()));
        	String charName = BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString().toLowerCase(Locale.getDefault()));
        	String description = "�豸: " + deviceName + " ����: " + serviceName + " ����: " + charName;
        	
        	if(status == BluetoothGatt.GATT_SUCCESS) {
        		 mUiCallback.uiSuccessfulWrite(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic, description);
        	}
        	else {
        		 mUiCallback.uiFailedWrite(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic, description + " ״̬ = " + status);
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
