import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import App from './App.jsx';

describe('foundation connectivity', () => {
  it('shows a real successful health response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ status: 'UP', database: 'UP' }) }));
    render(<App />);
    expect(await screen.findByText('Connected — backend and database healthy')).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith('/api/health', expect.any(Object));
  });

  it('shows failure and lets the operator retry', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValueOnce(new Error('Network down')).mockResolvedValueOnce({ ok: true, json: async () => ({ status: 'UP', database: 'UP' }) }));
    render(<App />);
    expect(await screen.findByText(/Connection unavailable/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Check again' }));
    expect(await screen.findByText('Connected — backend and database healthy')).toBeInTheDocument();
  });

  it('does not report healthy when the API returns 503', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 503 }));
    render(<App />);
    expect(await screen.findByText(/Connection unavailable/)).toBeInTheDocument();
  });
});

describe('operations dashboard preview', () => {
  it('selects an incident from the map and shows its evidence tab', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ status: 'UP', database: 'UP' }) }));
    render(<App />);
    await screen.findByText('Connected — backend and database healthy');
    fireEvent.click(screen.getByRole('button', { name: 'Select Site E on map' }));
    expect(screen.getByText('Human response requested. This site has no drone coverage.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Evidence' }));
    expect(screen.getByRole('tabpanel')).toHaveTextContent('No evidence attached');
    expect(screen.getByRole('tab', { name: 'Evidence' })).toHaveAttribute('aria-selected', 'true');
  });

  it('keeps operational actions disabled in the demo workspace', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ status: 'UP', database: 'UP' }) }));
    render(<App />);
    await screen.findByText('Connected — backend and database healthy');
    expect(screen.getByText('DEMO WORKSPACE')).toBeInTheDocument();
    for (const name of ['Acknowledge', 'Create Mission', 'Dispatch Team', 'New Incident']) {
      expect(screen.getByRole('button', { name, exact: true })).toBeDisabled();
    }
    fireEvent.click(screen.getByRole('button', { name: 'Reports', exact: true }));
    expect(screen.getByText('Reports will be available in a later development phase.')).toBeInTheDocument();
  });
});
