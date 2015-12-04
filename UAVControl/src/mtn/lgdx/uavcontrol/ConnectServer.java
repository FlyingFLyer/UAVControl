package mtn.lgdx.uavcontrol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.R.integer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ConnectServer extends Thread {
	
	private Socket mSocket;

	private String ip;
	private int port;
	private BufferedWriter writer = null;
	private BufferedReader reader = null;
	private Handler mHandler = null;
	private static final String TAG = "ConnectThread";
	
	public ConnectServer(String ip,int port,Handler handler) {
		this.ip = ip;
		this.port = port;
		this.mHandler = handler;

	}
	
	@Override
	public void run() {
		
		try {
			mSocket = new Socket(ip, port);
			
			sendMessagetoHandler(1);		//连接成功
//			String line = null;
//			int len = -1;
//		
//			
//			byte buffer [] = new byte[256];
//			while ((len=mSocket.getInputStream().read(buffer)) !=-1) {
//				line = new String(buffer, 0, len);
//				sendMessagetoHandler(2,line);
//				for (int i = 0; (i < buffer.length)&&(buffer[i]!=0); i++) {
//					buffer[i] = 0;
//				}
//			}
			
			//reader.close();
			//writer.close();
			//mSocket.close();
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			sendMessagetoHandler(3, "");
			Log.e(TAG, "socket error");
		} catch (IOException e) {
			e.printStackTrace();
			sendMessagetoHandler(3, "");
			Log.e(TAG, "socket error");
		}
	}
	
	  public void WriteString(String str) {
		  Socket s = mSocket;
	        try {
	        	if (s!=null) {
					s.getOutputStream().write(str.getBytes());
					s.getOutputStream().flush();
				}
	        } catch (IOException e) {
	        	Log.e(TAG, "Socket write error");
	        }
	    }
	  
	  public void WriteInt(int oneByte){
		  Socket s = mSocket;
		  if (s != null) {
			try {
				//先发高八位，后发低八位
				s.getOutputStream().write((oneByte & 0xffff)>>8);
				s.getOutputStream().write(oneByte & 0xff);
				s.getOutputStream().flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	  }
	  
	  public void cancel() {
		  if (mSocket!=null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	  
	  private void sendMessagetoHandler(int code, String s){
		  if (mHandler != null){
			  	Message msg = new Message();
			  	msg.what = code;
				Bundle dataBundle = new Bundle();
				dataBundle.putString("MSG",s);
				msg.setData(dataBundle);
				mHandler.sendMessage(msg);
		  }
		  return;
	  }
	  
	  private void sendMessagetoHandler(int what){
		  if (mHandler != null) {
			  mHandler.sendEmptyMessage(what);
		  }
		  return;
	  }

	public Socket getSocket() {
		return mSocket;
	}

	public void setSocket(Socket mSocket) {
		this.mSocket = mSocket;
	}

}
