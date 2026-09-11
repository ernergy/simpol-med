"use client";

import { useEffect, useMemo, useState } from "react";

type UsageRow = {
  id: string;
  user_id: string | null;
  model: string;
  action: string;
  input_tokens: number;
  cached_input_tokens: number;
  output_tokens: number;
  total_tokens: number;
  estimated_cost_usd: number;
  request_ms: number | null;
  book_name: string | null;
  created_at: string;
};

type UsageSummary = {
  total_requests: number;
  total_input_tokens: number;
  total_cached_input_tokens: number;
  total_output_tokens: number;
  total_tokens: number;
  total_cost_usd: number;
  today_cost_usd: number;
  last_7_days_cost_usd: number;
  avg_cost_per_request: number;
  avg_request_ms: number;
  recent: UsageRow[];
};

type Entry = {
  id: string;
  category: string;
  provider: string;
  product_model: string | null;
  amount_paid_usd: number;
  balance_added_usd: number;
  purchased_at: string;
  notes: string | null;
};

type ModelUse = {
  model: string;
  requests: number;
  used_usd: number;
  avg_request_usd: number;
};

type Finance = {
  api_purchased_usd: number;
  api_used_usd: number;
  api_remaining_usd: number;
  cash_invested_usd: number;
  operating_expenses_month_usd: number;
  api_used_last_7d_usd: number;
  daily_burn_usd: number;
  estimated_days_left: number | null;
  purchase_status: string;
  entries: Entry[];
  model_usage: ModelUse[];
};

type Tab = "costos" | "presupuesto" | "usuarios" | "creditos" | "resumenes" | "configuracion";

const U = process.env.NEXT_PUBLIC_SUPABASE_URL || "";
const K = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY || "";

export default function Home() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState<string | null>(null);
  const [tab, setTab] = useState<Tab>("costos");

  const [usage, setUsage] = useState<UsageSummary | null>(null);
  const [finance, setFinance] = useState<Finance | null>(null);

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState(false);

  const [category, setCategory] = useState("api");
  const [provider, setProvider] = useState("OpenAI");
  const [model, setModel] = useState("gpt-5.6-luna");
  const [amount, setAmount] = useState("");
  const [balance, setBalance] = useState("");
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [notes, setNotes] = useState("");

  useEffect(() => {
    const saved = localStorage.getItem("simple_admin_token");
    if (saved) setToken(saved);
  }, []);

  useEffect(() => {
    if (token) refreshAll(token);
  }, [token]);

  const projected1000 = useMemo(
    () => Number(usage?.avg_cost_per_request || 0) * 1000,
    [usage]
  );

  async function login() {
    if (!U || !K) {
      setError("Falta configurar Supabase en Vercel.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const r = await fetch(`${U}/auth/v1/token?grant_type=password`, {
        method: "POST",
        headers: { apikey: K, "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      const d = await r.json();

      if (!r.ok) {
        throw new Error(d?.error_description || d?.msg || "No se pudo ingresar.");
      }

      localStorage.setItem("simple_admin_token", d.access_token);
      setToken(d.access_token);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Error de ingreso.");
    } finally {
      setLoading(false);
    }
  }

  async function callRpc(t: string, rpc: string) {
    const r = await fetch(`${U}/rest/v1/rpc/${rpc}`, {
      method: "POST",
      headers: {
        apikey: K,
        Authorization: `Bearer ${t}`,
        "Content-Type": "application/json",
      },
      body: "{}",
    });

    const text = await r.text();
    const data = text ? JSON.parse(text) : null;

    if (!r.ok) {
      if (r.status === 401) {
        localStorage.removeItem("simple_admin_token");
        setToken(null);
      }
      throw new Error(data?.message || `No se pudo cargar ${rpc}.`);
    }

    return data;
  }

  async function refreshAll(t = token || "") {
    if (!t) return;
    setLoading(true);
    setError("");

    try {
      const [usageData, financeData] = await Promise.all([
        callRpc(t, "get_ai_usage_summary"),
        callRpc(t, "admin_finance_summary"),
      ]);
      setUsage(usageData as UsageSummary);
      setFinance(financeData as Finance);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Error al cargar los datos.");
    } finally {
      setLoading(false);
    }
  }

  async function saveFinance() {
    if (!token) return;

    const paid = Number(amount || 0);
    const added = category === "api" ? Number(balance || 0) : 0;

    if (!provider.trim() || paid <= 0) {
      setError("Completa proveedor y monto.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const r = await fetch(`${U}/rest/v1/admin_cost_entries`, {
        method: "POST",
        headers: {
          apikey: K,
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
          Prefer: "return=minimal",
        },
        body: JSON.stringify({
          entry_type: category === "api" ? "purchase" : "expense",
          category,
          provider,
          product_model: model || null,
          amount_paid_usd: paid,
          balance_added_usd: added,
          purchased_at: date,
          notes: notes || null,
        }),
      });

      if (!r.ok) throw new Error("No se pudo guardar.");

      setAmount("");
      setBalance("");
      setNotes("");
      setForm(false);
      await refreshAll(token);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Error al guardar.");
    } finally {
      setLoading(false);
    }
  }

  async function deleteFinance(id: string) {
    if (!token || !confirm("¿Eliminar registro?")) return;

    const r = await fetch(`${U}/rest/v1/admin_cost_entries?id=eq.${id}`, {
      method: "DELETE",
      headers: { apikey: K, Authorization: `Bearer ${token}` },
    });

    if (!r.ok) {
      setError("No se pudo eliminar el registro.");
      return;
    }

    await refreshAll(token);
  }

  function logout() {
    localStorage.removeItem("simple_admin_token");
    setToken(null);
    setUsage(null);
    setFinance(null);
    setEmail("");
    setPassword("");
  }

  if (!token) {
    return (
      <main className="loginShell">
        <section className="loginCard">
          <div className="logo">💡</div>
          <h1>SIMPLE Control</h1>
          <p>Panel administrativo y control de costos.</p>

          <label>Correo administrador</label>
          <input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            type="email"
            placeholder="correo@ejemplo.com"
          />

          <label>Contraseña</label>
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
          />

          <button onClick={login} disabled={loading || !email || !password}>
            {loading ? "INGRESANDO..." : "INGRESAR"}
          </button>

          {error && <div className="error">{error}</div>}
        </section>
      </main>
    );
  }

  return (
    <main className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="logo small">💡</div>
          <div>
            <strong>SIMPLE</strong>
            <span>Control</span>
          </div>
        </div>

        <nav>
          <NavButton active={tab === "costos"} onClick={() => setTab("costos")}>
            📊 Costos IA
          </NavButton>
          <NavButton active={tab === "presupuesto"} onClick={() => setTab("presupuesto")}>
            💰 Presupuesto
          </NavButton>
          <NavButton active={tab === "usuarios"} onClick={() => setTab("usuarios")}>
            👥 Usuarios
          </NavButton>
          <NavButton active={tab === "creditos"} onClick={() => setTab("creditos")}>
            🪙 Créditos
          </NavButton>
          <NavButton active={tab === "resumenes"} onClick={() => setTab("resumenes")}>
            📄 Resúmenes
          </NavButton>
          <NavButton active={tab === "configuracion"} onClick={() => setTab("configuracion")}>
            ⚙️ Configuración
          </NavButton>
        </nav>

        <button className="refreshSide" onClick={() => refreshAll()}>
          {loading ? "⏳ Actualizando..." : "🔄 Actualizar"}
        </button>

        <button className="logout" onClick={logout}>
          Cerrar sesión
        </button>
      </aside>

      <section className="content">
        {error && <div className="error top">{error}</div>}

        {tab === "costos" && (
          <CostosPanel
            usage={usage}
            loading={loading}
            projected1000={projected1000}
            onRefresh={() => refreshAll()}
          />
        )}

        {tab === "presupuesto" && (
          <PresupuestoPanel
            data={finance}
            form={form}
            setForm={setForm}
            category={category}
            setCategory={setCategory}
            provider={provider}
            setProvider={setProvider}
            model={model}
            setModel={setModel}
            amount={amount}
            setAmount={setAmount}
            balance={balance}
            setBalance={setBalance}
            date={date}
            setDate={setDate}
            notes={notes}
            setNotes={setNotes}
            save={saveFinance}
            del={deleteFinance}
          />
        )}

        {tab === "usuarios" && (
          <ComingSoon
            title="Usuarios"
            text="Aquí mantendremos el control de usuarios de SIMPLE. Esta sección vuelve al menú sin alterar el control financiero."
          />
        )}

        {tab === "creditos" && (
          <ComingSoon
            title="Créditos"
            text="Sección reservada para administración de créditos de usuarios y futuras recargas."
          />
        )}

        {tab === "resumenes" && (
          <ComingSoon
            title="Resúmenes"
            text="Sección reservada para estadísticas y control de resúmenes generados."
          />
        )}

        {tab === "configuracion" && (
          <ComingSoon
            title="Configuración"
            text="Ajustes administrativos de SIMPLE. No se han modificado tus claves ni variables de Vercel."
          />
        )}
      </section>
    </main>
  );
}

function NavButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button className={active ? "active" : ""} onClick={onClick}>
      {children}
    </button>
  );
}

function CostosPanel({
  usage,
  loading,
  projected1000,
  onRefresh,
}: {
  usage: UsageSummary | null;
  loading: boolean;
  projected1000: number;
  onRefresh: () => void;
}) {
  return (
    <>
      <header>
        <div>
          <h1>Control de costos IA</h1>
          <p>Detalle real de consumo registrado por SIMPLE.</p>
        </div>
        <button className="refresh" onClick={onRefresh}>
          {loading ? "Actualizando..." : "Actualizar"}
        </button>
      </header>

      {usage ? (
        <>
          <div className="metrics">
            <Metric title="Costo acumulado" value={moneyPrecise(usage.total_cost_usd)} note="Desde que activamos medición" />
            <Metric title="Costo hoy" value={moneyPrecise(usage.today_cost_usd)} note="Consumo del día" />
            <Metric title="Solicitudes" value={format(usage.total_requests)} note="Consultas reales a OpenAI" />
            <Metric title="Costo promedio" value={moneyPrecise(usage.avg_cost_per_request)} note="Por solicitud" />
          </div>

          <div className="metrics secondary">
            <Metric title="Tokens entrada" value={format(usage.total_input_tokens)} note={`Caché: ${format(usage.total_cached_input_tokens)}`} />
            <Metric title="Tokens salida" value={format(usage.total_output_tokens)} note="Texto generado" />
            <Metric title="Últimos 7 días" value={moneyPrecise(usage.last_7_days_cost_usd)} note="Costo semanal" />
            <Metric title="Proyección 1.000 consultas" value={moneyPrecise(projected1000)} note="Con el promedio actual" />
          </div>

          <section className="panel">
            <h2>Referencia para definir nuestros créditos</h2>
            <p>Estas cifras usan el costo real promedio observado. Todavía no son el precio final al cliente.</p>
            <div className="pricingGrid">
              <PriceBox multiplier={3} avg={usage.avg_cost_per_request} label="Costo x3" />
              <PriceBox multiplier={5} avg={usage.avg_cost_per_request} label="Costo x5" />
              <PriceBox multiplier={10} avg={usage.avg_cost_per_request} label="Costo x10" />
            </div>
            <p className="hint">
              Luego podemos incorporar infraestructura, procesamiento de archivos, comisiones de pago y margen.
            </p>
          </section>

          <section className="panel">
            <h2>Últimas consultas reales</h2>
            {usage.recent?.length ? (
              <div className="tableWrap">
                <table>
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Modelo</th>
                      <th>Material</th>
                      <th>Entrada</th>
                      <th>Salida</th>
                      <th>Total</th>
                      <th>Costo</th>
                      <th>Tiempo</th>
                    </tr>
                  </thead>
                  <tbody>
                    {usage.recent.map((row) => (
                      <tr key={row.id}>
                        <td>{new Date(row.created_at).toLocaleString("es-BO")}</td>
                        <td>{row.model}</td>
                        <td>{row.book_name || "—"}</td>
                        <td>{format(row.input_tokens)}</td>
                        <td>{format(row.output_tokens)}</td>
                        <td>{format(row.total_tokens)}</td>
                        <td>{moneyPrecise(row.estimated_cost_usd)}</td>
                        <td>{row.request_ms ? `${(row.request_ms / 1000).toFixed(1)} s` : "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="empty">Aún no hay consultas registradas.</div>
            )}
          </section>
        </>
      ) : (
        <div className="empty">{loading ? "Cargando datos reales..." : "Sin datos de consumo."}</div>
      )}
    </>
  );
}

function PresupuestoPanel(props: {
  data: Finance | null;
  form: boolean;
  setForm: (v: boolean) => void;
  category: string;
  setCategory: (v: string) => void;
  provider: string;
  setProvider: (v: string) => void;
  model: string;
  setModel: (v: string) => void;
  amount: string;
  setAmount: (v: string) => void;
  balance: string;
  setBalance: (v: string) => void;
  date: string;
  setDate: (v: string) => void;
  notes: string;
  setNotes: (v: string) => void;
  save: () => void;
  del: (id: string) => void;
}) {
  const {
    data, form, setForm, category, setCategory, provider, setProvider, model,
    setModel, amount, setAmount, balance, setBalance, date, setDate, notes,
    setNotes, save, del
  } = props;

  const status = statusLabel(data?.purchase_status || "");

  return (
    <>
      <header>
        <div>
          <h1>Control financiero</h1>
          <p>Compras manuales + consumo real automático.</p>
        </div>
        <button className="primary" onClick={() => setForm(!form)}>
          ＋ Registrar compra/gasto
        </button>
      </header>

      {data ? (
        <>
          <div className="cards">
            <Card t="Dinero invertido" v={money(data.cash_invested_usd)} s="Total registrado" />
            <Card t="Saldo API comprado" v={money(data.api_purchased_usd)} s="Crédito agregado" />
            <Card t="API consumida" v={money(data.api_used_usd)} s="Uso real SIMPLE" />
            <Card t="Saldo API estimado" v={money(data.api_remaining_usd)} s="Comprado - consumido" />
          </div>

          <div className="cards">
            <Card t="Gastos operativos mes" v={money(data.operating_expenses_month_usd)} s="Internet, hosting, software" />
            <Card t="Consumo 7 días" v={money(data.api_used_last_7d_usd)} s="Ritmo reciente" />
            <Card t="Consumo diario" v={money(data.daily_burn_usd)} s="Promedio 7 días" />
            <Card t="Días de saldo" v={data.estimated_days_left == null ? "—" : `${data.estimated_days_left} días`} s="Con ritmo actual" />
          </div>

          <div className={`status ${status.c}`}>
            <h2>{status.t}</h2>
            <p>{status.m}</p>
          </div>

          {form && (
            <section className="panel">
              <h2>Registrar compra/gasto</h2>
              <div className="grid">
                <label>
                  Tipo
                  <select value={category} onChange={(e) => setCategory(e.target.value)}>
                    <option value="api">API</option>
                    <option value="internet">Internet</option>
                    <option value="hosting">Hosting</option>
                    <option value="software">Software</option>
                    <option value="other">Otro</option>
                  </select>
                </label>

                <label>
                  Proveedor
                  <input value={provider} onChange={(e) => setProvider(e.target.value)} />
                </label>

                <label>
                  Modelo / producto
                  <input value={model} onChange={(e) => setModel(e.target.value)} placeholder="gpt-5.6-luna / otro" />
                </label>

                <label>
                  Monto pagado USD
                  <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} />
                </label>

                {category === "api" && (
                  <label>
                    Saldo API agregado USD
                    <input type="number" value={balance} onChange={(e) => setBalance(e.target.value)} />
                  </label>
                )}

                <label>
                  Fecha
                  <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
                </label>

                <label>
                  Nota
                  <input value={notes} onChange={(e) => setNotes(e.target.value)} />
                </label>
              </div>

              <button className="primary" onClick={save}>Guardar</button>
            </section>
          )}

          <section className="panel">
            <h2>Consumo por modelo</h2>
            <div className="tableWrap">
              <table>
                <thead>
                  <tr>
                    <th>Modelo</th>
                    <th>Solicitudes</th>
                    <th>Consumo</th>
                    <th>Promedio/consulta</th>
                  </tr>
                </thead>
                <tbody>
                  {data.model_usage.map((m) => (
                    <tr key={m.model}>
                      <td>{m.model}</td>
                      <td>{m.requests}</td>
                      <td>{money(m.used_usd)}</td>
                      <td>{moneyPrecise(m.avg_request_usd)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="panel">
            <h2>Compras y gastos registrados</h2>
            <div className="tableWrap">
              <table>
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Tipo</th>
                    <th>Proveedor</th>
                    <th>Modelo/producto</th>
                    <th>Pagado</th>
                    <th>Saldo agregado</th>
                    <th>Nota</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {data.entries.map((e) => (
                    <tr key={e.id}>
                      <td>{e.purchased_at}</td>
                      <td>{e.category}</td>
                      <td>{e.provider}</td>
                      <td>{e.product_model || "—"}</td>
                      <td>{money(e.amount_paid_usd)}</td>
                      <td>{e.category === "api" ? money(e.balance_added_usd) : "—"}</td>
                      <td>{e.notes || "—"}</td>
                      <td><button className="dangerBtn" onClick={() => del(e.id)}>Eliminar</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      ) : (
        <div className="empty">Cargando información financiera...</div>
      )}
    </>
  );
}

function ComingSoon({ title, text }: { title: string; text: string }) {
  return (
    <>
      <header>
        <div>
          <h1>{title}</h1>
          <p>Panel administrativo SIMPLE.</p>
        </div>
      </header>
      <section className="panel placeholder">
        <h2>{title}</h2>
        <p>{text}</p>
        <span>Esta sección queda visible nuevamente y la habilitaremos con funciones reales sin reemplazar las demás.</span>
      </section>
    </>
  );
}

function Metric({ title, value, note }: { title: string; value: string; note: string }) {
  return (
    <article className="metric">
      <span>{title}</span>
      <strong>{value}</strong>
      <small>{note}</small>
    </article>
  );
}

function PriceBox({ multiplier, avg, label }: { multiplier: number; avg: number; label: string }) {
  return (
    <div className="priceBox">
      <span>{label}</span>
      <strong>{moneyPrecise(Number(avg || 0) * multiplier)}</strong>
      <small>referencia por consulta</small>
    </div>
  );
}

function Card({ t, v, s }: { t: string; v: string; s: string }) {
  return (
    <article>
      <span>{t}</span>
      <strong>{v}</strong>
      <small>{s}</small>
    </article>
  );
}

function money(v: number) {
  const n = Number(v || 0);
  return n < 0.01 ? `$${n.toFixed(6)}` : `$${n.toFixed(2)}`;
}

function moneyPrecise(v: number) {
  const n = Number(v || 0);
  if (n === 0) return "$0.000000";
  if (n < 0.01) return `$${n.toFixed(6)}`;
  return `$${n.toFixed(4)}`;
}

function format(v: number) {
  return Number(v || 0).toLocaleString("es-BO");
}

function statusLabel(s: string) {
  if (s === "COMPRAR_AHORA") return { t: "COMPRAR API AHORA", m: "El saldo registrado se agotó.", c: "bad" };
  if (s === "COMPRAR_PRONTO") return { t: "COMPRAR MÁS PRONTO", m: "Quedan menos de 7 días de saldo al ritmo actual.", c: "warn" };
  if (s === "SALDO_BAJO") return { t: "SALDO BAJO", m: "Queda menos del 20% del saldo comprado.", c: "warn" };
  if (s === "SIN_SALDO_REGISTRADO") return { t: "REGISTRA TU PRIMERA COMPRA", m: "Aún no sabemos cuánto saldo API tienes.", c: "neutral" };
  return { t: "SALDO SALUDABLE", m: "Por ahora no necesitas comprar más.", c: "good" };
}
