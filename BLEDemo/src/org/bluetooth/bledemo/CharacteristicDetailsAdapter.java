package org.bluetooth.bledemo;

import java.util.Locale;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import de.greenrobot.event.EventBus;

public class CharacteristicDetailsAdapter extends BaseAdapter {
	
	public final static String EXTRA_DATA = "EXTRA_DATA";
	public final static String EXTRA_UUID = "EXTRA_UUID";
	public final static String EXTRA_STATUS = "EXTRA_STATUS";

	public String  strTemp;
	
	static final String TAG = "CharacteristicDetailsAdapter";
	
	public PeripheralActivity parent;

	TextView textViewStringValue;
	
   	
	// Androidϵͳ��BluetoothGattCharacteristic. �ܱ��豸������.
	private BluetoothGattCharacteristic mCharacteristic = null;
	
	// ��̬������
	private LayoutInflater mInflater;    // ���캯������õ�ֵ.
	
	// ��װBle�Ķ���.
	private BleWrapper mBleWrapper = null;  //���캯����õ�ֵ.
	
	// ����.
	private byte[] mRawValue = null;
	
	private	int		mIntValue = 0;
	private	String	mAsciiValue = "";
	public	String	mStrValue = "";	
	private String	mLastUpdateTime = "";
	private boolean mNotificationEnabled = false;
	
	//���캯��,�Ѹ�view��������,����Ble
	public CharacteristicDetailsAdapter(PeripheralActivity parent, BleWrapper ble) {
		super();
		mBleWrapper = ble;
		mInflater = parent.getLayoutInflater();
		
	}
	
	public void setCharacteristic(BluetoothGattCharacteristic ch) {
		
		this.mCharacteristic = ch;
		
		mRawValue = null;
		mIntValue = 0;
		mAsciiValue = "";
		mStrValue = "";
		mLastUpdateTime = "-";
		mNotificationEnabled = false;
		
	}
	
	public BluetoothGattCharacteristic getCharacteristic(int index) {
		return mCharacteristic;
	}

	public void clearCharacteristic() {
		mCharacteristic = null;
	}
	
	@Override
	public int getCount() {
		return (mCharacteristic != null) ? 1 : 0;
	}

	@Override
	public Object getItem(int position) {
		return mCharacteristic;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void newValueForCharacteristic(	final BluetoothGattCharacteristic ch, 
											final String strVal, 
											final int intVal, 
											final byte[] rawValue, 
											final String timestamp) {
		
		if(!ch.equals(this.mCharacteristic)) 
			return;
		
		mIntValue = intVal;
		mStrValue = strVal;
		mRawValue = rawValue;
		
		// �ٷ��Ƽ��Ķ�Ҫ����0x��ͷ,���Թٷ������λ�û�ȥ���յ��������1,����
		// ����Ӳ������ʦ��ֱ����16���Ƶ�������Ϊ����,����,���ﲻ��ȥ��1.
		// ����Ժ�Ҫ����ͨ�õ�BLE���Թ���,����Ҫ�޸����,�������1,�Ӷ����Ϲ��ʹ���.
        if (mRawValue != null && mRawValue.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(mRawValue.length);
            for(byte byteChar : mRawValue)
                stringBuilder.append(String.format("%02X", byteChar));  
        }

        mLastUpdateTime = timestamp;
        if(mLastUpdateTime == null) 
        	mLastUpdateTime = "";
	}
	
	public void setNotificationEnabledForService(final BluetoothGattCharacteristic ch) {
		
		if((!ch.equals(this.mCharacteristic)) || (mNotificationEnabled == true)) 
			return;
		mNotificationEnabled = true;
		notifyDataSetChanged();
	}
	
	public byte[] parseHexStringToBytes(final String hex) {
		
		String tmp = hex.substring(2).replaceAll("[^[0-9][a-f]]", "");
		byte[] bytes = new byte[tmp.length() / 2]; // every two letters in the string are one byte finally
		
		String part = "";
		
		for(int i = 0; i < bytes.length; ++i) {
			part = "0x" + tmp.substring(i*2, i*2+2);
			bytes[i] = Long.decode(part).byteValue();
		}
		
		return bytes;
	}
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup p) {
		
		// ע��EventBus. ok. �����յ�����.
		if(!EventBus.getDefault().isRegistered(this)){
        	
        	EventBus.getDefault().register(this); 
        	
        }
		
		// get already available view or create new if necessary
		FieldReferences fields;
		
        if (convertView == null) {
        	
        	// ȡ�ؼ��Ķ���.
        	
        	convertView = mInflater.inflate(R.layout.peripheral_details_characteristic_item, null);
        	fields = new FieldReferences();
        	fields.charPeripheralName 		= (TextView)convertView.findViewById(R.id.char_details_peripheral_name);
        	fields.charPeripheralAddress 	= (TextView)convertView.findViewById(R.id.char_details_peripheral_address);
        	fields.charServiceName 			= (TextView)convertView.findViewById(R.id.char_details_service);
        	fields.charServiceUuid 			= (TextView)convertView.findViewById(R.id.char_details_service_uuid);
        	fields.charName 				= (TextView)convertView.findViewById(R.id.char_details_name);
        	fields.charUuid	 				= (TextView)convertView.findViewById(R.id.char_details_uuid);
        	
        	fields.charDataType 	= (TextView) convertView.findViewById(R.id.char_details_type);
        	fields.charProperties 	= (TextView) convertView.findViewById(R.id.char_details_properties);
        	
        	textViewStringValue = (TextView)convertView.findViewById(R.id.char_details_ascii_value);
        	fields.charDecValue 	= (TextView) convertView.findViewById(R.id.char_details_decimal_value);
    
        	fields.charHexValue 	= (EditText) convertView.findViewById(R.id.char_details_hex_value);
        	fields.charDateValue 	= (TextView) convertView.findViewById(R.id.char_details_timestamp);
        	//;
        	fields.notificationBtn = (ToggleButton) convertView.findViewById(R.id.char_details_notification_switcher);
        	fields.readBtn	= (Button) convertView.findViewById(R.id.char_details_read_btn);
        	fields.writeBtn = (Button) convertView.findViewById(R.id.char_details_write_btn);
        	fields.writeBtn.setTag(fields.charHexValue);
        	
        	//����ť������
        	fields.readBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mBleWrapper.requestCharacteristicValue(mCharacteristic);
				}
			});

        	//д��ť������  �ⷢ����Ϣ����
        	fields.writeBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					
					EditText hex = (EditText) v.getTag();
					String newValue =  hex.getText().toString().toLowerCase(Locale.getDefault());
					byte[] dataToWrite = parseHexStringToBytes(newValue); 
					
					// ����ΧBle�豸д����.
					mBleWrapper.writeDataToCharacteristic(mCharacteristic, dataToWrite);
				}
			});          	
        	
        	// ֪ͨ�ļ�����.   �����һ��
        	//����������Notifiy��ʹ��.
        	fields.notificationBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        		
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked == mNotificationEnabled) 
						return; // no need to update anything
					
					
					//mBleWrapper.setNotificationForCharacteristic(mCharacteristic, isChecked);
					// �����Լ���д�Ľӿ� ok
					mBleWrapper.setCharacteristicNotification(mCharacteristic, isChecked);
					mNotificationEnabled = isChecked;
				}
			} );
        	
            convertView.setTag(fields);
        } else {
            fields = (FieldReferences) convertView.getTag();
        }	

        // ������ֵ���õ�view��.
        fields.charPeripheralName.setText(mBleWrapper.getDevice().getName()); 			//����:
        fields.charPeripheralAddress.setText(mBleWrapper.getDevice().getAddress());		//��ַ:
        
        // ������
        String tmp = mCharacteristic.getService().getUuid().toString().toLowerCase(Locale.getDefault());
        fields.charServiceUuid.setText(tmp);
        fields.charServiceName.setText(BleNamesResolver.resolveServiceName(tmp));
        
        //�����uuid
        String uuid = mCharacteristic.getUuid().toString().toLowerCase(Locale.getDefault());
        String name = BleNamesResolver.resolveCharacteristicName(uuid);
        
        
        fields.charName.setText(name);
        fields.charUuid.setText(uuid);
        
        //������ֵ.
        int format = mBleWrapper.getValueFormat(mCharacteristic);
        fields.charDataType.setText(BleNamesResolver.resolveValueTypeDescription(format));
        int props = mCharacteristic.getProperties();
        String propertiesString = String.format("0x%04X [", props);
        
        if((props & BluetoothGattCharacteristic.PROPERTY_READ) != 0) 
        	propertiesString += "�� ";
        
        if((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) 
        	propertiesString += "д ";
        
        if((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) 
        	propertiesString += "notify ";
        
        if((props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) 
        	propertiesString += "indicate ";
        
        if((props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) 
        	propertiesString += "write_no_response ";
        fields.charProperties.setText(propertiesString + "]");
        
        fields.notificationBtn.setEnabled((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0);
        fields.notificationBtn.setChecked(mNotificationEnabled);
        fields.readBtn.setEnabled((props & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
        fields.writeBtn.setEnabled((props & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0);
        fields.charHexValue.setEnabled(fields.writeBtn.isEnabled());
        
        fields.charHexValue.setText(mAsciiValue);
        //fields.charStrValue.setText(mStrValue);
        
        fields.charDecValue.setText(String.format("%d", mIntValue));
        fields.charDateValue.setText(mLastUpdateTime);
   
        return convertView;
	}
		
	// ����EventBus
	public void  onEventMainThread( EventBusString event){
		
		//�յ�����.
		strTemp = event.getMsg();
		//setAsciiValue(strTemp);
		
		//Log.v("e", "������,�յ���! EventBus: "+strTemp);   // ^_^			
	}
    	
	//private class FieldReferences {
	public class FieldReferences {
		TextView charPeripheralName;
		TextView charPeripheralAddress;
		TextView charServiceName;
		TextView charServiceUuid;
		TextView charUuid;
		TextView charName;
		TextView charDataType;
		//TextView charStrValue;
		EditText charHexValue;  
		TextView charDecValue;
		TextView charDateValue;
		TextView charProperties;
		
		ToggleButton notificationBtn;
		Button readBtn;
		Button writeBtn;
	}
	
	public static class  StringUtil{
		
		public static byte[] HexCommandtoByte(byte[] data){
			if(data == null){
				return null;
			}
			int nLength = data.length;
			
			String strTemString = new String(data, 0, nLength);
			String[] strings = strTemString.split(" ");
			nLength = strings.length;
			data = new byte[nLength];
			
			for(int i=0;i<nLength;i++){
				if(strings[i].length()!=2){
					data[i] = 00;
					continue ;
				}
				try{
					data[i] = (byte)Integer.parseInt(strings[i],16);
				}
				catch(Exception e){
					data[i]=00;
					continue;
				}
			}	
			return data;
		}
		
	}
	
}

