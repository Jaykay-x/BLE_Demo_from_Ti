/*********************************************************************************************************/
/* ��Activity																							 */
/* PeripheralActivtityʵ������BLE�ĸ��ֻص������ӿ�                                                                                                                                                    */
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
    
    private String mDeviceName;		//�豸����
    private String mDeviceAddress;	//�豸��ַ
    private String mDeviceRSSI;		//�豸�ź�ǿ��

    private BleWrapper mBleWrapper;	
    
  	// �õ��Ŀؼ�
    private TextView mDeviceNameView;		
    private TextView mDeviceAddressView;
    private TextView mDeviceRssiView;
    private TextView mDeviceStatus;
    private ListView mListView;				
    private View     mListViewHeader;
    private TextView mHeaderTitle;
    private TextView mHeaderBackButton;
    
    
    // ���Ƿ����ListActivity,������GattService�Ͷ�̬����
    private ServicesListAdapter mServicesListAdapter = null;
    
    // ����������ListActivity����ʾ������ϸ��Ϣ���ּ�����.
    private CharacteristicsListAdapter mCharacteristicsListAdapter = null;
    
    // ��������ϸ����ʾ������ϸ��Ϣ�Ĳ�����
    private CharacteristicDetailsAdapter mCharDetailsAdapter = null;  
    
    public void uiOnCharcteristicChanged(final BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){
    	// û����...
    }
    
    public void uionDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
    	
    	// �ⷽ��Զ��Bleû����... 
    }
    
    
    // ��������
    public void uiBroadcastUpdate(String action,BluetoothGattCharacteristic characteristic){
    	
    	// ��CharacteristicDetailsAdapter�������ӿں�,����notification����ʱ����������Ļص�	
    	broadcastUpdate(action,characteristic); 
    }
    
      
    // �յ����ݾ�������������洦��
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
    
    
    // �����ܱ��豸.
    public void uiDeviceConnected(final BluetoothGatt gatt, final BluetoothDevice device)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceStatus.setText("����");
				invalidateOptionsMenu();
			}
    	});
    }
    
    // �Ͽ������ܱ��豸.
    public void uiDeviceDisconnected(final BluetoothGatt gatt, final BluetoothDevice device)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceStatus.setText("�Ͽ�");
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
    
    // ����
    public void uiAvailableServices(final BluetoothGatt gatt, final BluetoothDevice device,  final List<BluetoothGattService> services)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mServicesListAdapter.clearList();
				mListType = ListType.GATT_SERVICES;
				mListView.setAdapter(mServicesListAdapter);
				mHeaderTitle.setText(mDeviceName + "�ṩ�ķ���:");
				mHeaderBackButton.setVisibility(View.INVISIBLE);
				
    			for(BluetoothGattService service : mBleWrapper.getCachedServices()) {
            		mServicesListAdapter.addService(service);
            	}				
    			mServicesListAdapter.notifyDataSetChanged();
			}    		
    	});
    }
   
    // ���������
    public void uiCharacteristicForService(final BluetoothGatt gatt,  final BluetoothDevice device,
    									   final BluetoothGattService service,  final List<BluetoothGattCharacteristic> chars)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mCharacteristicsListAdapter.clearList();
		    	mListType = ListType.GATT_CHARACTERISTICS;
		    	mListView.setAdapter(mCharacteristicsListAdapter);
		    	mHeaderTitle.setText(BleNamesResolver.resolveServiceName(service.getUuid().toString().toLowerCase(Locale.getDefault())) + "������:");
		    	mHeaderBackButton.setVisibility(View.VISIBLE);
		    	
		    	for(BluetoothGattCharacteristic ch : chars) {
		    		mCharacteristicsListAdapter.addCharacteristic(ch);
		    	}
		    	mCharacteristicsListAdapter.notifyDataSetChanged();
			}
    	});
    }
    
    // ��������ϸ.
    public void uiCharacteristicsDetails(final BluetoothGatt gatt, final BluetoothDevice device,
										 final BluetoothGattService service, final BluetoothGattCharacteristic characteristic)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				
				mListType = ListType.GATT_CHARACTERISTIC_DETAILS;
				
				mListView.setAdapter(mCharDetailsAdapter);
		    	mHeaderTitle.setText(BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString().toLowerCase(Locale.getDefault())) + "ϸ��:");
		    	mHeaderBackButton.setVisibility(View.VISIBLE);
		    	
		    	mCharDetailsAdapter.setCharacteristic(characteristic);
		    	mCharDetailsAdapter.notifyDataSetChanged();
			}
    	});
    }

    // ��������ֵ
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
    	// ����û���õ�. �������BleҲû���õ���.
    }
    
    // ����д��,�ɹ�
	public void uiSuccessfulWrite(final BluetoothGatt gatt, final BluetoothDevice device,
            					  final BluetoothGattService service,  final BluetoothGattCharacteristic ch,
            					  final String description)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "д�뵽 " + description + " �ɹ����", Toast.LENGTH_LONG).show();
			}
		});
	}
	
	// ����д��ʧ��
	public void uiFailedWrite(final BluetoothGatt gatt,
							  final BluetoothDevice device,
							  final BluetoothGattService service,
							  final BluetoothGattCharacteristic ch,
							  final String description)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "д�뵽 " + description + " ʧ��!", Toast.LENGTH_LONG).show();
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

	// �����豸.ɶ��Ҳ���ø�. ��Ϊ��������ʱ��,�����Ѿ�д��������.
	@Override
	public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {}  	
	
	//ListView�ļ�����
    private AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {
    	
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
			--position; 
			if(position < 0) { 
				
				if(mListType.equals(ListType.GATT_SERVICES)) //�������������Ƿ���,
					return; //ֱ�ӷ���
				
				if(mListType.equals(ListType.GATT_CHARACTERISTICS)) { //������������
					
					// ����ʵ�ֵĽӿ�.
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
				if(mListType.equals(ListType.GATT_SERVICES)) { // ���Ҫ���ص��Ƿ��� 
					
					BluetoothGattService service = mServicesListAdapter.getService(position);
					mBleWrapper.getCharacteristicsForService(service);
				}
				else if(mListType.equals(ListType.GATT_CHARACTERISTICS)) {  // Ҫ���ص�������
					
					// ���ݲ����õ�����
					BluetoothGattCharacteristic ch = mCharacteristicsListAdapter.getCharacteristic(position);
					
					// �ڽ����ϸ�������
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
		
		// ��̬�������ͷ. ( �豸��+�ṩ�ķ���)
		mListViewHeader = (View) getLayoutInflater().inflate(R.layout.peripheral_list_services_header, null, false);
		
		
		handler = new mHandler();  // handler����ת��,done
		connectViewsVariables();	// ȡactivit_peripheral���������.
		
        final Intent intent = getIntent(); //��ȡ��һActivity�����Ĳ���.
        
        //String���� (�������ļ��ϵ��豸��,��ַ,�ź�ǿ��. ����һҳ�淢�������ݸ�ֵ.
        mDeviceName 		= intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress 		= intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceRSSI 		= intent.getIntExtra(EXTRAS_DEVICE_RSSI, 0) + " db";
        
        // ����Ѵ���������Ϣ���õ�����ҳ��.
        mDeviceNameView.setText(mDeviceName); 
        mDeviceAddressView.setText(mDeviceAddress);
        mDeviceRssiView.setText(mDeviceRSSI);
        
        // ����ҳ���"̧ͷ"
        getActionBar().setTitle(mDeviceName);
        
        // ����LiveView��"̧ͷ"
        mListView.addHeaderView(mListViewHeader);
        
        // ����ListView�ļ�����.
        mListView.setOnItemClickListener(listClickListener);
        
    }
	
	//�����õ�Handler,���ڿ��ٲ鿴�Ƿ��յ�Notify����,����Լ��¼ӵ�. 
	class mHandler extends Handler{

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		
			Toast toast=null;
			strTemp = (String) msg.obj;
			
			// ���﷢��EventBus��Ϣ.
			EventBus.getDefault().post(	new EventBusString(strTemp));
			toast = Toast.makeText(getApplicationContext(), "�յ�Notify����: " + strTemp  , Toast.LENGTH_LONG);//.show();
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
    	mDeviceStatus.setText("������ ...");
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
            	mDeviceStatus.setText("������ ...");
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
