package org.bluetooth.bledemo;

import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
//import android.bluetooth.BluetoothGattCallback;

/********************************************************************/
/*  ���Ǹ��ӿ���                                                                                                                                            */
/*  �Ѹ��ֻص�������Ui��װ��һ��										*/
/********************************************************************/
public interface BleWrapperUiCallbacks {
	
	/*����ӿ�Ҫʵ��,������ղ���notification, �Լ��ӵ�*/
	public void uionCharacteristicChanged(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic);
	
	/*�Ӹ��㲥�Ľӿ�,�������notification�������.�Լ���*/
	public void uiBroadcastUpdate(String action,BluetoothGattCharacteristic characteristic);
	
	public void uionDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status);

	public void uiDeviceFound(final BluetoothDevice device, int rssi, byte[] record);
	
	public void uiDeviceConnected(final BluetoothGatt gatt, final BluetoothDevice device);
	
	public void uiDeviceDisconnected(final BluetoothGatt gatt, final BluetoothDevice device);
	
	public void uiAvailableServices(final BluetoothGatt gatt,
			  		        	    final BluetoothDevice device,
			  						final List<BluetoothGattService> services);
	
	public void uiCharacteristicForService(final BluetoothGatt gatt,
            		 					   final BluetoothDevice device,
            							   final BluetoothGattService service,
            							   final List<BluetoothGattCharacteristic> chars);

	public void uiCharacteristicsDetails(final BluetoothGatt gatt,
			  							 final BluetoothDevice device,
			  							 final BluetoothGattService service,
			  							 final BluetoothGattCharacteristic characteristic);	
	
	public void uiNewValueForCharacteristic(final BluetoothGatt gatt,
            								final BluetoothDevice device,
            								final BluetoothGattService service,
            								final BluetoothGattCharacteristic ch,
            								final String strValue,
            								final int intValue,
            								final byte[] rawValue,
            								final String timestamp);
	
	public void uiGotNotification(final BluetoothGatt gatt,
                                  final BluetoothDevice device,
                                  final BluetoothGattService service,
                                  final BluetoothGattCharacteristic characteristic);
	
	public void uiSuccessfulWrite(final BluetoothGatt gatt,
                                  final BluetoothDevice device,
                                  final BluetoothGattService service,
                                  final BluetoothGattCharacteristic ch,
                                  final String description);
	
	public void uiFailedWrite(final BluetoothGatt gatt,
			                  final BluetoothDevice device,
			                  final BluetoothGattService service,
			                  final BluetoothGattCharacteristic ch,
			                  final String description);
	
	//public void uiOnCharcteristicChanged(final BluetoothGatt gatt,
	//		BluetoothGattCharacteristic characteristic);
	
	public void uiNewRssiAvailable(final BluetoothGatt gatt, final BluetoothDevice device, final int rssi);
	
	// Ϊ�ӿڶ���յ���������
	public static class Null implements BleWrapperUiCallbacks {
		
		// ���¾��Ǹ��ֽӿ�
		@Override  //�������������,���ղ���notify
		public void uionCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {}
		
		@Override  
		public void uiBroadcastUpdate(String action,BluetoothGattCharacteristic characteristic){}
		//public void uiOnCharcteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {}
		
		@Override
		public void uiDeviceConnected(BluetoothGatt gatt, BluetoothDevice device) {}
		
		@Override
		public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {}
		
		@Override
		public void uiAvailableServices(BluetoothGatt gatt, BluetoothDevice device,
				List<BluetoothGattService> services) {}
		
		@Override
		public void uiCharacteristicForService(BluetoothGatt gatt,
				BluetoothDevice device, BluetoothGattService service,
				List<BluetoothGattCharacteristic> chars) {}
		
		@Override
		public void uiCharacteristicsDetails(BluetoothGatt gatt,
				BluetoothDevice device, BluetoothGattService service,
				BluetoothGattCharacteristic characteristic) {}
		
		@Override
		public void uiNewValueForCharacteristic(BluetoothGatt gatt,
				BluetoothDevice device, BluetoothGattService service,
				BluetoothGattCharacteristic ch, String strValue, int intValue,
				byte[] rawValue, String timestamp) {}
		
		@Override
		public void uiGotNotification(BluetoothGatt gatt, BluetoothDevice device,
				BluetoothGattService service,
				BluetoothGattCharacteristic characteristic) {}
		
		@Override
		public void uiSuccessfulWrite(BluetoothGatt gatt, BluetoothDevice device,
				BluetoothGattService service, BluetoothGattCharacteristic ch,
				String description) {}
		
		@Override
		public void uiFailedWrite(BluetoothGatt gatt, BluetoothDevice device,
				BluetoothGattService service, BluetoothGattCharacteristic ch,
				String description) {}
		
		@Override
		public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device,int rssi) {}
		
		@Override
		public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {}	
		
		@Override 
		public void uionDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){}
	}
}
