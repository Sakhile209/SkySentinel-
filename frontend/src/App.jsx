import React, { useEffect, useState } from 'react';
import Icon from './components/Icon.jsx';
import Panel from './components/Panel.jsx';
import OperationsMap from './components/OperationsMap.jsx';
import DemoFeed from './components/DemoFeed.jsx';
import { incidents, drones, teams } from './data/demo.js';

const navigation = [['Dashboard','dashboard','dashboard'],['Live Map','map','map'],['Incidents','alert','incidents'],['Drones','drone','fleet'],['Missions','mission','missions'],['Response Teams','team','teams'],['Sites','site'],['Panic Buttons','pin'],['Users','team'],['Reports','report'],['Evidence','evidence'],['Audit Log','report'],['Settings','settings']];
const badgeColor = value => ['HIGH','NEW'].includes(value) ? 'red' : ['MEDIUM','CHARGING','DISPATCHED'].includes(value) ? 'amber' : ['AVAILABLE','COMPLETED','LOW'].includes(value) ? 'green' : 'cyan';
function Badge({ value }) { return <span className={`badge ${badgeColor(value)}`}>{value.replaceAll('_',' ')}</span>; }
function Stat({ icon, title, value, detail, color = 'cyan' }) { return <article className="stat"><span className={`stat-icon ${color}`}><Icon name={icon} size={29}/></span><div><h2>{title}</h2><strong>{value}</strong><small className={color}>{detail}</small></div></article>; }

export default function App() {
  const [selected, setSelected] = useState(incidents[0]);
  const [tab, setTab] = useState('Location');
  const [state, setState] = useState('checking');
  const [attempt, setAttempt] = useState(0);
  const [now, setNow] = useState(new Date());
  const [notice, setNotice] = useState('');
  const [activeNav, setActiveNav] = useState('Dashboard');
  useEffect(() => { const timer = setInterval(() => setNow(new Date()), 1000); return () => clearInterval(timer); }, []);
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    const timeout = setTimeout(() => controller.abort(), 8000);
    setState('checking');
    async function check() {
      try {
        const response = await fetch('/api/health', { signal: controller.signal });
        if (!response.ok) throw new Error();
        const result = await response.json();
        if (result.status !== 'UP' || result.database !== 'UP') throw new Error();
        if (active) setState('connected');
      } catch { if (active) setState('unavailable'); }
      finally { clearTimeout(timeout); }
    }
    check();
    return () => { active = false; controller.abort(); clearTimeout(timeout); };
  }, [attempt]);
  const chooseIncident = incident => { setSelected(incident); setTab('Location'); };
  const navigate = (name, id) => {
    if (!id) { setNotice(`${name} will be available in a later development phase.`); return; }
    setActiveNav(name);
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  };
  return <div className="app-shell" id="dashboard">
    <header className="topbar">
      <div className="brand"><div className="brand-symbol"><Icon name="shield" size={37}/></div><div><div><b>SKYSENTINEL</b> <span>SECURITY</span></div><small>See sooner. Respond smarter.</small></div></div>
      <h1>SECURITY OPERATIONS CENTRE</h1>
      <div className="header-clock"><small>{now.toLocaleDateString('en-ZA', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })}</small><strong>{now.toLocaleTimeString('en-ZA', { hour12: false })}</strong><span className={state === 'connected' ? 'green' : 'amber'}>● {state === 'connected' ? 'System online' : state === 'checking' ? 'Connecting' : 'Backend unavailable'}</span></div>
      <button className="header-tool" aria-label="Show demo alerts" onClick={() => setNotice('Demo alerts: high-priority panic at Warehouse B; SS-003 battery at 38%.')}><Icon name="bell" size={22}/><i>2</i><small>Alerts</small></button>
      <button className="header-tool" aria-label="Show messages" onClick={() => setNotice('No messaging service connected. This is a dashboard preview.')}><Icon name="mail" size={22}/><small>Messages</small></button>
      <div className="profile"><span className="avatar"><Icon name="team"/></span><div><b>Operator preview</b><small>Control room · Demo</small></div></div>
    </header>
    <aside className="sidebar"><nav aria-label="Main navigation">{navigation.map(([name, icon, id]) => <button key={name} className={activeNav === name ? 'nav-active' : ''} onClick={() => navigate(name,id)}><Icon name={icon}/><span>{name}</span>{name === 'Incidents' && <i>4</i>}</button>)}</nav><div className="sidebar-footer"><Icon name="shield" size={26}/><b>SKYSENTINEL</b><small>SECURITY OPERATIONS</small><span>© 2026 SkySentinel Security</span></div></aside>
    <main className="workspace">
      <div className="preview-strip"><span><span className="demo-dot"/> DEMO WORKSPACE</span><span>Simulated operational data · Human authorization required</span></div>
      {notice && <div className="notice" role="status">{notice}<button aria-label="Dismiss notice" onClick={() => setNotice('')}>×</button></div>}
      <div className="stats-grid"><Stat icon="alert" title="Active incidents" value="4" detail="1 awaiting acknowledgement" color="red"/><Stat icon="drone" title="Drones" value={<>2 <em>/ 3</em></>} detail="2 available" color="green"/><Stat icon="mission" title="Active missions" value="0" detail="Awaiting authorization"/><Stat icon="team" title="Response teams" value="4" detail="2 available" color="green"/><Stat icon="site" title="Protected sites" value="5" detail="Shared · Dedicated · No drone"/><Stat icon="pulse" title="System status" value={state === 'connected' ? 'Online' : state === 'checking' ? 'Checking' : 'Offline'} detail={state === 'connected' ? 'Backend & database connected' : 'Check service connection'} color={state === 'connected' ? 'green' : 'amber'}/></div>
      <div className="main-grid">
        <Panel title="Live incidents" id="incidents" action={<span className="panel-meta">4 open</span>}><div className="incident-list">{incidents.map(incident => <button key={incident.id} className={`incident-card ${selected.id === incident.id ? 'incident-selected' : ''}`} onClick={() => chooseIncident(incident)} aria-pressed={selected.id === incident.id}><span className={`incident-icon ${badgeColor(incident.priority)}`}><Icon name="alert" size={24}/></span><span className="incident-copy"><b>{incident.site}</b><small>{incident.type}</small></span><span className="incident-status"><Badge value={incident.priority}/><time>{incident.time}</time><small className={badgeColor(incident.status)}>{incident.status}</small></span></button>)}</div><div className="panel-bottom"><span className="red">●</span> Operator acknowledgement required</div></Panel>
        <Panel title="Live map" id="map" className="map-panel" action={<span className="panel-meta">Industrial Area North <span className="map-filter">All resources⌄</span></span>}><OperationsMap selected={selected} onSelect={chooseIncident}/></Panel>
        <Panel title="Selected incident" className="details-panel" action={<Badge value={selected.priority}/>}><div className="selected-banner"><Icon name="alert" size={29}/><div><h3>{selected.site}</h3><small>{selected.type}</small></div><span>{selected.id}</span></div><dl className="incident-details"><dt>Site:</dt><dd>{selected.site}</dd><dt>Location:</dt><dd>{selected.zone}</dd><dt>Device:</dt><dd>{selected.device}</dd><dt>Time:</dt><dd>Demo event · {selected.time}:32</dd><dt>Priority:</dt><dd><Badge value={selected.priority}/></dd><dt>Status:</dt><dd><Badge value={selected.status}/></dd><dt>Description:</dt><dd>{selected.description}</dd></dl><div className="incident-actions"><button disabled className="danger">Acknowledge</button><button disabled>Create Mission</button><button disabled className="success">Dispatch Team</button></div><p className="action-hint">Preview only · Operational actions are not enabled</p><div className="tabs" role="tablist" aria-label="Incident details">{['Location','Notes','Evidence','History'].map(name => <button role="tab" id={`tab-${name}`} aria-controls="incident-tab-panel" aria-selected={tab === name} key={name} onClick={() => setTab(name)}>{name}</button>)}</div><div role="tabpanel" id="incident-tab-panel" aria-labelledby={`tab-${tab}`} className="tab-content">{tab === 'Location' ? <div className="location-preview"><Icon name="pin" size={27}/><div><b>{selected.zone}</b><small>{selected.site} · Illustrative location</small></div></div> : tab === 'Notes' ? 'No operator notes in this preview.' : tab === 'Evidence' ? 'No evidence attached. Camera preview is illustrative only.' : `${selected.time} · Demo incident presented for operator review.`}</div></Panel>
      </div>
      <div className="operations-grid">
        <Panel title="Drone fleet" id="fleet" action={<span className="panel-meta">3 simulated</span>}>{drones.map(drone => <div className="resource-row" key={drone.code}><Icon name="drone" size={34}/><div className="resource-name"><b>{drone.code}</b><small>{drone.base}</small></div><span className={drone.battery < 40 ? 'amber' : 'green'}>▰ {drone.battery}%</span><Badge value={drone.state}/></div>)}<div className="fleet-summary"><span>Manufacturer-independent simulation</span><small>No physical aircraft connected</small></div></Panel>
        <Panel title="Active missions" id="missions" action={<span className="panel-meta">Human authorized</span>}><div className="mission-empty"><span className="mission-emblem"><Icon name="mission" size={32}/></span><h3>No active observation missions</h3><p>An operator must review an incident and explicitly authorize a mission.</p><span className="subtle-tag">NO AUTOMATIC LAUNCH</span></div></Panel>
        <Panel title="Response teams" id="teams" action={<span className="panel-meta">4 teams</span>}>{teams.map(team => <div className="resource-row team-row" key={team.name}><Icon name="car" size={28}/><div className="resource-name"><b>{team.name}</b><small>{team.zone}</small></div><Badge value={team.state}/></div>)}</Panel>
        <Panel title="Live drone feed" action={<span className="preview-badge">● DEMO</span>}><div className="feed-layout"><DemoFeed/><dl className="telemetry"><dt>Battery</dt><dd>76%</dd><dt>Altitude</dt><dd>—</dd><dt>Speed</dt><dd>—</dd><dt>Status</dt><dd className="cyan">PREVIEW</dd><dt>Signal</dt><dd>Not connected</dd></dl></div><div className="feed-footer"><span><Icon name="evidence"/> Camera placeholder</span><span className="green">SIMULATED</span></div></Panel>
      </div>
      <div className="bottom-grid"><Panel title="Recent activity"><ul className="activity-list"><li><time>19:14</time><span>Panic event preview · PB-WH-002</span><small>Demo</small></li><li><time>19:14</time><span>Incident shown · INC-10024</span><small>Demo</small></li><li><time>18:03</time><span>Perimeter alarm · Facility D</span><small>Demo</small></li><li><time>17:24</time><span>Response Team 02 · On scene</span><small>Demo</small></li></ul></Panel><Panel title="System alerts"><div className="system-alert"><Icon name="alert"/><div><b>High-priority incident</b><small>Warehouse B requires operator review</small></div></div><div className="system-alert amber"><Icon name="drone"/><div><b>SS-003 battery at 38%</b><small>Charging at dedicated base DB-002</small></div></div><div className={`system-alert ${state === 'connected' ? 'green' : 'amber'}`}><Icon name="shield"/><div><b role="status">{state === 'connected' ? 'Connected — backend and database healthy' : state === 'checking' ? 'Checking connection…' : 'Connection unavailable — check backend and database'}</b><button className="text-button" disabled={state === 'checking'} onClick={() => setAttempt(value => value + 1)}>Check again</button></div></div></Panel><Panel title="Weather & conditions" action={<span className="panel-meta">Sample</span>}><div className="weather"><div><small>Industrial Area North</small><div><Icon name="cloud" size={40}/><strong>14°C</strong></div><span>Partly cloudy · Demo</span></div><dl><dt>Wind</dt><dd>12 km/h</dd><dt>Humidity</dt><dd>68%</dd><dt>Visibility</dt><dd>10 km</dd><dt>Precipitation</dt><dd>0%</dd></dl></div></Panel><Panel title="Quick actions"><div className="quick-actions">{['New Incident','Register Panic Button','Add Drone','Add Site'].map(label => <button disabled key={label}>{label}</button>)}</div><small className="quick-hint">Available in later phases</small></Panel></div>
      <footer className="workspace-footer"><span>SKYSENTINEL SECURITY <span> / </span> OPERATIONS PREVIEW</span><span>People in control. Always.</span></footer>
    </main>
  </div>;
}
