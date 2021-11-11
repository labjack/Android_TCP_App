# LabJack Modbus over TCP Demonstration for Android Devices
11/11/2021
[support@labjack.com](mailto:support@labjack.com)


This package contains an app and its source code demonstrating Modbus over TCP control of a LabJack on Android. Current LabJacks that support direct Modbus TCP communications are the T4, T7, and UE9(EOL).


## App installation:

1. Copy the LJModbusDemo.apk file to your Android device. Two easy ways to do this is to connect your Android device to your computer over USB and copy LJModbusDemo.apk over to your Android device's internal storage or email LJModbusDemo.apk to an email account that is setup on your Android device.

2. On your Android device, go to Settings->Security and enable "Unknown sources" to install apps that are not from the Play Store. After installing LJModbusDemo.apk you can disable this setting again.

3. From there, using a file manager or the email attachment open LJModbusDemo.apk. You will be given the option to install the app.


## App usage:

- First make sure your LabJack and Android devices are connected to your network. Using the IP Address you specify the LabJack device to connect to. Both the LabJack T7 and UE9 use port 502 for TCP Modbus.

- To write a value, enter the Modbus address and the value to send, and specify the value's data type (UINT16, INT32, UINT32, FLOAT32). Press the "Write" button to send the value to your LabJack device.

- To read a value, enter the Modbus address and specify the value's data type. Press the "Read" button to read the value from your LabJack device. The read value will be displayed in the "Value" box.

- Under "Data Type", and the "Write" and "Read" buttons is the additional information text area. Additional information and errors will be displayed here.


## Modbus:

Detailed Modbus information can be found [here](https://labjack.com/support/software/api/modbus)

The [Modbus Map](https://labjack.com/support/software/api/modbus/modbus-map) provides Modbus addresses, data types and read/write information. Addresses map to a LabJack device's sensor, setting, or configuration.


## Source code:

The app's source code is in the LJModbusDemo folder. It is set up as a Android Studio project that uses Gradle to build.

The project was updated to use a target SDK API 30 and minimum required SDK API 9.

TCP and Modbus operations are implemented using the third party jamod library, which is included in the LJModbusDemo\libs folder. jamod copyright and license information can be found in its [download](http://jamod.sourceforge.net)

LabJack provided source code is licensed under MIT X11. The license can be found in the LICENSE.txt file.
