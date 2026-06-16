"use client";

import React, { useCallback, useEffect, useRef, useState } from "react";

type Pixel = {
  x: number;
  y: number;
  color: string;
  ctx: CanvasRenderingContext2D;
  speed: number;
  size: number;
  sizeStep: number;
  minSize: number;
  maxSizeInt: number;
  maxSize: number;
  delay: number;
  counter: number;
  counterStep: number;
  isIdle: boolean;
  isReverse: boolean;
  isShimmer: boolean;
  draw: () => void;
  appear: () => void;
  disappear: () => void;
  shimmer: () => void;
};

function createPixel(
  ctx: CanvasRenderingContext2D,
  canvas: HTMLCanvasElement,
  x: number,
  y: number,
  color: string,
  baseSpeed: number,
  delay: number
): Pixel {
  const maxSizeInt = Math.round(Math.random() * 4) + 2;
  const minSize = 0;
  const maxSize = maxSizeInt;
  const sizeStep = (maxSize - minSize) / 50;
  const speed = baseSpeed * Math.random();

  const pixel: Pixel = {
    x,
    y,
    color,
    ctx,
    speed,
    size: 0,
    sizeStep,
    minSize,
    maxSizeInt,
    maxSize,
    delay,
    counter: 0,
    counterStep: Math.random() * 4 + 4,
    isIdle: true,
    isReverse: false,
    isShimmer: false,

    draw() {
      this.ctx.globalAlpha = 0.8;
      this.ctx.fillStyle = this.color;
      this.ctx.fillRect(this.x, this.y, this.size, this.size);
      this.ctx.globalAlpha = 1;
    },

    appear() {
      if (this.counter >= this.delay) {
        if (this.size < this.maxSize) {
          this.size += this.sizeStep;
        } else {
          this.isIdle = false;
          this.isShimmer = true;
        }
      }
      this.counter += this.counterStep;
    },

    disappear() {
      if (this.size > this.minSize) {
        this.size -= this.sizeStep;
      } else {
        this.isReverse = true;
        this.counter = 0;
        this.isShimmer = false;
      }
    },

    shimmer() {
      this.x += this.speed;
      if (!this.isReverse) {
        this.disappear();
      } else {
        this.isIdle = true;
        this.isShimmer = false;
        this.size = 0;
        this.counter = 0;
      }
    },
  };

  return pixel;
}

function getColor(): string {
  const colors = ["#3b82f6", "#60a5fa", "#1e40af"];
  return colors[Math.floor(Math.random() * colors.length)];
}

function useResizeCanvas(): {
  canvas: React.RefObject<HTMLCanvasElement>;
  size: { width: number; height: number };
} {
  const canvas = useRef<HTMLCanvasElement>(null);
  const [size, setSize] = useState({ width: 0, height: 0 });

  useEffect(() => {
    const handleResize = () => {
      if (canvas.current) {
        const width = window.innerWidth;
        const height = window.innerHeight;
        canvas.current.width = width;
        canvas.current.height = height;
        setSize({ width, height });
      }
    };

    handleResize();
    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  return { canvas, size };
}

export function PixelBackground() {
  const { canvas, size } = useResizeCanvas();
  const pixelsRef = useRef<Pixel[]>([]);
  const animationIdRef = useRef<number>();

  const handleCanvasClick = useCallback(
    (e: React.MouseEvent<HTMLCanvasElement>) => {
      if (!canvas.current) return;

      const rect = canvas.current.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;

      const ctx = canvas.current.getContext("2d");
      if (!ctx) return;

      const pixelCount = 20;
      for (let i = 0; i < pixelCount; i++) {
        const angle = (i / pixelCount) * Math.PI * 2;
        const velocity = 2 + Math.random() * 3;
        const distance = 50 + Math.random() * 100;

        const pixelX = x + Math.cos(angle) * distance;
        const pixelY = y + Math.sin(angle) * distance;

        const pixel = createPixel(
          ctx,
          canvas.current,
          pixelX,
          pixelY,
          getColor(),
          velocity,
          0
        );

        pixel.x = x;
        pixel.y = y;
        pixelsRef.current.push(pixel);
      }
    },
    [canvas]
  );

  useEffect(() => {
    const ctx = canvas.current?.getContext("2d");
    if (!ctx || size.width === 0) return;

    const animate = () => {
      ctx.clearRect(0, 0, size.width, size.height);

      pixelsRef.current.forEach((pixel) => {
        if (pixel.isIdle) {
          pixel.appear();
        } else if (pixel.isShimmer) {
          pixel.shimmer();
        }
        pixel.draw();
      });

      pixelsRef.current = pixelsRef.current.filter((pixel) => !pixel.isReverse);

      animationIdRef.current = requestAnimationFrame(animate);
    };

    animationIdRef.current = requestAnimationFrame(animate);

    return () => {
      if (animationIdRef.current) {
        cancelAnimationFrame(animationIdRef.current);
      }
    };
  }, [size]);

  return (
    <canvas
      ref={canvas}
      onClick={handleCanvasClick}
      className="fixed inset-0 w-full h-full bg-black cursor-crosshair pointer-events-auto"
    />
  );
}
