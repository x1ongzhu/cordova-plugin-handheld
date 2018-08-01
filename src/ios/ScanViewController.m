//
//  ScanViewController.m
//  qrCodeScanner
//
//  Created by 熊竹 on 18/1/23.
//  Copyright © 2016年 熊竹. All rights reserved.
//

#import "ScanViewController.h"
#import <AVFoundation/AVFoundation.h>

/**
 *  屏幕 高 宽 边界
 */
#define SCREEN_HEIGHT [UIScreen mainScreen].bounds.size.height
#define SCREEN_WIDTH  [UIScreen mainScreen].bounds.size.width
#define SCREEN_BOUNDS  [UIScreen mainScreen].bounds

#define TOP (SCREEN_HEIGHT-220)/2
#define LEFT (SCREEN_WIDTH-220)/2

#define kScanRect CGRectMake(LEFT, TOP, 220, 220)

@interface ScanViewController ()<AVCaptureMetadataOutputObjectsDelegate>{
    int num;
    BOOL upOrdown;
    NSTimer * timer;
    CAShapeLayer *cropLayer;
}
@property (strong,nonatomic)AVCaptureDevice * device;
@property (strong,nonatomic)AVCaptureDeviceInput * input;
@property (strong,nonatomic)AVCaptureMetadataOutput * output;
@property (strong,nonatomic)AVCaptureSession * session;
@property (strong,nonatomic)AVCaptureVideoPreviewLayer * preview;

@property (nonatomic, strong) UIImageView * line;
@property (nonatomic, strong) UIButton * btnFlash;

@end

@implementation ScanViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    
    [self configView];
}

-(void)configView{
    [self.view setBackgroundColor:[UIColor blackColor]];
    UIImageView * imageView = [[UIImageView alloc]initWithFrame:kScanRect];
    NSString * bundlePath = [[ NSBundle mainBundle] pathForResource: @ "qrCodeScanner" ofType :@ "bundle"];
    NSBundle *resourceBundle = [NSBundle bundleWithPath:bundlePath];
    imageView.image = [UIImage imageNamed:@"pick_bg"
                                 inBundle:resourceBundle
            compatibleWithTraitCollection:nil];
    imageView.image = [imageView.image imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
    [_line setTintColor:[self getColor:@"#2face9"]];
    [self.view addSubview:imageView];
    
    upOrdown = NO;
    num =0;
    _line = [[UIImageView alloc] initWithFrame:CGRectMake(LEFT, TOP+10, 220, 2)];
    _line.image = [UIImage imageNamed:@"line.png"
                             inBundle:resourceBundle
        compatibleWithTraitCollection:nil];
    _line.image = [_line.image imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
    [_line setTintColor:[self getColor:@"#2face9"]];
    [self.view addSubview:_line];
    
    timer = [NSTimer scheduledTimerWithTimeInterval:.02 target:self selector:@selector(animation1) userInfo:nil repeats:YES];
    
}

-(void) toggleFlash:(UIButton*)button{
    if (self.btnFlash.isSelected) {
        if ([_device hasTorch]) {
            [_device lockForConfiguration:nil];
            [_device setTorchMode: AVCaptureTorchModeOff];
            [_device unlockForConfiguration];
            [_btnFlash setSelected:NO];
        }
    }else{
        NSError *error = nil;
        if ([_device hasTorch]) {
            BOOL locked = [_device lockForConfiguration:&error];
            if (locked) {
                _device.torchMode = AVCaptureTorchModeOn;
                [_device unlockForConfiguration];
                [_btnFlash setSelected:YES];
            }
        }
    }
}

-(void)viewWillAppear:(BOOL)animated{
    
    [self setCropRect:kScanRect];
    
    NSString * bundlePath = [[ NSBundle mainBundle] pathForResource: @ "qrCodeScanner" ofType :@ "bundle"];
    NSBundle *resourceBundle = [NSBundle bundleWithPath:bundlePath];
    _btnFlash = [[UIButton alloc] initWithFrame:CGRectMake(([UIScreen mainScreen].bounds.size.width - 72) / 2,
                                                           ([UIScreen mainScreen].bounds.size.height + 220) / 2 + 50,
                                                           72, 72)];
    [_btnFlash setImage:[UIImage imageNamed:@"ic_shortcut_flash_off"
                                   inBundle:resourceBundle
              compatibleWithTraitCollection:nil]
               forState:UIControlStateNormal];
    [_btnFlash setImage:[UIImage imageNamed:@"ic_shortcut_flash_on"
                                   inBundle:resourceBundle
              compatibleWithTraitCollection:nil]
               forState:UIControlStateSelected];
    
    [_btnFlash addTarget:self action:@selector(toggleFlash:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:_btnFlash];
    
    UIButton* btnBack = [[UIButton alloc] initWithFrame:CGRectMake(10, 30, 42, 42)];
    [btnBack setImage:[UIImage imageNamed:@"prev" inBundle:resourceBundle compatibleWithTraitCollection:nil] forState:UIControlStateNormal];
    [btnBack addTarget:self action:@selector(close:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:btnBack];
    
    [self performSelector:@selector(setupCamera) withObject:nil afterDelay:0];
    
}

-(void) close:(UIButton*)button{
    [self dismissViewControllerAnimated:YES completion:nil];
}

-(void)animation1
{
    if (upOrdown == NO) {
        num ++;
        _line.frame = CGRectMake(LEFT, TOP+10+2*num, 220, 2);
        if (2*num == 200) {
            upOrdown = YES;
        }
    }
    else {
        num --;
        _line.frame = CGRectMake(LEFT, TOP+10+2*num, 220, 2);
        if (num == 0) {
            upOrdown = NO;
        }
    }
    
}


- (void)setCropRect:(CGRect)cropRect{
    cropLayer = [[CAShapeLayer alloc] init];
    CGMutablePathRef path = CGPathCreateMutable();
    CGPathAddRect(path, nil, cropRect);
    CGPathAddRect(path, nil, self.view.bounds);
    
    [cropLayer setFillRule:kCAFillRuleEvenOdd];
    [cropLayer setPath:path];
    [cropLayer setFillColor:[UIColor blackColor].CGColor];
    [cropLayer setOpacity:0.6];
    
    
    [cropLayer setNeedsDisplay];
    
    [self.view.layer addSublayer:cropLayer];
}

- (void)setupCamera
{
    AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    if (device==nil) {
        UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"提示" message:@"设备没有摄像头" preferredStyle:UIAlertControllerStyleAlert];
        [alert addAction:[UIAlertAction actionWithTitle:@"确认" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
            
        }]];
        [self presentViewController:alert animated:YES completion:nil];
        return;
    }
    // Device
    _device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    
    // Input
    _input = [AVCaptureDeviceInput deviceInputWithDevice:self.device error:nil];
    
    // Output
    _output = [[AVCaptureMetadataOutput alloc]init];
    [_output setMetadataObjectsDelegate:self queue:dispatch_get_main_queue()];
    
    //设置扫描区域
    CGFloat top = TOP/SCREEN_HEIGHT;
    CGFloat left = LEFT/SCREEN_WIDTH;
    CGFloat width = 220/SCREEN_WIDTH;
    CGFloat height = 220/SCREEN_HEIGHT;
    ///top 与 left 互换  width 与 height 互换
    [_output setRectOfInterest:CGRectMake(top,left, height, width)];
    
    
    // Session
    _session = [[AVCaptureSession alloc]init];
    [_session setSessionPreset:AVCaptureSessionPresetHigh];
    if ([_session canAddInput:self.input])
    {
        [_session addInput:self.input];
    }
    
    if ([_session canAddOutput:self.output])
    {
        [_session addOutput:self.output];
    }
    
    // 条码类型 AVMetadataObjectTypeQRCode
    [_output setMetadataObjectTypes:[NSArray arrayWithObjects:AVMetadataObjectTypeQRCode, nil]];
    
    // Preview
    _preview =[AVCaptureVideoPreviewLayer layerWithSession:_session];
    _preview.videoGravity = AVLayerVideoGravityResizeAspectFill;
    _preview.frame =self.view.layer.bounds;
    [self.view.layer insertSublayer:_preview atIndex:0];
    
    // Start
    [_session startRunning];
}

#pragma mark AVCaptureMetadataOutputObjectsDelegate
- (void)captureOutput:(AVCaptureOutput *)captureOutput didOutputMetadataObjects:(NSArray *)metadataObjects fromConnection:(AVCaptureConnection *)connection
{
    NSString *stringValue;
    
    if ([metadataObjects count] >0)
    {
        //停止扫描
        [_session stopRunning];
        [timer setFireDate:[NSDate distantFuture]];
        
        AVMetadataMachineReadableCodeObject * metadataObject = [metadataObjects objectAtIndex:0];
        stringValue = metadataObject.stringValue;
        NSLog(@"扫描结果：%@",stringValue);
        
        //        NSArray *arry = metadataObject.corners;
        //        for (id temp in arry) {
        //            NSLog(@"%@",temp);
        //        }
        //
        //
        //        UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"扫描结果" message:stringValue preferredStyle:UIAlertControllerStyleAlert];
        //        [alert addAction:[UIAlertAction actionWithTitle:@"确认" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        //            if (_session != nil && timer != nil) {
        //                [_session startRunning];
        //                [timer setFireDate:[NSDate date]];
        //            }
        //
        //        }]];
        //        [self presentViewController:alert animated:YES completion:nil];
        if(self.delegate){
            [self.delegate onResult:stringValue];
        }
        [self dismissViewControllerAnimated:YES completion:nil];
        
    } else {
        NSLog(@"无扫描信息");
        return;
    }
    
}


- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (UIColor *)getColor:(NSString *)hexColor
{
    if ([hexColor hasPrefix:@"#"]) {
        hexColor = [hexColor substringFromIndex:1];
    }
    unsigned int red,green,blue;
    NSRange range;
    range.length = 2;
    range.location = 0;
    [[NSScanner scannerWithString:[hexColor substringWithRange:range]] scanHexInt:&red];
    range.location = 2;
    [[NSScanner scannerWithString:[hexColor substringWithRange:range]] scanHexInt:&green];
    range.location = 4;
    [[NSScanner scannerWithString:[hexColor substringWithRange:range]] scanHexInt:&blue];
    return [UIColor colorWithRed:(float)(red/255.0f) green:(float)(green / 255.0f) blue:(float)(blue / 255.0f) alpha:1.0f];
}

-(UIStatusBarStyle)preferredStatusBarStyle{
    return UIStatusBarStyleLightContent;
}

-(BOOL)prefersStatusBarHidden{
    return YES;
}

@end

