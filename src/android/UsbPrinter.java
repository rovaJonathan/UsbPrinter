package cordova.plugin.usbprinter;

import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
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
import jdk.nashorn.api.scripting.JSObject;

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

    HashMap<String, UsbDevice> mDeviceList;
    Iterator<UsbDevice> mDeviceIterator;

    byte[] testBytes;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(action.equals("scanDevices")){
            this.scanDevices(args, callbackContext);
            return true;
        }else if(action.equals("print")){
            this.print(args, callbackContext);
            return true;
        }
        return false;
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
                        // mUsbManager.requestPermission(mDevice, mPermissionIntent);
                        //Log.d("SUB", "permission denied for device " + device);
                    }
                }
            }
        }
    };

    public void createConn(Context context) {
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        final String ACTION_USB_PERMISSION =
                "cordova.plugin.usbprinter.USB_PERMISSION";
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);
    }

    private void scanDevices(JSONArray args, CallbackContext callback){
        Context context = this.cordova.getActivity().getApplicationContext();
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        this.createConn(context);

        mDeviceList = mUsbManager.getDeviceList();

        if (mDeviceList.size() > 0) {
            mDeviceIterator = mDeviceList.values().iterator();

            String usbDevice = "";
            while (mDeviceIterator.hasNext()) {
                UsbDevice usbDevice1 = mDeviceIterator.next();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    JSONObject myDevice = new JSONObject();
                    try{
                        myDevice.put("DeviceID", usbDevice1.getDeviceId());
                        myDevice.put("DeviceName", usbDevice1.getDeviceId());
                        myDevice.put("Protocol", usbDevice1.getDeviceProtocol());
                        myDevice.put("ProductName", usbDevice1.getProductName());
                        myDevice.put("ManufacturerName", usbDevice1.getManufacturerName());
                        myDevice.put("DeviceClass", usbDevice1.getDeviceClass() + " - " + translateDeviceClass(usbDevice1.getDeviceClass()));
                        myDevice.put("DeviceSubClass", usbDevice1.getDeviceSubclass());
                        myDevice.put("VendorID", usbDevice1.getVendorId());
                        myDevice.put("ProductID", usbDevice1.getProductId());

                        callback.success(myDevice);
                    }catch (Exception ex){
                        callback.error(ex);
                    }





                    // usbDevice += "\n" +
                    //         "DeviceID: " + usbDevice1.getDeviceId() + "\n" +
                    //         "DeviceName: " + usbDevice1.getDeviceName() + "\n" +
                    //         "Protocol: " + usbDevice1.getDeviceProtocol() + "\n" +
                    //         "Product Name: " + usbDevice1.getProductName() + "\n" +
                    //         "Manufacturer Name: " + usbDevice1.getManufacturerName() + "\n" +
                    //         "DeviceClass: " + usbDevice1.getDeviceClass() + " - " + translateDeviceClass(usbDevice1.getDeviceClass()) + "\n" +
                    //         "DeviceSubClass: " + usbDevice1.getDeviceSubclass() + "\n" +
                    //         "VendorID: " + usbDevice1.getVendorId() + "\n" +
                    //         "ProductID: " + usbDevice1.getProductId() + "\n";

                }
                        
                int interfaceCount = usbDevice1.getInterfaceCount();
                
                mDevice = usbDevice1;
            }
            
            mUsbManager.requestPermission(mDevice, mPermissionIntent);
        } else {
            callback.error("Please attach printer via USB");
        }

    }

    private void print(JSONArray args, CallbackContext callback) {
        if(args != null){
            try{
                final String test = args.getJSONObject(0).getString("msg") + "\n\n\n\n\n\n";
                testBytes = test.getBytes();
            }catch (Exception ex){
                callback.error("JSON error : " + ex);
            }
        }else{
            callback.error("Please don't pass null value");
        }


        if (mInterface == null) {
            callback.error("INTERFACE IS NULL");            
        } else if (mConnection == null) {
            callback.error("CONNECTION IS NULL");    
        } else if (forceCLaim == null) {
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
