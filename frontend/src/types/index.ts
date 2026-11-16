export type Role = 'ROLE_CUSTOMER' | 'ROLE_ADMIN';

export interface User {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string;
  role: Role;
  active: boolean;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  user: User;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export type EventCategory = 'CONCERT' | 'SPORTS' | 'THEATER' | 'CONFERENCE' | 'FESTIVAL';
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';

export interface Event {
  id: number;
  venueId: number;
  venueName: string;
  venueCity: string;
  title: string;
  description: string;
  category: EventCategory;
  startTime: string;
  endTime: string;
  status: EventStatus;
  totalSeats: number;
  availableSeats: number;
  createdAt: string;
}

export type SeatType = 'STANDARD' | 'VIP' | 'ACCESSIBLE';
export type SeatStatus = 'AVAILABLE' | 'HELD' | 'BOOKED' | 'BLOCKED';

export interface EventSeat {
  id: number;
  eventId: number;
  seatId: number;
  section: string;
  rowNumber: string;
  seatNumber: string;
  seatType: SeatType;
  price: number;
  status: SeatStatus;
  version: number;
}

export interface ReservationResponse {
  reservationToken: string;
  eventId: number;
  eventTitle: string;
  userId: number;
  seatIds: number[];
  totalAmount: number;
  expiresAt: string;
  expiresInSeconds: number;
  status: 'ACTIVE' | 'EXPIRED' | 'CONFIRMED' | 'CANCELLED';
}

export type PaymentMethod = 'CARD' | 'UPI' | 'NET_BANKING' | 'DIGITAL_WALLET';
export type BookingStatus = 'CONFIRMED' | 'CANCELLED' | 'REFUNDED';

export interface TicketItem {
  eventSeatId: number;
  section: string;
  rowNumber: string;
  seatNumber: string;
  seatType: SeatType;
  price: number;
}

export interface BookingResponse {
  bookingReference: string;
  eventId: number;
  eventTitle: string;
  venueName: string;
  venueAddress: string;
  eventStartTime: string;
  status: BookingStatus;
  totalAmount: number;
  paymentReference: string;
  paymentMethod: PaymentMethod;
  tickets: TicketItem[];
  createdAt: string;
}

export interface Venue {
  id: number;
  name: string;
  address: string;
  city: string;
  state?: string;
  country: string;
  postalCode?: string;
  totalCapacity: number;
  configuredSeatsCount: number;
}

export interface AdminStats {
  totalVenues: number;
  totalEvents: number;
  totalBookings: number;
  totalTicketsSold: number;
  totalGrossRevenue: number;
  activeHoldsCount: number;
}
