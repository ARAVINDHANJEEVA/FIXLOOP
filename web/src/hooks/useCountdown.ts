import { useState, useEffect } from 'react';

export const useCountdown = (targetDate: string) => {
  const getTimeLeft = () => {
    const now = new Date().getTime();
    const target = new Date(targetDate).getTime();
    const diff = target - now;
    return diff;
  };

  const [timeLeft, setTimeLeft] = useState(getTimeLeft());

  useEffect(() => {
    const interval = setInterval(() => {
      setTimeLeft(getTimeLeft());
    }, 1000);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [targetDate]);

  const isExpired = timeLeft <= 0;
  const absDiff = Math.abs(timeLeft);

  const hours = Math.floor(absDiff / (1000 * 60 * 60));
  const minutes = Math.floor((absDiff % (1000 * 60 * 60)) / (1000 * 60));
  const seconds = Math.floor((absDiff % (1000 * 60)) / 1000);

  const formatted = isExpired
    ? `Overdue ${hours}h ${minutes}m`
    : hours > 0
    ? `${hours}h ${minutes}m`
    : `${minutes}m ${seconds}s`;

  const urgency: 'normal' | 'warning' | 'critical' | 'overdue' = isExpired
    ? 'overdue'
    : hours < 1
    ? 'critical'
    : hours < 3
    ? 'warning'
    : 'normal';

  return { timeLeft, isExpired, formatted, urgency, hours, minutes, seconds };
};
