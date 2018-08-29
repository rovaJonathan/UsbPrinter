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

    

}
