export type TransportType = 'bike' | 'hike' | 'running' | 'vehicle';

export interface Tour {
  id: string;
  name: string;
  description: string;
  from: string;
  to: string;
  transportType: TransportType;
  // Exact coordinates of the picked autocomplete suggestions. Null when the
  // user typed a free-text address — the backend then geocodes it as fallback.
  fromLat: number | null;
  fromLng: number | null;
  toLat: number | null;
  toLng: number | null;
  distance: number;
  estimatedTime: number;      // seconds — formatDuration() converts to h/min display
  childFriendliness: number;
  routeImagePath: string;
  routeGeometry: string | null; // [[lat,lng],...] as JSON — null if ORS failed
  createdAt: Date;
}

export const TRANSPORT_TYPES: { value: TransportType; label: string }[] = [
  { value: 'bike', label: 'Bike' },
  { value: 'hike', label: 'Hike' },
  { value: 'running', label: 'Running' },
  { value: 'vehicle', label: 'Vehicle' },
];
