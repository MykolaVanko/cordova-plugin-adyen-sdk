package com.adyensdk.plugin;

import com.adyen.checkout.components.ActionComponentData;
import com.adyen.checkout.components.PaymentComponentState;
import com.adyen.checkout.dropin.service.DropInService;
import com.getcapacitor.JSObject;

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

  public void onDestroy() {
    super.onDestroy();
    this.callResultFinished();
  }

  @Override
  public void onDetailsCallRequested(ActionComponentData actionComponentData, JSONObject actionComponentJson) {
    // this is called after the "action" (for additional details) completes

    JSObject result = new JSObject();
    result.put("action", "onAdditionalDetails");
    result.put("data", actionComponentJson);

    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));

  }

  @Override
  public void onPaymentsCallRequested(PaymentComponentState paymentComponentState, JSONObject paymentComponentJson) {
    // this is called after the user picked one of the payment methods from the list
    try {
      lastPaymentResponse = paymentComponentJson;
      JSObject result = new JSObject();
      JSObject data = new JSObject();

      data.put("paymentMethod", paymentComponentJson.getString("paymentMethod"));

      result.put("action", "onSubmit");
      result.put("data", data);

      callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));

    } catch (JSONException e) {
      callbackContext.error("Error in AdyenPluginDropInService.onPaymentsCallRequested: " + e.getMessage());
    }
  }

  void callResultFinished() {
    // Note that the content here is send as the RESULT_KEY in the intent, so we could use that in AdyenPlugin.java,
    // however, that would require AndroidManifest.xml need this to be added o the activity: android:launchMode="singleInstance"
    // because otherwise onNewIntent in AdyenPlugin.java won't fire. So doing it here is more robust.

    if (lastPaymentResponse == null) {
      callbackContext.success("closed");
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
    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, action));
  }
}
