import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Ticket, User as UserIcon, LogOut, Shield } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-slate-900 border-b border-slate-800 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2">
          <div className="bg-brand-600 text-white p-2 rounded-xl">
            <Ticket className="w-5 h-5" />
          </div>
          <span className="text-xl font-bold tracking-tight text-white">
            Velocity<span className="text-indigo-400">Seat</span>
          </span>
        </Link>

        <div className="flex items-center gap-6">
          <Link to="/" className="text-slate-300 hover:text-white text-sm font-medium transition-colors">
            Events
          </Link>

          {isAuthenticated ? (
            <>
              <Link to="/my-bookings" className="text-slate-300 hover:text-white text-sm font-medium transition-colors">
                My Bookings
              </Link>

              {isAdmin && (
                <Link
                  to="/admin"
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-950 text-indigo-300 border border-indigo-800 hover:bg-indigo-900 text-xs font-semibold uppercase tracking-wider transition-colors"
                >
                  <Shield className="w-3.5 h-3.5" />
                  Admin
                </Link>
              )}

              <div className="flex items-center gap-4 pl-4 border-l border-slate-800">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center text-slate-300">
                    <UserIcon className="w-4 h-4" />
                  </div>
                  <span className="text-sm font-medium text-slate-200 hidden md:inline">
                    {user?.fullName}
                  </span>
                </div>

                <button
                  onClick={handleLogout}
                  className="text-slate-400 hover:text-rose-400 transition-colors p-1"
                  title="Sign Out"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            </>
          ) : (
            <div className="flex items-center gap-3">
              <Link
                to="/login"
                className="text-sm font-medium text-slate-300 hover:text-white px-3 py-2 rounded-lg transition-colors"
              >
                Sign In
              </Link>
              <Link
                to="/register"
                className="text-sm font-semibold bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-lg transition-colors"
              >
                Sign Up
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};
