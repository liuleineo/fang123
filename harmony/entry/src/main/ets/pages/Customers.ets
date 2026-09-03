import router from '@ohos.router';
import { promptAction } from '@kit.ArkUI';
import { pasteboard } from '@kit.BasicServicesKit';
import { apiGet, apiPost, apiPut, apiDelete } from '../common/Api';
import { AppContext } from '../common/AppContext';
import { Customer, PageData, FollowUp, ShareUser } from '../model/Models';

const INTENTIONS: string[] = ['高', '中', '低'];
const FOLLOW_METHODS: string[] = ['电话', '微信', '到访', '其他'];

interface SortOpt {
  label: string;
  value: number;
}

const SORT_OPTIONS: SortOpt[] = [
  { label: '默认', value: 0 },
  { label: '跟进新→旧', value: 1 },
  { label: '跟进旧→新', value: 2 }
];

/** 意向标签颜色 */
function intentionColor(v: string): string {
  if (v === '高') {
    return '#E5484D';
  }
  if (v === '中') {
    return '#ED7B2F';
  }
  return '#999999';
}

@Entry
@Component
struct Customers {
  @State list: Customer[] = [];
  @State total: number = 0;
  @State page: number = 1;
  @State size: number = 20;
  @State loading: boolean = true;
  @State refreshing: boolean = false;
  @State loadingMore: boolean = false;

  // 筛选
  @State keyword: string = '';
  @State intention: string = '';          // '' 不限 / 高/中/低
  @State sort: number = 0;                 // 0 默认 / 1 跟进新到旧 / 2 跟进旧到新

  // 弹层控制
  @State showAdd: boolean = false;
  @State showFollow: boolean = false;
  @State showShare: boolean = false;
  @State showAi: boolean = false;

  // 当前操作对象
  @State cur: Customer | null = null;
  @State isEdit: boolean = false;

  // 新增/编辑表单
  @State formName: string = '';
  @State formPhone: string = '';
  @State formIntention: string = '';
  @State formRemark: string = '';
  @State saving: boolean = false;

  // 跟进弹层
  @State followList: FollowUp[] = [];
  @State followContent: string = '';
  @State followMethod: string = '电话';
  @State followLoading: boolean = false;

  // 分享弹层
  @State shareUsers: ShareUser[] = [];
  @State shareKw: string = '';
  @State shareTarget: ShareUser | null = null;
  @State shareSaving: boolean = false;

  // AI 文案
  @State aiLoading: boolean = false;
  @State aiCopy: string = '';

  aboutToAppear(): void {
    this.reload();
  }

  private buildQuery(): string {
    let q: string = 'page=' + this.page + '&size=' + this.size;
    const kw = this.keyword.trim();
    if (kw !== '') {
      q += '&keyword=' + encodeURIComponent(kw);
    }
    if (this.intention !== '') {
      q += '&intention=' + encodeURIComponent(this.intention);
    }
    if (this.sort === 1) {
      q += '&sortField=lastFollowUpTime&sortOrder=desc';
    } else if (this.sort === 2) {
      q += '&sortField=lastFollowUpTime&sortOrder=asc';
    }
    return q;
  }

  async fetchPage(page: number): Promise<void> {
    this.page = page;
    const q: string = this.buildQuery();
    try {
      const res = await apiGet<PageData<Customer>>('/api/user/customers?' + q);
      const records: Customer[] = res.records === undefined ? [] : res.records;
      if (page === 1) {
        this.list = records;
      } else {
        this.list = this.list.concat(records);
      }
      this.total = res.total === undefined ? this.list.length : res.total;
    } catch (e) {
      const msg = (e instanceof Error) ? e.message : '加载失败';
      promptAction.showToast({ message: msg });
      if (this.list.length === 0) {
        // 未登录跳回登录
        AppContext.clearToken();
        router.replaceUrl({ url: 'pages/Login' });
      }
    } finally {
      this.loading = false;
      this.refreshing = false;
      this.loadingMore = false;
    }
  }

  reload(): void {
    this.loading = true;
    this.fetchPage(1);
  }

  onReachEndLoad(): void {
    if (this.loading || this.loadingMore || this.refreshing) {
      return;
    }
    if (this.list.length >= this.total) {
      return;
    }
    this.loadingMore = true;
    this.fetchPage(this.page + 1);
  }

  // ===== 新增/编辑 =====
  openAdd(): void {
    this.isEdit = false;
    this.formName = '';
    this.formPhone = '';
    this.formIntention = '';
    this.formRemark = '';
    this.showAdd = true;
  }

  openEdit(c: Customer): void {
    this.isEdit = true;
    this.cur = c;
    this.formName = c.name;
    this.formPhone = c.phone;
    this.formIntention = c.intention;
    this.formRemark = c.remark;
    this.showAdd = true;
  }

  async saveCustomer(): Promise<void> {
    const name = this.formName.trim();
    if (name === '') {
      promptAction.showToast({ message: '请填写客户姓名' });
      return;
    }
    this.saving = true;
    try {
      const body = {
        name: name,
        phone: this.formPhone.trim(),
        intention: this.formIntention,
        remark: this.formRemark.trim()
      };
      if (this.isEdit && this.cur !== null) {
        await apiPut('/api/user/customers/' + this.cur.id, body);
      } else {
        await apiPost('/api/user/customers', body);
      }
      promptAction.showToast({ message: '保存成功' });
      this.showAdd = false;
      this.reload();
    } catch (e) {
      const msg = (e instanceof Error) ? e.message : '保存失败';
      promptAction.showToast({ message: msg });
    } finally {
      this.saving = false;
    }
  }

  async deleteCustomer(): Promise<void> {
    if (this.cur === null) {
      return;
    }
    const cid: number = this.cur.id;
    this.showAdd = false;
    try {
      await apiDelete('/api/user/customers/' + cid);
      promptAction.showToast({ message: '已删除' });
      this.reload();
    } catch (e) {
      const msg = (e instanceof Error) ? e.message : '删除失败';
      promptAction.showToast({ message: msg });
    }
  }

  // ===== 跟进 =====
  async openFollow(c: Customer): Promise<void> {
    this.cur = c;
    this.followList = [];
    this.followContent = '';
    this.followLoading = true;
    this.showFollow = true;
    try {
      const list = await apiGet<FollowUp[]>('/api/user/customers/' + c.id + '/follow-ups');
      this.followList = list === null ? [] : list;
    } catch (e) {
      this.followList = [];
    } finally {
      this.followLoading = false;
    }
  }

  async addFollow(): Promise<void> {
    if (this.cur === null || this.followContent.trim() === '') {
      promptAction.showToast({ message: '请填写跟进内容' });
      return;
    }
    try {
      await apiPost('/api/user/customers/' + this.cur.id + '/follow-ups', {
        content: this.followContent.trim(),
        method: this.followMethod
      });
      promptAction.showToast({ message: '已添加' });
      this.followContent = '';
      const list = await apiGet<FollowUp[]>('/api/user/customers/' + this.cur.id + '/follow-ups');
      this.followList = list === null ? [] : list;
      this.reload();
    } catch (e) {
      const msg = (e instanceof Error) ? e.message : '添加失败';
      promptAction.showToast({ message: msg });
    }
  }

  // ===== 分享 =====
  async openShare(c: Customer): Promise<void> {
    this.cur = c;
    this.shareUsers = [];
    this.shareKw = '';
    this.shareTarget = null;
    this.showShare = true;
  }

  async searchShareUsers(): Promise<void> {
    const kw = this.shareKw.trim();
    if (kw === '') {
      this.shareUsers = [];
      return;
    }
    try {
      const list = await apiGet<ShareUser[]>('/api/user/customers/user-search?keyword=' + encodeURIComponent(kw));
      this.shareUsers = list === null ? [] : list;
    } catch (e) {
      this.shareUsers = [];
    }
  }

  async doShare(): Promise<void> {
    if (this.cur === null || this.shareTarget === null) {
      promptAction.showToast({ message: '请选择要分享的用户' });
      return;
    }
    this.shareSaving = true;
    try {
      await apiPost('/api/user/customers/' + this.cur.id + '/share', { userId: String(this.shareTarget.id) });
      promptAction.showToast({ message: '分享成功' });
      this.showShare = false;
      this.reload();
    } catch (e) {
      const msg = (e instanceof Error) ? e.message : '分享失败';
      promptAction.showToast({ message: msg });
    } finally {
      this.shareSaving = false;
    }
  }

  // ===== AI 文案 =====
  async openAi(c: Customer): Promise<void> {
    this.cur = c;
    this.aiCopy = '';
    this.aiLoading = true;
    this.showAi = true;
    try {
      const copy = await apiPost<string>('/api/user/customers/' + c.id + '/ai-suggest', {});
      this.aiCopy = copy === null ? '' : copy;
    } catch (e) {
      const msg = (e instanceof Error) ? e.message : 'AI分析失败';
      promptAction.showToast({ message: msg });
    } finally {
      this.aiLoading = false;
    }
  }

  copyAi(): void {
    const data = pasteboard.createData(pasteboard.MIMETYPE_TEXT_PLAIN, this.aiCopy);
    pasteboard.getSystemPasteboard().setData(data).then(() => {
      promptAction.showToast({ message: '已复制' });
    }).catch(() => {
      promptAction.showToast({ message: '复制失败' });
    });
  }

  // ===== 卡片操作菜单 =====
  showActions(c: Customer): void {
    this.cur = c;
    const buttons: promptAction.ActionMenuButtonOptions[] = [
      { text: '跟进记录', color: '#0052D9' },
      { text: '编辑客户', color: '#0052D9' },
      { text: '分享给同事', color: '#0052D9' },
      { text: 'AI沟通文案', color: '#ED7B2F' },
      { text: '删除', color: '#E5484D' }
    ];
    promptAction.showActionMenu({ title: c.name, buttons: buttons }).then((res) => {
      const idx = res.index;
      if (idx === 0) {
        this.openFollow(c);
      } else if (idx === 1) {
        this.openEdit(c);
      } else if (idx === 2) {
        this.openShare(c);
      } else if (idx === 3) {
        this.openAi(c);
      } else if (idx === 4) {
        this.confirmDelete(c);
      }
    }).catch(() => {
    });
  }

  confirmDelete(c: Customer): void {
    this.cur = c;
    promptAction.showDialog({
      title: '删除客户',
      message: '确定删除客户“' + c.name + '”吗？',
      buttons: [
        { text: '取消', color: '#999999' },
        { text: '删除', color: '#E5484D' }
      ]
    }).then((r) => {
      if (r.index === 1) {
        this.doDelete(c.id);
      }
    }).catch(() => {
    });
  }

  async doDelete(cid: number): Promise<void> {
    try {
      await apiDelete('/api/user/customers/' + cid);
      promptAction.showToast({ message: '已删除' });
      this.reload();
    } catch (e) {
      const msg = (e instanceof Error) ? e.message : '删除失败';
      promptAction.showToast({ message: msg });
    }
  }

  logout(): void {
    AppContext.clearToken();
    router.replaceUrl({ url: 'pages/Login' });
  }

  // ===== 视图 =====
  @Builder
  intentTag(v: string) {
    Text(v)
      .fontSize(10)
      .fontColor(Color.White)
      .backgroundColor(intentionColor(v))
      .padding({ left: 5, right: 5, top: 1, bottom: 1 })
      .borderRadius(4)
  }

  @Builder
  customerItem(c: Customer) {
    Row() {
      Column() {
        Text((c.name !== '' && c.name !== undefined) ? c.name.slice(0, 1) : '客')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor('#0052D9')
      }
      .width(44)
      .height(44)
      .justifyContent(FlexAlign.Center)
      .borderRadius(22)
      .backgroundColor('#EAF1FF')

      Column() {
        Row() {
          if (c.intention !== '' && c.intention !== null && c.intention !== undefined) {
            this.intentTag(c.intention)
          }
          Text((c.name === undefined) ? '' : c.name)
            .fontSize(15)
            .fontWeight(FontWeight.Medium)
            .fontColor('#1D2129')
            .margin({ left: 6 })
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .layoutWeight(1)
          if (c.phone !== '' && c.phone !== null && c.phone !== undefined) {
            Text(c.phone)
              .fontSize(12)
              .fontColor('#999999')
          }
        }
        .width('100%')

        if (c.remark !== '' && c.remark !== null && c.remark !== undefined) {
          Text('需求：' + c.remark)
            .fontSize(12)
            .fontColor('#666666')
            .width('100%')
            .margin({ top: 4 })
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }

        if (c.lastFollowUpTime !== '' && c.lastFollowUpTime !== null && c.lastFollowUpTime !== undefined) {
          Text('最后跟进 ' + this.fmtDate(c.lastFollowUpTime))
            .fontSize(11)
            .fontColor('#999999')
            .width('100%')
            .margin({ top: 2 })
        }

        if (c.shareDesc !== '' && c.shareDesc !== null && c.shareDesc !== undefined) {
          Text(c.shareDesc)
            .fontSize(11)
            .fontColor('#8B5CF6')
            .width('100%')
            .margin({ top: 2 })
        }
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
      .margin({ left: 12 })

      Text('›')
        .fontSize(20)
        .fontColor('#C0C4CC')
    }
    .width('100%')
    .padding(14)
    .borderRadius(12)
    .backgroundColor(Color.White)
    .onClick(() => {
      this.showActions(c);
    })
  }

  fmtDate(t: string): string {
    if (t === '' || t === null || t === undefined) {
      return '';
    }
    const d: string = t.substring(0, 10);
    return d;
  }

  fmtTime(t: string): string {
    if (t === '' || t === null || t === undefined) {
      return '';
    }
    return t.substring(0, 16).replace('T', ' ');
  }

  // ===== 新增/编辑 sheet =====
  @Builder
  addSheetBuilder() {
    Column() {
      Row() {
        Text(this.isEdit ? '编辑客户' : '添加客户')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .layoutWeight(1)
        Text('关闭')
          .fontSize(14)
          .fontColor('#999999')
          .onClick(() => { this.showAdd = false; })
      }
      .width('100%')
      .padding({ bottom: 14 })

      TextInput({ text: this.formName, placeholder: '客户姓名' })
        .height(44)
        .fontSize(14)
        .padding({ left: 10 })
        .borderRadius(8)
        .backgroundColor('#F5F6F8')
        .onChange((v: string) => { this.formName = v; })

      TextInput({ text: this.formPhone, placeholder: '手机号' })
        .type(InputType.PhoneNumber)
        .maxLength(11)
        .height(44)
        .fontSize(14)
        .padding({ left: 10 })
        .margin({ top: 10 })
        .borderRadius(8)
        .backgroundColor('#F5F6F8')
        .onChange((v: string) => { this.formPhone = v; })

      Row() {
        ForEach(['不限'].concat(INTENTIONS), (v: string) => {
          Text(v)
            .fontSize(13)
            .fontColor(this.formIntention === v || (v === '不限' && this.formIntention === '') ? Color.White : '#666666')
            .backgroundColor(this.formIntention === v || (v === '不限' && this.formIntention === '') ? '#0052D9' : '#F0F1F3')
            .padding({ left: 14, right: 14, top: 6, bottom: 6 })
            .borderRadius(14)
            .margin({ right: 8 })
            .onClick(() => {
              this.formIntention = (v === '不限') ? '' : v;
            })
        }, (v: string) => v)
      }
      .margin({ top: 12 })

      TextArea({ text: this.formRemark, placeholder: '备注/购房需求' })
        .height(80)
        .fontSize(14)
        .margin({ top: 10 })
        .borderRadius(8)
        .backgroundColor('#F5F6F8')
        .onChange((v: string) => { this.formRemark = v; })

      Row() {
        if (this.isEdit) {
          Text('删除')
            .fontSize(15)
            .fontColor('#E5484D')
            .height(44)
            .textAlign(TextAlign.Center)
            .layoutWeight(1)
            .onClick(() => { this.deleteCustomer(); })
        }
        Button('保 存')
          .type(ButtonType.Capsule)
          .fontSize(15)
          .height(44)
          .layoutWeight(1)
          .margin({ left: this.isEdit ? 12 : 0 })
          .enabled(!this.saving)
          .onClick(() => { this.saveCustomer(); })
      }
      .width('100%')
      .margin({ top: 16 })
    }
    .padding(20)
    .width('100%')
    .backgroundColor(Color.White)
    .borderRadius({ topLeft: 16, topRight: 16 })
  }

  // ===== 跟进 sheet =====
  @Builder
  followSheetBuilder() {
    Column() {
      Row() {
        Text('跟进记录 - ' + ((this.cur === null || this.cur.name === '') ? '' : this.cur.name))
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .layoutWeight(1)
        Text('关闭')
          .fontSize(14)
          .fontColor('#999999')
          .onClick(() => { this.showFollow = false; })
      }
      .width('100%')
      .padding({ bottom: 12 })

      // 添加区
      Column() {
        TextInput({ text: this.followContent, placeholder: '填写跟进内容' })
          .height(40)
          .fontSize(14)
          .padding({ left: 10 })
          .borderRadius(8)
          .backgroundColor('#F5F6F8')
          .onChange((v: string) => { this.followContent = v; })

        Row() {
          ForEach(FOLLOW_METHODS, (m: string) => {
            Text(m)
              .fontSize(12)
              .fontColor(this.followMethod === m ? Color.White : '#666666')
              .backgroundColor(this.followMethod === m ? '#0052D9' : '#F0F1F3')
              .padding({ left: 12, right: 12, top: 5, bottom: 5 })
              .borderRadius(12)
              .margin({ right: 8 })
              .onClick(() => { this.followMethod = m; })
          }, (m: string) => m)
          Button('添加')
            .type(ButtonType.Capsule)
            .fontSize(13)
            .height(32)
            .layoutWeight(1)
            .onClick(() => { this.addFollow(); })
        }
        .width('100%')
        .margin({ top: 10 })
      }
      .margin({ bottom: 12 })

      if (this.followLoading) {
        Blank()
        LoadingProgress()
          .width(32)
          .height(32)
          .color('#0052D9')
        Blank()
      } else if (this.followList.length === 0) {
        Text('暂无跟进记录')
          .fontSize(13)
          .fontColor('#999999')
          .margin({ top: 20 })
      } else {
        List({ space: 8 }) {
          ForEach(this.followList, (f: FollowUp) => {
            ListItem() {
              Column() {
                Row() {
                  if (f.method !== '' && f.method !== null) {
                    Text(f.method)
                      .fontSize(10)
                      .fontColor('#0052D9')
                      .backgroundColor('#EAF1FF')
                      .padding({ left: 5, right: 5, top: 1, bottom: 1 })
                      .borderRadius(4)
                  }
                  Text((f.userNickname === undefined || f.userNickname === '') ? '我' : f.userNickname)
                    .fontSize(12)
                    .fontColor('#333333')
                    .margin({ left: 6 })
                  Blank()
                  Text(this.fmtTime((f.followUpTime === null || f.followUpTime === undefined || f.followUpTime === '') ? f.createdAt : f.followUpTime))
                    .fontSize(11)
                    .fontColor('#999999')
                }
                .width('100%')

                if (f.content !== '' && f.content !== null && f.content !== undefined) {
                  Text(f.content)
                    .fontSize(13)
                    .fontColor('#333333')
                    .width('100%')
                    .margin({ top: 4 })
                }
              }
              .alignItems(HorizontalAlign.Start)
              .width('100%')
              .padding(12)
              .borderRadius(10)
              .backgroundColor('#F7F8FA')
            }
          }, (f: FollowUp) => String(f.id))
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off)
      }
    }
    .padding(20)
    .width('100%')
    .height('92%')
    .backgroundColor(Color.White)
    .borderRadius({ topLeft: 16, topRight: 16 })
  }

  // ===== 分享 sheet =====
  @Builder
  shareSheetBuilder() {
    Column() {
      Row() {
        Text('分享客户')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .layoutWeight(1)
        Text('关闭')
          .fontSize(14)
          .fontColor('#999999')
          .onClick(() => { this.showShare = false; })
      }
      .width('100%')
      .padding({ bottom: 12 })

      Text('分享“' + ((this.cur === null || this.cur.name === undefined) ? '' : this.cur.name) + '”给同事：')
        .fontSize(13)
        .fontColor('#666666')
        .width('100%')

      Row() {
        TextInput({ text: this.shareKw, placeholder: '输入昵称或手机号搜索' })
          .height(40)
          .fontSize(14)
          .padding({ left: 10 })
          .layoutWeight(1)
          .borderRadius(8)
          .backgroundColor('#F5F6F8')
          .onChange((v: string) => {
            this.shareKw = v;
            this.searchShareUsers();
          })
      }
      .width('100%')
      .margin({ top: 10 })

      if (this.shareUsers.length === 0) {
        Text('输入关键字搜索可分享的用户')
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 16 })
      } else {
        List({ space: 8 }) {
          ForEach(this.shareUsers, (u: ShareUser) => {
            ListItem() {
              Row() {
                Text((u.nickname === undefined || u.nickname === '') ? '未命名' : u.nickname)
                  .fontSize(15)
                  .fontColor(this.shareTarget !== null && this.shareTarget.id === u.id ? '#0052D9' : '#1D2129')
                  .layoutWeight(1)
                Text((u.phone === undefined || u.phone === '') ? '' : u.phone)
                  .fontSize(12)
                  .fontColor('#999999')
              }
              .width('100%')
              .padding(12)
              .borderRadius(10)
              .backgroundColor(this.shareTarget !== null && this.shareTarget.id === u.id ? '#EAF1FF' : '#F7F8FA')
              .onClick(() => {
                this.shareTarget = u;
              })
            }
          }, (u: ShareUser) => String(u.id))
        }
        .layoutWeight(1)
        .margin({ top: 12 })
        .scrollBar(BarState.Off)
      }

      Button('确认分享')
        .type(ButtonType.Capsule)
        .width('100%')
        .height(44)
        .margin({ top: 14 })
        .enabled(!this.shareSaving)
        .onClick(() => { this.doShare(); })
    }
    .padding(20)
    .width('100%')
    .height('75%')
    .backgroundColor(Color.White)
    .borderRadius({ topLeft: 16, topRight: 16 })
  }

  // ===== AI sheet =====
  @Builder
  aiSheetBuilder() {
    Column() {
      Row() {
        Text('AI 沟通文案')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .layoutWeight(1)
        Text('关闭')
          .fontSize(14)
          .fontColor('#999999')
          .onClick(() => { this.showAi = false; })
      }
      .width('100%')
      .padding({ bottom: 14 })

      if (this.aiLoading) {
        Column() {
          LoadingProgress()
            .width(36)
            .height(36)
            .color('#0052D9')
          Text('正在AI分析客户信息，请稍候...')
            .fontSize(13)
            .fontColor('#999999')
            .margin({ top: 12 })
        }
        .width('100%')
        .justifyContent(FlexAlign.Center)
      } else if (this.aiCopy !== '') {
        Column() {
          Scroll() {
            Text(this.aiCopy)
              .fontSize(14)
              .lineHeight(22)
              .fontColor('#333333')
          }
          .layoutWeight(1)
          .scrollBar(BarState.Off)

          Row() {
            Button('复制文案')
              .type(ButtonType.Capsule)
              .fontSize(14)
              .height(42)
              .layoutWeight(1)
              .onClick(() => { this.copyAi(); })
          }
          .width('100%')
          .margin({ top: 14 })
        }
        .width('100%')
        .height('80%')
      } else {
        Text('AI分析失败，请重试')
          .fontSize(13)
          .fontColor('#999999')
          .margin({ top: 20 })
      }
    }
    .padding(20)
    .width('100%')
    .height('70%')
    .backgroundColor(Color.White)
    .borderRadius({ topLeft: 16, topRight: 16 })
  }

  build() {
    Column() {
      // 顶部
      Row() {
        Text('我的客户')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor('#1D2129')
          .layoutWeight(1)
        Text('退出')
          .fontSize(13)
          .fontColor('#999999')
          .onClick(() => { this.logout(); })
      }
      .width('100%')
      .height(52)
      .padding({ left: 16, right: 16 })
      .backgroundColor(Color.White)

      // 筛选区
      Column() {
        Row() {
          TextInput({ text: this.keyword, placeholder: '搜索姓名/手机号' })
            .height(38)
            .fontSize(13)
            .padding({ left: 10 })
            .layoutWeight(1)
            .borderRadius(19)
            .backgroundColor('#F0F2F5')
            .onChange((v: string) => { this.keyword = v; })
            .onSubmit(() => { this.reload(); })
          Button('搜索')
            .type(ButtonType.Capsule)
            .fontSize(13)
            .height(36)
            .margin({ left: 8 })
            .onClick(() => { this.reload(); })
        }
        .width('100%')

        Row() {
          ForEach(['不限'].concat(INTENTIONS), (v: string) => {
            Text(v)
              .fontSize(12)
              .fontColor(this.intention === v || (v === '不限' && this.intention === '') ? Color.White : '#666666')
              .backgroundColor(this.intention === v || (v === '不限' && this.intention === '') ? '#0052D9' : '#F0F1F3')
              .padding({ left: 12, right: 12, top: 5, bottom: 5 })
              .borderRadius(13)
              .margin({ right: 6 })
              .onClick(() => {
                this.intention = (v === '不限') ? '' : v;
                this.reload();
              })
          }, (v: string) => v)
        }
        .width('100%')
        .margin({ top: 10 })

        Row() {
          Text('排序：')
            .fontSize(12)
            .fontColor('#999999')
          ForEach(SORT_OPTIONS, (o: SortOpt) => {
            Text(o.label)
              .fontSize(12)
              .fontColor(this.sort === o.value ? Color.White : '#666666')
              .backgroundColor(this.sort === o.value ? '#0052D9' : '#F0F1F3')
              .padding({ left: 10, right: 10, top: 4, bottom: 4 })
              .borderRadius(12)
              .margin({ right: 6 })
              .onClick(() => {
                this.sort = o.value;
                this.reload();
              })
          }, (o: SortOpt) => String(o.value))
        }
        .width('100%')
        .margin({ top: 10 })
      }
      .padding({ left: 16, right: 16, top: 10, bottom: 10 })
      .backgroundColor(Color.White)

      // 列表
      Stack({ alignContent: Alignment.BottomEnd }) {
        Refresh({ refreshing: $$this.refreshing }) {
          List({ space: 10 }) {
            ForEach(this.list, (c: Customer) => {
              ListItem() {
                this.customerItem(c)
              }
            }, (c: Customer) => String(c.id))

            if (this.list.length > 0) {
              ListItem() {
                Text(this.list.length >= this.total ? '共 ' + this.total + ' 位客户' : '上拉加载更多...')
                  .fontSize(12)
                  .fontColor('#999999')
                  .width('100%')
                  .textAlign(TextAlign.Center)
                  .padding({ top: 8, bottom: 8 })
              }
            }
          }
          .layoutWeight(1)
          .padding({ left: 14, right: 14, top: 12 })
          .onReachEnd(() => { this.onReachEndLoad(); })
        }
        .layoutWeight(1)
        .onRefreshing(() => { this.reload(); })
        .backgroundColor('#F7F8FA')

        Button()
          .type(ButtonType.Circle)
          .width(54)
          .height(54)
          .margin({ right: 18, bottom: 30 })
          .backgroundColor('#0052D9')
          .shadow({ radius: 12, color: '#330052D9', offsetY: 4 })
          .onClick(() => { this.openAdd(); })
          .stateStyles({
            pressed: {
              scale: { x: 0.9, y: 0.9 }
            }
          })
          .overlay(Text('+').fontSize(26).fontColor(Color.White).fontWeight(FontWeight.Medium))
      }
      .width('100%')
      .layoutWeight(1)
      .align(Alignment.BottomEnd)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F7F8FA')
    .bindSheet($$this.showAdd, this.addSheetBuilder(), { height: 500, dragBar: false })
    .bindSheet($$this.showFollow, this.followSheetBuilder(), { dragBar: false })
    .bindSheet($$this.showShare, this.shareSheetBuilder(), { dragBar: false })
    .bindSheet($$this.showAi, this.aiSheetBuilder(), { dragBar: false })
  }
}
