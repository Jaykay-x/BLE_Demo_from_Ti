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
	
   	
	// Android系统的BluetoothGattCharacteristic. 周边设备的特征.
	private BluetoothGattCharacteristic mCharacteristic = null;
	
	// 动态布局器
	private LayoutInflater mInflater;    // 构造函数里面得到值.
	
	// 封装Ble的东东.
	private BleWrapper mBleWrapper = null;  //构造函数里得到值.
	
	// 数组.
	private byte[] mRawValue = null;
	
	private	int		mIntValue = 0;
	private	String	mAsciiValue = "";
	public	String	mStrValue = "";	
	private String	mLastUpdateTime = "";
	private boolean mNotificationEnabled = false;
	
	//构造函数,把父view接收下来,还有Ble
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
		
		// 官方推荐的都要求以0x开头,所以官方在这个位置会去让收到数组里减1,但是
		// 这里硬件工程师是直接以16进制的数组作为命令,所以,这里不用去减1.
		// 如果以后要做成通用的BLE测试工具,则需要修改这个,让数组减1,从而符合国际惯例.
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
		
		// 注册EventBus. ok. 可以收到数据.
		if(!EventBus.getDefault().isRegistered(this)){
        	
        	EventBus.getDefault().register(this); 
        	
        }
		
		// get already available view or create new if necessary
		FieldReferences fields;
		
        if (convertView == null) {
        	
        	// 取控件的对象.
        	
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
        	
        	//读按钮监听器
        	fields.readBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mBleWrapper.requestCharacteristicValue(mCharacteristic);
				}
			});

        	//写按钮监听器  这发个消息看看
        	fields.writeBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					
					EditText hex = (EditText) v.getTag();
					String newValue =  hex.getText().toString().toLowerCase(Locale.getDefault());
					byte[] dataToWrite = parseHexStringToBytes(newValue); 
					
					// 向外围Ble设备写数据.
					mBleWrapper.writeDataToCharacteristic(mCharacteristic, dataToWrite);
				}
			});          	
        	
        	// 通知的监听器.   这里改一下
        	//这里是设置Notifiy的使能.
        	fields.notificationBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        		
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked == mNotificationEnabled) 
						return; // no need to update anything
					
					
					//mBleWrapper.setNotificationForCharacteristic(mCharacteristic, isChecked);
					// 调用自己改写的接口 ok
					mBleWrapper.setCharacteristicNotification(mCharacteristic, isChecked);
					mNotificationEnabled = isChecked;
				}
			} );
        	
            convertView.setTag(fields);
        } else {
            fields = (FieldReferences) convertView.getTag();
        }	

        // 把属性值设置到view上.
        fields.charPeripheralName.setText(mBleWrapper.getDevice().getName()); 			//外设:
        fields.charPeripheralAddress.setText(mBleWrapper.getDevice().getAddress());		//地址:
        
        // 服务名
        String tmp = mCharacteristic.getService().getUuid().toString().toLowerCase(Locale.getDefault());
        fields.charServiceUuid.setText(tmp);
        fields.charServiceName.setText(BleNamesResolver.resolveServiceName(tmp));
        
        //服务的uuid
        String uuid = mCharacteristic.getUuid().toString().toLowerCase(Locale.getDefault());
        String name = BleNamesResolver.resolveCharacteristicName(uuid);
        
        
        fields.charName.setText(name);
        fields.charUuid.setText(uuid);
        
        //拿特征值.
        int format = mBleWrapper.getValueFormat(mCharacteristic);
        fields.charDataType.setText(BleNamesResolver.resolveValueTypeDescription(format));
        int props = mCharacteristic.getProperties();
        String propertiesString = String.format("0x%04X [", props);
        
        if((props & BluetoothGattCharacteristic.PROPERTY_READ) != 0) 
        	propertiesString += "读 ";
        
        if((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) 
        	propertiesString += "写 ";
        
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
		
	// 接收EventBus
	public void  onEventMainThread( EventBusString event){
		
		//收到数据.
		strTemp = event.getMsg();
		//setAsciiValue(strTemp);
		
		//Log.v("e", "哈哈哈,收到了! EventBus: "+strTemp);   // ^_^			
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

