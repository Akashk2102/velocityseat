import api from './client';
import {
  AdminStats,
  ApiResponse,
  AuthResponse,
  BookingResponse,
  Event,
  EventSeat,
  ReservationResponse,
  Venue,
} from '../types';

export const authApi = {
  login: async (email: string, password: string) => {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/login', { email, password });
    return res.data.data;
  },
  register: async (data: { email: string; password: string; fullName: string; phoneNumber?: string; role?: string }) => {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/register', data);
    return res.data.data;
  },
  getProfile: async () => {
    const res = await api.get<ApiResponse<any>>('/users/me');
    return res.data.data;
  },
};

export const eventApi = {
  getEvents: async (params?: { category?: string; keyword?: string }) => {
    const res = await api.get<ApiResponse<Event[]>>('/events', { params });
    return res.data.data;
  },
  getEventById: async (id: number) => {
    const res = await api.get<ApiResponse<Event>>(`/events/${id}`);
    return res.data.data;
  },
  getEventSeats: async (id: number) => {
    const res = await api.get<ApiResponse<EventSeat[]>>(`/events/${id}/seats`);
    return res.data.data;
  },
  getAvailableSeats: async (id: number) => {
    const res = await api.get<ApiResponse<EventSeat[]>>(`/events/${id}/seats/available`);
    return res.data.data;
  },
  createEvent: async (data: any) => {
    const res = await api.post<ApiResponse<Event>>('/events', data);
    return res.data.data;
  },
};

export const reservationApi = {
  holdSeats: async (eventId: number, seatIds: number[]) => {
    const res = await api.post<ApiResponse<ReservationResponse>>('/reservations/hold', {
      eventId,
      seatIds,
    });
    return res.data.data;
  },
  releaseReservation: async (token: string) => {
    const res = await api.post<ApiResponse<void>>(`/reservations/${token}/release`);
    return res.data.data;
  },
  getReservation: async (token: string) => {
    const res = await api.get<ApiResponse<ReservationResponse>>(`/reservations/${token}`);
    return res.data.data;
  },
};

export const bookingApi = {
  confirmBooking: async (
    reservationToken: string,
    paymentMethod: string,
    idempotencyKey: string
  ) => {
    const res = await api.post<ApiResponse<BookingResponse>>(
      '/bookings/confirm',
      { reservationToken, paymentMethod },
      {
        headers: {
          'Idempotency-Key': idempotencyKey,
        },
      }
    );
    return res.data.data;
  },
  getMyBookings: async () => {
    const res = await api.get<ApiResponse<BookingResponse[]>>('/bookings/my-bookings');
    return res.data.data;
  },
  getBookingByReference: async (reference: string) => {
    const res = await api.get<ApiResponse<BookingResponse>>(`/bookings/${reference}`);
    return res.data.data;
  },
  cancelBooking: async (reference: string) => {
    const res = await api.post<ApiResponse<void>>(`/bookings/${reference}/cancel`);
    return res.data.data;
  },
};

export const venueApi = {
  getVenues: async () => {
    const res = await api.get<ApiResponse<Venue[]>>('/venues');
    return res.data.data;
  },
  createVenue: async (data: any) => {
    const res = await api.post<ApiResponse<Venue>>('/venues', data);
    return res.data.data;
  },
  generateSeats: async (venueId: number, data: any) => {
    const res = await api.post<ApiResponse<any[]>>(`/venues/${venueId}/seats/generate`, data);
    return res.data.data;
  },
};

export const adminApi = {
  getStats: async () => {
    const res = await api.get<ApiResponse<AdminStats>>('/admin/stats');
    return res.data.data;
  },
};
