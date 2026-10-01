import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { beforeEach, describe, it, expect, vi } from 'vitest';
import App from './App.jsx';

const operator = { id: 1, email: 'ops@skysentinel.test', fullName: 'Ava Operator', role: 'CONTROL_ROOM_OPERATOR', badgeNumber: 'OP-101' };
const authResponse = { token: 'signed.jwt.token', user: operator };

beforeEach(() => {
  window.localStorage.clear();
  vi.restoreAllMocks();
});

function mockAuthenticatedFetch(healthResponse = { ok: true, json: async () => ({ status: 'UP', database: 'UP' }) }) {
  vi.stubGlobal('fetch', vi.fn(async (url) => {
    if (url === '/api/auth/login') return { ok: true, json: async () => authResponse };
    if (url === '/api/auth/register') return { ok: true, json: async () => authResponse };
    if (url === '/api/auth/me') return { ok: true, json: async () => operator };
    if (url === '/api/health') return healthResponse;
    return { ok: false, status: 404, json: async () => ({ message: 'Not found' }) };
  }));
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

describe('operations dashboard preview', () => {
  it('selects an incident from the map and shows its evidence tab', async () => {
    mockAuthenticatedFetch();
    render(<App />);
    await signIn();
    fireEvent.click(screen.getByRole('button', { name: 'Select Site E on map' }));
    expect(screen.getByText('Human response requested. This site has no drone coverage.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Evidence' }));
    expect(screen.getByRole('tabpanel')).toHaveTextContent('No evidence attached');
    expect(screen.getByRole('tab', { name: 'Evidence' })).toHaveAttribute('aria-selected', 'true');
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
