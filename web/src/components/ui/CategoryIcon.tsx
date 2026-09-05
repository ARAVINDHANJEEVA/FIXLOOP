import React from 'react';
import {
  Home,
  Utensils,
  BookOpen,
  Activity,
  Sparkles,
  Laptop,
  Lock,
  AlertTriangle,
  ShieldAlert,
  FileText,
} from 'lucide-react';

interface CategoryIconProps {
  name: string;
  size?: number;
  className?: string;
  style?: React.CSSProperties;
}

const CategoryIcon: React.FC<CategoryIconProps> = ({
  name,
  size = 16,
  className,
  style,
}) => {
  const iconProps = { size, className, style };

  switch (name) {
    case 'Hostel':
      return <Home {...iconProps} />;
    case 'Food':
      return <Utensils {...iconProps} />;
    case 'Academic':
      return <BookOpen {...iconProps} />;
    case 'Health':
      return <Activity {...iconProps} />;
    case 'Hygiene':
      return <Sparkles {...iconProps} />;
    case 'Tech Support':
      return <Laptop {...iconProps} />;
    case 'Harassment':
      return <Lock {...iconProps} />;
    case 'Malpractice':
      return <AlertTriangle {...iconProps} />;
    case 'Ragging':
      return <ShieldAlert {...iconProps} />;
    default:
      return <FileText {...iconProps} />;
  }
};

export default CategoryIcon;
