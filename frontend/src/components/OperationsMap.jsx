import React from 'react';
import Icon from './Icon.jsx';
import { incidents } from '../data/demo.js';

export default function OperationsMap({ selected, onSelect }) {
  return <div className="operations-map">
    <svg className="map-art" viewBox="0 0 700 440" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <defs><pattern id="blocks" width="85" height="65" patternUnits="userSpaceOnUse" patternTransform="rotate(-22)"><rect width="85" height="65" fill="#111e22"/><path d="M0 0H85V65" fill="none" stroke="#3c4442" strokeWidth="5"/><path d="M8 8h24v15H8Zm33 0h33v15H41ZM8 33h39v23H8Zm48 0h18v23H56Z" fill="#26332f" stroke="#48504a" strokeWidth="1"/><path d="M13 12h15m18 0h22M13 39h27" stroke="#53574a" strokeWidth="2"/></pattern><radialGradient id="map-vignette"><stop offset="30%" stopColor="#00121b" stopOpacity="0"/><stop offset="100%" stopColor="#00121b" stopOpacity=".7"/></radialGradient></defs>
      <rect width="700" height="440" fill="url(#blocks)"/>
      <path d="M-10 280Q160 310 220 210T470 140T720 40" fill="none" stroke="#233d38" strokeWidth="62"/>
      <path d="M-10 280Q160 310 220 210T470 140T720 40" fill="none" stroke="#375b4b" strokeWidth="33"/>
      <g fill="none" stroke="#858070" strokeWidth="5"><path d="M-20 380 180 290 290 40 400-20M130 480 360 275 740 300M-20 110 250 165 520 460"/><path d="m530-20-55 210 200 230"/></g>
      <g fill="none" stroke="#bcaa74" strokeWidth="1.5"><path d="M-20 380 180 290 290 40 400-20M130 480 360 275 740 300M-20 110 250 165 520 460"/><path d="m530-20-55 210 200 230"/></g>
      <path d="m125 100 245-55 200 113-12 170-294 34-135-115Z" fill="#08799b" fillOpacity=".07" stroke="#3690a3" strokeDasharray="7 6"/>
      <rect width="700" height="440" fill="url(#map-vignette)"/>
      <g fill="#8d9e9e" fontFamily="sans-serif" fontSize="11"><text x="250" y="105">INDUSTRIAL AREA NORTH</text><text x="310" y="373">North access road</text><text x="44" y="240" transform="rotate(-24 44 240)">Service road</text></g>
    </svg>
    <span className="map-label">SIMULATED AREA · NOT FLIGHT COVERAGE</span>
    <div className="map-base" style={{ left: '26%', top: '24%' }}><Icon name="drone" size={25}/><span>DB-001<small>SS-001 · Available</small></span></div>
    <div className="map-base" style={{ left: '75%', top: '12%' }}><Icon name="drone" size={23}/><span>DB-002<small>Dedicated base</small></span></div>
    <div className="map-site" style={{ left: '29%', top: '53%' }}><Icon name="site"/><small>Warehouse A</small></div>
    {incidents.map(incident => <button key={incident.id} className={`map-marker ${incident.priority.toLowerCase()} ${selected.id === incident.id ? 'selected' : ''}`} style={{ left: `${incident.x}%`, top: `${incident.y}%` }} onClick={() => onSelect(incident)} aria-label={`Select ${incident.site} on map`}><Icon name={incident.priority === 'HIGH' ? 'alert' : 'site'} size={21}/><span>{incident.site}</span></button>)}
    <div className="map-team"><Icon name="car"/><span>Response Team 03<small>Dispatched · demo</small></span></div>
    <div className="map-legend"><span className="red">● Incident</span><span className="green">● Drone base</span><span className="amber">● Response team</span><span className="cyan">● Site</span></div>
  </div>;
}
