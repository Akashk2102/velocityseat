import React from 'react';
import { EventSeat } from '../types';

interface SeatMapProps {
  seats: EventSeat[];
  selectedSeatIds: number[];
  onToggleSeat: (seat: EventSeat) => void;
}

export const SeatMap: React.FC<SeatMapProps> = ({ seats, selectedSeatIds, onToggleSeat }) => {
  // Group seats by section, then by row
  const sections = Array.from(new Set(seats.map((s) => s.section)));

  const getSeatColor = (seat: EventSeat) => {
    if (selectedSeatIds.includes(seat.id)) {
      return 'bg-indigo-600 text-white ring-2 ring-indigo-400 scale-105';
    }
    switch (seat.status) {
      case 'AVAILABLE':
        return seat.seatType === 'VIP'
          ? 'bg-amber-500/20 text-amber-300 border border-amber-500/50 hover:bg-amber-500 hover:text-slate-950'
          : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 hover:bg-emerald-500 hover:text-slate-950';
      case 'HELD':
        return 'bg-amber-900/40 text-amber-500 border border-amber-800 cursor-not-allowed opacity-60';
      case 'BOOKED':
      case 'BLOCKED':
        return 'bg-slate-800 text-slate-600 border border-slate-700/50 cursor-not-allowed opacity-40';
      default:
        return 'bg-slate-800 text-slate-400';
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 flex flex-col items-center">
      {/* Venue Stage Representation */}
      <div className="w-full max-w-2xl mb-12 flex flex-col items-center">
        <div className="w-3/4 h-3 bg-gradient-to-r from-indigo-500 via-purple-500 to-indigo-500 rounded-full shadow-[0_0_25px_rgba(99,102,241,0.6)]"></div>
        <span className="text-xs font-bold tracking-widest text-slate-400 uppercase mt-2">
          STAGE / SCREEN
        </span>
      </div>

      {/* Sections and Rows */}
      <div className="space-y-8 w-full flex flex-col items-center overflow-x-auto pb-4">
        {sections.map((section) => {
          const sectionSeats = seats.filter((s) => s.section === section);
          const rows = Array.from(new Set(sectionSeats.map((s) => s.rowNumber))).sort(
            (a, b) => Number(a) - Number(b)
          );

          return (
            <div key={section} className="flex flex-col items-center bg-slate-950/60 p-4 rounded-xl border border-slate-800/80">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-300 mb-4 px-3 py-1 bg-slate-800 rounded-md">
                Section: {section}
              </span>

              <div className="space-y-3">
                {rows.map((row) => {
                  const rowSeats = sectionSeats
                    .filter((s) => s.rowNumber === row)
                    .sort((a, b) => Number(a.seatNumber) - Number(b.seatNumber));

                  return (
                    <div key={row} className="flex items-center gap-2">
                      <span className="w-6 text-xs font-semibold text-slate-300 text-right">
                        R{row}
                      </span>

                      <div className="flex gap-2">
                        {rowSeats.map((seat) => {
                          const isAvailable = seat.status === 'AVAILABLE';
                          const isSelected = selectedSeatIds.includes(seat.id);

                          return (
                            <button
                              key={seat.id}
                              onClick={() => isAvailable && onToggleSeat(seat)}
                              disabled={!isAvailable && !isSelected}
                              title={`Seat ${seat.section}-${seat.rowNumber}-${seat.seatNumber} (${seat.seatType}) - $${seat.price} [${seat.status}]`}
                              className={`w-8 h-8 rounded-lg text-xs font-bold flex items-center justify-center transition-all duration-150 ${getSeatColor(
                                seat
                              )}`}
                            >
                              {seat.seatNumber}
                            </button>
                          );
                        })}
                      </div>

                      <span className="w-6 text-xs font-semibold text-slate-300 text-left">
                        R{row}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>

      {/* Legend */}
      <div className="mt-8 pt-6 border-t border-slate-800 flex flex-wrap items-center justify-center gap-6 text-xs text-slate-300">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded bg-emerald-500/20 border border-emerald-500/50"></div>
          <span>Standard ($)</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded bg-amber-500/20 border border-amber-500/50"></div>
          <span>VIP ($$$)</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded bg-indigo-600 ring-2 ring-indigo-400"></div>
          <span>Selected</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded bg-amber-900/40 border border-amber-800 opacity-60"></div>
          <span>Temporary Hold</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded bg-slate-800 border border-slate-700/50 opacity-40"></div>
          <span>Sold Out</span>
        </div>
      </div>
    </div>
  );
};
