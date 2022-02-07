 import Cordova
 import Adyen
 import PassKit
 import SwiftUI

  // see https:docs.adyen.com/checkout/ios/drop-in
 @objc(AdyenPlugin)
 class AdyenPlugin: CDVPlugin {
   var command: CDVInvokedUrlCommand!
   var dropInComponent: DropInComponent!
   var lastPaymentResponse: PaymentComponentData!

   @objc(presentDropIn:)
   func presentDropIn(command: CDVInvokedUrlCommand) {
     self.lastPaymentResponse = nil
     self.command = command
     let obj: NSDictionary = command.arguments[0] as! NSDictionary

     let env: String = obj["environment"] as! String? ?? "test"
     var environment = Environment.test;
     if (env == "live") {
       environment = Environment.live
     }

     let paymentMethodsResponse: String = obj["paymentMethodsResponse"] as! String
     let currencyCode: String = obj["currencyCode"] as! String
     let amount: Int = obj["amount"] as! Int
     let countryCode: String = "NL";
     let clientKey: String = obj["clientKey"] as! String

     self.commandDelegate.run(inBackground: {

       let apiContext = APIContext(environment: environment, clientKey: clientKey)
       let configuration = DropInComponent.Configuration(apiContext: apiContext)

       let paymentMethodsConfiguration: NSDictionary = obj["paymentMethodsConfiguration"] as! NSDictionary

       let card = paymentMethodsConfiguration["card"] as? NSDictionary


       if (card != nil) {
         configuration.card.showsHolderNameField = card!["holderNameRequired"] as? Bool ?? true
         configuration.card.showsStorePaymentMethodField = card!["showStorePaymentField"] as? Bool ?? true
         configuration.card.allowedCardTypes = [CardType.visa, CardType.masterCard]
       }

       configuration.payment = Payment(amount: Amount(value: amount, currencyCode: currencyCode), countryCode: countryCode)

       let paymentMethods = (try? JSONDecoder().decode(PaymentMethods.self, from: Data(paymentMethodsResponse.utf8) ))


       self.dropInComponent = DropInComponent(paymentMethods: paymentMethods!, configuration: configuration)
       self.dropInComponent.delegate = self

       DispatchQueue.main.async {
         self.viewController.present(self.dropInComponent.viewController, animated: true)
       }
     })
   }

 @objc(handleAction:)
   func handleAction(command: CDVInvokedUrlCommand) {
    //  note: not running in background, because fi. the "redirect" type needs to perform actions on the UI thread
     let actionStr: String = command.arguments[0] as! String
     let action = (try? JSONDecoder().decode(Action.self, from: Data(actionStr.utf8)))!
       self.dropInComponent.handle(action)
   }

 @objc(dismissDropIn:)
   func dismissDropIn(command: CDVInvokedUrlCommand) {
     self.viewController.dismiss(animated: true)
     if (self.lastPaymentResponse == nil) {
       self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_OK), callbackId:self.command.callbackId)
     } else {
       let jsonEncoder = JSONEncoder()
       let jsonData = try? jsonEncoder.encode(self.lastPaymentResponse.paymentMethod.encodable)
       let json = String(data: jsonData!, encoding: String.Encoding.utf8)
       self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_OK, messageAs: ["paymentMethod": json as Any]), callbackId:self.command.callbackId)
     }
   }

 }

 extension AdyenPlugin: DropInComponentDelegate {

   func didComplete(from component: DropInComponent) {
     DispatchQueue.main.async {
       self.viewController.dismiss(animated: true)
     }
   }

   func didSubmit(_ data: PaymentComponentData, for paymentMethod: PaymentMethod, from component: DropInComponent) {
     self.lastPaymentResponse = data
     let jsonEncoder = JSONEncoder()
     let jsonData = try? jsonEncoder.encode(self.lastPaymentResponse.paymentMethod.encodable)
     let json = String(data: jsonData!, encoding: String.Encoding.utf8)
     let result: [String: Any] = [
       "action":
     "onSubmit",
       "data":[
       "paymentMethod": json as Any,
       "storePaymentMethod": self.lastPaymentResponse.storePaymentMethod,
       "browserInfo": ["userAgent": self.lastPaymentResponse.browserInfo?.userAgent]
       ]
     ]
     let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
     pluginResult!.keepCallback = NSNumber(true)
     self.commandDelegate.send(pluginResult, callbackId:self.command.callbackId)
   }

   func didProvide(_ data: ActionComponentData, from component: DropInComponent) {
     DispatchQueue.main.async {
       self.viewController.dismiss(animated: true)
     }
     let result: [String: Any] = ["action": "onAdditionalDetails", "data": ["paymentData": data.paymentData!, "details": data.details.encodable]]
     let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
     pluginResult!.keepCallback = NSNumber(true)
     self.commandDelegate.send(pluginResult, callbackId:self.command.callbackId)
   }

   // also invoked when cancelled (the close icon was pressed)
   func didFail(with error: Error, from component: DropInComponent) {
     DispatchQueue.main.async {
       self.viewController.dismiss(animated: true)
     }
     let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "\(error)")
     self.commandDelegate.send(pluginResult, callbackId:command.callbackId)
   }

 }
