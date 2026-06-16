'use client';

import React, { useRef, useEffect, useState } from 'react';
import { motion, useMotionValue, useSpring, useTransform, useScroll, animate } from 'framer-motion';
import Image from 'next/image';

export default function InteractiveCard() {
  const containerRef = useRef<HTMLDivElement>(null);
  const [isMouseOver, setIsMouseOver] = useState(false);

  // 1. Scroll-linked viewport transforms (the Bloom scale effect)
  const { scrollYProgress } = useScroll();
  const scrollScale = useTransform(scrollYProgress, [0, 0.3, 1], [1, 1.1, 1.05]);
  const scrollY = useTransform(scrollYProgress, [0, 0.3, 1], [0, 60, 120]);
  const scrollRotateZ = useTransform(scrollYProgress, [0, 0.3, 1], [0, -8, -12]);

  // 2. Mouse position values relative to the card center (-0.5 to 0.5)
  const mouseX = useMotionValue(0);
  const mouseY = useMotionValue(0);

  // Spring configuration matching previous high-fidelity dampening
  const springConfig = { damping: 25, stiffness: 150, mass: 0.6 };

  // Highlight/glare positions
  const glareX = useSpring(useTransform(mouseX, [-0.5, 0.5], ['0%', '100%']), springConfig);
  const glareY = useSpring(useTransform(mouseY, [-0.5, 0.5], ['0%', '100%']), springConfig);
  
  // Fade glare in as mouse moves away from center
  const glareOpacity = useSpring(
    useTransform(
      [mouseX, mouseY],
      ([x, y]) => {
        const dist = Math.sqrt(Number(x) * Number(x) + Number(y) * Number(y));
        return Math.min(dist * 1.5, 0.3);
      }
    ),
    springConfig
  );

  // Shadow displacement
  const shadowX = useSpring(useTransform(mouseX, [-0.5, 0.5], [-12, 12]), springConfig);
  const shadowY = useSpring(useTransform(mouseY, [-0.5, 0.5], [-12, 12]), springConfig);

  // Re-engineered rotation MotionValues for state switching
  const rotateX = useMotionValue(0);
  const rotateY = useMotionValue(0);

  // Counter-flip scaleX transformer for back hemisphere rotation (mirror fix)
  const scaleX = useTransform(rotateY, (y) => {
    const angle = ((Number(y) % 360) + 360) % 360;
    return (angle > 90 && angle < 270) ? -1 : 1;
  });

  useEffect(() => {
    const hero = document.getElementById('hero-viewport');
    if (!hero) return;

    let heroRect = hero.getBoundingClientRect();
    let cardRect = containerRef.current?.getBoundingClientRect();

    const updateRects = () => {
      heroRect = hero.getBoundingClientRect();
      cardRect = containerRef.current?.getBoundingClientRect();
    };

    window.addEventListener('resize', updateRects);
    window.addEventListener('scroll', updateRects);

    const handleMouseMoveWindow = (e: MouseEvent) => {
      // Check if mouse is inside the hero container bounds
      const isInside = 
        e.clientX >= heroRect.left &&
        e.clientX <= heroRect.right &&
        e.clientY >= heroRect.top &&
        e.clientY <= heroRect.bottom;

      if (isInside) {
        setIsMouseOver(true);

        if (!cardRect) return;
        
        // Distance relative to card center
        const dx = e.clientX - (cardRect.left + cardRect.width / 2);
        const dy = e.clientY - (cardRect.top + cardRect.height / 2);
        
        // Normalize relative to screen size (half width/height)
        const nx = dx / (window.innerWidth / 2);
        const ny = dy / (window.innerHeight / 2);
        
        // Clamp normalized mouse positions to prevent extreme tilts
        const clampedX = Math.max(-0.5, Math.min(0.5, nx));
        const clampedY = Math.max(-0.5, Math.min(0.5, ny));
        
        mouseX.set(clampedX);
        mouseY.set(clampedY);

        // Map to max 15 degree rotation (as specified)
        const targetRotateX = clampedY * -30; // maps [-0.5, 0.5] to [15, -15]
        const targetRotateY = clampedX * 30;  // maps [-0.5, 0.5] to [-15, 15]

        animate(rotateX, targetRotateX, {
          type: "spring",
          ...springConfig
        });
        animate(rotateY, targetRotateY, {
          type: "spring",
          ...springConfig
        });
      } else {
        setIsMouseOver(false);
      }
    };

    const handleMouseLeaveWindow = () => {
      setIsMouseOver(false);
    };

    // Initial measurement
    updateRects();

    window.addEventListener('mousemove', handleMouseMoveWindow);
    document.addEventListener('mouseleave', handleMouseLeaveWindow);

    return () => {
      window.removeEventListener('resize', updateRects);
      window.removeEventListener('scroll', updateRects);
      window.removeEventListener('mousemove', handleMouseMoveWindow);
      document.removeEventListener('mouseleave', handleMouseLeaveWindow);
    };
  }, [rotateX, rotateY, mouseX, mouseY]);

  useEffect(() => {
    if (!isMouseOver) {
      // Idle state animations: Infinite linear rotation Y + reset X & mouse position
      const currentY = rotateY.get();
      const controlsY = animate(rotateY, [currentY, currentY + 360], {
        duration: 20,
        ease: "linear",
        repeat: Infinity,
      });

      const controlsX = animate(rotateX, 0, {
        duration: 0.8,
        ease: "easeInOut",
      });

      const controlsMouseX = animate(mouseX, 0, { duration: 0.8 });
      const controlsMouseY = animate(mouseY, 0, { duration: 0.8 });

      return () => {
        controlsY.stop();
        controlsX.stop();
        controlsMouseX.stop();
        controlsMouseY.stop();
      };
    }
  }, [isMouseOver, rotateX, rotateY, mouseX, mouseY]);

  return (
    /* Level 1: Outer Scroll Engagement Wrapper */
    <motion.div
      style={{
        y: scrollY,
        rotateZ: scrollRotateZ,
        scale: scrollScale,
      }}
      className="w-full max-w-[500px] flex items-center justify-center bg-transparent"
    >
      {/* Level 2: Ambient Float Wrapper */}
      <motion.div
        animate={{ y: [-8, 8] }}
        transition={{
          y: {
            duration: 5,
            repeat: Infinity,
            repeatType: 'reverse',
            ease: 'easeInOut',
          },
        }}
        className="w-full flex items-center justify-center bg-transparent"
      >
        {/* Level 3: Mouse-Tracking 3D Tilt Viewport */}
        <div
          ref={containerRef}
          className="relative w-full aspect-[1.67/1] cursor-pointer bg-transparent select-none active:scale-[0.98] transition-transform duration-150"
          style={{ perspective: 1200 }}
          id="interactive-card-container"
        >
          <motion.div
            className="relative w-full h-full rounded-2xl bg-transparent overflow-hidden"
            style={{
              rotateX,
              rotateY,
              transformStyle: 'preserve-3d',
            }}
          >
            {/* Transparent Card Image with counter-flip on back-face rotation */}
            <motion.div 
              style={{ scaleX }}
              className="absolute inset-0 bg-transparent flex items-center justify-center pointer-events-none"
            >
              <Image
                src="/halt-premium-card.png"
                alt="halt. matte black NFC keycard"
                width={500}
                height={299}
                priority
                fetchPriority="high"
                className="w-full h-auto object-contain bg-transparent pointer-events-none"
                style={{
                  backgroundColor: 'transparent',
                  mixBlendMode: 'normal'
                }}
              />
            </motion.div>

            {/* Glare/Highlight Overlay */}
            <motion.div
              className="absolute inset-0 pointer-events-none mix-blend-overlay"
              style={{
                opacity: glareOpacity,
                background: useTransform(
                  [glareX, glareY],
                  ([gx, gy]) => `radial-gradient(circle at ${gx} ${gy}, rgba(255, 255, 255, 0.4) 0%, rgba(255, 255, 255, 0) 50%)`
                ),
              }}
            />

            {/* Inner Border Highlight */}
            <div className="absolute inset-0 border border-white/5 rounded-2xl pointer-events-none" />
          </motion.div>

          {/* Dynamic 3D Shadow */}
          <motion.div
            className="absolute -inset-2 bg-black/60 blur-xl rounded-3xl pointer-events-none z-[-1]"
            style={{
              x: shadowX,
              y: shadowY,
              scale: 0.9,
            }}
          />
        </div>
      </motion.div>
    </motion.div>
  );
}
