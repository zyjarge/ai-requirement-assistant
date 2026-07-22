import { useEffect, useState } from 'react';

const MOBILE_BREAKPOINT = 768;

export function useDeviceType(): 'mobile' | 'pc' {
  const [width, setWidth] = useState(() => window.innerWidth);

  useEffect(() => {
    const onResize = () => setWidth(window.innerWidth);
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  return width < MOBILE_BREAKPOINT ? 'mobile' : 'pc';
}
