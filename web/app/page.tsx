const cards = [
  ["Users", "1,248", "Active accounts"],
  ["Credits", "12,450", "Credits sold"],
  ["Summaries", "3,280", "Saved study materials"],
  ["Sessions", "860", "Active this month"],
];

export default function Home() {
  return (
    <main className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brandMark">S</span>
          <div><strong>SIMPLE</strong><small>Admin</small></div>
        </div>
        <nav>
          {["Dashboard", "Users", "Credits", "Study Materials", "Reports", "Support", "Settings"].map((x, i) =>
            <button key={x} className={i === 0 ? "active" : ""}>{x}</button>
          )}
        </nav>
      </aside>

      <section className="content">
        <header>
          <div>
            <h1>Dashboard</h1>
            <p>Platform overview</p>
          </div>
          <button className="account">Administrator</button>
        </header>

        <div className="grid">
          {cards.map(([title, value, sub]) => (
            <article className="metric" key={title}>
              <span>{title}</span>
              <strong>{value}</strong>
              <small>{sub}</small>
            </article>
          ))}
        </div>

        <div className="twoCol">
          <article className="panel">
            <h2>User activity</h2>
            <div className="chart">
              {[32,48,41,65,54,72,61,82,70,91,77,95].map((v,i) =>
                <i key={i} style={{height: `${v}%`}} />
              )}
            </div>
          </article>

          <article className="panel">
            <h2>Storage strategy</h2>
            <p>Original books are temporary. Only user-selected summaries and study materials remain saved, with account storage limits.</p>
            <div className="badge">Cost-controlled architecture</div>
          </article>
        </div>

        <article className="panel">
          <h2>Recent users</h2>
          <div className="table">
            <div><b>ana.lopez@email.com</b><span>12 credits</span><span>Active</span></div>
            <div><b>carlos.med@gmail.com</b><span>5 credits</span><span>Active</span></div>
            <div><b>maria.residente@outlook.com</b><span>22 credits</span><span>Active</span></div>
          </div>
        </article>
      </section>
    </main>
  );
}
