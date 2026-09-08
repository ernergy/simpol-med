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
    if (!supabaseUrl || !supabaseKey) {
      setError("Falta configurar Supabase en web/.env.local.");
      return;
    }

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
        throw new Error(
          data?.error_description || data?.msg || "No se pudo ingresar."
        );
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
            "Tu usuario todavía no está marcado como administrador en Supabase."
          );
        }

        throw new Error(
          data?.message || "No se pudieron cargar los datos de consumo."
        );
      }

      setSummary(data as Summary);
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
    setEmail("");
    setPassword("");
  }

  const projected1000 = useMemo(() => {
    return Number(summary?.avg_cost_per_request || 0) * 1000;
  }, [summary]);

  if (!token) {
    return (
      <main className="loginShell">
        <section className="loginCard">
          <div className="logo">💡</div>
          <h1>SIMPLE Control</h1>
          <p>Panel real de costos y consumo de inteligencia artificial.</p>

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

          <button
            onClick={login}
            disabled={loading || !email || !password}
          >
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
          <button disabled>👥 Usuarios</button>
          <button disabled>🪙 Créditos</button>
          <button disabled>📄 Resúmenes</button>
          <button disabled>⚙️ Configuración</button>
        </nav>

        <button className="logout" onClick={logout}>
          Cerrar sesión
        </button>
      </aside>

      <section className="content">
        <header>
          <div>
            <h1>Control de costos IA</h1>
            <p>Datos reales registrados por SIMPLE.</p>
          </div>

          <button
            className="refresh"
            onClick={() => token && loadSummary(token)}
          >
            {loading ? "Actualizando..." : "Actualizar"}
          </button>
        </header>

        {error && <div className="error top">{error}</div>}

        {summary && (
          <>
            <div className="metrics">
              <Metric
                title="Costo acumulado"
                value={money(summary.total_cost_usd)}
                note="Desde que activamos medición"
              />
              <Metric
                title="Costo hoy"
                value={money(summary.today_cost_usd)}
                note="Consumo del día"
              />
              <Metric
                title="Solicitudes"
                value={format(summary.total_requests)}
                note="Consultas reales a OpenAI"
              />
              <Metric
                title="Costo promedio"
                value={money(summary.avg_cost_per_request)}
                note="Por solicitud"
              />
            </div>

            <div className="metrics secondary">
              <Metric
                title="Tokens entrada"
                value={format(summary.total_input_tokens)}
                note={`Caché: ${format(summary.total_cached_input_tokens)}`}
              />
              <Metric
                title="Tokens salida"
                value={format(summary.total_output_tokens)}
                note="Texto generado"
              />
              <Metric
                title="Últimos 7 días"
                value={money(summary.last_7_days_cost_usd)}
                note="Costo semanal"
              />
              <Metric
                title="Proyección 1.000 consultas"
                value={money(projected1000)}
                note="Con el promedio actual"
              />
            </div>

            <section className="panel">
              <h2>Referencia para definir nuestros créditos</h2>
              <p>
                Estas cifras usan el costo real promedio observado. Todavía no
                son el precio final al cliente.
              </p>

              <div className="pricingGrid">
                <PriceBox
                  multiplier={3}
                  avg={summary.avg_cost_per_request}
                  label="Costo x3"
                />
                <PriceBox
                  multiplier={5}
                  avg={summary.avg_cost_per_request}
                  label="Costo x5"
                />
                <PriceBox
                  multiplier={10}
                  avg={summary.avg_cost_per_request}
                  label="Costo x10"
                />
              </div>

              <p className="hint">
                Luego añadiremos almacenamiento, procesamiento de PDF,
                PowerPoint, infraestructura, comisiones de pago y margen.
              </p>
            </section>

            <section className="panel">
              <h2>Últimas consultas reales</h2>

              {summary.recent?.length ? (
                <div className="tableWrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Fecha</th>
                        <th>Modelo</th>
                        <th>Libro</th>
                        <th>Entrada</th>
                        <th>Salida</th>
                        <th>Total</th>
                        <th>Costo</th>
                        <th>Tiempo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {summary.recent.map((row) => (
                        <tr key={row.id}>
                          <td>
                            {new Date(row.created_at).toLocaleString("es-BO")}
                          </td>
                          <td>{row.model}</td>
                          <td>{row.book_name || "—"}</td>
                          <td>{format(row.input_tokens)}</td>
                          <td>{format(row.output_tokens)}</td>
                          <td>{format(row.total_tokens)}</td>
                          <td>{money(row.estimated_cost_usd)}</td>
                          <td>
                            {row.request_ms
                              ? `${(row.request_ms / 1000).toFixed(1)} s`
                              : "—"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="empty">
                  Aún no hay consultas registradas después de activar la medición.
                  Haz una consulta nueva desde la app y pulsa Actualizar.
                </div>
              )}
            </section>
          </>
        )}

        {!summary && loading && (
          <div className="empty">Cargando datos reales...</div>
        )}
      </section>
    </main>
  );
}

function Metric({
  title,
  value,
  note,
}: {
  title: string;
  value: string;
  note: string;
}) {
  return (
    <article className="metric">
      <span>{title}</span>
      <strong>{value}</strong>
      <small>{note}</small>
    </article>
  );
}

function PriceBox({
  multiplier,
  avg,
  label,
}: {
  multiplier: number;
  avg: number;
  label: string;
}) {
  const value = Number(avg || 0) * multiplier;

  return (
    <div className="priceBox">
      <span>{label}</span>
      <strong>{money(value)}</strong>
      <small>referencia por consulta</small>
    </div>
  );
}

function money(value: number) {
  const n = Number(value || 0);

  if (n === 0) return "$0.000000";
  if (n < 0.01) return `$${n.toFixed(6)}`;

  return `$${n.toFixed(4)}`;
}

function format(value: number) {
  return Number(value || 0).toLocaleString("es-BO");
}
