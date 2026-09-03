import { common } from '@kit.AbilityKit';
import { Preferences } from '@kit.ArkData';

const TOKEN_KEY = 'fang123_token';
const PREFS_NAME = 'fang123_store';

/**
 * 全局 Application/UIAbility Context 持有者。
 * 在 EntryAbility.onCreate 中调用 AppContext.init(this.context) 完成初始化。
 */
export class AppContext {
  private static ctx: common.UIAbilityContext | null = null;

  static init(context: common.UIAbilityContext): void {
    AppContext.ctx = context;
  }

  static get(): common.UIAbilityContext {
    if (AppContext.ctx === null) {
      throw new Error('AppContext 未初始化');
    }
    return AppContext.ctx;
  }

  private static prefs(): Preferences {
    return Preferences.getPreferencesSync(AppContext.get(), { name: PREFS_NAME });
  }

  static saveToken(token: string): void {
    const p = AppContext.prefs();
    p.putSync(TOKEN_KEY, token);
    p.flush();
  }

  static getToken(): string {
    try {
      const p = AppContext.prefs();
      const v = p.getSync(TOKEN_KEY, '') as string;
      return v === null ? '' : v;
    } catch (e) {
      return '';
    }
  }

  static clearToken(): void {
    try {
      const p = AppContext.prefs();
      p.deleteSync(TOKEN_KEY);
      p.flush();
    } catch (e) {
      // ignore
    }
  }
}
