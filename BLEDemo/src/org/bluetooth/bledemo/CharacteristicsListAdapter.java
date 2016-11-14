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
	
		//��̬���鱣������Χ�豸������
    	private ArrayList<BluetoothGattCharacteristic> mCharacteristics;
    	
    	//���ּ�����
    	private LayoutInflater mInflater;
    	
    	//CharacteristicsListAdapter���캯��
    	public CharacteristicsListAdapter(Activity parent) {
    		
    		super();
    		
    		//���ɶ�̬�������.
    		mCharacteristics  = new ArrayList<BluetoothGattCharacteristic>();
    		
    		// ��������һ��.
    		mInflater = parent.getLayoutInflater();
    		
    	}
    	
    	// ���һ������
    	public void addCharacteristic(BluetoothGattCharacteristic ch) {
    		if(mCharacteristics.contains(ch) == false) {
    			mCharacteristics.add(ch);
    		}
    	}
    	
    	// ͨ��index��ȡһ������
    	public BluetoothGattCharacteristic getCharacteristic(int index) {
    		return mCharacteristics.get(index);
    	}

    	// �������
    	public void clearList() {
    		mCharacteristics.clear();
    	}
    	
		@Override	// ��ȡ����������
		public int getCount() {
			return mCharacteristics.size();
		}

		@Override	// ͨ��λ�û�ȡ����
		public Object getItem(int position) {
			return getCharacteristic(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override //��ListView��������.
		public View getView(int position, View convertView, ViewGroup parent) {
			
			FieldReferences fields;
			
            if (convertView == null) { //getView�ļ��ص���ȷ����.
            	
            	//������ز���. ����peripheral_list_characteristic_item.xml,����Service��������.
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
            
            // ͨ��ch�õ�������uuid
            String uuid = ch.getUuid().toString().toLowerCase(Locale.getDefault());
            
            // ͨ��uuid�õ�����������
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
