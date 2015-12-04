package mtn.lgdx.uavcontrol;

import android.R.integer;

public class UAVCmd {
	
	public static final int ACCELERATOR 	= 0X1;	//����
	public static final int RIGHT_LEFT 		= 0X2;	//����
	public static final int ROTATE 			= 0X3;	//��ת
	public static final int FORWARD_BACK 	= 0X4;	//ǰ��
	
	public static final int START 			= 0X5;	//��������
	public static final int START_DELAY 	= 0X6;	//������ʱ����
	
	
	public static final int MEDIAN 		= 0X3FF; 	//���������м�λ�÷��͵�����

	public static int FormatCmd(int type , int data ){
		//���������ֽڵ�����,��ߵ���λ��ʾ��������
		return ((type<<12 & 0xffff) + data);
	}
}
