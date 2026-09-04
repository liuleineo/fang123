import{p as i}from"./index-FWcFWH4v.js";/**
 * @license lucide-vue-next v0.469.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const l=i("CameraIcon",[["path",{d:"M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z",key:"1tc9qg"}],["circle",{cx:"12",cy:"13",r:"3",key:"1vg3eu"}]]);/**
 * @license lucide-vue-next v0.469.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const u=i("PlusIcon",[["path",{d:"M5 12h14",key:"1ays0h"}],["path",{d:"M12 5v14",key:"s699le"}]]);function p(t){return new Promise((o,n)=>{if(!t){n(new Error("无内容可复制"));return}if(navigator.clipboard&&window.isSecureContext){navigator.clipboard.writeText(t).then(o).catch(()=>r(t,o,n));return}r(t,o,n)})}function r(t,o,n){try{const e=document.createElement("textarea");e.value=t,e.setAttribute("readonly",""),e.style.position="fixed",e.style.top="-9999px",e.style.opacity="0",document.body.appendChild(e);const a=document.createRange();a.selectNodeContents(e);const c=window.getSelection();c.removeAllRanges(),c.addRange(a),e.setSelectionRange(0,t.length);const s=document.execCommand("copy");document.body.removeChild(e),s?o():n(new Error("复制失败"))}catch(e){n(e)}}export{l as C,u as P,p as c};
