package ru.dzakhov;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;	 
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;

/**
 * �����, ����������� ���� � ���������� ������ �� ������ Windows-����������.
 * @author �������
 *
 */
public class TcpServer implements Runnable {
	/**
	 * ������ �� ������ Handler �� RoboHeadActivity. �� �������������� ��� �����������
	 * � Android-���������� ���������: � �� Arduino, � �� Windows.
	 */
	private Handler mHandler;
	
	/**
	 * ������� ��������� ������� ������.
	 */
	private boolean mTerminate = false;

	/**
	 * ��� ������ ������ �� ������ ������ ����� ������� ����� �� ������ ����������� �� ������ ������
	 * � ����� �������� �� ��������� ��������� ������. ���� ������������ ��� ���������� ������������
	 * �������.
	 */
	private String mPreviousCommandsRest = "";
	
	/**
	 * ����������� ������.
	 * @param handler ������ �� ������ Handler �� RoboHeadActivity.
	 */
	public TcpServer(final Handler handler) {
		super();
		mHandler = handler;
	}
	
	/**
	 * ��������� ������� ������ (��������� �������� terminate ��� ������ �� ������ run()).
	 */
	public final void stopRun() {
		mTerminate = true;
	}
	
	/**
	 * ���������� ���������� Runnable.
	 */
	public final void run() {
		while (true) {
			Socket socket = null;
			ServerSocket serverSocket = null;
			try {
				Logger.d("TcpServer: Waiting for client to connect...");
				serverSocket = new ServerSocket(Settings.MESSAGESOCKETPORT);
				socket = serverSocket.accept();
				Logger.d("TcpServer: Connected.");
				while (true) {
					// �������� ������ �������� �� ������ ������ ������:
					List<String> commandList = getCommandsFromStream(socket.getInputStream());
					if (commandList.size() > 0) {
						Logger.d("commandList.size() = " + commandList.size());
					}						
					
					// ��������� ������ �������� �������:
					for (int i = 0; i < commandList.size(); i++) {
						String command = commandList.get(i);
						
						// ������� ���������� � RoboHeadActivity:
						Message message = new Message();
						message.obj = command;
						mHandler.sendMessage(message);
					}
					
					if (mTerminate) {
						break;
					}
				} // read while cycle
			} catch (Exception e) {
				Logger.d("TcpServer error (1): " + e.getLocalizedMessage());
			}
	
			if ((socket != null) && (!socket.isClosed())) {
				try {
					socket.close();
				} catch (Exception e2) {
					Logger.d("TcpServer error (socket.close()): " + e2.getLocalizedMessage());
				}
			}
			socket = null;
	
			if ((serverSocket != null) && (!serverSocket.isClosed())) {
				try {
					serverSocket.close();
				} catch (Exception e2) {
					Logger.d("TcpServer error (serverSocket.close()): " + e2.getLocalizedMessage());
				}
			}
			serverSocket = null;
			
			if (mTerminate) {
				break;
			}
		} // accept while cycle
	} // run

	/**
	 * ��������� �� �������� ������ ������������ �������, ���������� ��������� #13, #10.
	 * �������, ��� ����������� �������� �� ������� ����� �� ������������ � �������� ������ ������.
	 * �������� � �������� ����� ����� ������� ������������� �� ���������� ������ ������.
	 * @param inputStream ����� ����� (��������� �� ������).
	 * @return ������ ������ ������.
	 * @throws IOException ������ ������ �� ������ ����� (�� ������).
	 */
	public final List<String> getCommandsFromStream(final InputStream inputStream) throws IOException {
		List<String> result = new ArrayList<String>();
		
		DataInputStream dataInputStream = new DataInputStream(inputStream);
		while (true) {
			int bytesAvailable = dataInputStream.available(); 
			if (bytesAvailable <= 0) {
				break;
			}
			
			String command = dataInputStream.readLine();
			if (command == null) { // (������� ����� ����)
				break;
			} else {
				// ���� �� ���������� �������� ����� ������� ���� ���������, ��������� �������:
				if (!mPreviousCommandsRest.equals("")) {
					command = mPreviousCommandsRest + command;
				}
				
				// ���� �� ��� ������� ���������, �������� ��� ����������� �� ��������� ��������,
				// ����� �������� ����������� ������� � ������:
				if (command.length() < Settings.MESSAGE_LENGTH) {
					mPreviousCommandsRest = command;
				} else {
					result.add(command);
					mPreviousCommandsRest = "";
				}
			}
		}
		return result;
	}
} // class
