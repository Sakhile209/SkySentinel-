import React, { useEffect, useMemo, useState } from 'react';
import Icon from './Icon.jsx';

const TILE_SIZE = 256;

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function lngToTileX(lng, zoom) {
  return ((lng + 180) / 360) * 2 ** zoom;
}

function latToTileY(lat, zoom) {
  const radians = lat * Math.PI / 180;
  return (1 - Math.log(Math.tan(radians) + 1 / Math.cos(radians)) / Math.PI) / 2 * 2 ** zoom;
}

function tileXToLng(x, zoom) {
  return x / 2 ** zoom * 360 - 180;
}

function tileYToLat(y, zoom) {
  const radians = Math.atan(Math.sinh(Math.PI * (1 - 2 * y / 2 ** zoom)));
  return radians * 180 / Math.PI;
}

function formatCoord(value, axis) {
  const direction = axis === 'lat' ? value >= 0 ? 'N' : 'S' : value >= 0 ? 'E' : 'W';
  return `${Math.abs(value).toFixed(5)} ${direction}`;
}

export default function OperationsMap({ location, onRequestLocation }) {
  const [zoom, setZoom] = useState(15);
  const [center, setCenter] = useState(null);

  useEffect(() => {
    if (location?.coords) {
      setCenter({ lat: location.coords.latitude, lng: location.coords.longitude });
    }
  }, [location?.coords?.latitude, location?.coords?.longitude]);

  const mapModel = useMemo(() => {
    if (!center) return null;
    const centerX = lngToTileX(center.lng, zoom);
    const centerY = latToTileY(center.lat, zoom);
    const baseX = Math.floor(centerX) - 1;
    const baseY = Math.floor(centerY) - 1;
    const offsetX = (centerX - Math.floor(centerX)) * TILE_SIZE;
    const offsetY = (centerY - Math.floor(centerY)) * TILE_SIZE;
    const tiles = [];
    for (let row = 0; row < 4; row += 1) {
      for (let col = 0; col < 4; col += 1) {
        const x = baseX + col;
        const y = baseY + row;
        const max = 2 ** zoom;
        if (y >= 0 && y < max) {
          tiles.push({
            key: `${zoom}-${x}-${y}`,
            src: `https://tile.openstreetmap.org/${zoom}/${((x % max) + max) % max}/${y}.png`,
            left: col * TILE_SIZE - offsetX,
            top: row * TILE_SIZE - offsetY,
          });
        }
      }
    }
    return { tiles };
  }, [center, zoom]);

  const pan = (latDelta, lngDelta) => {
    setCenter(value => value ? {
      lat: clamp(value.lat + latDelta, -85, 85),
      lng: clamp(value.lng + lngDelta, -180, 180),
    } : value);
  };

  if (location?.status === 'denied') {
    return <div className="operations-map real-map map-empty">
      <Icon name="pin" size={34}/>
      <h3>Location access required</h3>
      <p>Enable location permission to show your current position on the live map and load location-based weather.</p>
      <button onClick={onRequestLocation}>Enable Location</button>
    </div>;
  }

  if (location?.status === 'unsupported') {
    return <div className="operations-map real-map map-empty">
      <Icon name="pin" size={34}/>
      <h3>Location unavailable</h3>
      <p>This browser does not support geolocation. Live map and weather features require location access.</p>
    </div>;
  }

  if (!center || location?.status === 'requesting') {
    return <div className="operations-map real-map map-empty">
      <Icon name="map" size={34}/>
      <h3>Waiting for location permission</h3>
      <p>SkySentinel needs your current location to load real map tiles and weather for this control room.</p>
      <button onClick={onRequestLocation}>{location?.status === 'requesting' ? 'Requesting...' : 'Enable Location'}</button>
    </div>;
  }

  const panStep = 0.0035 * (16 - zoom);

  return <div className="operations-map real-map">
    <div className="osm-tiles" aria-label="OpenStreetMap live map centered on your current location">
      {mapModel.tiles.map(tile => <img key={tile.key} src={tile.src} alt="" style={{ left: tile.left, top: tile.top }} draggable="false" />)}
      <div className="user-location-marker" aria-label="Your current location"><span /></div>
    </div>
    <div className="map-label">OPENSTREETMAP · LIVE MAP DATA</div>
    <div className="map-coordinate-card">
      <b>Your location</b>
      <small>{formatCoord(center.lat, 'lat')} · {formatCoord(center.lng, 'lng')}</small>
      {location.coords?.accuracy && <small>Accuracy ±{Math.round(location.coords.accuracy)} m</small>}
    </div>
    <div className="map-controls" aria-label="Map controls">
      <button aria-label="Zoom in" onClick={() => setZoom(value => clamp(value + 1, 3, 18))}>+</button>
      <button aria-label="Zoom out" onClick={() => setZoom(value => clamp(value - 1, 3, 18))}>−</button>
      <button aria-label="Recenter map on current location" onClick={() => setCenter({ lat: location.coords.latitude, lng: location.coords.longitude })}><Icon name="pin" size={15}/></button>
    </div>
    <div className="map-pan-controls" aria-label="Pan map controls">
      <button aria-label="Pan north" onClick={() => pan(panStep, 0)}>↑</button>
      <button aria-label="Pan west" onClick={() => pan(0, -panStep)}>←</button>
      <button aria-label="Pan east" onClick={() => pan(0, panStep)}>→</button>
      <button aria-label="Pan south" onClick={() => pan(-panStep, 0)}>↓</button>
    </div>
    <div className="map-legend"><span className="cyan">● Current location</span><span>© OpenStreetMap contributors</span></div>
  </div>;
}
