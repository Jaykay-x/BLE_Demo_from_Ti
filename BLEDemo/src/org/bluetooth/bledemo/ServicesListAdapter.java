package org.bluetooth.bledemo;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

// ���������еķ�����ʾ����
public class ServicesListAdapter extends BaseAdapter {
	
	//	��������,�ö�̬���鱣����, ������BluetoothGattService,������mBTServices;
	private ArrayList<BluetoothGattService> mBTServices;
	
	// ���ڼ��ز���. setContentView�ڲ�Ҳ�ǵ���������ɲ��ֵļ��ص�.
	private LayoutInflater mInflater;
	
	//���캯��
	public ServicesListAdapter(Activity parent) {
		super();
		//����һ�����Gatt����Ķ�̬����.
		mBTServices  = new ArrayList<BluetoothGattService>();
		
		//��̬���ز���,����������
		mInflater = parent.getLayoutInflater();
	}
	
	//����һ������.
	public void addService(BluetoothGattService service) {
		
		if(mBTServices.contains(service) == false) {
			mBTServices.add(service);
		}
	}
	
	//����indexȡ��һ������
	public BluetoothGattService getService(int index) {
		return mBTServices.get(index);
	}

	//��������б�.(��̬������).
	public void clearList() {
		mBTServices.clear();
	}
	
	@Override  //ͳ�Ʒ��������.
	public int getCount() {
		return mBTServices.size();
	}

	@Override  //����λ��ȡһ������.
	public Object getItem(int position) {
		return getService(position);
	}

	@Override   //���ݸ�����λ�û�ȡλ��.
	public long getItemId(int position) {
		return position;
	}

	@Override   //ListView��������
	public View getView(int position, View convertView, ViewGroup parent) {
		// get already available view or create new if necessary
		FieldReferences fields; //�ֶ�����  ����һ���ṹ��.
		
        if (convertView == null) { //getView�ļ��ص���ȷ����.
        	
        	//������ز���. ����peripheral_list_services_item.xml
        	convertView = mInflater.inflate(R.layout.peripheral_list_services_item, null);
        	
        	fields = new FieldReferences();
        	
        	//����ͨ��id�õ�����.
        	fields.serviceName = (TextView)convertView.findViewById(R.id.peripheral_list_services_name);
        	fields.serviceUuid = (TextView)convertView.findViewById(R.id.peripheral_list_services_uuid);
        	fields.serviceType = (TextView)convertView.findViewById(R.id.peripheral_list_service_type);
            convertView.setTag(fields);
        } 
        else {
            fields = (FieldReferences) convertView.getTag();
        }			
		
        
        BluetoothGattService service = mBTServices.get(position);
        String uuid = service.getUuid().toString().toLowerCase(Locale.getDefault());
        String name = BleNamesResolver.resolveServiceName(uuid);
        String type = (service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY) ? "��" : "��";
        
        fields.serviceName.setText(name);
        fields.serviceUuid.setText(uuid);
        fields.serviceType.setText(type);

		return convertView;
	}
	
	private class FieldReferences {
		TextView serviceName;
		TextView serviceUuid;
		TextView serviceType;
	}
}
