package com.adyensdk.plugin;

import android.content.Intent;

import com.adyen.checkout.base.model.PaymentMethodsApiResponse;
import com.adyen.checkout.base.model.payments.Amount;
import com.adyen.checkout.card.CardConfiguration;
import com.adyen.checkout.core.api.Environment;
import com.adyen.checkout.dropin.DropIn;
import com.adyen.checkout.dropin.DropInConfiguration;
import com.adyen.checkout.googlepay.GooglePayConfiguration;
import com.adyen.checkout.sepa.SepaConfiguration;
import com.google.android.gms.wallet.WalletConstants;

import org.apache.cordova.*;
import org.json.JSONException;
import org.json.JSONObject;

public class AdyenPlugin extends CordovaPlugin {

  private static final String ACTION_PRESENT_DROP_IN = "presentDropIn";
  private static final String ACTION_HANDLE_ACTION = "handleAction";
  private static final String ACTION_DISMISS_DROP_IN = "dismissDropIn";

  private static final String INTENT_ACTION_PRESENTDROPIN = "INTENT_ACTION_PRESENTDROPIN";

  private CallbackContext callbackContext;

  @Override
  public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
    if (ACTION_PRESENT_DROP_IN.equals(action)) {
      presentDropIn(args, callbackContext);
      return true;
    } else if (ACTION_HANDLE_ACTION.equals(action)) {
      handleAction(args);
      return true;
    } else if (ACTION_DISMISS_DROP_IN.equals(action)) {
      dismissDropIn();
      return true;
    } else {
      return false;
    }
  }

  private void presentDropIn(CordovaArgs args, CallbackContext callbackContext) throws JSONException {
    cordova.getThreadPool().execute(() -> {
      try {
        this.callbackContext = callbackContext;
        AdyenPluginDropInService.callbackContext = callbackContext;
        AdyenPluginDropInService.lastPaymentResponse = null;
        JSONObject options = args.getJSONObject(0);
        String environment = options.optString("environment", "test");
        int amount = options.getInt("amount");
        String currencyCode = options.getString("currencyCode");
        String paymentMethodsResponse = options.getString("paymentMethodsResponse");
        JSONObject paymentMethodsConfiguration = options.getJSONObject("paymentMethodsConfiguration");

        PaymentMethodsApiResponse paymentMethodsApiResponse = PaymentMethodsApiResponse.SERIALIZER.deserialize(new JSONObject(paymentMethodsResponse));

        Intent intent = new Intent(cordova.getContext(), cordova.getActivity().getClass());
        intent.setAction(INTENT_ACTION_PRESENTDROPIN);

        DropInConfiguration.Builder dropInConfigurationBuilder = new DropInConfiguration.Builder(cordova.getContext(), intent, AdyenPluginDropInService.class);
        dropInConfigurationBuilder.addSepaConfiguration(new SepaConfiguration.Builder(cordova.getContext()).build());

        if (paymentMethodsConfiguration.has("card")) {
          JSONObject card = paymentMethodsConfiguration.getJSONObject("card");
          CardConfiguration cardConfiguration = new CardConfiguration.Builder(cordova.getContext(), card.getString("publicKey"))
              .setHolderNameRequire(card.getBoolean("holderNameRequired"))
              .setShowStorePaymentField(card.getBoolean("showStorePaymentField"))
              .build();
          dropInConfigurationBuilder.addCardConfiguration(cardConfiguration);
        }

        if (paymentMethodsConfiguration.has("paywithgoogle")) {
          JSONObject paywithgoogle = paymentMethodsConfiguration.getJSONObject("paywithgoogle");
          GooglePayConfiguration googlePayConfiguration = new GooglePayConfiguration.Builder(cordova.getContext(), paywithgoogle.getJSONObject("configuration").getString("gatewayMerchantId"))
              .setGooglePayEnvironment("live".equals(environment) ? WalletConstants.ENVIRONMENT_PRODUCTION : WalletConstants.ENVIRONMENT_TEST)
              .build();
          dropInConfigurationBuilder.addGooglePayConfiguration(googlePayConfiguration);
        }

        if(amount > 0) {
          Amount dropInAmount = new Amount();
          dropInAmount.setCurrency(currencyCode);
          dropInAmount.setValue(amount);
          dropInConfigurationBuilder.setAmount(dropInAmount);
        }

        dropInConfigurationBuilder.setEnvironment("live".equals(environment) ? Environment.EUROPE : Environment.TEST);
        DropInConfiguration dropInConfiguration = dropInConfigurationBuilder.build();

        DropIn.startPayment(cordova.getContext(), paymentMethodsApiResponse, dropInConfiguration);

      } catch (JSONException e) {
        callbackContext.error(e.getMessage());
      }
    });
  }

  private void handleAction(CordovaArgs args) {
    cordova.getThreadPool().execute(() -> {
      try {
        String action = args.getString(0);
        AdyenPluginDropInService.getInstance().callResultAction(action);
      } catch (JSONException e) {
        callbackContext.error(e.getMessage());
      }
    });
  }

  private void dismissDropIn() {
    AdyenPluginDropInService.getInstance().callResultFinished();
  }

  // Note this is only invoked if in AndroidManifest.xml this is added to the activity: android:launchMode="singleInstance".
  // If that's a problem, then ignore this intent and move the callback stuff to AdyenPluginDropInService.callResultFinished.
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    System.out.println("ignoring intent - already handled by our service class");
    /*
    if (INTENT_ACTION_PRESENTDROPIN.equals(intent.getAction())) {
      String paymentMethod = intent.getStringExtra(DropIn.RESULT_KEY);
      if (paymentMethod == null) {
        callbackContext.success();
      } else {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, new JSONObject(paymentMethod)));
        } catch (JSONException e) {
          callbackContext.error("Error in AdyenPlugin.onNewIntent: " + e.getMessage());
        }
      }
    }
    */
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
  }
}
