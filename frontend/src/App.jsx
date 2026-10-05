import React, { useCallback, useEffect, useRef, useState } from 'react';
import Icon from './components/Icon.jsx';
import Panel from './components/Panel.jsx';
import OperationsMap from './components/OperationsMap.jsx';
import DemoFeed from './components/DemoFeed.jsx';
import { incidents, drones, teams } from './data/demo.js';

const AUTH_STORAGE_KEY = 'skysentinel.auth';
const OTP_SENT_MESSAGE = 'A 6-digit verification code has been sent to your registered phone number.';
const navigation = [['Dashboard','dashboard','dashboard'],['Live Map','map','map'],['Incidents','alert','incidents'],['Drones','drone','fleet'],['Missions','mission','missions'],['Response Teams','team','teams'],['Sites','site'],['Panic Buttons','pin'],['Users','team'],['Reports','report'],['Evidence','evidence'],['Audit Log','report'],['Settings','settings']];
const badgeColor = value => ['HIGH','NEW'].includes(value) ? 'red' : ['MEDIUM','CHARGING','DISPATCHED'].includes(value) ? 'amber' : ['AVAILABLE','COMPLETED','LOW'].includes(value) ? 'green' : 'cyan';
function Badge({ value }) { return <span className={`badge ${badgeColor(value)}`}>{value.replaceAll('_',' ')}</span>; }
function Stat({ icon, title, value, detail, color = 'cyan' }) { return <article className="stat"><span className={`stat-icon ${color}`}><Icon name={icon} size={29}/></span><div><h2>{title}</h2><strong>{value}</strong><small className={color}>{detail}</small></div></article>; }
const weatherCodeText = code => ({
  0: 'Clear sky',
  1: 'Mainly clear',
  2: 'Partly cloudy',
  3: 'Overcast',
  45: 'Fog',
  48: 'Depositing rime fog',
  51: 'Light drizzle',
  53: 'Moderate drizzle',
  55: 'Dense drizzle',
  61: 'Slight rain',
  63: 'Moderate rain',
  65: 'Heavy rain',
  71: 'Slight snow',
  73: 'Moderate snow',
  75: 'Heavy snow',
  80: 'Slight rain showers',
  81: 'Moderate rain showers',
  82: 'Violent rain showers',
  95: 'Thunderstorm',
  96: 'Thunderstorm with hail',
  99: 'Thunderstorm with hail',
}[code] || 'Weather data available');

function WeatherPanel({ location, weather, onRequestLocation }) {
  if (location.status === 'denied') {
    return <div className="weather weather-empty"><Icon name="pin" size={34}/><div><b>Location access required</b><span>Enable location permission to load live weather for your current position.</span><button onClick={onRequestLocation}>Enable Location</button></div></div>;
  }
  if (location.status === 'unsupported') {
    return <div className="weather weather-empty"><Icon name="pin" size={34}/><div><b>Location unavailable</b><span>This browser cannot provide current location, so live local weather cannot be loaded.</span></div></div>;
  }
  if (weather.status === 'loading' || location.status === 'requesting') {
    return <div className="weather weather-empty"><Icon name="cloud" size={34}/><div><b>Loading live weather</b><span>Waiting for current location and weather service response.</span></div></div>;
  }
  if (weather.status === 'error') {
    return <div className="weather weather-empty"><Icon name="cloud" size={34}/><div><b>Weather unavailable</b><span>{weather.error || 'Unable to load current weather from the weather service.'}</span><button onClick={onRequestLocation}>Retry Location</button></div></div>;
  }
  if (!weather.data) {
    return <div className="weather weather-empty"><Icon name="cloud" size={34}/><div><b>Location required</b><span>SkySentinel never uses fake weather. Grant location access to load live conditions.</span><button onClick={onRequestLocation}>Enable Location</button></div></div>;
  }
  return <div className="weather">
    <div>
      <small>{weather.data.latitude.toFixed(4)}, {weather.data.longitude.toFixed(4)}</small>
      <div><Icon name="cloud" size={40}/><strong>{Math.round(weather.data.temperature)}°C</strong></div>
      <span>{weather.data.condition}</span>
    </div>
    <dl>
      <dt>Wind</dt><dd>{Math.round(weather.data.windSpeed)} km/h</dd>
      <dt>Humidity</dt><dd>{Math.round(weather.data.humidity)}%</dd>
      <dt>Updated</dt><dd>{new Date(weather.data.updatedAt).toLocaleTimeString('en-ZA', { hour: '2-digit', minute: '2-digit', hour12: false })}</dd>
      <dt>Source</dt><dd>Open-Meteo</dd>
    </dl>
  </div>;
}

function getPayloadMessage(payload, fallback) {
  if (payload?.message) return payload.message;
  if (payload?.error && payload.error !== 'Bad Request') return payload.error;
  const fieldError = payload?.errors?.[0]?.defaultMessage || payload?.errors?.[0]?.message;
  return fieldError || fallback;
}

function secondsUntil(value) {
  const expiry = new Date(value).getTime();
  if (!Number.isFinite(expiry)) return 0;
  return Math.max(0, Math.ceil((expiry - Date.now()) / 1000));
}

function AuthenticationScreen({ onAuthenticated }) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ fullName: '', email: '', cellphoneNumber: '', password: '', badgeNumber: '' });
  const [challenge, setChallenge] = useState(null);
  const [otpCode, setOtpCode] = useState('');
  const [otpSeconds, setOtpSeconds] = useState(0);
  const [status, setStatus] = useState('idle');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const isRegistering = mode === 'register';
  const update = event => setForm(value => ({ ...value, [event.target.name]: event.target.value }));
  useEffect(() => {
    if (!challenge?.expiresAt) return;
    const updateCountdown = () => setOtpSeconds(secondsUntil(challenge.expiresAt));
    updateCountdown();
    const timer = setInterval(updateCountdown, 1000);
    return () => clearInterval(timer);
  }, [challenge?.expiresAt]);
  const submit = async event => {
    event.preventDefault();
    setStatus('submitting');
    setError('');
    setMessage('');
    try {
      const body = isRegistering
        ? { fullName: form.fullName, email: form.email, cellphoneNumber: form.cellphoneNumber, password: form.password, badgeNumber: form.badgeNumber }
        : { email: form.email, password: form.password };
      const response = await fetch(`/api/auth/${isRegistering ? 'register' : 'login'}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(getPayloadMessage(payload, 'Authentication failed'));
      if (isRegistering) {
        if (!payload.challengeId) {
          throw new Error('Registration succeeded, but no verification challenge was returned. Please sign in to request an OTP.');
        }
        setChallenge(payload);
        setOtpCode('');
        setMessage(payload.message || OTP_SENT_MESSAGE);
        setForm(value => ({ ...value, password: '' }));
      } else {
        if (!payload.challengeId) {
          throw new Error('Sign in requires a verification OTP before access can be granted. Please request a new OTP.');
        }
        setChallenge(payload);
        setOtpCode('');
        setMessage(payload.message || OTP_SENT_MESSAGE);
      }
      setStatus('idle');
    } catch (err) {
      setError(err.message || 'Authentication failed');
      setStatus('idle');
    }
  };
  const verifyOtp = async event => {
    event.preventDefault();
    setStatus('verifying');
    setError('');
    setMessage('');
    if (!challenge?.challengeId) {
      setError('Verification challenge expired. Please sign in again.');
      setChallenge(null);
      setOtpCode('');
      setStatus('idle');
      return;
    }
    try {
      const response = await fetch('/api/auth/verify-otp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ challengeId: challenge.challengeId, code: otpCode }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(getPayloadMessage(payload, 'Verification failed'));
      setChallenge(null);
      setOtpCode('');
      onAuthenticated(payload);
      setStatus('idle');
    } catch (err) {
      setError(err.message || 'Verification failed');
      setStatus('idle');
    }
  };
  const resendOtp = async () => {
    setStatus('resending');
    setError('');
    setMessage('');
    try {
      const response = await fetch('/api/auth/resend-otp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ challengeId: challenge.challengeId }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(getPayloadMessage(payload, 'Unable to resend verification code'));
      setChallenge(payload);
      setOtpCode('');
      setMessage(payload.message || OTP_SENT_MESSAGE);
      setStatus('idle');
    } catch (err) {
      setError(err.message || 'Unable to resend verification code');
      setStatus('idle');
    }
  };
  if (challenge) {
    return <main className="auth-shell">
      <section className="auth-brand">
        <div className="brand"><div className="brand-symbol"><Icon name="shield" size={44}/></div><div><div><b>SKYSENTINEL</b> <span>SECURITY</span></div><small>Two-step operator verification</small></div></div>
        <h1>Verify Operator Sign In</h1>
        <p>Enter the 6-digit code sent by SMS to your registered South African cellphone number. The code expires in 5 minutes and can only be used once.</p>
        <div className="auth-status-grid">
          <span><b>OTP required</b><small>Control Room access starts only after verification.</small></span>
          <span><b>Single use</b><small>Expired or used codes cannot create a session.</small></span>
        </div>
      </section>
      <section className="auth-panel" aria-labelledby="otp-title">
        <form onSubmit={verifyOtp}>
          <h2 id="otp-title">Verification code</h2>
          <p className="auth-copy">{otpSeconds > 0 ? `Code expires in ${Math.floor(otpSeconds / 60)}:${String(otpSeconds % 60).padStart(2, '0')}` : 'This verification code has expired.'}</p>
          <label>6-digit OTP<input name="otp" inputMode="numeric" pattern="\d{6}" autoComplete="one-time-code" value={otpCode} onChange={event => setOtpCode(event.target.value.replace(/\D/g, '').slice(0, 6))} required /></label>
          {message && <div className="auth-message" role="status">{message}</div>}
          {error && <div className="auth-error" role="alert">{error}</div>}
          <button className="auth-submit" disabled={status === 'verifying' || otpCode.length !== 6}>{status === 'verifying' ? 'Verifying...' : 'Verify and Enter Control Room'}</button>
          <div className="auth-secondary-actions">
            <button type="button" onClick={resendOtp} disabled={status === 'resending'}>{status === 'resending' ? 'Sending...' : 'Request New OTP'}</button>
            <button type="button" onClick={() => { setChallenge(null); setOtpCode(''); setError(''); setMessage(''); }}>Back to sign in</button>
          </div>
        </form>
      </section>
    </main>;
  }

  return <main className="auth-shell">
    <section className="auth-brand">
      <div className="brand"><div className="brand-symbol"><Icon name="shield" size={44}/></div><div><div><b>SKYSENTINEL</b> <span>SECURITY</span></div><small>Authenticated operations platform</small></div></div>
      <h1>Security Operations Platform</h1>
      <p>Sign in with an operator account before entering the Control Room. New supervisors can provision an operator account from this gateway.</p>
      <div className="auth-status-grid">
        <span><b>Protected access</b><small>Dashboard and platform APIs require a bearer token.</small></span>
        <span><b>Operator identity</b><small>Activity is tied to the signed-in user profile.</small></span>
      </div>
    </section>
    <section className="auth-panel" aria-labelledby="auth-title">
      <div className="auth-tabs" role="tablist" aria-label="Authentication mode">
        <button type="button" role="tab" aria-selected={!isRegistering} onClick={() => { setMode('login'); setError(''); setMessage(''); }}>Sign In</button>
        <button type="button" role="tab" aria-selected={isRegistering} onClick={() => { setMode('register'); setError(''); setMessage(''); }}>Sign Up</button>
      </div>
      <form onSubmit={submit}>
        <h2 id="auth-title">{isRegistering ? 'Create operator account' : 'Operator sign in'}</h2>
        {isRegistering && <label>Full name<input name="fullName" autoComplete="name" value={form.fullName} onChange={update} required /></label>}
        <label>Email<input name="email" type="email" autoComplete="email" value={form.email} onChange={update} required /></label>
        {isRegistering && <label>South African cellphone number<input name="cellphoneNumber" type="tel" autoComplete="tel" placeholder="0821234567 or +27821234567" value={form.cellphoneNumber} onChange={update} required /></label>}
        <label>Password<input name="password" type="password" autoComplete={isRegistering ? 'new-password' : 'current-password'} value={form.password} onChange={update} minLength={6} required /></label>
        {isRegistering && <label>Badge number<input name="badgeNumber" autoComplete="off" value={form.badgeNumber} onChange={update} required /></label>}
        {message && <div className="auth-message" role="status">{message}</div>}
        {error && <div className="auth-error" role="alert">{error}</div>}
        <button className="auth-submit" disabled={status === 'submitting'}>{status === 'submitting' ? 'Checking credentials...' : isRegistering ? 'Create Account' : 'Send OTP'}</button>
      </form>
    </section>
  </main>;
}

export default function App() {
  const [auth, setAuth] = useState(null);
  const [authState, setAuthState] = useState('signed-out');
  const [selected, setSelected] = useState(incidents[0]);
  const [tab, setTab] = useState('Location');
  const [state, setState] = useState('checking');
  const [attempt, setAttempt] = useState(0);
  const [now, setNow] = useState(new Date());
  const [notice, setNotice] = useState('');
  const [activeNav, setActiveNav] = useState('Dashboard');
  const [location, setLocation] = useState({ status: 'idle', coords: null, error: '' });
  const [weather, setWeather] = useState({ status: 'idle', data: null, error: '' });
  const healthCheckId = useRef(0);
  const requestLocation = useCallback(() => {
    if (!navigator.geolocation) {
      setLocation({ status: 'unsupported', coords: null, error: 'This browser does not support geolocation.' });
      setWeather({ status: 'idle', data: null, error: '' });
      return;
    }
    setLocation(value => ({ ...value, status: 'requesting', error: '' }));
    navigator.geolocation.getCurrentPosition(
      position => {
        setLocation({
          status: 'granted',
          coords: {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
          },
          error: '',
        });
      },
      error => {
        const denied = error.code === error.PERMISSION_DENIED;
        setLocation({
          status: denied ? 'denied' : 'error',
          coords: null,
          error: denied ? 'Location permission was denied.' : error.message || 'Unable to read current location.',
        });
        setWeather({ status: 'idle', data: null, error: '' });
      },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 60000 }
    );
  }, []);
  useEffect(() => { const timer = setInterval(() => setNow(new Date()), 1000); return () => clearInterval(timer); }, []);
  useEffect(() => {
    if (!auth?.token) return;
    let active = true;
    async function validateSession() {
      try {
        const response = await fetch('/api/auth/me', { headers: { Authorization: `Bearer ${auth.token}` } });
        if (!response.ok) throw new Error();
        const user = await response.json();
        if (!active) return;
        setAuth(value => ({ ...value, user }));
        setAuthState('signed-in');
      } catch {
        if (!active) return;
        window.localStorage.removeItem(AUTH_STORAGE_KEY);
        setAuth(null);
        setAuthState('signed-out');
      }
    }
    validateSession();
    return () => { active = false; };
  }, [auth?.token]);
  useEffect(() => {
    if (authState !== 'signed-in') return;
    const checkId = healthCheckId.current + 1;
    healthCheckId.current = checkId;
    const isCurrentCheck = () => healthCheckId.current === checkId;
    const timeout = setTimeout(() => { if (isCurrentCheck()) setState('unavailable'); }, 8000);
    setState('checking');
    async function check() {
      try {
        const response = await fetch('/api/health', { headers: { Authorization: `Bearer ${auth.token}` } });
        if (!response.ok) throw new Error();
        const result = await response.json();
        if (result.status !== 'UP' || result.database !== 'UP') throw new Error();
        if (isCurrentCheck()) setState('connected');
      } catch { if (isCurrentCheck()) setState('unavailable'); }
      finally { clearTimeout(timeout); }
    }
    check();
    return () => { clearTimeout(timeout); };
  }, [attempt, authState, auth?.token]);
  useEffect(() => {
    if (authState === 'signed-in' && location.status === 'idle') {
      requestLocation();
    }
  }, [authState, location.status, requestLocation]);
  useEffect(() => {
    if (location.status !== 'granted' || !location.coords) return;
    const controller = new AbortController();
    async function loadWeather() {
      setWeather({ status: 'loading', data: null, error: '' });
      try {
        const params = new URLSearchParams({
          latitude: String(location.coords.latitude),
          longitude: String(location.coords.longitude),
          current: 'temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m',
          timezone: 'auto',
        });
        const response = await fetch(`https://api.open-meteo.com/v1/forecast?${params.toString()}`, { signal: controller.signal });
        if (!response.ok) throw new Error('Weather service returned an error.');
        const result = await response.json();
        const current = result.current;
        if (!current) throw new Error('Weather service returned no current conditions.');
        setWeather({
          status: 'ready',
          error: '',
          data: {
            latitude: result.latitude,
            longitude: result.longitude,
            temperature: current.temperature_2m,
            humidity: current.relative_humidity_2m,
            windSpeed: current.wind_speed_10m,
            condition: weatherCodeText(current.weather_code),
            updatedAt: current.time,
          },
        });
      } catch (error) {
        if (error.name !== 'AbortError') {
          setWeather({ status: 'error', data: null, error: error.message || 'Unable to load live weather.' });
        }
      }
    }
    loadWeather();
    return () => controller.abort();
  }, [location.status, location.coords?.latitude, location.coords?.longitude]);
  const signOut = () => {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    setAuth(null);
    setAuthState('signed-out');
    setNotice('');
    setLocation({ status: 'idle', coords: null, error: '' });
    setWeather({ status: 'idle', data: null, error: '' });
  };
  const chooseIncident = incident => { setSelected(incident); setTab('Location'); };
  const navigate = (name, id) => {
    if (!id) { setNotice(`${name} will be available in a later development phase.`); return; }
    setActiveNav(name);
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  };
  if (authState === 'checking') {
    return <main className="auth-shell auth-loading" role="status"><Icon name="shield" size={48}/><h1>Verifying operator session</h1></main>;
  }
  if (authState !== 'signed-in') {
    return <AuthenticationScreen onAuthenticated={payload => { setAuth(payload); setAuthState('checking'); }} />;
  }
  const operator = auth?.user || {};
  return <div className="app-shell" id="dashboard">
    <header className="topbar">
      <div className="brand"><div className="brand-symbol"><Icon name="shield" size={37}/></div><div><div><b>SKYSENTINEL</b> <span>SECURITY</span></div><small>See sooner. Respond smarter.</small></div></div>
      <h1>SECURITY OPERATIONS CENTRE</h1>
      <div className="header-clock"><small>{now.toLocaleDateString('en-ZA', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })}</small><strong>{now.toLocaleTimeString('en-ZA', { hour12: false })}</strong><span className={state === 'connected' ? 'green' : 'amber'}>● {state === 'connected' ? 'System online' : state === 'checking' ? 'Connecting' : 'Backend unavailable'}</span></div>
      <button className="header-tool" aria-label="Show demo alerts" onClick={() => setNotice('Demo alerts: high-priority panic at Warehouse B; SS-003 battery at 38%.')}><Icon name="bell" size={22}/><i>2</i><small>Alerts</small></button>
      <button className="header-tool" aria-label="Show messages" onClick={() => setNotice('No messaging service connected. This is a dashboard preview.')}><Icon name="mail" size={22}/><small>Messages</small></button>
      <div className="profile"><span className="avatar"><Icon name="team"/></span><div><b>{operator.fullName || 'Operator'}</b><small>{operator.role?.replaceAll('_',' ') || 'Control room'}</small></div><button className="sign-out" onClick={signOut}>Sign Out</button></div>
    </header>
    <aside className="sidebar"><nav aria-label="Main navigation">{navigation.map(([name, icon, id]) => <button key={name} className={activeNav === name ? 'nav-active' : ''} onClick={() => navigate(name,id)}><Icon name={icon}/><span>{name}</span>{name === 'Incidents' && <i>4</i>}</button>)}</nav><div className="sidebar-footer"><Icon name="shield" size={26}/><b>SKYSENTINEL</b><small>SECURITY OPERATIONS</small><span>© 2026 SkySentinel Security</span></div></aside>
    <main className="workspace">
      <div className="preview-strip"><span><span className="demo-dot"/> DEMO WORKSPACE</span><span>Simulated operational data · Human authorization required</span></div>
      {notice && <div className="notice" role="status">{notice}<button aria-label="Dismiss notice" onClick={() => setNotice('')}>×</button></div>}
      <div className="stats-grid"><Stat icon="alert" title="Active incidents" value="4" detail="1 awaiting acknowledgement" color="red"/><Stat icon="drone" title="Drones" value={<>2 <em>/ 3</em></>} detail="2 available" color="green"/><Stat icon="mission" title="Active missions" value="0" detail="Awaiting authorization"/><Stat icon="team" title="Response teams" value="4" detail="2 available" color="green"/><Stat icon="site" title="Protected sites" value="5" detail="Shared · Dedicated · No drone"/><Stat icon="pulse" title="System status" value={state === 'connected' ? 'Online' : state === 'checking' ? 'Checking' : 'Offline'} detail={state === 'connected' ? 'Backend & database connected' : 'Check service connection'} color={state === 'connected' ? 'green' : 'amber'}/></div>
      <div className="main-grid">
        <Panel title="Live incidents" id="incidents" action={<span className="panel-meta">4 open</span>}><div className="incident-list">{incidents.map(incident => <button key={incident.id} className={`incident-card ${selected.id === incident.id ? 'incident-selected' : ''}`} onClick={() => chooseIncident(incident)} aria-pressed={selected.id === incident.id}><span className={`incident-icon ${badgeColor(incident.priority)}`}><Icon name="alert" size={24}/></span><span className="incident-copy"><b>{incident.site}</b><small>{incident.type}</small></span><span className="incident-status"><Badge value={incident.priority}/><time>{incident.time}</time><small className={badgeColor(incident.status)}>{incident.status}</small></span></button>)}</div><div className="panel-bottom"><span className="red">●</span> Operator acknowledgement required</div></Panel>
        <Panel title="Live map" id="map" className="map-panel" action={<span className="panel-meta">{location.status === 'granted' ? 'Current location' : 'Location required'} <button className="map-filter" onClick={requestLocation}>Refresh location</button></span>}><OperationsMap location={location} onRequestLocation={requestLocation}/></Panel>
        <Panel title="Selected incident" className="details-panel" action={<Badge value={selected.priority}/>}><div className="selected-banner"><Icon name="alert" size={29}/><div><h3>{selected.site}</h3><small>{selected.type}</small></div><span>{selected.id}</span></div><dl className="incident-details"><dt>Site:</dt><dd>{selected.site}</dd><dt>Location:</dt><dd>{selected.zone}</dd><dt>Device:</dt><dd>{selected.device}</dd><dt>Time:</dt><dd>Demo event · {selected.time}:32</dd><dt>Priority:</dt><dd><Badge value={selected.priority}/></dd><dt>Status:</dt><dd><Badge value={selected.status}/></dd><dt>Description:</dt><dd>{selected.description}</dd></dl><div className="incident-actions"><button disabled className="danger">Acknowledge</button><button disabled>Create Mission</button><button disabled className="success">Dispatch Team</button></div><p className="action-hint">Preview only · Operational actions are not enabled</p><div className="tabs" role="tablist" aria-label="Incident details">{['Location','Notes','Evidence','History'].map(name => <button role="tab" id={`tab-${name}`} aria-controls="incident-tab-panel" aria-selected={tab === name} key={name} onClick={() => setTab(name)}>{name}</button>)}</div><div role="tabpanel" id="incident-tab-panel" aria-labelledby={`tab-${tab}`} className="tab-content">{tab === 'Location' ? <div className="location-preview"><Icon name="pin" size={27}/><div><b>{selected.zone}</b><small>{selected.site} · Illustrative location</small></div></div> : tab === 'Notes' ? 'No operator notes in this preview.' : tab === 'Evidence' ? 'No evidence attached. Camera preview is illustrative only.' : `${selected.time} · Demo incident presented for operator review.`}</div></Panel>
      </div>
      <div className="operations-grid">
        <Panel title="Drone fleet" id="fleet" action={<span className="panel-meta">3 simulated</span>}>{drones.map(drone => <div className="resource-row" key={drone.code}><Icon name="drone" size={34}/><div className="resource-name"><b>{drone.code}</b><small>{drone.base}</small></div><span className={drone.battery < 40 ? 'amber' : 'green'}>▰ {drone.battery}%</span><Badge value={drone.state}/></div>)}<div className="fleet-summary"><span>Manufacturer-independent simulation</span><small>No physical aircraft connected</small></div></Panel>
        <Panel title="Active missions" id="missions" action={<span className="panel-meta">Human authorized</span>}><div className="mission-empty"><span className="mission-emblem"><Icon name="mission" size={32}/></span><h3>No active observation missions</h3><p>An operator must review an incident and explicitly authorize a mission.</p><span className="subtle-tag">NO AUTOMATIC LAUNCH</span></div></Panel>
        <Panel title="Response teams" id="teams" action={<span className="panel-meta">4 teams</span>}>{teams.map(team => <div className="resource-row team-row" key={team.name}><Icon name="car" size={28}/><div className="resource-name"><b>{team.name}</b><small>{team.zone}</small></div><Badge value={team.state}/></div>)}</Panel>
        <Panel title="Live drone feed" action={<span className="preview-badge">● DEMO</span>}><div className="feed-layout"><DemoFeed/><dl className="telemetry"><dt>Battery</dt><dd>76%</dd><dt>Altitude</dt><dd>—</dd><dt>Speed</dt><dd>—</dd><dt>Status</dt><dd className="cyan">PREVIEW</dd><dt>Signal</dt><dd>Not connected</dd></dl></div><div className="feed-footer"><span><Icon name="evidence"/> Camera placeholder</span><span className="green">SIMULATED</span></div></Panel>
      </div>
      <div className="bottom-grid"><Panel title="Recent activity"><ul className="activity-list"><li><time>19:14</time><span>Panic event preview · PB-WH-002</span><small>Demo</small></li><li><time>19:14</time><span>Incident shown · INC-10024</span><small>Demo</small></li><li><time>18:03</time><span>Perimeter alarm · Facility D</span><small>Demo</small></li><li><time>17:24</time><span>Response Team 02 · On scene</span><small>Demo</small></li></ul></Panel><Panel title="System alerts"><div className="system-alert"><Icon name="alert"/><div><b>High-priority incident</b><small>Warehouse B requires operator review</small></div></div><div className="system-alert amber"><Icon name="drone"/><div><b>SS-003 battery at 38%</b><small>Charging at dedicated base DB-002</small></div></div><div className={`system-alert ${state === 'connected' ? 'green' : 'amber'}`}><Icon name="shield"/><div><b role="status">{state === 'connected' ? 'Connected — backend and database healthy' : state === 'checking' ? 'Checking connection…' : 'Connection unavailable — check backend and database'}</b><button className="text-button" disabled={state === 'checking'} onClick={() => setAttempt(value => value + 1)}>Check again</button></div></div></Panel><Panel title="Live weather" action={<span className="panel-meta">{weather.status === 'ready' ? 'Current location' : 'Location based'}</span>}><WeatherPanel location={location} weather={weather} onRequestLocation={requestLocation}/></Panel><Panel title="Quick actions"><div className="quick-actions">{['New Incident','Register Panic Button','Add Drone','Add Site'].map(label => <button disabled key={label}>{label}</button>)}</div><small className="quick-hint">Available in later phases</small></Panel></div>
      <footer className="workspace-footer"><span>SKYSENTINEL SECURITY <span> / </span> OPERATIONS PREVIEW</span><span>People in control. Always.</span></footer>
    </main>
  </div>;
}
