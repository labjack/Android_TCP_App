package com.labjack.ljmodbusdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteMultipleRegistersRequest;
import net.wimpi.modbus.msg.WriteMultipleRegistersResponse;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;
import net.wimpi.modbus.util.ModbusUtil;

import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.Arrays;


public class MainActivity extends Activity {
	private TextView outputText;
	private EditText ipAddrEText;
	private EditText portEText;
	private EditText mAddrEText;
	private EditText mValueEText;
	private Spinner mTypeSpinner;
	
	private Handler uiHandler;

	private ModbusThread modbusThread;

	public MainActivity() {
		uiHandler = new Handler(Looper.getMainLooper());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		outputText = (TextView)findViewById(R.id.outputText);
		ipAddrEText = (EditText)findViewById(R.id.ipAddrEText);
		portEText = (EditText)findViewById(R.id.portEText);
		mAddrEText = (EditText)findViewById(R.id.mAddrEText);
		mValueEText = (EditText)findViewById(R.id.mValueEText);
		mTypeSpinner = (Spinner)findViewById(R.id.mTypeSpinner);

		mTypeSpinner.setSelection(3); //FLOAT32
		
		modbusThread = new ModbusThread(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void writeClick(View view) {
		if(!modbusThread.isAlive()) {
			modbusThread = new ModbusThread(true);
			modbusThread.start();
		}
	}

	public void readClick(View view) {
		if(!modbusThread.isAlive()) {
			modbusThread = new ModbusThread(false);
			modbusThread.start();
		}
	}

	public void setValueText(String str) {
		final String fStr = str;
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                // This gets executed on the UI thread so it can safely modify Views
            	mValueEText.setText(fStr);
            }
        });
	}

	public void setOutputText(String str) {
		final String fStr = str;
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                // This gets executed on the UI thread so it can safely modify Views
                outputText.setText(fStr);
            }
        });
	}
	
	class ModbusThread extends Thread {
		public boolean write;
		
		public ModbusThread(boolean write) {
			this.write = write;
		}
		
		@Override
		public void run() {
			TCPMasterConnection conn = null; //the connection
			ModbusTCPTransaction trans = null; //the transaction
			Register[] regs;
			byte[] bytes;
			String outStr = "";

			try {
				//Get user settings
				InetAddress iAddr = InetAddress.getByName(ipAddrEText.getText().toString()); //The device's address
				int port = Integer.parseInt(portEText.getText().toString()); //The device's port
				int mAddr = Integer.parseInt(mAddrEText.getText().toString()); //Modbus starting register address
				String mValue = mValueEText.getText().toString();
				String mType = mTypeSpinner.getSelectedItem().toString();
				
				//Connect to device
				setOutputText("Connecting to device ...");
				conn = new TCPMasterConnection(iAddr);
				conn.setPort(port);
				conn.connect();
				
				if(write) {
					setOutputText("Writing to device ...");

					//Convert value to bytes
					if(mType.equals("UINT16")) {
						int val = 0;
						try {
							val = Integer.parseInt(mValue);
							if(val < 0 || val > 65535) {
								throw new Exception("");
							}
						}
						catch(Exception ex) {
							throw new NumberFormatException("Invalid uint16:\"" + mValue + "\"");
						}
						
						bytes = ModbusUtil.unsignedShortToRegister(val);
					}
					else if(mType.equals("INT32")) {
						bytes = ModbusUtil.intToRegisters(Integer.parseInt(mValue));
					}
					else if(mType.equals("UINT32")) {
						long val = 0;
						try {
							//ModbusUtil doesn't provide a uint32 to registers converter
							val = Long.parseLong(mValue);
							if(val < 0 || val > 4294967295L) {
								throw new Exception("");
							}
						}
						catch(Exception ex) {
							throw new NumberFormatException("Invalid uint32:\"" + mValue + "\"");
						}
						bytes = new byte[4];
						System.arraycopy(ModbusUtil.longToRegisters(val), 4, bytes, 0, 4);
					}
					else if(mType.equals("FLOAT32")) {
						bytes = ModbusUtil.floatToRegisters(Float.parseFloat(mValue));
					}
					else {
						throw new Exception("Invalid data type (" + mType + ")?");
					}
					
					//Convert bytes to registers
					regs = new Register[bytes.length/2];
					for(int i = 0; i < bytes.length/2; i++) {
						regs[i] = new SimpleRegister(0);
						regs[i].setValue(Arrays.copyOfRange(bytes, i*2, i*2+2));
					}
					
					//Setup the Modbus request  
					WriteMultipleRegistersRequest writeReq = new WriteMultipleRegistersRequest(mAddr, regs);
					//The data length consists of 7 bytes for this function plus the data bytes
					//The jamod library sets data length to whatever you pass +2 (so subtract 2 from actual)
					writeReq.setDataLength(5+bytes.length);
					writeReq.setUnitID(1);
	
					outStr = "Modbus request bytes (hex): " + writeReq.getHexMessage();
					
					trans = new ModbusTCPTransaction(conn);
					trans.setRequest(writeReq);
					
					//Send/Receive the Modbus request/response
					trans.execute();

					WriteMultipleRegistersResponse writeRes = (WriteMultipleRegistersResponse) trans.getResponse();
	
					outStr += "\nModbus response bytes (hex): " + writeRes.getHexMessage();
				}
				else {
					setOutputText("Reading from device ...");

					int numRegs = 0;
					if(mType.equals("UINT16")) {
						numRegs = 1;
					}
					else {
						numRegs = 2;
					}
					
					ReadMultipleRegistersRequest readReq = new ReadMultipleRegistersRequest(mAddr, numRegs);

					outStr = "Modbus request bytes (hex): " + readReq.getHexMessage();

					//Setup the Modbus request  
					trans = new ModbusTCPTransaction(conn);
					trans.setRequest(readReq);
					
					//Send/Receive the Modbus request/response
					trans.execute();

					//Get the Modbus response data
					ReadMultipleRegistersResponse readRes = (ReadMultipleRegistersResponse) trans.getResponse();
					
					//Convert registers to bytes
					regs = readRes.getRegisters();
					bytes = new byte[regs.length*2];
					for(int i = 0; i < regs.length; i++) {
						System.arraycopy(regs[i].toBytes(), 0, bytes, i*2, 2);
					}
					
					//Convert bytes to values
					if(mType.equals("UINT16")) {
						mValue = String.format("%d", ModbusUtil.registerToUnsignedShort(bytes));
					}
					else if(mType.equals("INT32")) {
						mValue = String.format("%d", ModbusUtil.registersToInt(bytes));
					}
					else if(mType.equals("UINT32")) {
						//ModbusUtil doesn't provide a registers/bytes to uint32 converter
						byte[] bytes2 = new byte[8];
						System.arraycopy(bytes, 0, bytes2, 4, 4);
						mValue = String.format("%d", ModbusUtil.registersToLong(bytes2));
					}
					else if(mType.equals("FLOAT32")) {
						mValue = new DecimalFormat("0.######").format(ModbusUtil.registersToFloat(bytes));
					}
					else {
						throw new Exception("Invalid data type (" + mType + ")?");
					}
					
					setValueText(mValue);

					outStr += "\nModbus response bytes (hex): " + readRes.getHexMessage();
				}
			}
			catch(ModbusSlaveException me) {
				outStr = "Modbus response exception " + me.getType() + ".";
			}
			catch(Exception e) {
				outStr = "Error Detected:\n" + e.toString();
			}
			
			try{
				if(conn != null) {
					//Close connection
					conn.close();
				}
			}
			catch(Exception e) {
			}

			try {
				setOutputText(outStr + "\n\nDone.");
			}
			catch(Exception e) {
			}
		}
	}
}
