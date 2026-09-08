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

type Summary = {
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

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || "";
const supabaseKey = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY || "";

export default function Home() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState<string | null>(null);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem("simple_admin_token");
    if (saved) setToken(saved);
  }, []);

  useEffect(() => {
    if (token) loadSummary(token);
  }, [token]);

  async function login() {
    setLoading(true);
    setError("");
    try {
      const response = await fetch(
        `${supabaseUrl}/auth/v1/token?grant_type=password`,
        {
          method: "POST",
          headers: {
            apikey: supabaseKey,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ email, password }),
        }
      );

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data?.error_description || data?.msg || "No se pudo ingresar.");
      }

      localStorage.setItem("simple_admin_token", data.access_token);
      setToken(data.access_token);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Error de ingreso.");
    } finally {
      setLoading(false);
    }
  }

  async function loadSummary(accessToken: string) {
    setLoading(true);
    setError("");
    try {
      const response = await fetch(
        `${supabaseUrl}/rest/v1/rpc/get_ai_usage_summary`,
        {
          method: "POST",
          headers: {
            apikey: supabaseKey,
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: "{}",
        }
      );

      const text = await response.text();
      const data = text ? JSON.parse(text) : null;

      if (!response.ok) {
        if (response.status === 401) {
          localStorage.removeItem("simple_admin_token");
          setToken(null);
        }
        if (String(data?.message || "").includes("not_authorized")) {
          throw new Error(
            "Tu cuenta existe, pero todavía no está marcada como administrador en Supabase."
          );
        }
        throw new Error(data?.message || "No se pudieron cargar los datos.");
      }

      setSummary(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Error al cargar datos.");
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    localStorage.removeItem("simple_admin_token");
    setToken(null);
    setSummary(null);
  }

  const projected1000 = useMemo(() => {
    const avg = Number(summary?.avg_cost_per_request || 0);
    return avg * 1000;
  }, [summary]);

  if (!token) {
    return (
      <main className="loginShell">
        <section className="loginCard">
          <div className="logo">💡</div>
          <h1>SIMPLE Control</h1>
          <p>Panel administrativo de consumo y costos de inteligencia artificial.</p>

          <label>Correo administrador</label>
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" />

          <label>Contraseña</label>
          <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" />

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
          <button className="active">📊 Costos IA</button>
          <button>👥 Usuarios</button>
          <button>🪙 Créditos</button>
          <button>📄 Resúmenes</button>
          <button>⚙️ Configuración</button>
        </nav>

        <button className="logout" onClick={logout}>Cerrar sesión</button>
      </aside>

      <section className="content">
        <header>
          <div>
            <h1>Control de costos IA</h1>
            <p>Uso real registrado por SIMPLE.</p>
          </div>
          <button className="refresh" onClick={() => token && loadSummary(token)}>
            {loading ? "Actualizando..." : "Actualizar"}
          </button>
        </header>

        {error && <div className="error top">{error}</div>}

        {summary && (
          <>
            <div className="metrics">
              <Metric title="Costo acumulado" value={money(summary.total_cost_usd)} note="Desde que activamos medición" />
              <Metric title="Costo hoy" value={money(summary.today_cost_usd)} note="Consumo del día" />
              <Metric title="Solicitudes" value={format(summary.total_requests)} note="Consultas reales a OpenAI" />
              <Metric title="Costo promedio" value={money(summary.avg_cost_per_request)} note="Por solicitud" />
            </div>

            <div className="metrics secondary">
              <Metric title="Tokens entrada" value={format(summary.total_input_tokens)} note={`Caché: ${format(summary.total_cached_input_tokens)}`} />
              <Metric title="Tokens salida" value={format(summary.total_output_tokens)} note="Texto generado" />
              <Metric title="Últimos 7 días" value={money(summary.last_7_days_cost_usd)} note="Costo acumulado semanal" />
              <Metric title="Proyección 1.000 consultas" value={money(projected1000)} note="Con el promedio actual" />
            </div>

            <section className="panel">
              <div className="panelHeader">
                <div>
                  <h2>¿Cuánto deberíamos cobrar?</h2>
                  <p>Este panel irá tomando decisiones con datos reales.</p>
                </div>
              </div>

              <div className="pricingGrid">
                <PriceBox multiplier={3} avg={summary.avg_cost_per_request} label="Margen x3" />
                <PriceBox multiplier={5} avg={summary.avg_cost_per_request} label="Margen x5" />
                <PriceBox multiplier={10} avg={summary.avg_cost_per_request} label="Margen x10" />
              </div>

              <p className="hint">
                Estas cifras solo consideran el costo de IA. Antes de fijar el precio final añadiremos almacenamiento,
                generación de PowerPoint, infraestructura, pagos y margen comercial.
              </p>
            </section>

            <section className="panel">
              <h2>Últimas consultas</h2>
              <div className="tableWrap">
                <table>
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Modelo</th>
                      <th>Libro</th>
                      <th>Entrada</th>
                      <th>Salida</th>
                      <th>Total tokens</th>
                      <th>Costo</th>
                      <th>Tiempo</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(summary.recent || []).map((row) => (
                      <tr key={row.id}>
                        <td>{new Date(row.created_at).toLocaleString("es-BO")}</td>
                        <td>{row.model}</td>
                        <td>{row.book_name || "—"}</td>
                        <td>{format(row.input_tokens)}</td>
                        <td>{format(row.output_tokens)}</td>
                        <td>{format(row.total_tokens)}</td>
                        <td>{money(row.estimated_cost_usd)}</td>
                        <td>{row.request_ms ? `${(row.request_ms / 1000).toFixed(1)} s` : "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>

            <section className="panel info">
              <h2>Tarifa usada para la estimación</h2>
              <p>
                GPT‑5.6 Luna: USD 0,20 / 1M tokens de entrada, USD 0,02 / 1M de entrada en caché
                y USD 1,20 / 1M tokens de salida. Para solicitudes de contexto muy largo, el backend
                aplica el multiplicador correspondiente.
              </p>
            </section>
          </>
        )}

        {!summary && !loading && !error && (
          <div className="empty">Todavía no hay información de consumo.</div>
        )}
      </section>
    </main>
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
  const perRequest = Number(avg || 0) * multiplier;
  return (
    <div className="priceBox">
      <span>{label}</span>
      <strong>{money(perRequest)}</strong>
      <small>precio mínimo IA por consulta</small>
    </div>
  );
}

function money(value: number) {
  const n = Number(value || 0);
  if (n < 0.01) return `$${n.toFixed(6)}`;
  return `$${n.toFixed(4)}`;
}

function format(value: number) {
  return Number(value || 0).toLocaleString("es-BO");
}
