package org.bluetooth.bledemo;

// 这个类 想取代Handler用的,但是最终还是没用上,可以删.
public class EventBusString {
	
	String mMsg;
	
	public EventBusString(String msg){
		mMsg = msg;
	}
	
	public String getMsg(){
		return mMsg;
	}

}
