package org.bluetooth.bledemo;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CharacteristicsListAdapter extends BaseAdapter {
	
		//动态数组保存着外围设备的特征
    	private ArrayList<BluetoothGattCharacteristic> mCharacteristics;
    	
    	//布局加载器
    	private LayoutInflater mInflater;
    	
    	//CharacteristicsListAdapter构造函数
    	public CharacteristicsListAdapter(Activity parent) {
    		
    		super();
    		
    		//生成动态数组对象.
    		mCharacteristics  = new ArrayList<BluetoothGattCharacteristic>();
    		
    		// 布局用上一个.
    		mInflater = parent.getLayoutInflater();
    		
    	}
    	
    	// 添加一个特征
    	public void addCharacteristic(BluetoothGattCharacteristic ch) {
    		if(mCharacteristics.contains(ch) == false) {
    			mCharacteristics.add(ch);
    		}
    	}
    	
    	// 通过index获取一个特征
    	public BluetoothGattCharacteristic getCharacteristic(int index) {
    		return mCharacteristics.get(index);
    	}

    	// 清除数组
    	public void clearList() {
    		mCharacteristics.clear();
    	}
    	
		@Override	// 获取特征的数量
		public int getCount() {
			return mCharacteristics.size();
		}

		@Override	// 通过位置获取特征
		public Object getItem(int position) {
			return getCharacteristic(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override //给ListView加载数据.
		public View getView(int position, View convertView, ViewGroup parent) {
			
			FieldReferences fields;
			
            if (convertView == null) { //getView的加载的正确姿势.
            	
            	//这里加载布局. 包含peripheral_list_characteristic_item.xml,就像Service加载那样.
            	convertView = mInflater.inflate(R.layout.peripheral_list_characteristic_item, null);
            	fields = new FieldReferences();
            	fields.charName = (TextView)convertView.findViewById(R.id.peripheral_list_characteristic_name);
            	fields.charUuid = (TextView)convertView.findViewById(R.id.peripheral_list_characteristic_uuid);
                convertView.setTag(fields);
            } 
            else {
                fields = (FieldReferences) convertView.getTag();
            }			
			
           
            BluetoothGattCharacteristic ch = getCharacteristic(position);
            
            // 通过ch得到特征的uuid
            String uuid = ch.getUuid().toString().toLowerCase(Locale.getDefault());
            
            // 通过uuid得到特征的名字
            String name = BleNamesResolver.resolveCharacteristicName(uuid);
            
            fields.charName.setText(name);
            fields.charUuid.setText(uuid);
   
			return convertView;
		}
    	
		private class FieldReferences {
			TextView charName;
			TextView charUuid;
		}
}
