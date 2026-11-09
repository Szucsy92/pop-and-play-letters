import React, { useState, useEffect, useRef } from 'react';
import { Lock, Unlock } from 'lucide-react';

interface ChildLockWrapperProps {
  children: React.ReactNode;
}

export const ChildLockWrapper: React.FC<ChildLockWrapperProps> = ({ children }) => {
  const [isLocked, setIsLocked] = useState(true);
  const [tapCount, setTapCount] = useState(0);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Automatikus visszazárás, ha elhagyják az alkalmazást vagy visszatérnek a játékhoz
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        setIsLocked(true);
        setTapCount(0);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  // Lakat koppintások kezelése
  const handleLockClick = () => {
    if (!isLocked) {
      setIsLocked(true);
      setTapCount(0);
      return;
    }

    const newCount = tapCount + 1;
    setTapCount(newCount);

    if (newCount >= 5) {
      setIsLocked(false);
      setTapCount(0);
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    } else {
      // Ha 2 másodpercig nem kattintanak újra, a számláló nullázódik
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(() => {
        setTapCount(0);
      }, 2000);
    }
  };

  return (
    <div className="relative w-full h-full select-none overflow-hidden">
      {/* Játék tartalom: ha le van zárva, a pointer-events-none tilt minden gesztust és kattintást */}
      <div className={`w-full h-full transition-opacity duration-300 ${isLocked ? 'pointer-events-none opacity-95' : 'pointer-events-auto'}`}>
        {children}
      </div>

      {/* Lakat Gomb - Mindig interaktív (pointer-events-auto) */}
      <div className="absolute top-4 right-4 z-50 pointer-events-auto">
        <button
          onClick={handleLockClick}
          className="flex items-center gap-2 bg-slate-900/80 backdrop-blur-md text-white px-4 py-3 rounded-full shadow-2xl border border-white/20 active:scale-95 transition-transform cursor-pointer"
          aria-label="Gyerekzár lakat"
        >
          {isLocked ? (
            <>
              <Lock className="w-6 h-6 text-red-400 animate-pulse" />
              <span className="text-xs font-bold bg-red-500/20 px-2 py-0.5 rounded-full text-red-300">
                {tapCount}/5
              </span>
            </>
          ) : (
            <>
              <Unlock className="w-6 h-6 text-green-400" />
              <span className="text-xs font-bold bg-green-500/20 px-2 py-0.5 rounded-full text-green-300">
                Nyitva
              </span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};
