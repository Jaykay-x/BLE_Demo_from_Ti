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

// 这个类把所有的服务都显示出来
public class ServicesListAdapter extends BaseAdapter {
	
	//	蓝牙服务,用动态数组保存着, 类型是BluetoothGattService,变量名mBTServices;
	private ArrayList<BluetoothGattService> mBTServices;
	
	// 用于加载布局. setContentView内部也是调用它来完成布局的加载的.
	private LayoutInflater mInflater;
	
	//构造函数
	public ServicesListAdapter(Activity parent) {
		super();
		//生成一个存放Gatt服务的动态数组.
		mBTServices  = new ArrayList<BluetoothGattService>();
		
		//动态加载布局,这里先用了
		mInflater = parent.getLayoutInflater();
	}
	
	//加入一个服务.
	public void addService(BluetoothGattService service) {
		
		if(mBTServices.contains(service) == false) {
			mBTServices.add(service);
		}
	}
	
	//根据index取出一个服务
	public BluetoothGattService getService(int index) {
		return mBTServices.get(index);
	}

	//清除服务列表.(动态数组里).
	public void clearList() {
		mBTServices.clear();
	}
	
	@Override  //统计服务的数量.
	public int getCount() {
		return mBTServices.size();
	}

	@Override  //根据位置取一条服务.
	public Object getItem(int position) {
		return getService(position);
	}

	@Override   //根据给出的位置获取位置.
	public long getItemId(int position) {
		return position;
	}

	@Override   //ListView加载数据
	public View getView(int position, View convertView, ViewGroup parent) {
		// get already available view or create new if necessary
		FieldReferences fields; //字段引用  类似一个结构体.
		
        if (convertView == null) { //getView的加载的正确姿势.
        	
        	//这里加载布局. 包含peripheral_list_services_item.xml
        	convertView = mInflater.inflate(R.layout.peripheral_list_services_item, null);
        	
        	fields = new FieldReferences();
        	
        	//类似通过id得到对象.
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
        String type = (service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY) ? "主" : "次";
        
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
