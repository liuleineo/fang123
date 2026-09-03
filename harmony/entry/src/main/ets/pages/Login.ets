import router from '@ohos.router';
import { promptAction } from '@kit.ArkUI';
import { apiPost } from '../common/Api';
import { AppContext } from '../common/AppContext';

@Entry
@Component
struct Login {
  @State phone: string = '';
  @State password: string = '';
  @State loading: boolean = false;

  async doLogin(): Promise<void> {
    if (this.phone.length < 11) {
      promptAction.showToast({ message: '请输入正确的手机号' });
      return;
    }
    if (this.password.length === 0) {
      promptAction.showToast({ message: '请输入密码' });
      return;
    }
    this.loading = true;
    try {
      const token = await apiPost<string>('/api/auth/login', {
        phone: this.phone,
        password: this.password
      });
      if (token) {
        AppContext.saveToken(token);
        promptAction.showToast({ message: '登录成功' });
        router.replaceUrl({ url: 'pages/Customers' });
      }
    } catch (e) {
      const msg = (e instanceof Error) ? e.message : '登录失败';
      promptAction.showToast({ message: msg });
    } finally {
      this.loading = false;
    }
  }

  goRegister(): void {
    router.pushUrl({ url: 'pages/Register' });
  }

  build() {
    Column() {
      // 标题区
      Column() {
        Text('F')
          .fontSize(40)
          .fontWeight(FontWeight.Bold)
          .fontColor(Color.White)
          .width(72)
          .height(72)
          .textAlign(TextAlign.Center)
          .borderRadius(20)
          .backgroundColor('#0052D9')
        Text('欢迎回来')
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .margin({ top: 20 })
        Text('登录您的杭房一二三账号')
          .fontSize(13)
          .fontColor('#999999')
          .margin({ top: 8 })
      }
      .margin({ top: 60, bottom: 40 })

      // 表单
      Column() {
        Row() {
          Text('+86')
            .fontSize(15)
            .fontColor('#333333')
          Blank()
        }
        .height(52)
        .padding({ left: 12 })
        .borderRadius(10)
        .backgroundColor('#F5F6F8')

        TextInput({ text: this.phone, placeholder: '请输入手机号' })
          .type(InputType.PhoneNumber)
          .maxLength(11)
          .height(52)
          .fontSize(15)
          .padding({ left: 12 })
          .borderRadius(10)
          .backgroundColor('#F5F6F8')
          .onChange((v: string) => { this.phone = v; })

        TextInput({ text: this.password, placeholder: '请输入密码' })
          .type(InputType.Password)
          .maxLength(32)
          .height(52)
          .fontSize(15)
          .padding({ left: 12 })
          .margin({ top: 14 })
          .borderRadius(10)
          .backgroundColor('#F5F6F8')
          .onChange((v: string) => { this.password = v; })

        Button('登 录', { type: ButtonType.Capsule })
          .width('100%')
          .height(52)
          .margin({ top: 30 })
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .enabled(!this.loading)
          .onClick(() => { this.doLogin(); })

        Text('还没有账号？立即注册')
          .fontSize(14)
          .fontColor('#0052D9')
          .margin({ top: 24 })
          .onClick(() => { this.goRegister(); })
      }
      .padding({ left: 24, right: 24 })
      .width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Color.White)
  }
}
