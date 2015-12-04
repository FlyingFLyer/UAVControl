package mtn.lgdx.uavcontrol;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class MainActivity extends Activity implements OnLongClickListener, OnSeekBarChangeListener, OnClickListener {
	
	private static final String IP_DNS = "uavuav.oicp.net";		//服务器域名
	private static final int PORT = 1111;						//服务器端口号
	
	private EditText ipAddress;			//IP地址输入框
	private Button btnConnectServer;	//连接服务器
	
	private ImageButton btnStartButton;		//启动按钮
	
	private SeekBar skb_accelerator;		//油门
	private SeekBar skb_FB;					//前进后退
	private SeekBar skb_RL;					//左转右转
	private SeekBar skb_rotate;				//旋转
	
	private  Handler handler;
	private ConnectServer connectServer = null; 
	
	public static final int ACCELERATE_MAX 		= 1523;			//油门的最大值
    public static final int RIGHT_LEFT_MAX 		= 1523;			//左右的最大值
    public static final int ROTATE_MAX 			= 1523;			//旋转的最大值
    public static final int FORWARD_BACK_MAX	= 1523;			//前后的最大值
    
    public static final int ACCELERATE_MIN 		= 523;			//油门的最小值
    public static final int RIGHT_LEFT_MIN 		= 523;			//左右的最小值
    public static final int ROTATE_MIN 			= 523;			//旋转的最小值
    public static final int FORWARD_BACK_MIN	= 523;			//前后的最小值
    
    public static final int SEEKBAR_MAX = 1000;					//滑动条最大值
    
    private String ipaddr = "";			//保存服务器IP地址或域名
    
    SharedPreferences preferences;
    SharedPreferences.Editor preferences_editor;
    
    private int accelerate_cnt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ipAddress = (EditText) findViewById(R.id.ip);
        btnConnectServer = (Button) findViewById(R.id.connect);
        btnConnectServer.setOnClickListener(this);
        
        btnStartButton = (ImageButton) findViewById(R.id.btnStart);
        btnStartButton.setOnLongClickListener(this);
        
        skb_accelerator = (SeekBar) findViewById(R.id.skb1);
        skb_FB 			= (SeekBar) findViewById(R.id.skb4);
        skb_RL			= (SeekBar) findViewById(R.id.skb2);
        skb_rotate		= (SeekBar) findViewById(R.id.skb3);
        
        skb_accelerator.setMax(SEEKBAR_MAX);
        skb_accelerator.setProgress(SEEKBAR_MAX/2);
        skb_RL.setMax(SEEKBAR_MAX);
        skb_RL.setProgress(SEEKBAR_MAX/2);
        skb_rotate.setMax(SEEKBAR_MAX);
        skb_rotate.setProgress(SEEKBAR_MAX/2);
        skb_FB.setMax(SEEKBAR_MAX);
        skb_FB.setProgress(SEEKBAR_MAX/2);
        
        skb_accelerator.setOnSeekBarChangeListener(this);
        skb_FB.setOnSeekBarChangeListener(this);
        skb_RL.setOnSeekBarChangeListener(this);
        skb_rotate.setOnSeekBarChangeListener(this);
        
        setEnable(false);	//没有连上服务器的时候禁用操作
        
        handler = new Handler(){
        	@Override
        	public void handleMessage(Message msg) {
        		super.handleMessage(msg);
        		switch (msg.what) {
				case 1:
					setEnable(true);
					Toast.makeText(MainActivity.this, "成功连接到服务器", Toast.LENGTH_SHORT).show();
					break;
				case 3:
					Toast.makeText(MainActivity.this, "无法连接到服务器", Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
				}
        	}
        };
        
        preferences = getSharedPreferences("ipaddr", Context.MODE_PRIVATE);
        preferences_editor = preferences.edit();
        if (preferences != null) {
        	ipaddr = preferences.getString("ipaddr", null);
			ipAddress.setText(ipaddr);
		}
        
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


	@Override
	public boolean onLongClick(View v) {
		
		switch (v.getId()) {
		case R.id.btnStart:
			SendStartCmd();
			break;
		case R.id.connect:
			//ConnectToServer();
			break;
		default:
			break;
		}
		return true;
	}
	
	/**
     * SeekBar.OnSeekBarChangeListener 
     */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		
		int cmd_data = 0;
		int cmd_type = 0;
		switch (seekBar.getId()) {
		case R.id.skb1:		//油门
			cmd_type = UAVCmd.ACCELERATOR;
			cmd_data = (int)(ACCELERATE_MIN+progress);	//progress数值变大是降速
			break;
		case R.id.skb2:		//左右
			cmd_type = UAVCmd.RIGHT_LEFT;
			cmd_data = (int)(RIGHT_LEFT_MIN+progress);
			break;	
		case R.id.skb3:		//旋转
			cmd_type = UAVCmd.ROTATE;
			cmd_data = (int)(ROTATE_MIN+progress);
			break;
		case R.id.skb4:		//前后
			cmd_type = UAVCmd.FORWARD_BACK;
			cmd_data = (int)(FORWARD_BACK_MIN+progress);
			break;
		default:
			break;
		}
		SendCmdToServer(connectServer, UAVCmd.FormatCmd(cmd_type, cmd_data));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		
		int cmd_type = 0;
		seekBar.setProgress(seekBar.getMax()/2);
		switch (seekBar.getId()) {
		case R.id.skb1:		//油门
			cmd_type = UAVCmd.ACCELERATOR;
			break;
		case R.id.skb2:		//左右
			cmd_type = UAVCmd.RIGHT_LEFT;
			break;
		case R.id.skb3:		//旋转
			cmd_type = UAVCmd.ROTATE;
			break;
		case R.id.skb4:		//前后
			cmd_type = UAVCmd.FORWARD_BACK;
			break;
		default:
			break;
		}
		SendCmdToServer(connectServer, UAVCmd.FormatCmd(cmd_type, UAVCmd.MEDIAN));
	}


	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.connect:
			ConnectToServer();
			break;
		case R.id.btnStart:
			//SendCmdToServer(connectServer, UAVCmd.FormatCmd(UAVCmd.START, 0));
			break;

		default:
			break;
		}
		
	}
	
	/**
	 * 创建连接服务器线程去连接服务器
	 */
	private void ConnectToServer(){
		connectServer = new ConnectServer(ipAddress.getText().toString(), PORT, handler);	
		connectServer.start();
		//保存IP地址
		preferences_editor.putString("ipaddr", ipAddress.getText().toString());
		preferences_editor.commit();
	}
	
	/**
	 * 把无人机操作命令发送给服务器
	 * @param cmd
	 */
	private void SendCmdToServer(ConnectServer thread , int cmd){
		if (thread != null) {
			thread.WriteInt(cmd);
		}
	}
	
	/**
     * 该方法用来enable/disable这几个控件，只有成功连接服务器以后，这些控件才可用
    */
    private void setEnable(boolean enabled){
    	skb_accelerator.setEnabled(enabled);
    	skb_RL.setEnabled(enabled);
    	skb_FB.setEnabled(enabled);
    	skb_rotate.setEnabled(enabled);
    	btnStartButton.setEnabled(enabled);
    }
	
    private void SendStartCmd(){
			SendCmdToServer(connectServer, UAVCmd.START<<12);
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(500);
						//mConnectThread.write("1".getBytes());
						//mConnectThread.write(UAVCmd.START_DELAY<<12);
						SendCmdToServer(connectServer, UAVCmd.START_DELAY<<12);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
	}
	
}
