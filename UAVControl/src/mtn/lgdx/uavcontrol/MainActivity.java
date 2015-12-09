package mtn.lgdx.uavcontrol;

import org.apache.http.Header;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity implements OnLongClickListener,
		OnSeekBarChangeListener, OnClickListener {

	private static final int PORT = 1111; // 服务器端口号

	private ProgressDialog progDialog = null; //
	private static final String URL = "http://uavuav.oicp.net/ip.php";

	private ImageButton btnStartButton; // 启动按钮

	private SeekBar skb_accelerator; // 油门
	private SeekBar skb_FB; // 前进后退
	private SeekBar skb_RL; // 左转右转
	private SeekBar skb_rotate; // 旋转

	private ConnectServer connectServer = null;

	private static final int ACCELERATE_MAX 	= 1523; // 油门最大值
	private static final int RIGHT_LEFT_MAX 	= 1523; // 左右最大值
	private static final int ROTATE_MAX 		= 1523; // 旋转最大值
	private static final int FORWARD_BACK_MAX 	= 1523; // 前后最大值

	private static final int ACCELERATE_MIN 	= 523; // 油门的最小值
	private static final int RIGHT_LEFT_MIN 	= 523; // 左右的最小值
	private static final int ROTATE_MIN 		= 523; // 旋转的最小值
	private static final int FORWARD_BACK_MIN 	= 523; // 前后的最小值

	private static final int SEEKBAR_MAX = 1000; // 滑动条最大值

	private String ipaddr = ""; // 保存服务器IP地址或域名

	SharedPreferences preferences;
	SharedPreferences.Editor preferences_editor;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 1:				//成功连接到服务器
				setEnable(true);
				Toast.makeText(MainActivity.this, "成功连接到服务器",
						Toast.LENGTH_SHORT).show();
				break;
			case 3:			//连接服务器失败
				ShowFailedConnectServerDialog();
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnStartButton = (ImageButton) findViewById(R.id.btnStart);
		btnStartButton.setOnLongClickListener(this);

		skb_accelerator = (SeekBar) findViewById(R.id.skb1);
		skb_FB = (SeekBar) findViewById(R.id.skb4);
		skb_RL = (SeekBar) findViewById(R.id.skb2);
		skb_rotate = (SeekBar) findViewById(R.id.skb3);

		skb_accelerator.setMax(SEEKBAR_MAX);
		skb_accelerator.setProgress(SEEKBAR_MAX / 2);
		skb_RL.setMax(SEEKBAR_MAX);
		skb_RL.setProgress(SEEKBAR_MAX / 2);
		skb_rotate.setMax(SEEKBAR_MAX);
		skb_rotate.setProgress(SEEKBAR_MAX / 2);
		skb_FB.setMax(SEEKBAR_MAX);
		skb_FB.setProgress(SEEKBAR_MAX / 2);

		skb_accelerator.setOnSeekBarChangeListener(this);
		skb_FB.setOnSeekBarChangeListener(this);
		skb_RL.setOnSeekBarChangeListener(this);
		skb_rotate.setOnSeekBarChangeListener(this);

		setEnable(false); // 没有连上服务器的时候禁用操作

		GetServerIP();		//获取服务器IP地址然后连接服务器程序	

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
		case R.id.skb1: // 油门
			cmd_type = UAVCmd.ACCELERATOR;
			cmd_data = (int) (ACCELERATE_MIN + progress);
			break;
		case R.id.skb2: // 左右
			cmd_type = UAVCmd.RIGHT_LEFT;
			cmd_data = (int) (RIGHT_LEFT_MIN + progress);
			break;
		case R.id.skb3: // 旋转
			cmd_type = UAVCmd.ROTATE;
			cmd_data = (int) (ROTATE_MIN + progress);
			break;
		case R.id.skb4: // 前后
			cmd_type = UAVCmd.FORWARD_BACK;
			cmd_data = (int) (FORWARD_BACK_MIN + progress);
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
		seekBar.setProgress(seekBar.getMax() / 2);
		switch (seekBar.getId()) {
		case R.id.skb1: // 油门
			cmd_type = UAVCmd.ACCELERATOR;
			break;
		case R.id.skb2: // 左右
			cmd_type = UAVCmd.RIGHT_LEFT;
			break;
		case R.id.skb3: // 旋转
			cmd_type = UAVCmd.ROTATE;
			break;
		case R.id.skb4: // 前后
			cmd_type = UAVCmd.FORWARD_BACK;
			break;
		default:
			break;
		}
		SendCmdToServer(connectServer,
				UAVCmd.FormatCmd(cmd_type, UAVCmd.MEDIAN));
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.btnStart:
			
			break;

		default:
			break;
		}

	}

	/**
	 * 创建连接服务器线程去连接服务器
	 */
	private void ConnectToServer() {
		connectServer = new ConnectServer(ipaddr, PORT,	handler);
		connectServer.start();
	}

	/**
	 * 把无人机操作命令发送给服务器
	 * 
	 * @param cmd
	 */
	private void SendCmdToServer(ConnectServer thread, int cmd) {
		if (thread != null) {
			thread.WriteInt(cmd);
		}
	}

	/**
	 * 该方法用来enable/disable这几个控件，只有成功连接服务器以后，这些控件才可用
	 */
	private void setEnable(boolean enabled) {
		skb_accelerator.setEnabled(enabled);
		skb_RL.setEnabled(enabled);
		skb_FB.setEnabled(enabled);
		skb_rotate.setEnabled(enabled);
		btnStartButton.setEnabled(enabled);
	}

	private void SendStartCmd() {
		SendCmdToServer(connectServer, UAVCmd.START << 12);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(500);
					SendCmdToServer(connectServer, UAVCmd.START_DELAY << 12);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * 显示进度框
	 */
	private void showProgressDialog() {
		if (progDialog == null)
			progDialog = new ProgressDialog(this);
		progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progDialog.setIndeterminate(false);
		progDialog.setCancelable(false);
		progDialog.setMessage("正在查询服务器的IP地址");
		progDialog.show();
	}

	/**
	 * 隐藏进度框
	 */
	private void dissmissProgressDialog() {
		if (progDialog != null) {
			progDialog.dismiss();
		}
	}

	/**
	 * 向服务器发送http请求，查询服务器的IP地址。发送http请求使用了开源框架android-async-http
	 */
	private void GetServerIP() {
		
		showProgressDialog();	

		AsyncHttpClient client = new AsyncHttpClient();
		client.get(URL, new TextHttpResponseHandler() {		//向服务器发送http请求，查询服务器的IP地址

			@Override
			public void onSuccess(int arg0, Header[] arg1, String response) {
				dissmissProgressDialog();
				ipaddr = response;			//服务器返回的IP地址赋给该变量
				ConnectToServer();			//连接服务器
			}

			@Override
			public void onFailure(int arg0, Header[] arg1, String arg2,
					Throwable arg3) {
				dissmissProgressDialog();
				ShowFailedGetAddrDialog();
			}
		});

	}

	/**
	 * 获取服务器IP地址失败时显示该对话框
	 */
	private void ShowFailedGetAddrDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("无法获取服务器地址！");

		builder.setPositiveButton("重试",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						GetServerIP();		//重新向服务器发送请求获取其IP地址
					}
				});
		builder.setNegativeButton("退出",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();		//退出程序
					}
				});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/**
	 * 连接服务器失败时显示该对话框
	 */
	private void ShowFailedConnectServerDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("无法连接到服务器！");

		builder.setPositiveButton("重试",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ConnectToServer();		//重新尝试连接服务器
					}
				});
		builder.setNegativeButton("退出",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();			//退出程序
					}
				});
		AlertDialog dialog = builder.create();
		dialog.show();
	}
}
