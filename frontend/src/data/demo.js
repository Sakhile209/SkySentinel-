// Presentation fixtures only. These are not persisted incidents or operational resources.
export const incidents = [
  { id: 'INC-10024', site: 'Warehouse B', type: 'PANIC BUTTON', priority: 'HIGH', status: 'NEW', time: '19:14', zone: 'Loading bay · Zone B', device: 'PB-WH-002', description: 'Panic button activated at loading bay.', x: 46, y: 44 },
  { id: 'INC-10023', site: 'High-Risk Facility D', type: 'PERIMETER ALARM', priority: 'MEDIUM', status: 'ACKNOWLEDGED', time: '18:03', zone: 'East perimeter', device: 'Demo alarm', description: 'Perimeter alarm requires operator review.', x: 78, y: 30 },
  { id: 'INC-10022', site: 'Warehouse C', type: 'MANUAL REPORT', priority: 'MEDIUM', status: 'ACTIVE', time: '17:24', zone: 'Vehicle entrance', device: 'Control room', description: 'Guard requested assistance at the entrance.', x: 67, y: 70 },
  { id: 'INC-10021', site: 'Site E', type: 'MANUAL REPORT', priority: 'LOW', status: 'ACKNOWLEDGED', time: '16:11', zone: 'Main gate · No drone coverage', device: 'Control room', description: 'Human response requested. This site has no drone coverage.', x: 20, y: 72 },
];
export const drones = [
  { code: 'SS-001', base: 'Shared base DB-001', battery: 94, state: 'AVAILABLE' },
  { code: 'SS-002', base: 'Shared base DB-001', battery: 76, state: 'AVAILABLE' },
  { code: 'SS-003', base: 'Dedicated base DB-002', battery: 38, state: 'CHARGING' },
];
export const teams = [
  { name: 'Team 01', zone: 'North sector', state: 'AVAILABLE' },
  { name: 'Team 02', zone: 'Warehouse C', state: 'ON_SCENE' },
  { name: 'Team 03', zone: 'Central sector', state: 'DISPATCHED' },
  { name: 'Team 04', zone: 'South sector', state: 'AVAILABLE' },
];
