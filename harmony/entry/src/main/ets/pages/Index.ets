import router from '@ohos.router';
import { AppContext } from '../common/AppContext';

/**
 * 启动页：根据登录态分发
 */
@Entry
@Component
struct Index {
  aboutToAppear(): void {
    setTimeout(() => {
      const token: string = AppContext.getToken();
      if (token !== '') {
        router.replaceUrl({ url: 'pages/Customers' });
      } else {
        router.replaceUrl({ url: 'pages/Login' });
      }
    }, 200);
  }

  build() {
    Column() {
      Column() {
        Text('F')
          .fontSize(56)
          .fontWeight(FontWeight.Bold)
          .fontColor(Color.White)
          .width(88)
          .height(88)
          .textAlign(TextAlign.Center)
          .borderRadius(24)
          .backgroundColor('#0052D9')
        Text('杭房一二三')
          .fontSize(20)
          .fontWeight(FontWeight.Medium)
          .margin({ top: 16 })
        Text('让买房变简单')
          .fontSize(13)
          .fontColor('#999999')
          .margin({ top: 6 })
      }
      .justifyContent(FlexAlign.Center)
      .width('100%')
      .height('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F8FAFE')
  }
}
