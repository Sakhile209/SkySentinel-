import React from 'react';
const paths = {
  dashboard: 'M3 10 12 3l9 7v11h-6v-7H9v7H3Z',
  map: 'm3 5 6-2 6 2 6-2v16l-6 2-6-2-6 2Zm6-2v16m6-14v16',
  alert: 'm12 3 10 18H2Zm0 6v5m0 3v1',
  drone: 'M8 8h8v8H8Zm-4-4 4 4m8 8 4 4M4 20l4-4m8-8 4-4M1 4h6M4 1v6m13-3h6m-3-3v6M1 20h6m-3-3v6m13-3h6m-3-3v6',
  mission: 'm3 10 18-7-7 18-3-8Zm8 3L21 3',
  team: 'M16 21v-3a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v3m16-7a4 4 0 0 1 4 4v3M9 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8m8 0a4 4 0 0 1 0 8',
  site: 'M4 21V7h10v14m0-11h6v11M7 10h4m-4 4h4m-4 4h4M2 21h20M7 7V3h4v4',
  pulse: 'M2 12h5l3-9 4 18 3-9h5',
  pin: 'M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0ZM15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  report: 'M5 2h10l4 4v16H5Zm9 0v5h5M8 11h8m-8 4h8m-8 4h5',
  evidence: 'M3 6h5l2-3h4l2 3h5v15H3Zm13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  settings: 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8M12 2v3m0 14v3M2 12h3m14 0h3M5 5l2 2m10 10 2 2M5 19l2-2M17 7l2-2',
  bell: 'M5 17h14l-2-4V8a5 5 0 0 0-10 0v5Zm5 3h4',
  mail: 'M2 5h20v14H2Zm0 0 10 8L22 5',
  car: 'm3 10 3-6h12l3 6v9H3Zm0 0h18M6 14h2m8 0h2M5 19v3m14-3v3',
  cloud: 'M6 19a5 5 0 1 1 0-10 7 7 0 0 1 13-1 5.5 5.5 0 0 1 0 11Z',
  shield: 'm12 2 9 4v6c0 6-9 10-9 10S3 18 3 12V6Zm-4 10 3 3 5-6',
};
export default function Icon({ name, size = 18, ...props }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}><path d={paths[name] || paths.report}/></svg>;
}
