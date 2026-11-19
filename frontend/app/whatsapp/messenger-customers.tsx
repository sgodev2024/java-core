"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { AppIcon } from "../components/app-icon";

type CustomerInfo={id:string;pageId:string;phone:string;name:string;psid:string;createdAt:string;updatedAt:string};
const token=()=>window.localStorage.getItem("core-access-token")||window.sessionStorage.getItem("core-access-token")||"";
async function json<T>(response:Response):Promise<T>{const body=await response.json().catch(()=>({}));if(!response.ok)throw new Error(body.detail||body.message||"Yêu cầu không thành công");return body as T;}

export default function MessengerCustomers({apiUrl}:{apiUrl:string}){
  const [customers,setCustomers]=useState<CustomerInfo[]>([]),[query,setQuery]=useState(""),[error,setError]=useState(""),[loading,setLoading]=useState(true);
  const load=useCallback(async()=>{try{const items=await fetch(`${apiUrl}/api/v1/facebook/messenger/customers`,{headers:{Authorization:`Bearer ${token()}`}}).then(json<CustomerInfo[]>);setCustomers(items);setError("");}catch(reason){setError(reason instanceof Error?reason.message:"Không thể tải khách hàng Messenger");}finally{setLoading(false);}},[apiUrl]);
  useEffect(()=>{void load();const timer=window.setInterval(()=>void load(),5000);return()=>window.clearInterval(timer);},[load]);
  const filtered=useMemo(()=>{const value=query.trim().toLocaleLowerCase("vi");if(!value)return customers;return customers.filter(item=>`${item.name} ${item.phone} ${item.psid} ${item.pageId}`.toLocaleLowerCase("vi").includes(value));},[customers,query]);
  const today=new Date().toLocaleDateString("vi-VN");
  const savedToday=customers.filter(item=>new Date(item.createdAt).toLocaleDateString("vi-VN")===today).length;
  const pages=new Set(customers.map(item=>item.pageId)).size;

  return <div className="messenger-customers-page">
    <div className="page-heading"><div><span className="eyebrow">Customer data</span><h1>Khách hàng Messenger</h1><p>Danh sách số điện thoại tự động nhận diện từ nội dung khách hàng gửi trong cuộc trò chuyện.</p></div><button className="secondary-button" onClick={()=>void load()}><AppIcon name="refresh" size={15}/> Làm mới</button></div>
    <section className="customer-phone-stats"><article><span><AppIcon name="users"/></span><div><small>Tổng khách hàng</small><strong>{customers.length}</strong></div></article><article><span><AppIcon name="phone"/></span><div><small>SĐT lưu hôm nay</small><strong>{savedToday}</strong></div></article><article><span><AppIcon name="message-circle"/></span><div><small>Fanpage ghi nhận</small><strong>{pages}</strong></div></article></section>
    <section className="panel customer-phone-panel">
      <div className="customer-phone-toolbar"><div><h2>Số điện thoại đã lưu</h2><p>Dữ liệu được cập nhật tự động khi khách hàng gửi số di động Việt Nam hợp lệ.</p></div><label className="search-field"><AppIcon name="search" size={15}/><input value={query} onChange={event=>setQuery(event.target.value)} placeholder="Tìm tên, số điện thoại hoặc PSID..."/></label></div>
      {error&&<p className="auth-error" role="alert">{error}</p>}
      {loading?<div className="auth-loading customer-phone-loading"><span/></div>:filtered.length===0?<div className="customer-phone-empty"><AppIcon name="phone" size={30}/><strong>{query?"Không tìm thấy khách hàng":"Chưa có số điện thoại được lưu"}</strong><p>{query?"Thử tìm bằng tên, số điện thoại hoặc PSID khác.":"Khi khách hàng gửi SĐT trong Messenger, thông tin sẽ tự động xuất hiện tại đây."}</p></div>:<div className="customer-phone-table-wrap"><table className="customer-phone-table"><thead><tr><th>Khách hàng Facebook</th><th>Số điện thoại</th><th>PSID</th><th>Fanpage</th><th>Thời gian lưu</th><th>Trạng thái</th></tr></thead><tbody>{filtered.map(item=><tr key={item.id}><td><div className="customer-phone-name"><span>{item.name.split(" ").filter(Boolean).slice(-2).map(part=>part[0]).join("").toUpperCase()}</span><strong>{item.name}</strong></div></td><td><strong className="customer-phone-number">{item.phone}</strong></td><td><code>{item.psid}</code></td><td><code>{item.pageId}</code></td><td>{new Date(item.updatedAt).toLocaleString("vi-VN",{hour:"2-digit",minute:"2-digit",day:"2-digit",month:"2-digit",year:"numeric"})}</td><td><span className="customer-phone-status"><AppIcon name="check-circle" size={12}/> Đã lưu</span></td></tr>)}</tbody></table></div>}
    </section>
  </div>;
}
