/********* qrCodeScanner.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#import "ScanViewController.h"

@interface Handheld : CDVPlugin<ScanDelegate> {
  // Member variables go here.
}
@property (nonatomic, strong) CDVInvokedUrlCommand* command;
- (void)coolMethod:(CDVInvokedUrlCommand*)command;
- (void)scanCode:(CDVInvokedUrlCommand*)command;
@end

@implementation Handheld

- (void)scanCode:(CDVInvokedUrlCommand*)command{
    ScanViewController* scanVC = [[ScanViewController alloc] init];
    scanVC.delegate = self;
    self.command = command;
    [self.viewController presentViewController:scanVC animated:YES completion:nil];
}

- (void)coolMethod:(CDVInvokedUrlCommand*)command
{
    
    CDVPluginResult* pluginResult = nil;
    NSString* echo = [command.arguments objectAtIndex:0];

    if (echo != nil && [echo length] > 0) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:echo];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)onResult:(NSString *)result {
    NSLog(@"scan result:%@", result);
    if (self.command) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:result];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
    }
}

@end
