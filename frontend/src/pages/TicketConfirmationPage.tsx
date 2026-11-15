import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { bookingApi } from '../api/endpoints';
import { BookingResponse } from '../types';
import { CheckCircle, Calendar, MapPin, Ticket, Printer, ArrowLeft, QrCode } from 'lucide-react';

export const TicketConfirmationPage: React.FC = () => {
  const { bookingReference } = useParams<{ bookingReference: string }>();
  const [booking, setBooking] = useState<BookingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchBooking = async () => {
      if (!bookingReference) return;
      try {
        setLoading(true);
        const data = await bookingApi.getBookingByReference(bookingReference);
        setBooking(data);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to load booking');
      } finally {
        setLoading(false);
      }
    };

    fetchBooking();
  }, [bookingReference]);

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-20 text-center">
        <div className="w-10 h-10 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
        <p className="text-slate-400 text-sm">Generating digital ticket...</p>
      </div>
    );
  }

  if (!booking) {
    return (
      <div className="max-w-md mx-auto px-4 py-20 text-center text-slate-400">
        Booking not found.
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-10">
      {/* Success Notification */}
      <div className="text-center mb-8">
        <div className="inline-flex p-3 rounded-2xl bg-emerald-950 text-emerald-400 border border-emerald-800 mb-4 shadow-xl shadow-emerald-950/50">
          <CheckCircle className="w-8 h-8" />
        </div>
        <h1 className="text-3xl font-extrabold text-white tracking-tight">Booking Confirmed!</h1>
        <p className="text-sm text-slate-400 mt-1">
          Your payment was processed and your seats are guaranteed.
        </p>
      </div>

      {/* Ticket Boarding Pass Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-3xl overflow-hidden shadow-2xl relative">
        <div className="bg-gradient-to-r from-indigo-600 to-purple-600 p-6 text-white">
          <div className="flex justify-between items-start">
            <div>
              <span className="text-xs uppercase tracking-widest font-semibold text-indigo-200">
                Official Digital Pass
              </span>
              <h2 className="text-2xl font-black mt-1">{booking.eventTitle}</h2>
            </div>
            <div className="text-right">
              <span className="text-xs text-indigo-200 uppercase font-semibold">Ref Code</span>
              <span className="block font-mono font-bold text-lg">{booking.bookingReference}</span>
            </div>
          </div>
        </div>

        <div className="p-6 sm:p-8 space-y-6">
          <div className="grid grid-cols-2 gap-4 pb-6 border-b border-slate-800">
            <div>
              <span className="text-xs text-slate-500 uppercase tracking-wider block mb-1">Date & Time</span>
              <div className="flex items-center gap-1.5 text-sm font-semibold text-slate-200">
                <Calendar className="w-4 h-4 text-indigo-400" />
                <span>{new Date(booking.eventStartTime).toLocaleString()}</span>
              </div>
            </div>

            <div>
              <span className="text-xs text-slate-500 uppercase tracking-wider block mb-1">Venue</span>
              <div className="flex items-center gap-1.5 text-sm font-semibold text-slate-200">
                <MapPin className="w-4 h-4 text-indigo-400" />
                <span>{booking.venueName}</span>
              </div>
            </div>
          </div>

          {/* Reserved Seats List */}
          <div>
            <span className="text-xs text-slate-400 uppercase tracking-wider font-semibold block mb-3">
              Reserved Seating
            </span>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {booking.tickets.map((ticket, idx) => (
                <div
                  key={idx}
                  className="bg-slate-950 p-3 rounded-xl border border-slate-800 flex items-center justify-between"
                >
                  <div className="flex items-center gap-2">
                    <Ticket className="w-4 h-4 text-indigo-400" />
                    <div>
                      <span className="text-sm font-bold text-white">
                        Sec {ticket.section}, R{ticket.rowNumber}-S{ticket.seatNumber}
                      </span>
                      <span className="block text-xs text-slate-500">{ticket.seatType}</span>
                    </div>
                  </div>
                  <span className="font-mono text-sm font-semibold text-slate-300">
                    ${ticket.price.toFixed(2)}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Payment & QR */}
          <div className="pt-6 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-6">
            <div>
              <span className="text-xs text-slate-500 uppercase tracking-wider block mb-1">Payment Reference</span>
              <span className="font-mono text-xs font-semibold text-slate-300">{booking.paymentReference}</span>
              <div className="mt-2 text-xs text-emerald-400 font-semibold flex items-center gap-1">
                <CheckCircle className="w-3.5 h-3.5" /> Paid with {booking.paymentMethod}
              </div>
            </div>

            <div className="p-3 bg-white rounded-2xl flex items-center justify-center shadow-lg">
              <QrCode className="w-20 h-20 text-slate-950" />
            </div>
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="mt-8 flex gap-4 justify-center">
        <button
          onClick={() => window.print()}
          className="bg-slate-800 hover:bg-slate-700 text-slate-200 px-5 py-2.5 rounded-xl text-sm font-semibold transition-colors flex items-center gap-2"
        >
          <Printer className="w-4 h-4" />
          Print Ticket
        </button>
        <Link
          to="/my-bookings"
          className="bg-indigo-600 hover:bg-indigo-500 text-white px-5 py-2.5 rounded-xl text-sm font-semibold transition-colors flex items-center gap-2"
        >
          <ArrowLeft className="w-4 h-4" />
          View All Bookings
        </Link>
      </div>
    </div>
  );
};
