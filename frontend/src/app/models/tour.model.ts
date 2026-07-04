export type TransportType = 'bike' | 'hike' | 'running' | 'vehicle';

export interface Tour {
  id: string;
  name: string;
  description: string;
  from: string;
  to: string;
  transportType: TransportType;
  fromLat: number | null;
  fromLng: number | null;
  toLat: number | null;
  toLng: number | null;
  distance: number;
  estimatedTime: number;
  childFriendliness: number;
  difficulty?: number | null;
  ownerUserId?: number | null;
  creatorName?: string | null;
  routeImagePath: string;
  routeGeometry: string | null;
  createdAt: Date;
}

export interface WeatherForecast {
  tourId: string;
  locationName: string | null;
  days: WeatherForecastDay[];
}

export interface WeatherForecastDay {
  date: string;
  temperature: number;
  minTemperature: number;
  maxTemperature: number;
  description: string;
  icon: string;
  humidity?: number | null;
  windSpeed?: number | null;
}

export const TRANSPORT_TYPES: { value: TransportType; label: string }[] = [
  { value: 'bike', label: 'Bike' },
  { value: 'hike', label: 'Hike' },
  { value: 'running', label: 'Running' },
  { value: 'vehicle', label: 'Vehicle' },
];
