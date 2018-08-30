package cordova.plugin.usbprinter;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;

/**
 * This class echoes a string called from JavaScript.
 */
public class UsbPrinter extends CordovaPlugin {

    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbInterface mInterface;
    private UsbEndpoint mEndPoint;
    private PendingIntent mPermissionIntent;
    private static final String ACTION_USB_PERMISSION = "cordova.plugin.usbprinter.USB_PERMISSION";
    private static Boolean forceCLaim = true;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(action.equals("add")){
            this.add(args, callbackContext);
            return true;
        }
        else if(action.equals("substract")){
            this.substract(args, callbackContext);
            return true;
        }
        return false;
    }


    private void add(JSONArray args, CallbackContext callback){
        if(args != null){
            try{
                int p1 = Integer.parseInt(args.getJSONObject(0).getString("param1"));
                int p2 = Integer.parseInt(args.getJSONObject(0).getString("param2"));
                callback.success(""+(p1+p2));
            }catch(Exception ex){
                callback.error("Something went wrong " + ex);
            }
        }else{
            callback.error("Please don't pass null value");
        }
    }

    private void substract(JSONArray args, CallbackContext callback){
        if(args != null){
            try{
                int p1 = Integer.parseInt(args.getJSONObject(0).getString("param1"));
                int p2 = Integer.parseInt(args.getJSONObject(0).getString("param2"));
                callback.success(""+(p1-p2));
            }catch(Exception ex){
                callback.error("Something went wrong " + ex);
            }
        }else{
            callback.error("Please don't pass null value");
        }
    }

    final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            mInterface = device.getInterface(0);
                            mEndPoint = mInterface.getEndpoint(0);
                            mConnection = mUsbManager.openDevice(device);

                            //setup();
                        }
                    } else {
                        //Log.d("SUB", "permission denied for device " + device);
                        Toast.makeText(context, "PERMISSION DENIED FOR THIS DEVICE", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    private void scanDevices(JSONArray args, CallbackContext callback){
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mDeviceList = mUsbManager.getDeviceList();

        if (mDeviceList.size() > 0) {
            mDeviceIterator = mDeviceList.values().iterator();

            Toast.makeText(this, "Device List Size: " + String.valueOf(mDeviceList.size()), Toast.LENGTH_SHORT).show();

            String usbDevice = "";
            while (mDeviceIterator.hasNext()) {
                UsbDevice usbDevice1 = mDeviceIterator.next();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    usbDevice += "\n" +
                            "DeviceID: " + usbDevice1.getDeviceId() + "\n" +
                            "DeviceName: " + usbDevice1.getDeviceName() + "\n" +
                            "Protocol: " + usbDevice1.getDeviceProtocol() + "\n" +
                            "Product Name: " + usbDevice1.getProductName() + "\n" +
                            "Manufacturer Name: " + usbDevice1.getManufacturerName() + "\n" +
                            "DeviceClass: " + usbDevice1.getDeviceClass() + " - " + translateDeviceClass(usbDevice1.getDeviceClass()) + "\n" +
                            "DeviceSubClass: " + usbDevice1.getDeviceSubclass() + "\n" +
                            "VendorID: " + usbDevice1.getVendorId() + "\n" +
                            "ProductID: " + usbDevice1.getProductId() + "\n";
                }

                int interfaceCount = usbDevice1.getInterfaceCount();
                Toast.makeText(this, "INTERFACE COUNT: " + String.valueOf(interfaceCount), Toast.LENGTH_SHORT).show();

                mDevice = usbDevice1;

                Toast.makeText(this, "Device is attached", Toast.LENGTH_SHORT).show();
                callback.success(usbDevice);
            }

            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            registerReceiver(mUsbReceiver, filter);

            mUsbManager.requestPermission(mDevice, mPermissionIntent);
        } else {
            Toast.makeText(this, "Please attach printer via USB", Toast.LENGTH_SHORT).show();
            callback.error("Please attach printer via USB");
        }

    }

    private void print(JSONArray args, CallbackContext callback) {

        final String test = args.getJSONObject(0).getString("msg") + "\n\n\n\n\n\n";
        testBytes = test.getBytes();

        if (mInterface == null) {
            Toast.makeText(this, "INTERFACE IS NULL", Toast.LENGTH_SHORT).show();
            callback.error("INTERFACE IS NULL");            
        } else if (mConnection == null) {
            Toast.makeText(this, "CONNECTION IS NULL", Toast.LENGTH_SHORT).show();
            callback.error("CONNECTION IS NULL");    
        } else if (forceCLaim == null) {
            Toast.makeText(this, "FORCE CLAIM IS NULL", Toast.LENGTH_SHORT).show();
            callback.error("FORCE CLAIM IS NULL");
        } else {

            mConnection.claimInterface(mInterface, forceCLaim);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    byte[] cut_paper = {0x1D, 0x56, 0x01};
                    mConnection.bulkTransfer(mEndPoint, testBytes, testBytes.length, 0);
                    mConnection.bulkTransfer(mEndPoint, cut_paper, cut_paper.length, 0);
                }
            });
            thread.run();
            callback.success("Impression réussie");
        }
    }

    private String translateDeviceClass(int deviceClass) {

        switch (deviceClass) {

            case UsbConstants.USB_CLASS_APP_SPEC:
                return "Application specific USB class";

            case UsbConstants.USB_CLASS_AUDIO:
                return "USB class for audio devices";

            case UsbConstants.USB_CLASS_CDC_DATA:
                return "USB class for CDC devices (communications device class)";

            case UsbConstants.USB_CLASS_COMM:
                return "USB class for communication devices";

            case UsbConstants.USB_CLASS_CONTENT_SEC:
                return "USB class for content security devices";

            case UsbConstants.USB_CLASS_CSCID:
                return "USB class for content smart card devices";

            case UsbConstants.USB_CLASS_HID:
                return "USB class for human interface devices (for example, mice and keyboards)";

            case UsbConstants.USB_CLASS_HUB:
                return "USB class for USB hubs";

            case UsbConstants.USB_CLASS_MASS_STORAGE:
                return "USB class for mass storage devices";

            case UsbConstants.USB_CLASS_MISC:
                return "USB class for wireless miscellaneous devices";

            case UsbConstants.USB_CLASS_PER_INTERFACE:
                return "USB class indicating that the class is determined on a per-interface basis";

            case UsbConstants.USB_CLASS_PHYSICA:
                return "USB class for physical devices";

            case UsbConstants.USB_CLASS_PRINTER:
                return "USB class for printers";

            case UsbConstants.USB_CLASS_STILL_IMAGE:
                return "USB class for still image devices (digital cameras)";

            case UsbConstants.USB_CLASS_VENDOR_SPEC:
                return "Vendor specific USB class";

            case UsbConstants.USB_CLASS_VIDEO:
                return "USB class for video devices";

            case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
                return "USB class for wireless controller devices";

            default:
                return "Unknown USB class!";
        }
    }
    

}
