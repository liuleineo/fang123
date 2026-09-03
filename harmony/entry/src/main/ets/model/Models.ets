/**
 * 客户 CRM 相关数据模型（与后端实体字段对齐）
 */

/** 分页结果（后端 Page 结构） */
export interface PageData<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

/** 客户 */
export interface Customer {
  id: number;
  name: string;
  phone: string;
  remark: string;
  intention: string;   // 高/中/低
  photo: string;       // 头像
  lastFollowUpTime: string;
  lastFollowUpContent: string;
  shareDesc: string;   // 分享给了谁/被谁分享 描述
}

/** 跟进记录 */
export interface FollowUp {
  id: number;
  content: string;
  method: string;      // 电话/微信/到访/其他
  followUpTime: string;
  createdAt: string;
  userNickname: string;
  photos: string;
}

/** 可分享用户（user-search 返回） */
export interface ShareUser {
  id: number;
  nickname: string;
  phone: string;
}

/** 登录返回（/api/auth/login data = token 字符串，Register data = UserInfoVO） */
export interface UserInfoVO {
  id: number;
  phone: string;
  nickname: string;
}

/** 保存客户时提交的字段 */
export class CustomerDraft {
  name: string = '';
  phone: string = '';
  intention: string = '';
  remark: string = '';
  photo: string = '';
}
