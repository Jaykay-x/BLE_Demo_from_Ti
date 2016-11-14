/*********************************************************************************************************/
/* 次Activity																							 */
/* PeripheralActivtity实现蓝牙BLE的各种回调函数接口                                                                                                                                                    */
/*********************************************************************************************************/
package org.bluetooth.bledemo;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.greenrobot.event.EventBus;
import org.bluetooth.bledemo.Conversion;

public class PeripheralActivity extends Activity implements BleWrapperUiCallbacks {
		
	private Handler handler;
	static final String TAG = "PeripheralActivity";
	
    public static final String EXTRAS_DEVICE_NAME    = "BLE_DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "BLE_DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_RSSI    = "BLE_DEVICE_RSSI";
    
	public final static String EXTRA_DATA	= "EXTRA_DATA";
	public final static String EXTRA_UUID	= "EXTRA_UUID";
	public final static String EXTRA_STATUS = "EXTRA_STATUS";
	public String  strTemp;

    
    public enum ListType {
    	GATT_SERVICES,
    	GATT_CHARACTERISTICS,
    	GATT_CHARACTERISTIC_DETAILS
    }
    
    private ListType mListType = ListType.GATT_SERVICES;
    
    private String mDeviceName;		//设备名字
    private String mDeviceAddress;	//设备地址
    private String mDeviceRSSI;		//设备信号强度

    private BleWrapper mBleWrapper;	
    
  	// 用到的控件
    private TextView mDeviceNameView;		
    private TextView mDeviceAddressView;
    private TextView mDeviceRssiView;
    private TextView mDeviceStatus;
    private ListView mListView;				
    private View     mListViewHeader;
    private TextView mHeaderTitle;
    private TextView mHeaderBackButton;
    
    
    // 这是服务的ListActivity,保存着GattService和动态布局
    private ServicesListAdapter mServicesListAdapter = null;
    
    // 保存特征的ListActivity和显示特征详细信息布局加载器.
    private CharacteristicsListAdapter mCharacteristicsListAdapter = null;
    
    // 特征的详细和显示特征详细信息的布局器
    private CharacteristicDetailsAdapter mCharDetailsAdapter = null;  
    
    public void uiOnCharcteristicChanged(final BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){
    	// 没调用...
    }
    
    public void uionDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
    	
    	// 这方法远程Ble没调用... 
    }
    
    
    // 接收数据
    public void uiBroadcastUpdate(String action,BluetoothGattCharacteristic characteristic){
    	
    	// 在CharacteristicDetailsAdapter类调用完接口后,当有notification来的时候这就是它的回调	
    	broadcastUpdate(action,characteristic); 
    }
    
      
    // 收到数据就在这个方法里面处理
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) //, final int status)
    {
    	   	
    	final Intent intent = new Intent(action);
    	intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
    	intent.putExtra(EXTRA_DATA, characteristic.getValue());
    		
    	String txt = new String(characteristic.getValue(),0,characteristic.getValue().length);
    	boolean isGood = Conversion.isAsciiPrintable(txt);
    	
    	if( !isGood  )
    		txt = Conversion.BytetohexString(characteristic.getValue(), characteristic.getValue().length);
    	Message msg =  handler.obtainMessage();
    	msg.obj = txt;
    	handler.sendMessage(msg);
    	
    	sendBroadcast(intent);
    }
    
    
    // 连接周边设备.
    public void uiDeviceConnected(final BluetoothGatt gatt, final BluetoothDevice device)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceStatus.setText("连接");
				invalidateOptionsMenu();
			}
    	});
    }
    
    // 断开连接周边设备.
    public void uiDeviceDisconnected(final BluetoothGatt gatt, final BluetoothDevice device)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceStatus.setText("断开");
				mServicesListAdapter.clearList();
				mCharacteristicsListAdapter.clearList();
				mCharDetailsAdapter.clearCharacteristic();
				
				invalidateOptionsMenu();
				
				mHeaderTitle.setText("");
				mHeaderBackButton.setVisibility(View.INVISIBLE);
				mListType = ListType.GATT_SERVICES;
				mListView.setAdapter(mServicesListAdapter);
			}
    	});    	
    }
    
    // RSSI
    public void uiNewRssiAvailable(final BluetoothGatt gatt,final BluetoothDevice device, final int rssi)
    {
    	runOnUiThread(new Runnable() {
	    	@Override
			public void run() {
				mDeviceRSSI = rssi + " db";
				mDeviceRssiView.setText(mDeviceRSSI);
			}
		});    	
    }
    
    // 服务
    public void uiAvailableServices(final BluetoothGatt gatt, final BluetoothDevice device,  final List<BluetoothGattService> services)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mServicesListAdapter.clearList();
				mListType = ListType.GATT_SERVICES;
				mListView.setAdapter(mServicesListAdapter);
				mHeaderTitle.setText(mDeviceName + "提供的服务:");
				mHeaderBackButton.setVisibility(View.INVISIBLE);
				
    			for(BluetoothGattService service : mBleWrapper.getCachedServices()) {
            		mServicesListAdapter.addService(service);
            	}				
    			mServicesListAdapter.notifyDataSetChanged();
			}    		
    	});
    }
   
    // 服务的特征
    public void uiCharacteristicForService(final BluetoothGatt gatt,  final BluetoothDevice device,
    									   final BluetoothGattService service,  final List<BluetoothGattCharacteristic> chars)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mCharacteristicsListAdapter.clearList();
		    	mListType = ListType.GATT_CHARACTERISTICS;
		    	mListView.setAdapter(mCharacteristicsListAdapter);
		    	mHeaderTitle.setText(BleNamesResolver.resolveServiceName(service.getUuid().toString().toLowerCase(Locale.getDefault())) + "的特征:");
		    	mHeaderBackButton.setVisibility(View.VISIBLE);
		    	
		    	for(BluetoothGattCharacteristic ch : chars) {
		    		mCharacteristicsListAdapter.addCharacteristic(ch);
		    	}
		    	mCharacteristicsListAdapter.notifyDataSetChanged();
			}
    	});
    }
    
    // 特征的详细.
    public void uiCharacteristicsDetails(final BluetoothGatt gatt, final BluetoothDevice device,
										 final BluetoothGattService service, final BluetoothGattCharacteristic characteristic)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				
				mListType = ListType.GATT_CHARACTERISTIC_DETAILS;
				
				mListView.setAdapter(mCharDetailsAdapter);
		    	mHeaderTitle.setText(BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString().toLowerCase(Locale.getDefault())) + "细节:");
		    	mHeaderBackButton.setVisibility(View.VISIBLE);
		    	
		    	mCharDetailsAdapter.setCharacteristic(characteristic);
		    	mCharDetailsAdapter.notifyDataSetChanged();
			}
    	});
    }

    // 特征的新值
    public void uiNewValueForCharacteristic(final BluetoothGatt gatt,final BluetoothDevice device,
											final BluetoothGattService service,	final BluetoothGattCharacteristic characteristic,
											final String strValue, final int intValue,final byte[] rawValue,
											final String timestamp)
    {
    	if(mCharDetailsAdapter == null || mCharDetailsAdapter.getCharacteristic(0) == null) return;
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mCharDetailsAdapter.newValueForCharacteristic(characteristic, strValue, intValue, rawValue, timestamp);
				mCharDetailsAdapter.notifyDataSetChanged();
			}
    	});
    }
 
    public void uionCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
    	// 这里没调用到. 这个方法Ble也没调用到它.
    }
    
    // 特征写入,成功
	public void uiSuccessfulWrite(final BluetoothGatt gatt, final BluetoothDevice device,
            					  final BluetoothGattService service,  final BluetoothGattCharacteristic ch,
            					  final String description)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "写入到 " + description + " 成功完成", Toast.LENGTH_LONG).show();
			}
		});
	}
	
	// 特征写入失败
	public void uiFailedWrite(final BluetoothGatt gatt,
							  final BluetoothDevice device,
							  final BluetoothGattService service,
							  final BluetoothGattCharacteristic ch,
							  final String description)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "写入到 " + description + " 失败!", Toast.LENGTH_LONG).show();
			}
		});	
	}

	// Notiication
	public void uiGotNotification(final BluetoothGatt gatt,
								  final BluetoothDevice device,
								  final BluetoothGattService service,
								  final BluetoothGattCharacteristic ch)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// at this moment we only need to send this "signal" do characteristic's details view
				mCharDetailsAdapter.setNotificationEnabledForService(ch);
			}			
		});
	}

	// 发现设备.啥鸟也不用干. 因为调用它的时候,里面已经写了有陷了.
	@Override
	public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {}  	
	
	//ListView的监听器
    private AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {
    	
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
			--position; 
			if(position < 0) { 
				
				if(mListType.equals(ListType.GATT_SERVICES)) //如果点击的类似是服务,
					return; //直接返回
				
				if(mListType.equals(ListType.GATT_CHARACTERISTICS)) { //如果点击的特征
					
					// 调用实现的接口.
					uiAvailableServices(mBleWrapper.getGatt(), mBleWrapper.getDevice(), mBleWrapper.getCachedServices());
					mCharacteristicsListAdapter.clearList(); 
					return;
				}
				
				if(mListType.equals(ListType.GATT_CHARACTERISTIC_DETAILS)) {  
					mBleWrapper.getCharacteristicsForService(mBleWrapper.getCachedService());
					mCharDetailsAdapter.clearCharacteristic();
					return;
				}
			}
			else { 
				if(mListType.equals(ListType.GATT_SERVICES)) { // 如果要加载的是服务 
					
					BluetoothGattService service = mServicesListAdapter.getService(position);
					mBleWrapper.getCharacteristicsForService(service);
				}
				else if(mListType.equals(ListType.GATT_CHARACTERISTICS)) {  // 要加载的是特征
					
					// 根据参数得到特征
					BluetoothGattCharacteristic ch = mCharacteristicsListAdapter.getCharacteristic(position);
					
					// 在界面上更新数据
					uiCharacteristicsDetails(mBleWrapper.getGatt(), mBleWrapper.getDevice(), mBleWrapper.getCachedService(), ch);
				} 
			}
		}     	
	};  
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_peripheral); 
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// 动态载入界面头. ( 设备名+提供的服务)
		mListViewHeader = (View) getLayoutInflater().inflate(R.layout.peripheral_list_services_header, null, false);
		
		
		handler = new mHandler();  // handler向上转型,done
		connectViewsVariables();	// 取activit_peripheral上组件对象.
		
        final Intent intent = getIntent(); //获取上一Activity传来的参数.
        
        //String变量 (给布局文件上的设备名,地址,信号强度. 用上一页面发来的数据赋值.
        mDeviceName 		= intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress 		= intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceRSSI 		= intent.getIntExtra(EXTRAS_DEVICE_RSSI, 0) + " db";
        
        // 这里把传过来的信息设置到本地页面.
        mDeviceNameView.setText(mDeviceName); 
        mDeviceAddressView.setText(mDeviceAddress);
        mDeviceRssiView.setText(mDeviceRSSI);
        
        // 设置页面的"抬头"
        getActionBar().setTitle(mDeviceName);
        
        // 加载LiveView的"抬头"
        mListView.addHeaderView(mListViewHeader);
        
        // 设置ListView的监听器.
        mListView.setOnItemClickListener(listClickListener);
        
    }
	
	//调试用的Handler,用于快速查看是否收到Notify数据,这个自己新加的. 
	class mHandler extends Handler{

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		
			Toast toast=null;
			strTemp = (String) msg.obj;
			
			// 这里发送EventBus消息.
			EventBus.getDefault().post(	new EventBusString(strTemp));
			toast = Toast.makeText(getApplicationContext(), "收到Notify数据: " + strTemp  , Toast.LENGTH_LONG);//.show();
			toast.setGravity(Gravity.TOP, 0, 0);
			toast.show();
		}
	}
	
	@Override
	protected void onResume() {
		
		super.onResume();
		if(mBleWrapper == null) 
			mBleWrapper = new BleWrapper(this, this);
		
		if(mBleWrapper.initialize() == false) {
			finish();
		}
		
		if(mServicesListAdapter == null) 
			mServicesListAdapter = new ServicesListAdapter(this);
		
		if(mCharacteristicsListAdapter == null) 
			mCharacteristicsListAdapter = new CharacteristicsListAdapter(this);
		
		if(mCharDetailsAdapter == null) 
			mCharDetailsAdapter = new CharacteristicDetailsAdapter(this, mBleWrapper);
		
		mListView.setAdapter(mServicesListAdapter);
		mListType = ListType.GATT_SERVICES;
		mHeaderBackButton.setVisibility(View.INVISIBLE);
		mHeaderTitle.setText("");
		
		// start automatically connecting to the device
    	mDeviceStatus.setText("连接中 ...");
    	mBleWrapper.connect(mDeviceAddress);
	};
	
	@Override
	protected void onPause() {
		super.onPause();
		
		mServicesListAdapter.clearList();
		mCharacteristicsListAdapter.clearList();
		mCharDetailsAdapter.clearCharacteristic();
		
		
		mBleWrapper.stopMonitoringRssiValue();
		mBleWrapper.diconnect();
		mBleWrapper.close();
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		
		getMenuInflater().inflate(R.menu.peripheral, menu);
		if (mBleWrapper.isConnected()) {
	        menu.findItem(R.id.device_connect).setVisible(false);
	        menu.findItem(R.id.device_disconnect).setVisible(true);
	    } 
		else {
	        menu.findItem(R.id.device_connect).setVisible(true);
	        menu.findItem(R.id.device_disconnect).setVisible(false);
	    }		
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.device_connect:
            	mDeviceStatus.setText("连接中 ...");
            	mBleWrapper.connect(mDeviceAddress);
                return true;
            case R.id.device_disconnect:
            	mBleWrapper.diconnect();
                return true;
            case android.R.id.home:
            	mBleWrapper.diconnect();
            	mBleWrapper.close();
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }	

    
    private void connectViewsVariables() {
    	mDeviceNameView		 = (TextView) findViewById(R.id.peripheral_name);
		mDeviceAddressView	 = (TextView) findViewById(R.id.peripheral_address);
		mDeviceRssiView		 = (TextView) findViewById(R.id.peripheral_rssi);
		mDeviceStatus		 = (TextView) findViewById(R.id.peripheral_status);
		mListView			 = (ListView) findViewById(R.id.listView);
		mHeaderTitle		 = (TextView) mListViewHeader.findViewById(R.id.peripheral_service_list_title);
		mHeaderBackButton	 = (TextView) mListViewHeader.findViewById(R.id.peripheral_list_service_back);
    }

}
