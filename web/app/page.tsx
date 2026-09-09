"use client";
import {useEffect,useState} from "react";

type Entry={id:string;category:string;provider:string;product_model:string|null;amount_paid_usd:number;balance_added_usd:number;purchased_at:string;notes:string|null};
type ModelUse={model:string;requests:number;used_usd:number;avg_request_usd:number};
type Finance={api_purchased_usd:number;api_used_usd:number;api_remaining_usd:number;cash_invested_usd:number;operating_expenses_month_usd:number;api_used_last_7d_usd:number;daily_burn_usd:number;estimated_days_left:number|null;purchase_status:string;entries:Entry[];model_usage:ModelUse[]};
const U=process.env.NEXT_PUBLIC_SUPABASE_URL||"";
const K=process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY||"";

export default function Home(){
 const [email,setEmail]=useState(""),[password,setPassword]=useState(""),[token,setToken]=useState<string|null>(null);
 const [data,setData]=useState<Finance|null>(null),[error,setError]=useState(""),[loading,setLoading]=useState(false),[form,setForm]=useState(false);
 const [category,setCategory]=useState("api"),[provider,setProvider]=useState("OpenAI"),[model,setModel]=useState("gpt-5.6-luna");
 const [amount,setAmount]=useState(""),[balance,setBalance]=useState(""),[date,setDate]=useState(new Date().toISOString().slice(0,10)),[notes,setNotes]=useState("");
 useEffect(()=>{const t=localStorage.getItem("simple_admin_token");if(t)setToken(t)},[]);
 useEffect(()=>{if(token)load(token)},[token]);

 async function login(){
  setLoading(true);setError("");
  try{
   const r=await fetch(`${U}/auth/v1/token?grant_type=password`,{method:"POST",headers:{apikey:K,"Content-Type":"application/json"},body:JSON.stringify({email,password})});
   const d=await r.json(); if(!r.ok)throw new Error(d?.error_description||d?.msg||"No se pudo ingresar.");
   localStorage.setItem("simple_admin_token",d.access_token);setToken(d.access_token);
  }catch(e){setError(e instanceof Error?e.message:"Error");}finally{setLoading(false)}
 }
 async function load(t=token||""){
  if(!t)return; setLoading(true);setError("");
  try{
   const r=await fetch(`${U}/rest/v1/rpc/admin_finance_summary`,{method:"POST",headers:{apikey:K,Authorization:`Bearer ${t}`,"Content-Type":"application/json"},body:"{}"});
   const x=await r.text(); const d=x?JSON.parse(x):null; if(!r.ok)throw new Error(d?.message||"No se pudo cargar."); setData(d);
  }catch(e){setError(e instanceof Error?e.message:"Error");}finally{setLoading(false)}
 }
 async function save(){
  if(!token)return; const paid=Number(amount||0), added=category==="api"?Number(balance||0):0;
  if(!provider.trim()||paid<=0){setError("Completa proveedor y monto.");return}
  setLoading(true);setError("");
  try{
   const r=await fetch(`${U}/rest/v1/admin_cost_entries`,{method:"POST",headers:{apikey:K,Authorization:`Bearer ${token}`,"Content-Type":"application/json",Prefer:"return=minimal"},
    body:JSON.stringify({entry_type:category==="api"?"purchase":"expense",category,provider,product_model:model||null,amount_paid_usd:paid,balance_added_usd:added,purchased_at:date,notes:notes||null})});
   if(!r.ok)throw new Error("No se pudo guardar.");
   setAmount("");setBalance("");setNotes("");setForm(false);await load(token);
  }catch(e){setError(e instanceof Error?e.message:"Error");}finally{setLoading(false)}
 }
 async function del(id:string){
  if(!token||!confirm("¿Eliminar registro?"))return;
  await fetch(`${U}/rest/v1/admin_cost_entries?id=eq.${id}`,{method:"DELETE",headers:{apikey:K,Authorization:`Bearer ${token}`}});
  await load(token);
 }
 if(!token)return <main className="login"><section><h1>SIMPLE Control</h1><input placeholder="correo" value={email} onChange={e=>setEmail(e.target.value)}/><input placeholder="contraseña" type="password" value={password} onChange={e=>setPassword(e.target.value)}/><button onClick={login}>{loading?"Ingresando...":"Ingresar"}</button>{error&&<p className="err">{error}</p>}</section></main>;
 const status=label(data?.purchase_status||"");
 return <main className="app"><aside><h2>💡 SIMPLE</h2><button className="active">💰 Presupuesto</button><button onClick={()=>load()}>🔄 Actualizar</button><button onClick={()=>{localStorage.removeItem("simple_admin_token");setToken(null)}}>Salir</button></aside>
 <section className="content"><header><div><h1>Control financiero</h1><p>Compras manuales + consumo real automático.</p></div><button className="primary" onClick={()=>setForm(!form)}>＋ Registrar compra/gasto</button></header>
 {error&&<p className="err">{error}</p>}
 {data&&<><div className="cards">
  <Card t="Dinero invertido" v={money(data.cash_invested_usd)} s="Total registrado"/>
  <Card t="Saldo API comprado" v={money(data.api_purchased_usd)} s="Crédito agregado"/>
  <Card t="API consumida" v={money(data.api_used_usd)} s="Uso real SIMPLE"/>
  <Card t="Saldo API estimado" v={money(data.api_remaining_usd)} s="Comprado - consumido"/>
 </div>
 <div className="cards">
  <Card t="Gastos operativos mes" v={money(data.operating_expenses_month_usd)} s="Internet, hosting, software"/>
  <Card t="Consumo 7 días" v={money(data.api_used_last_7d_usd)} s="Ritmo reciente"/>
  <Card t="Consumo diario" v={money(data.daily_burn_usd)} s="Promedio 7 días"/>
  <Card t="Días de saldo" v={data.estimated_days_left==null?"—":`${data.estimated_days_left} días`} s="Con ritmo actual"/>
 </div>
 <div className={`status ${status.c}`}><h2>{status.t}</h2><p>{status.m}</p></div>
 {form&&<section className="panel"><h2>Registrar compra/gasto</h2><div className="grid">
  <label>Tipo<select value={category} onChange={e=>setCategory(e.target.value)}><option value="api">API</option><option value="internet">Internet</option><option value="hosting">Hosting</option><option value="software">Software</option><option value="other">Otro</option></select></label>
  <label>Proveedor<input value={provider} onChange={e=>setProvider(e.target.value)}/></label>
  <label>Modelo / producto<input value={model} onChange={e=>setModel(e.target.value)} placeholder="gpt-5.6-luna / otro"/></label>
  <label>Monto pagado USD<input type="number" value={amount} onChange={e=>setAmount(e.target.value)}/></label>
  {category==="api"&&<label>Saldo API agregado USD<input type="number" value={balance} onChange={e=>setBalance(e.target.value)}/></label>}
  <label>Fecha<input type="date" value={date} onChange={e=>setDate(e.target.value)}/></label>
  <label>Nota<input value={notes} onChange={e=>setNotes(e.target.value)}/></label>
 </div><button className="primary" onClick={save}>Guardar</button></section>}
 <section className="panel"><h2>Consumo por modelo</h2><div className="table"><table><thead><tr><th>Modelo</th><th>Solicitudes</th><th>Consumo</th><th>Promedio/consulta</th></tr></thead><tbody>{data.model_usage.map(m=><tr key={m.model}><td>{m.model}</td><td>{m.requests}</td><td>{money(m.used_usd)}</td><td>{money(m.avg_request_usd)}</td></tr>)}</tbody></table></div></section>
 <section className="panel"><h2>Compras y gastos registrados</h2><div className="table"><table><thead><tr><th>Fecha</th><th>Tipo</th><th>Proveedor</th><th>Modelo/producto</th><th>Pagado</th><th>Saldo agregado</th><th>Nota</th><th></th></tr></thead><tbody>{data.entries.map(e=><tr key={e.id}><td>{e.purchased_at}</td><td>{e.category}</td><td>{e.provider}</td><td>{e.product_model||"—"}</td><td>{money(e.amount_paid_usd)}</td><td>{e.category==="api"?money(e.balance_added_usd):"—"}</td><td>{e.notes||"—"}</td><td><button onClick={()=>del(e.id)}>Eliminar</button></td></tr>)}</tbody></table></div></section>
 </>}</section></main>
}
function Card({t,v,s}:{t:string,v:string,s:string}){return <article><span>{t}</span><strong>{v}</strong><small>{s}</small></article>}
function money(v:number){const n=Number(v||0);return n<.01?`$${n.toFixed(6)}`:`$${n.toFixed(2)}`}
function label(s:string){if(s==="COMPRAR_AHORA")return{t:"COMPRAR API AHORA",m:"El saldo registrado se agotó.",c:"bad"};if(s==="COMPRAR_PRONTO")return{t:"COMPRAR MÁS PRONTO",m:"Quedan menos de 7 días de saldo al ritmo actual.",c:"warn"};if(s==="SALDO_BAJO")return{t:"SALDO BAJO",m:"Queda menos del 20% del saldo comprado.",c:"warn"};if(s==="SIN_SALDO_REGISTRADO")return{t:"REGISTRA TU PRIMERA COMPRA",m:"Aún no sabemos cuánto saldo API tienes.",c:"neutral"};return{t:"SALDO SALUDABLE",m:"Por ahora no necesitas comprar más.",c:"good"}}
