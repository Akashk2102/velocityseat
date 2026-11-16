import React, { useEffect, useState } from 'react';
import { venueApi, eventApi, adminApi } from '../api/endpoints';
import { Venue, EventCategory, AdminStats } from '../types';
import { Building2, Grid, CalendarPlus, CheckCircle, AlertCircle, BarChart3, DollarSign, Ticket, Users, Clock } from 'lucide-react';

export const AdminPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'venue' | 'seats' | 'event' | 'analytics'>('analytics');
  const [venues, setVenues] = useState<Venue[]>([]);
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [statsLoading, setStatsLoading] = useState(false);

  // Form states
  const [venueForm, setVenueForm] = useState({
    name: '',
    address: '',
    city: '',
    state: '',
    country: 'USA',
    postalCode: '',
    totalCapacity: 5000,
  });

  const [seatsForm, setSeatsForm] = useState({
    venueId: '',
    section: 'Floor',
    totalRows: 5,
    seatsPerRow: 10,
    seatType: 'STANDARD',
  });

  const [eventForm, setEventForm] = useState({
    venueId: '',
    title: '',
    description: '',
    category: 'CONCERT' as EventCategory,
    startTime: '',
    endTime: '',
    standardPrice: 75.0,
    vipPrice: 199.0,
  });

  const loadVenues = async () => {
    try {
      const data = await venueApi.getVenues();
      setVenues(data);
      if (data.length > 0) {
        setSeatsForm((prev) => ({ ...prev, venueId: String(data[0].id) }));
        setEventForm((prev) => ({ ...prev, venueId: String(data[0].id) }));
      }
    } catch (err) {
      console.error(err);
    }
  };

  const loadStats = async () => {
    try {
      setStatsLoading(true);
      const data = await adminApi.getStats();
      setStats(data);
    } catch (err) {
      console.error('Failed to load admin stats', err);
    } finally {
      setStatsLoading(false);
    }
  };

  useEffect(() => {
    loadVenues();
    loadStats();
  }, []);

  const clearNotifications = () => {
    setMessage('');
    setError('');
  };

  const handleCreateVenue = async (e: React.FormEvent) => {
    e.preventDefault();
    clearNotifications();
    setLoading(true);
    try {
      await venueApi.createVenue(venueForm);
      setMessage(`Venue "${venueForm.name}" created successfully!`);
      loadVenues();
      setVenueForm({
        name: '',
        address: '',
        city: '',
        state: '',
        country: 'USA',
        postalCode: '',
        totalCapacity: 5000,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create venue');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateSeats = async (e: React.FormEvent) => {
    e.preventDefault();
    clearNotifications();
    setLoading(true);
    try {
      const count = seatsForm.totalRows * seatsForm.seatsPerRow;
      await venueApi.generateSeats(Number(seatsForm.venueId), {
        section: seatsForm.section,
        totalRows: Number(seatsForm.totalRows),
        seatsPerRow: Number(seatsForm.seatsPerRow),
        seatType: seatsForm.seatType,
      });
      setMessage(`${count} seats generated successfully in section "${seatsForm.section}"!`);
      loadVenues();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to generate seats');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateEvent = async (e: React.FormEvent) => {
    e.preventDefault();
    clearNotifications();
    setLoading(true);
    try {
      await eventApi.createEvent({
        venueId: Number(eventForm.venueId),
        title: eventForm.title,
        description: eventForm.description,
        category: eventForm.category,
        startTime: new Date(eventForm.startTime).toISOString(),
        endTime: new Date(eventForm.endTime).toISOString(),
        standardPrice: Number(eventForm.standardPrice),
        vipPrice: Number(eventForm.vipPrice),
      });
      setMessage(`Event "${eventForm.title}" published with live seat inventory!`);
      setEventForm({
        venueId: venues[0] ? String(venues[0].id) : '',
        title: '',
        description: '',
        category: 'CONCERT',
        startTime: '',
        endTime: '',
        standardPrice: 75.0,
        vipPrice: 199.0,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create event');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-10">
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold text-white tracking-tight">Admin Console</h1>
        <p className="text-sm text-slate-400 mt-1">
          Manage physical venues, generate seating grids, and publish new events
        </p>
      </div>

      {message && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-950/80 border border-emerald-800 text-emerald-300 text-sm flex items-center gap-3">
          <CheckCircle className="w-5 h-5 flex-shrink-0" />
          <span>{message}</span>
        </div>
      )}

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-950/80 border border-rose-800 text-rose-300 text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Tabs */}
      <div className="flex border-b border-slate-800 mb-8 gap-4 overflow-x-auto">
        <button
          onClick={() => {
            clearNotifications();
            setActiveTab('analytics');
            loadStats();
          }}
          className={`pb-4 px-2 text-sm font-semibold flex items-center gap-2 border-b-2 whitespace-nowrap transition-all ${
            activeTab === 'analytics'
              ? 'border-indigo-500 text-white'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <BarChart3 className="w-4 h-4" />
          Live Analytics
        </button>

        <button
          onClick={() => {
            clearNotifications();
            setActiveTab('venue');
          }}
          className={`pb-4 px-2 text-sm font-semibold flex items-center gap-2 border-b-2 whitespace-nowrap transition-all ${
            activeTab === 'venue'
              ? 'border-indigo-500 text-white'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Building2 className="w-4 h-4" />
          1. Create Venue
        </button>

        <button
          onClick={() => {
            clearNotifications();
            setActiveTab('seats');
          }}
          className={`pb-4 px-2 text-sm font-semibold flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'seats'
              ? 'border-indigo-500 text-white'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Grid className="w-4 h-4" />
          2. Generate Seats Grid
        </button>

        <button
          onClick={() => {
            clearNotifications();
            setActiveTab('event');
          }}
          className={`pb-4 px-2 text-sm font-semibold flex items-center gap-2 border-b-2 transition-all ${
            activeTab === 'event'
              ? 'border-indigo-500 text-white'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <CalendarPlus className="w-4 h-4" />
          3. Publish Event
        </button>
      </div>

      {/* TAB 0: LIVE ANALYTICS */}
      {activeTab === 'analytics' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-bold text-white">Real-Time Platform Performance</h3>
            <button
              onClick={loadStats}
              disabled={statsLoading}
              className="text-xs bg-slate-800 hover:bg-slate-700 text-slate-300 px-3 py-1.5 rounded-lg transition-colors"
            >
              {statsLoading ? 'Refreshing...' : 'Refresh Metrics'}
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
              <div className="flex items-center justify-between text-slate-400 mb-2">
                <span className="text-xs font-semibold uppercase tracking-wider">Gross Revenue</span>
                <DollarSign className="w-5 h-5 text-emerald-400" />
              </div>
              <span className="text-3xl font-black text-white font-mono">
                ${stats ? stats.totalGrossRevenue.toFixed(2) : '0.00'}
              </span>
              <span className="block text-xs text-slate-500 mt-1">Confirmed transactions</span>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
              <div className="flex items-center justify-between text-slate-400 mb-2">
                <span className="text-xs font-semibold uppercase tracking-wider">Tickets Sold</span>
                <Ticket className="w-5 h-5 text-indigo-400" />
              </div>
              <span className="text-3xl font-black text-white font-mono">
                {stats ? stats.totalTicketsSold : 0}
              </span>
              <span className="block text-xs text-slate-500 mt-1">Seats permanently booked</span>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
              <div className="flex items-center justify-between text-slate-400 mb-2">
                <span className="text-xs font-semibold uppercase tracking-wider">Total Bookings</span>
                <Users className="w-5 h-5 text-purple-400" />
              </div>
              <span className="text-3xl font-black text-white font-mono">
                {stats ? stats.totalBookings : 0}
              </span>
              <span className="block text-xs text-slate-500 mt-1">Completed orders</span>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
              <div className="flex items-center justify-between text-slate-400 mb-2">
                <span className="text-xs font-semibold uppercase tracking-wider">Active Seat Holds</span>
                <Clock className="w-5 h-5 text-amber-400" />
              </div>
              <span className="text-3xl font-black text-amber-400 font-mono">
                {stats ? stats.activeHoldsCount : 0}
              </span>
              <span className="block text-xs text-slate-500 mt-1">Currently in 10-min Redis TTL</span>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
              <div className="flex items-center justify-between text-slate-400 mb-2">
                <span className="text-xs font-semibold uppercase tracking-wider">Events Active</span>
                <CalendarPlus className="w-5 h-5 text-sky-400" />
              </div>
              <span className="text-3xl font-black text-white font-mono">
                {stats ? stats.totalEvents : 0}
              </span>
              <span className="block text-xs text-slate-500 mt-1">Shows scheduled</span>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
              <div className="flex items-center justify-between text-slate-400 mb-2">
                <span className="text-xs font-semibold uppercase tracking-wider">Venues Registered</span>
                <Building2 className="w-5 h-5 text-blue-400" />
              </div>
              <span className="text-3xl font-black text-white font-mono">
                {stats ? stats.totalVenues : 0}
              </span>
              <span className="block text-xs text-slate-500 mt-1">Arenas and stadiums</span>
            </div>
          </div>
        </div>
      )}

      {/* TAB 1: CREATE VENUE */}
      {activeTab === 'venue' && (
        <form onSubmit={handleCreateVenue} className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
          <h3 className="text-lg font-bold text-white mb-4">Register Physical Venue</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Venue Name</label>
              <input
                type="text"
                required
                value={venueForm.name}
                onChange={(e) => setVenueForm({ ...venueForm, name: e.target.value })}
                placeholder="MetLife Stadium"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Total Capacity</label>
              <input
                type="number"
                required
                min={1}
                value={venueForm.totalCapacity}
                onChange={(e) => setVenueForm({ ...venueForm, totalCapacity: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Address</label>
              <input
                type="text"
                required
                value={venueForm.address}
                onChange={(e) => setVenueForm({ ...venueForm, address: e.target.value })}
                placeholder="1 MetLife Stadium Dr"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">City</label>
              <input
                type="text"
                required
                value={venueForm.city}
                onChange={(e) => setVenueForm({ ...venueForm, city: e.target.value })}
                placeholder="East Rutherford"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">State / Region</label>
              <input
                type="text"
                value={venueForm.state}
                onChange={(e) => setVenueForm({ ...venueForm, state: e.target.value })}
                placeholder="NJ"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-bold py-3 rounded-xl transition-colors mt-6 text-sm"
          >
            {loading ? 'Creating Venue...' : 'Save Venue'}
          </button>
        </form>
      )}

      {/* TAB 2: GENERATE SEATS */}
      {activeTab === 'seats' && (
        <form onSubmit={handleGenerateSeats} className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
          <h3 className="text-lg font-bold text-white mb-4">Batch Generate Physical Seats</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Select Venue</label>
              <select
                value={seatsForm.venueId}
                onChange={(e) => setSeatsForm({ ...seatsForm, venueId: e.target.value })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                {venues.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.name} ({v.configuredSeatsCount} / {v.totalCapacity} configured)
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Section Name</label>
              <input
                type="text"
                required
                value={seatsForm.section}
                onChange={(e) => setSeatsForm({ ...seatsForm, section: e.target.value })}
                placeholder="e.g. VIP-Floor, Lower-Bowl, Balcony"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Seat Type</label>
              <select
                value={seatsForm.seatType}
                onChange={(e) => setSeatsForm({ ...seatsForm, seatType: e.target.value })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="STANDARD">STANDARD</option>
                <option value="VIP">VIP</option>
                <option value="ACCESSIBLE">ACCESSIBLE</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Number of Rows</label>
              <input
                type="number"
                required
                min={1}
                max={50}
                value={seatsForm.totalRows}
                onChange={(e) => setSeatsForm({ ...seatsForm, totalRows: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Seats Per Row</label>
              <input
                type="number"
                required
                min={1}
                max={50}
                value={seatsForm.seatsPerRow}
                onChange={(e) => setSeatsForm({ ...seatsForm, seatsPerRow: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>

          <p className="text-xs text-indigo-400 mt-2">
            Will generate {seatsForm.totalRows * seatsForm.seatsPerRow} seats in Section {seatsForm.section}.
          </p>

          <button
            type="submit"
            disabled={loading || venues.length === 0}
            className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-bold py-3 rounded-xl transition-colors mt-6 text-sm"
          >
            {loading ? 'Generating Grid...' : 'Generate Seating Grid'}
          </button>
        </form>
      )}

      {/* TAB 3: PUBLISH EVENT */}
      {activeTab === 'event' && (
        <form onSubmit={handleCreateEvent} className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 space-y-4">
          <h3 className="text-lg font-bold text-white mb-4">Publish Event & Generate Inventory</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Venue</label>
              <select
                value={eventForm.venueId}
                onChange={(e) => setEventForm({ ...eventForm, venueId: e.target.value })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                {venues.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.name} ({v.configuredSeatsCount} seats available to publish)
                  </option>
                ))}
              </select>
            </div>
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Event Title</label>
              <input
                type="text"
                required
                value={eventForm.title}
                onChange={(e) => setEventForm({ ...eventForm, title: e.target.value })}
                placeholder="Taylor Swift | The Eras Tour"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Category</label>
              <select
                value={eventForm.category}
                onChange={(e) => setEventForm({ ...eventForm, category: e.target.value as EventCategory })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="CONCERT">CONCERT</option>
                <option value="SPORTS">SPORTS</option>
                <option value="THEATER">THEATER</option>
                <option value="CONFERENCE">CONFERENCE</option>
                <option value="FESTIVAL">FESTIVAL</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Start Time</label>
              <input
                type="datetime-local"
                required
                value={eventForm.startTime}
                onChange={(e) => setEventForm({ ...eventForm, startTime: e.target.value })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">End Time</label>
              <input
                type="datetime-local"
                required
                value={eventForm.endTime}
                onChange={(e) => setEventForm({ ...eventForm, endTime: e.target.value })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Standard Seat Price ($)</label>
              <input
                type="number"
                step="0.01"
                required
                min={1}
                value={eventForm.standardPrice}
                onChange={(e) => setEventForm({ ...eventForm, standardPrice: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">VIP Seat Price ($)</label>
              <input
                type="number"
                step="0.01"
                required
                min={1}
                value={eventForm.vipPrice}
                onChange={(e) => setEventForm({ ...eventForm, vipPrice: Number(e.target.value) })}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Description</label>
              <textarea
                rows={3}
                value={eventForm.description}
                onChange={(e) => setEventForm({ ...eventForm, description: e.target.value })}
                placeholder="Details about the show..."
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading || venues.length === 0}
            className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-3 rounded-xl transition-colors mt-6 text-sm"
          >
            {loading ? 'Publishing Event...' : 'Publish Event & Generate Inventory'}
          </button>
        </form>
      )}
    </div>
  );
};
