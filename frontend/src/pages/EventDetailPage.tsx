import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { eventApi, reservationApi } from '../api/endpoints';
import { Event, EventSeat } from '../types';
import { SeatMap } from '../components/SeatMap';
import { useAuth } from '../context/AuthContext';
import { Calendar, MapPin, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';

export const EventDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [event, setEvent] = useState<Event | null>(null);
  const [seats, setSeats] = useState<EventSeat[]>([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [holding, setHolding] = useState(false);
  const [error, setError] = useState('');

  const eventId = Number(id);

  const loadEventAndSeats = async () => {
    try {
      setLoading(true);
      const [eventData, seatsData] = await Promise.all([
        eventApi.getEventById(eventId),
        eventApi.getEventSeats(eventId),
      ]);
      setEvent(eventData);
      setSeats(seatsData);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load event details');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEventAndSeats();
  }, [eventId]);

  const handleToggleSeat = (seat: EventSeat) => {
    setError('');
    if (selectedSeatIds.includes(seat.id)) {
      setSelectedSeatIds(selectedSeatIds.filter((sid) => sid !== seat.id));
    } else {
      if (selectedSeatIds.length >= 6) {
        setError('Maximum 6 seats can be selected per transaction');
        return;
      }
      setSelectedSeatIds([...selectedSeatIds, seat.id]);
    }
  };

  const selectedSeats = seats.filter((s) => selectedSeatIds.includes(s.id));
  const totalPrice = selectedSeats.reduce((sum, s) => sum + s.price, 0);

  const handleProceedToHold = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    if (selectedSeatIds.length === 0) {
      setError('Please select at least one available seat');
      return;
    }

    setHolding(true);
    setError('');

    try {
      // 10-minute hold in Redis
      const reservation = await reservationApi.holdSeats(eventId, selectedSeatIds);
      navigate(`/checkout/${reservation.reservationToken}`);
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
          'Failed to reserve seats. Someone else may have grabbed them. Refreshing...'
      );
      // Reload live seat statuses on collision
      loadEventAndSeats();
      setSelectedSeatIds([]);
    } finally {
      setHolding(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-20 text-center">
        <div className="w-12 h-12 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
        <p className="text-slate-400 text-sm">Loading seat map & live inventory...</p>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-20 text-center text-slate-400">
        Event not found.
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Event Header Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 mb-8 flex flex-col md:flex-row md:items-center justify-between gap-6 shadow-xl">
        <div>
          <span className="px-3 py-1 rounded-md bg-indigo-950 text-indigo-400 border border-indigo-800 text-xs font-semibold uppercase tracking-wider">
            {event.category}
          </span>
          <h1 className="text-3xl font-extrabold text-white mt-2 mb-3 tracking-tight">
            {event.title}
          </h1>
          <div className="flex flex-wrap items-center gap-4 text-xs sm:text-sm text-slate-400">
            <div className="flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-indigo-400" />
              <span>{new Date(event.startTime).toLocaleString()}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <MapPin className="w-4 h-4 text-indigo-400" />
              <span>
                {event.venueName}, {event.venueCity}
              </span>
            </div>
          </div>
        </div>

        <div className="text-right">
          <span className="text-xs text-slate-500 uppercase tracking-wider block mb-1">Live Availability</span>
          <span className="text-2xl font-black text-emerald-400">
            {event.availableSeats}{' '}
            <span className="text-sm font-semibold text-slate-400">/ {event.totalSeats} seats</span>
          </span>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-950/80 border border-rose-800 text-rose-300 text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Main Grid: Seat Map + Sticky Cart */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <SeatMap
            seats={seats}
            selectedSeatIds={selectedSeatIds}
            onToggleSeat={handleToggleSeat}
          />
        </div>

        {/* Sidebar Summary */}
        <div className="lg:col-span-1">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sticky top-24 shadow-xl">
            <h3 className="text-lg font-bold text-white mb-4">Your Selection</h3>

            {selectedSeats.length === 0 ? (
              <p className="text-sm text-slate-500 py-6 text-center border-y border-slate-800/80">
                Click on any available green seat on the map to reserve it.
              </p>
            ) : (
              <div className="space-y-3 py-4 border-y border-slate-800/80 max-h-60 overflow-y-auto">
                {selectedSeats.map((seat) => (
                  <div key={seat.id} className="flex justify-between items-center text-sm">
                    <div>
                      <span className="font-semibold text-white">
                        Sec {seat.section}, Row {seat.rowNumber}, Seat {seat.seatNumber}
                      </span>
                      <span className="block text-xs text-slate-400">{seat.seatType}</span>
                    </div>
                    <span className="font-mono font-bold text-indigo-400">${seat.price.toFixed(2)}</span>
                  </div>
                ))}
              </div>
            )}

            <div className="pt-4 flex justify-between items-baseline mb-6">
              <span className="text-sm text-slate-400">Total Price:</span>
              <span className="text-2xl font-black text-white font-mono">${totalPrice.toFixed(2)}</span>
            </div>

            <button
              onClick={handleProceedToHold}
              disabled={selectedSeats.length === 0 || holding}
              className="w-full bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 text-white font-bold py-3.5 rounded-xl transition-all shadow-lg shadow-indigo-600/30 flex items-center justify-center gap-2 text-sm"
            >
              {holding ? 'Acquiring Redis Lock...' : 'Reserve & Proceed'}
              <ArrowRight className="w-4 h-4" />
            </button>

            <div className="mt-4 flex items-center gap-2 text-xs text-slate-500 justify-center">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Seats held for 10 mins with atomic lock</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
