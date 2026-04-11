export type TransportType = 'bike' | 'hike' | 'running' | 'vacation';

export interface Tour {
  id: string;
  name: string;
  description: string;
  from: string;
  to: string;
  transportType: TransportType;
  distance: number;
  estimatedTime: number;
  routeImagePath: string;
  createdAt: Date;
}

export const TRANSPORT_TYPES: { value: TransportType; label: string }[] = [
  { value: 'bike', label: 'Bike' },
  { value: 'hike', label: 'Hike' },
  { value: 'running', label: 'Running' },
  { value: 'vacation', label: 'Vacation' },
];
