var exec = require("cordova/exec");

var Adyen = function () {
};

Adyen.prototype.presentDropIn = function (options, onSuccess, onFail) {
  console.log('PRESENT DROP IN CALLED', options);
  var opts = options || {};

  // we need this as a string, not as an object
  if (typeof opts.paymentMethodsResponse !== "string") {
    opts.paymentMethodsResponse = JSON.stringify(opts.paymentMethodsResponse);
  }

  var onSuccessInternal = function (res) {
    if (res && res.action === "onSubmit") {
      if (typeof opts.onSubmit === "function") {
        opts.onSubmit(res);
      } else {
        onFail(">>> Clientside configuration error: please implement onSubmit <<<");
      }
    } else if (res && res.action === "onAdditionalDetails") {
      if (typeof opts.onAdditionalDetails === "function") {
        opts.onAdditionalDetails(res);
      } else {
        onFail(">>> Clientside configuration error: please implement onAdditionalDetails <<<");
      }
    } else if (res && res.action === "onLastPaymentDetails") {
      if (typeof opts.onLastPaymentDetails === "function") {
        opts.onLastPaymentDetails(res);
      } else {
        onFail(">>> Clientside configuration error: please implement onAdditionalDetails <<<");
      }
    }else {
      onSuccess(res);
    }
  }

  exec(onSuccessInternal, onFail, "AdyenPlugin", "presentDropIn", [opts]);
};

Adyen.prototype.handleAction = function (options) {
  console.log(">> handle action, options: ", options);

  // we need this as a string, not as an object
  if (typeof options !== "string") {
    options = JSON.stringify(options);
  }

  exec(null, null, "AdyenPlugin", "handleAction", [options]);
};

Adyen.prototype.dismissDropIn = function () {
  exec(null, null, "AdyenPlugin", "dismissDropIn", []);
}


Adyen.prototype.getLastPaymentResponse = function () {
  exec(null, null, "AdyenPlugin", "getLastPaymentResponse", []);
}


module.exports = new Adyen();
