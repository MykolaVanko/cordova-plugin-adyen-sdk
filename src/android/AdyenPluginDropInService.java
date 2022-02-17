package com.adyensdk.plugin;

import com.adyen.checkout.dropin.service.CallResult;
import com.adyen.checkout.dropin.service.DropInService;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * The methods here expect native apps to invoke your remote backend (which in turn should invoke Adyen),
 * but we want to have our JS client invoke the backend, so we need to pass control back to JS.
 */
public class AdyenPluginDropInService extends DropInService {
  public static JSONObject lastPaymentResponse;
  public static CallbackContext callbackContext;
  private static AdyenPluginDropInService INSTANCE;

  // TODO not entirely sure this is a singleton, so using this to be safe
  public static AdyenPluginDropInService getInstance() {
    return INSTANCE;
  }

  public void onCreate() {
    super.onCreate();
    INSTANCE = this;
  }

  @Override
  public CallResult makeDetailsCall(JSONObject jsonObject) {
    // this is called after the "action" (for additional details) completes

    try {
      JSONObject actionComponentData = jsonObject;

      JSONObject result = new JSONObject();
      result.put("action", "onAdditionalDetails");
      result.put("data", actionComponentData);
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
      pluginResult.setKeepCallback(true);
      callbackContext.sendPluginResult(pluginResult);

    } catch (JSONException e) {
      callbackContext.error("Error in AdyenPluginDropInService.makeDetailsCall: " + e.getMessage());
    }

    return new CallResult(CallResult.ResultType.WAIT, "Handled in JS, will complete async");
  }

  @Override
  public CallResult makePaymentsCall(JSONObject jsonObject) {
    // this is called after the user picked one of the payment methods from the list
    try {
      lastPaymentResponse = jsonObject;

      JSONObject result = new JSONObject();
      result.put("action", "onSubmit");
      result.put("data", lastPaymentResponse);
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
      pluginResult.setKeepCallback(true);
      callbackContext.sendPluginResult(pluginResult);

    } catch (JSONException e) {
      callbackContext.error("Error in AdyenPluginDropInService.makePaymentsCall: " + e.getMessage());
    }

    return new CallResult(CallResult.ResultType.WAIT, "Handled in JS, will complete async");
  }

  void callResultFinished() {
    // Note that the content here is send as the RESULT_KEY in the intent, so we could use that in AdyenPlugin.java,
    // however, that would require AndroidManifest.xml need this to be added o the activity: android:launchMode="singleInstance"
    // because otherwise onNewIntent in AdyenPlugin.java won't fire. So doing it here is more robust.
    asyncCallback(new CallResult(CallResult.ResultType.FINISHED, lastPaymentResponse.toString()));
    if (lastPaymentResponse == null) {
      callbackContext.success();
    } else {
      try {
        JSONObject result = new JSONObject();
        result.put("data", lastPaymentResponse);
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));

      } catch (JSONException e) {
        callbackContext.error("Error in AdyenPluginDropInService.callResultFinished: " + e.getMessage());
      }
    }
  }

  void callResultAction(String action) {
    asyncCallback(new CallResult(CallResult.ResultType.ACTION, action));
  }
}
