import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { bookingApi } from '../api/endpoints';
import { BookingResponse } from '../types';
import { Calendar, MapPin, Ticket, AlertCircle, Eye, XCircle } from 'lucide-react';

export const MyBookingsPage: React.FC = () => {
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancellingRef, setCancellingRef] = useState<string | null>(null);

  const loadBookings = async () => {
    try {
      setLoading(true);
      const data = await bookingApi.getMyBookings();
      setBookings(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch bookings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBookings();
  }, []);

  const handleCancel = async (bookingReference: string) => {
    if (!window.confirm('Are you sure you want to cancel this booking? A refund will be initiated and seats released.')) {
      return;
    }

    try {
      setCancellingRef(bookingReference);
      await bookingApi.cancelBooking(bookingReference);
      await loadBookings();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel booking');
    } finally {
      setCancellingRef(null);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">My Bookings</h1>
          <p className="text-sm text-slate-400 mt-1">Manage your active tickets and past orders</p>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-950/80 border border-rose-800 text-rose-300 text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="space-y-4">
          {[1, 2].map((n) => (
            <div key={n} className="h-32 rounded-2xl bg-slate-900 border border-slate-800 animate-pulse"></div>
          ))}
        </div>
      ) : bookings.length === 0 ? (
        <div className="text-center py-20 bg-slate-900/40 rounded-3xl border border-slate-800">
          <Ticket className="w-12 h-12 text-slate-600 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-slate-300 mb-1">No bookings yet</h3>
          <p className="text-slate-500 text-sm mb-6">Discover events and book your favorite seats.</p>
          <Link
            to="/"
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-semibold px-6 py-2.5 rounded-xl text-sm transition-colors"
          >
            Browse Events
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {bookings.map((booking) => {
            const isConfirmed = booking.status === 'CONFIRMED';
            const isEventFuture = new Date(booking.eventStartTime).getTime() > Date.now();

            return (
              <div
                key={booking.bookingReference}
                className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col md:flex-row md:items-center justify-between gap-6 hover:border-slate-700 transition-colors"
              >
                <div>
                  <div className="flex items-center gap-3 mb-2">
                    <span className="font-mono font-bold text-sm text-indigo-400">
                      {booking.bookingReference}
                    </span>
                    <span
                      className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                        isConfirmed
                          ? 'bg-emerald-950 text-emerald-400 border border-emerald-800'
                          : 'bg-rose-950 text-rose-400 border border-rose-800'
                      }`}
                    >
                      {booking.status}
                    </span>
                  </div>

                  <h3 className="text-xl font-bold text-white mb-2">{booking.eventTitle}</h3>

                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-400">
                    <div className="flex items-center gap-1.5">
                      <Calendar className="w-4 h-4 text-indigo-400" />
                      <span>{new Date(booking.eventStartTime).toLocaleString()}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <MapPin className="w-4 h-4 text-indigo-400" />
                      <span>{booking.venueName}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Ticket className="w-4 h-4 text-indigo-400" />
                      <span>{booking.tickets.length} seat(s)</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-4 border-t md:border-t-0 pt-4 md:pt-0 border-slate-800">
                  <div className="text-right mr-4">
                    <span className="text-xs text-slate-500 uppercase block">Total</span>
                    <span className="text-xl font-mono font-extrabold text-white">
                      ${booking.totalAmount.toFixed(2)}
                    </span>
                  </div>

                  <Link
                    to={`/tickets/${booking.bookingReference}`}
                    className="bg-slate-800 hover:bg-slate-700 text-slate-200 px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-colors"
                  >
                    <Eye className="w-4 h-4" />
                    View Ticket
                  </Link>

                  {isConfirmed && isEventFuture && (
                    <button
                      onClick={() => handleCancel(booking.bookingReference)}
                      disabled={cancellingRef === booking.bookingReference}
                      className="bg-rose-950/60 hover:bg-rose-900 text-rose-300 border border-rose-800 px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-colors"
                    >
                      <XCircle className="w-4 h-4" />
                      {cancellingRef === booking.bookingReference ? 'Cancelling...' : 'Cancel'}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
