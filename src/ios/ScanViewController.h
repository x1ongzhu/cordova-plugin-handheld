//
//  ScanViewController.h
//  qrCodeScanner
//
//  Created by 熊竹 on 18/1/23.
//  Copyright © 2016年 熊竹. All rights reserved.
//

#import <UIKit/UIKit.h>

@protocol ScanDelegate <NSObject>
- (void)onResult:(NSString *)result;
@end

@interface ScanViewController : UIViewController

@property (nonatomic, weak) id <ScanDelegate> delegate;

@end

