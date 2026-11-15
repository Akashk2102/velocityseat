import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { reservationApi, bookingApi } from '../api/endpoints';
import { ReservationResponse, PaymentMethod } from '../types';
import { CountdownTimer } from '../components/CountdownTimer';
import { ShieldCheck, CreditCard, Smartphone, Building, Wallet, AlertCircle, CheckCircle2 } from 'lucide-react';

export const CheckoutPage: React.FC = () => {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();

  const [reservation, setReservation] = useState<ReservationResponse | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CARD');
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState('');

  // Generate idempotency key once per checkout session
  const [idempotencyKey] = useState<string>(() => {
    return 'idem_' + (crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2));
  });

  useEffect(() => {
    const fetchReservation = async () => {
      if (!token) return;
      try {
        setLoading(true);
        const data = await reservationApi.getReservation(token);
        if (data.status !== 'ACTIVE') {
          setError('This reservation is no longer active.');
        }
        setReservation(data);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to load reservation');
      } finally {
        setLoading(false);
      }
    };

    fetchReservation();
  }, [token]);

  const handleConfirmPayment = async () => {
    if (!token) return;
    setPaying(true);
    setError('');

    try {
      const booking = await bookingApi.confirmBooking(token, paymentMethod, idempotencyKey);
      navigate(`/tickets/${booking.bookingReference}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Payment confirmation failed');
    } finally {
      setPaying(false);
    }
  };

  const handleRelease = async () => {
    if (!token) return;
    setCancelling(true);
    try {
      await reservationApi.releaseReservation(token);
      navigate('/');
    } catch (err) {
      navigate('/');
    }
  };

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center">
        <div className="w-10 h-10 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
        <p className="text-slate-400 text-sm">Validating temporary seat reservation...</p>
      </div>
    );
  }

  if (!reservation || reservation.status !== 'ACTIVE') {
    return (
      <div className="max-w-lg mx-auto px-4 py-20 text-center">
        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-8">
          <AlertCircle className="w-12 h-12 text-rose-400 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-white mb-2">Reservation Expired</h2>
          <p className="text-slate-400 text-sm mb-6">
            The 10-minute hold window for these seats has expired and they have been released back to the event inventory.
          </p>
          <button
            onClick={() => navigate('/')}
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-semibold px-6 py-2.5 rounded-xl text-sm"
          >
            Back to Events
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-10">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-10 shadow-2xl">
        {/* Header with Countdown */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800 mb-8">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-400">Checkout</span>
            <h1 className="text-2xl font-bold text-white mt-1">Complete Your Booking</h1>
          </div>
          <CountdownTimer
            expiresAt={reservation.expiresAt}
            onExpired={() => setError('Time expired! Your seats have been released.')}
          />
        </div>

        {error && (
          <div className="mb-6 p-4 rounded-xl bg-rose-950/80 border border-rose-800 text-rose-300 text-sm flex items-center gap-3">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Order Summary */}
        <div className="bg-slate-950 p-6 rounded-2xl border border-slate-800 mb-8">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-400 mb-3">
            Event & Seating Summary
          </h3>
          <p className="text-lg font-bold text-white mb-1">{reservation.eventTitle}</p>
          <p className="text-xs text-slate-400 mb-4">{reservation.seatIds.length} seat(s) held</p>

          <div className="pt-4 border-t border-slate-800/80 flex justify-between items-baseline">
            <span className="text-sm font-medium text-slate-300">Total Due:</span>
            <span className="text-3xl font-black text-white font-mono">
              ${reservation.totalAmount.toFixed(2)}
            </span>
          </div>
        </div>

        {/* Payment Methods */}
        <div className="mb-8">
          <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3">
            Select Payment Method
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { id: 'CARD', label: 'Credit Card', icon: CreditCard },
              { id: 'UPI', label: 'UPI / QR', icon: Smartphone },
              { id: 'NET_BANKING', label: 'Net Banking', icon: Building },
              { id: 'DIGITAL_WALLET', label: 'Wallet', icon: Wallet },
            ].map((method) => {
              const Icon = method.icon;
              const isSelected = paymentMethod === method.id;
              return (
                <button
                  key={method.id}
                  type="button"
                  onClick={() => setPaymentMethod(method.id as PaymentMethod)}
                  className={`p-4 rounded-2xl border flex flex-col items-center gap-2 text-xs font-semibold transition-all ${
                    isSelected
                      ? 'bg-indigo-600/10 border-indigo-500 text-white shadow-lg shadow-indigo-600/10 ring-1 ring-indigo-500'
                      : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <Icon className={`w-5 h-5 ${isSelected ? 'text-indigo-400' : 'text-slate-500'}`} />
                  <span>{method.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Actions */}
        <div className="space-y-3">
          <button
            onClick={handleConfirmPayment}
            disabled={paying}
            className="w-full bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-bold py-4 rounded-xl transition-all shadow-lg shadow-emerald-600/20 text-sm flex items-center justify-center gap-2"
          >
            {paying ? 'Executing ACID Transaction...' : `Confirm & Pay $${reservation.totalAmount.toFixed(2)}`}
            <CheckCircle2 className="w-4 h-4" />
          </button>

          <button
            onClick={handleRelease}
            disabled={cancelling}
            className="w-full text-slate-400 hover:text-rose-400 text-xs font-medium py-2 transition-colors text-center"
          >
            {cancelling ? 'Releasing hold...' : 'Cancel & Release Seats Now'}
          </button>
        </div>

        <div className="mt-6 flex items-center justify-center gap-2 text-xs text-slate-500">
          <ShieldCheck className="w-4 h-4 text-emerald-400" />
          <span>Protected by Idempotency Key & PostgreSQL Row-Level Lock</span>
        </div>
      </div>
    </div>
  );
};
