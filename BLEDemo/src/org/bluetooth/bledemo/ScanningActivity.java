/**************************************************************************************************/
/* ��Activity                                                                                     */
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
	
	private boolean mScanning = false;	//ɨ����
	private Handler mHandler = new Handler();	//�����̼߳�ͨ��.
	private DeviceListAdapter mDevicesListAdapter = null; //�豸�б�����.
	private BleWrapper mBleWrapper	= null; //
	
	
	/***********************************************************************************************/
	/* ��Activit������ʱ�������һ���ջص���BleWrapper����.Ȼ�����uiDeviceFoundȥ������Χ����                      */
	/***********************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        // ����һ��BleWrapper����.ʹ��һ���յĻص�����. ������new��һ���յĻص���������. 
        // BleWrapper�������������ĺ�������.���������ص�����.
        mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null() {
        	@Override
        	
        	// App����������ɨ��Ӳ��.
        	public void uiDeviceFound(	final BluetoothDevice device, 
        								final int rssi, 
        								final byte[] record) {
        		
        		//ɨ����ȥ���ñ���ķ���,��ɨ�赽�Ľ��д����̬�������汣������
        		handleFoundDevice(device, rssi, record);
        	}
        });
        
        // ����Ƿ���������֧��BLE.
        if(mBleWrapper.checkBleHardwareAvailable() == false) {
        	bleMissing();
        }
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	
       	// ��������Ƿ�����
    	if(mBleWrapper.isBtEnabled() == false) {
    		
    		// ���û����,Ҫ�����
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			
			// ������Activity����
		    startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_ID);
		}
    	
    	
    	// ����mBleWrapper����.��ʼ��.
        mBleWrapper.initialize();
    	
        // newһ����̬����.
    	mDevicesListAdapter = new DeviceListAdapter(this);
        setListAdapter(mDevicesListAdapter); // ListView�õĶ���.
    	
        // �Զ���ʼɨ����Χ�豸
    	mScanning = true;
		
    	// ����ɨ��ĳ�ʱʱ��
		addScanningTimeout();  
		
		//ɨ��
		mBleWrapper.startScanning();  
		
		//ɨ���н����ˢ�µ�ui.
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
            	mBleWrapper.startScanning(); //ɨ��
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
    // �û����б��ϵ�������һ���豸.
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	// �����û������λ��,ȡ���豸
        final BluetoothDevice device = mDevicesListAdapter.getDevice(position);
        
        if (device == null) //���û���豸
        	return;			//����
        
        // ��Intent�Ĳ������Կ���,�������PeripheralActivity��,
        // �豸��Ϊ��
        final Intent intent = new Intent(this, PeripheralActivity.class); // ����PeripheralActivity��.
        
        // ���豸������,��ַ,RSSiͨ��intent���ݸ� PeripheralActivity (�Ѿ�ʵ����Ble�ĸ��ֻص�����).
        intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_RSSI, mDevicesListAdapter.getRssi(position));
        
        if (mScanning) {
            mScanning = false;
            invalidateOptionsMenu();
            mBleWrapper.stopScanning();
        }

        startActivity(intent);  // ����PeripheralActivity.
    }    
    
    /*  ��Activityִ�����ص�������,���߻���һЩ��ģ������ݽ��������洦��.��Ҫ����onActivityResult.  */
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

	// ���Զ���豸����ǰ���豸�б���
    private void handleFoundDevice(final BluetoothDevice device,
            final int rssi,
            final byte[] scanRecord)
	{
		// ��ӵ�Ui
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				//����������Զ���豸д����̬����
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
