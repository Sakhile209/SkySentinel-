import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { beforeEach, describe, it, expect, vi } from 'vitest';
import App from './App.jsx';

const operator = { id: 1, email: 'ops@skysentinel.test', fullName: 'Ava Operator', role: 'CONTROL_ROOM_OPERATOR', badgeNumber: 'OP-101' };
const authResponse = { token: 'signed.jwt.token', user: operator };

beforeEach(() => {
  window.localStorage.clear();
  vi.restoreAllMocks();
  Object.defineProperty(navigator, 'geolocation', { value: undefined, configurable: true });
});

function mockAuthenticatedFetch(healthResponse = { ok: true, json: async () => ({ status: 'UP', database: 'UP' }) }) {
  vi.stubGlobal('fetch', vi.fn(async (url) => {
    if (url === '/api/auth/login') return { ok: true, json: async () => authResponse };
    if (url === '/api/auth/register') return { ok: true, json: async () => authResponse };
    if (url === '/api/auth/me') return { ok: true, json: async () => operator };
    if (url === '/api/health') return healthResponse;
    if (String(url).startsWith('https://api.open-meteo.com/')) return { ok: true, json: async () => ({ latitude: -26.2041, longitude: 28.0473, current: { temperature_2m: 21.4, relative_humidity_2m: 48, weather_code: 2, wind_speed_10m: 16.2, time: '2026-10-01T14:30' } }) };
    return { ok: false, status: 404, json: async () => ({ message: 'Not found' }) };
  }));
}

function mockLocationGranted() {
  Object.defineProperty(navigator, 'geolocation', {
    configurable: true,
    value: {
      getCurrentPosition: vi.fn(success => success({ coords: { latitude: -26.2041, longitude: 28.0473, accuracy: 22 } })),
    },
  });
}

function mockLocationDenied() {
  Object.defineProperty(navigator, 'geolocation', {
    configurable: true,
    value: {
      getCurrentPosition: vi.fn((success, error) => error({ code: 1, PERMISSION_DENIED: 1, message: 'Permission denied' })),
    },
  });
}

async function signIn() {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'ops@skysentinel.test' } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret123' } });
  fireEvent.click(screen.getByRole('button', { name: 'Enter Control Room' }));
  await screen.findByText('SECURITY OPERATIONS CENTRE');
}

describe('authentication gate', () => {
  it('requires sign in or sign up before showing the control room', () => {
    mockAuthenticatedFetch();
    render(<App />);
    expect(screen.getByRole('button', { name: 'Enter Control Room' })).toBeInTheDocument();
    expect(screen.queryByText('SECURITY OPERATIONS CENTRE')).not.toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalledWith('/api/health', expect.any(Object));
  });

  it('signs in and stores the returned operator session', async () => {
    mockAuthenticatedFetch();
    render(<App />);
    await signIn();
    await screen.findByText('Connected — backend and database healthy');
    expect(screen.getByText('SECURITY OPERATIONS CENTRE')).toBeInTheDocument();
    expect(screen.getByText('Ava Operator')).toBeInTheDocument();
    expect(JSON.parse(window.localStorage.getItem('skysentinel.auth')).token).toBe('signed.jwt.token');
    expect(fetch).toHaveBeenCalledWith('/api/auth/me', expect.objectContaining({ headers: { Authorization: 'Bearer signed.jwt.token' } }));
  });

  it('creates a new operator account from the sign up form', async () => {
    mockAuthenticatedFetch();
    render(<App />);
    fireEvent.click(screen.getByRole('tab', { name: 'Sign Up' }));
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Ava Operator' } });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'ops@skysentinel.test' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret123' } });
    fireEvent.change(screen.getByLabelText('Badge number'), { target: { value: 'OP-101' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create Account' }));
    await screen.findByText('SECURITY OPERATIONS CENTRE');
    expect(fetch).toHaveBeenCalledWith('/api/auth/register', expect.objectContaining({ method: 'POST' }));
  });
});

describe('foundation connectivity', () => {
  it('shows a real successful health response', async () => {
    mockAuthenticatedFetch();
    render(<App />);
    await signIn();
    expect(await screen.findByText('Connected — backend and database healthy')).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith('/api/health', expect.objectContaining({ headers: { Authorization: 'Bearer signed.jwt.token' } }));
  });

  it('shows failure and lets the operator retry', async () => {
    let healthCalls = 0;
    mockAuthenticatedFetch();
    fetch.mockImplementation(async (url) => {
      if (url === '/api/auth/login') return { ok: true, json: async () => authResponse };
      if (url === '/api/auth/me') return { ok: true, json: async () => operator };
      if (url === '/api/health') {
        healthCalls += 1;
        if (healthCalls === 1) throw new Error('Network down');
        return { ok: true, json: async () => ({ status: 'UP', database: 'UP' }) };
      }
      return { ok: false, status: 404, json: async () => ({ message: 'Not found' }) };
    });
    render(<App />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'ops@skysentinel.test' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Enter Control Room' }));
    await screen.findByText('SECURITY OPERATIONS CENTRE');
    expect(await screen.findByText(/Connection unavailable/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Check again' }));
    expect(await screen.findByText('Connected — backend and database healthy')).toBeInTheDocument();
  });

  it('does not report healthy when the API returns 503', async () => {
    mockAuthenticatedFetch({ ok: false, status: 503, json: async () => ({}) });
    render(<App />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'ops@skysentinel.test' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Enter Control Room' }));
    await screen.findByText('SECURITY OPERATIONS CENTRE');
    expect(await screen.findByText(/Connection unavailable/)).toBeInTheDocument();
  });
});

describe('location-based operations platform', () => {
  it('requests location and loads real map and weather data from the granted coordinates', async () => {
    mockLocationGranted();
    mockAuthenticatedFetch();
    render(<App />);
    await signIn();
    expect(await screen.findByLabelText('OpenStreetMap live map centered on your current location')).toBeInTheDocument();
    expect(await screen.findByText('21°C')).toBeInTheDocument();
    expect(screen.getByText('Partly cloudy')).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith(expect.stringContaining('https://api.open-meteo.com/v1/forecast?'), expect.any(Object));
  });

  it('clearly explains that location access is required when permission is denied', async () => {
    mockLocationDenied();
    mockAuthenticatedFetch();
    render(<App />);
    await signIn();
    expect(await screen.findAllByText('Location access required')).toHaveLength(2);
    expect(screen.getByText(/Enable location permission to load live weather/)).toBeInTheDocument();
  });

  it('keeps operational actions disabled in the demo workspace', async () => {
    mockAuthenticatedFetch();
    render(<App />);
    await signIn();
    expect(screen.getByText('DEMO WORKSPACE')).toBeInTheDocument();
    for (const name of ['Acknowledge', 'Create Mission', 'Dispatch Team', 'New Incident']) {
      expect(screen.getByRole('button', { name, exact: true })).toBeDisabled();
    }
    fireEvent.click(screen.getByRole('button', { name: 'Reports', exact: true }));
    expect(screen.getByText('Reports will be available in a later development phase.')).toBeInTheDocument();
  });
});
