export type TransportType = 'bike' | 'hike' | 'running' | 'vacation';

export interface Tour {
  id: string;
  name: string;
  description: string;
  from: string;
  to: string;
  transportType: TransportType;
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
  { value: 'vacation', label: 'Vacation' },
];
