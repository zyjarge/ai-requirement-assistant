import { useDeviceType } from '@/hooks/useDeviceType';
import PCLayout from './PCLayout';
import MobileLayout from './MobileLayout';

export default function ResponsiveLayout() {
  const type = useDeviceType();
  return type === 'mobile' ? <MobileLayout /> : <PCLayout />;
}
