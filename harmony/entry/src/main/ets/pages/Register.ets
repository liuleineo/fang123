import router from '@ohos.router';
import { promptAction } from '@kit.ArkUI';
import { apiPost } from '../common/Api';

@Entry
@Component
struct Register {
  @State phone: string = '';
  @State password: string = '';
  @State nickname: string = '';
  @State loading: boolean = false;

  async doRegister(): Promise<void> {
    if (this.phone.length !== 11) {
      promptAction.showToast({ message: '请输入正确的11位手机号' });
      return;
    }
    if (this.password.length < 6) {
      promptAction.showToast({ message: '密码至少6位' });
      return;
    }
    this.loading = true;
    try {
      await apiPost('/api/auth/register', {
        phone: this.phone,
        password: this.password,
        nickname: this.nickname
      });
      promptAction.showToast({ message: '注册成功，请登录' });
      router.back();
    } catch (e) {
      const msg = (e instanceof Error) ? e.message : '注册失败';
      promptAction.showToast({ message: msg });
    } finally {
      this.loading = false;
    }
  }

  build() {
    Column() {
      Row() {
        Text('注册')
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .fontColor('#1D2129')
      }
      .width('100%')
      .height(56)
      .padding({ left: 16 })
      .alignItems(VerticalAlign.Center)

      Column() {
        TextInput({ text: this.phone, placeholder: '手机号' })
          .type(InputType.PhoneNumber)
          .maxLength(11)
          .height(52)
          .fontSize(15)
          .padding({ left: 12 })
          .borderRadius(10)
          .backgroundColor('#F5F6F8')
          .onChange((v: string) => { this.phone = v; })

        TextInput({ text: this.nickname, placeholder: '昵称（选填）' })
          .height(52)
          .fontSize(15)
          .padding({ left: 12 })
          .margin({ top: 14 })
          .borderRadius(10)
          .backgroundColor('#F5F6F8')
          .onChange((v: string) => { this.nickname = v; })

        TextInput({ text: this.password, placeholder: '密码（6-32位）' })
          .type(InputType.Password)
          .maxLength(32)
          .height(52)
          .fontSize(15)
          .padding({ left: 12 })
          .margin({ top: 14 })
          .borderRadius(10)
          .backgroundColor('#F5F6F8')
          .onChange((v: string) => { this.password = v; })

        Button('注 册', { type: ButtonType.Capsule })
          .width('100%')
          .height(52)
          .margin({ top: 30 })
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .enabled(!this.loading)
          .onClick(() => { this.doRegister(); })
      }
      .padding({ left: 24, right: 24 })
      .width('100%')
      .margin({ top: 30 })
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Color.White)
  }
}
