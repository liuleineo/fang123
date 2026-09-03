/**
 * 接口配置与请求封装（HarmonyOS ArkTS / API 12）
 *
 * 注意：
 * - BASE_URL 默认指向线上 https://hangfang123.com
 * - 本地联调请改为 http://<电脑局域网IP>:8090（并处理明文 HTTP 权限，见 README）
 */
import { http } from '@kit.NetworkKit';
import { AppContext } from './AppContext';

export const BASE_URL: string = 'https://hangfang123.com';

interface RawResult {
  code: number;
  msg: string;
  data: Object;
}

/**
 * 通用请求。
 * method: GET / POST / PUT / DELETE
 * path: 以 / 开头（不含 BASE_URL），如 /api/user/customers
 * body: 需要序列化的对象（GET 忽略）
 * auth: 是否携带 Bearer token
 */
export function apiRequest<T>(method: string, path: string, body?: Object, auth: boolean = true): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const httpReq = http.createHttp();
    const url: string = BASE_URL + path;
    const header: Record<string, string> = {
      'Content-Type': 'application/json'
    };
    if (auth) {
      const token = AppContext.getToken();
      if (token !== '') {
        header['Authorization'] = 'Bearer ' + token;
      }
    }

    let m: http.RequestMethod = http.RequestMethod.GET;
    if (method === 'POST') {
      m = http.RequestMethod.POST;
    } else if (method === 'PUT') {
      m = http.RequestMethod.PUT;
    } else if (method === 'DELETE') {
      m = http.RequestMethod.DELETE;
    }

    httpReq.request(url, {
      method: m,
      header: header,
      extraData: (method !== 'GET' && body !== undefined) ? JSON.stringify(body) : undefined,
      connectTimeout: 15000,
      readTimeout: 60000
    }).then((resp: http.HttpResponse) => {
      httpReq.destroy();
      const code = resp.responseCode;
      const text = resp.result as string;
      if (code !== 200) {
        reject(new Error('网络错误(' + code + ')'));
        return;
      }
      let json: RawResult | null = null;
      try {
        json = JSON.parse(text) as RawResult;
      } catch (e) {
        reject(new Error('响应解析失败'));
        return;
      }
      if (json !== null && json.code === 200) {
        resolve(json.data as T);
      } else if (json !== null) {
        if (json.code === 401) {
          AppContext.clearToken();
        }
        reject(new Error(json.msg !== '' ? json.msg : '请求失败'));
      } else {
        reject(new Error('请求失败'));
      }
    }).catch((err: Error) => {
      httpReq.destroy();
      reject(new Error(err.message !== '' ? err.message : '网络异常'));
    });
  });
}

/** GET */
export function apiGet<T>(path: string): Promise<T> {
  return apiRequest<T>('GET', path, undefined, true);
}

/** POST（JSON） */
export function apiPost<T>(path: string, body?: Object): Promise<T> {
  return apiRequest<T>('POST', path, body, true);
}

/** PUT（JSON） */
export function apiPut<T>(path: string, body?: Object): Promise<T> {
  return apiRequest<T>('PUT', path, body, true);
}

/** DELETE */
export function apiDelete<T>(path: string): Promise<T> {
  return apiRequest<T>('DELETE', path, undefined, true);
}

/** 格式化提示错误信息 */
export function errMsg(e: Error): string {
  return e.message;
}
