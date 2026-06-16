"use client";

import { useEffect, useRef } from "react";

interface Particle {
  x: number;
  y: number;
  z: number;
  size: number;
  vx: number;
  vy: number;
  vz: number;
  baseAlpha: number;
}

export default function ParticleGridBackground() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const mouseRef = useRef({ x: 0, y: 0, targetX: 0, targetY: 0 });

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animationFrameId: number;
    let width = (canvas.width = window.innerWidth);
    let height = (canvas.height = window.innerHeight);

    // 3D projection parameters
    const focalLength = 350;
    const maxZ = 1000;
    
    // Parallax angle parameters
    let rx = 0; // pitch
    let ry = 0; // yaw

    // Setup particles
    const particleCount = 120;
    const particles: Particle[] = [];
    for (let i = 0; i < particleCount; i++) {
      particles.push({
        x: (Math.random() - 0.5) * 1600,
        y: (Math.random() - 0.5) * 1000,
        z: Math.random() * maxZ,
        size: Math.random() * 1.8 + 0.8,
        vx: (Math.random() - 0.5) * 0.15,
        vy: (Math.random() - 0.5) * 0.15,
        vz: -Math.random() * 0.4 - 0.2, // Moving towards the camera
        baseAlpha: Math.random() * 0.4 + 0.3,
      });
    }

    // Grid parameters
    const gridSpacing = 100;
    const gridYFloor = 220;
    const gridYCeil = -220;
    const gridWidth = 1000;
    let gridZOffset = 0;
    const gridSpeed = 0.6; // Flow speed

    // Radial pulse parameters
    let pulseRadius = 0;
    const pulseSpeed = 3.5;
    const pulseMaxRadius = 1200;
    const pulseCenterZ = 450; // Center depth of pulse

    // Track window resize
    const handleResize = () => {
      if (!canvas) return;
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
    };
    window.addEventListener("resize", handleResize);

    // Track mouse movement
    const handleMouseMove = (e: MouseEvent) => {
      // Normalize mouse to [-1, 1] range
      mouseRef.current.targetX = (e.clientX / window.innerWidth - 0.5) * 2;
      mouseRef.current.targetY = (e.clientY / window.innerHeight - 0.5) * 2;
    };
    window.addEventListener("mousemove", handleMouseMove);

    // Project 3D point to 2D
    const project = (x: number, y: number, z: number) => {
      // Apply rotation around Y-axis (yaw)
      let x1 = x * Math.cos(ry) - z * Math.sin(ry);
      let z1 = x * Math.sin(ry) + z * Math.cos(ry);

      // Apply rotation around X-axis (pitch)
      let y2 = y * Math.cos(rx) - z1 * Math.sin(rx);
      let z2 = y * Math.sin(rx) + z1 * Math.cos(rx);

      // Camera offset
      const finalZ = z2 + 250;

      if (finalZ <= 30) return null;

      const scale = focalLength / finalZ;
      return {
        x: width / 2 + x1 * scale,
        y: height / 2 + y2 * scale,
        scale,
        depth: finalZ,
      };
    };

    // Main animation loop
    const animate = () => {
      // Clear screen with a solid pitch black/dark gray base
      ctx.fillStyle = "#000000";
      ctx.fillRect(0, 0, width, height);

      // Smooth mouse rotation
      rx += (mouseRef.current.targetY * 0.08 - rx) * 0.05;
      ry += (mouseRef.current.targetX * 0.08 - ry) * 0.05;

      // Animate pulse
      pulseRadius += pulseSpeed;
      if (pulseRadius > pulseMaxRadius) {
        pulseRadius = 0;
      }

      // Animate grid offset
      gridZOffset -= gridSpeed;
      if (gridZOffset < -gridSpacing) {
        gridZOffset += gridSpacing;
      }

      // Helper to draw a single grid plane
      const drawGridPlane = (yPos: number) => {
        // 1. Draw Longitudinal lines (Z lines going to horizon)
        for (let x = -gridWidth; x <= gridWidth; x += gridSpacing) {
          for (let z = 50; z < maxZ; z += 50) {
            const p1 = project(x, yPos, z);
            const p2 = project(x, yPos, z + 50);

            if (p1 && p2) {
              const midZ = z + 25;
              const depthFade = Math.max(0, 1 - midZ / maxZ);

              // Calculate radial distance from pulse center in 3D space
              const distToPulse = Math.sqrt(x * x + yPos * yPos + Math.pow(midZ - pulseCenterZ, 2));
              const pulseDiff = Math.abs(distToPulse - pulseRadius);
              
              let pulseIntensity = 0;
              if (pulseDiff < 180) {
                pulseIntensity = Math.pow(1 - pulseDiff / 180, 2);
              }

              // Blend colors dynamically: metallic silver for ambient, white for pulse
              const alpha = (0.06 + pulseIntensity * 0.22) * depthFade;
              
              ctx.beginPath();
              ctx.moveTo(p1.x, p1.y);
              ctx.lineTo(p2.x, p2.y);
              ctx.lineWidth = pulseIntensity > 0.1 ? 1.5 : 1;
              ctx.strokeStyle = pulseIntensity > 0.1 
                ? `rgba(255, 255, 255, ${alpha * 1.5})` 
                : `rgba(113, 113, 122, ${alpha})`;
              ctx.stroke();
            }
          }
        }

        // 2. Draw Transverse lines (X lines moving towards camera)
        for (let z = 50; z <= maxZ; z += gridSpacing) {
          const currentZ = z + gridZOffset;
          if (currentZ < 50 || currentZ > maxZ) continue;

          const depthFade = Math.max(0, 1 - currentZ / maxZ);

          for (let x = -gridWidth; x < gridWidth; x += 100) {
            const p1 = project(x, yPos, currentZ);
            const p2 = project(x + 100, yPos, currentZ);

            if (p1 && p2) {
              const midX = x + 50;
              
              // Calculate radial distance from pulse center
              const distToPulse = Math.sqrt(midX * midX + yPos * yPos + Math.pow(currentZ - pulseCenterZ, 2));
              const pulseDiff = Math.abs(distToPulse - pulseRadius);

              let pulseIntensity = 0;
              if (pulseDiff < 180) {
                pulseIntensity = Math.pow(1 - pulseDiff / 180, 2);
              }

              const alpha = (0.06 + pulseIntensity * 0.22) * depthFade;

              ctx.beginPath();
              ctx.moveTo(p1.x, p1.y);
              ctx.lineTo(p2.x, p2.y);
              ctx.lineWidth = pulseIntensity > 0.1 ? 1.5 : 1;
              ctx.strokeStyle = pulseIntensity > 0.1 
                ? `rgba(255, 255, 255, ${alpha * 1.5})` 
                : `rgba(113, 113, 122, ${alpha})`;
              ctx.stroke();
            }
          }
        }
      };

      // Draw Floor & Ceiling
      drawGridPlane(gridYFloor);
      drawGridPlane(gridYCeil);

      // Draw & Update Particles
      particles.forEach((p) => {
        // Apply movement
        p.x += p.vx;
        p.y += p.vy;
        p.z += p.vz;

        // Wrap around boundary
        if (p.z <= 10) {
          p.z = maxZ;
          p.x = (Math.random() - 0.5) * 1600;
          p.y = (Math.random() - 0.5) * 1000;
        } else if (p.z > maxZ) {
          p.z = 10;
        }

        // Calculate pulse intersection
        const distToPulse = Math.sqrt(p.x * p.x + p.y * p.y + Math.pow(p.z - pulseCenterZ, 2));
        const pulseDiff = Math.abs(distToPulse - pulseRadius);

        let pulseIntensity = 0;
        if (pulseDiff < 200) {
          pulseIntensity = Math.pow(1 - pulseDiff / 200, 1.5);
        }

        const pt = project(p.x, p.y, p.z);
        if (pt) {
          const depthFade = Math.max(0, 1 - p.z / maxZ);
          const size = p.size * pt.scale * 0.5;
          const alpha = (p.baseAlpha * depthFade) + (pulseIntensity * 0.5 * depthFade);

          ctx.beginPath();
          ctx.arc(pt.x, pt.y, Math.max(0.4, size), 0, Math.PI * 2);
          
          if (pulseIntensity > 0.1) {
            // Shiny active particle (white)
            ctx.fillStyle = `rgba(255, 255, 255, ${Math.min(1, alpha * 1.5)})`;
          } else {
            // Subtle zinc/silver particle
            ctx.fillStyle = `rgba(161, 161, 170, ${alpha})`;
          }
          ctx.fill();
        }
      });

      // Subtle vignette layer to fade edges
      const vignette = ctx.createRadialGradient(
        width / 2,
        height / 2,
        200,
        width / 2,
        height / 2,
        Math.max(width, height) * 0.7
      );
      vignette.addColorStop(0, "rgba(0, 0, 0, 0)");
      vignette.addColorStop(1, "rgba(0, 0, 0, 0.75)");
      ctx.fillStyle = vignette;
      ctx.fillRect(0, 0, width, height);

      animationFrameId = requestAnimationFrame(animate);
    };

    animate();

    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener("resize", handleResize);
      window.removeEventListener("mousemove", handleMouseMove);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      className="absolute inset-0 w-full h-full pointer-events-none select-none"
    />
  );
}
