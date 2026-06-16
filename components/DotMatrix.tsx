import React from 'react';

export default function DotMatrix() {
  return (
    <div
      id="dot-matrix-bg"
      className="absolute inset-0 pointer-events-none overflow-hidden"
      style={{
        backgroundColor: 'transparent',
        backgroundImage: `radial-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px)`,
        backgroundSize: '24px 24px',
      }}
    >
      {/* Premium radial gradient overlay for high-end spotlight lighting effect */}
      <div 
        className="absolute inset-0 pointer-events-none"
        style={{
          background: 'radial-gradient(circle at 50% 50%, rgba(10, 10, 10, 0) 20%, rgba(0, 0, 0, 0.85) 80%)',
        }}
      />
    </div>
  );
}
