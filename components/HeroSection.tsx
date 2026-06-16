'use client'

import { motion } from 'framer-motion'
import { useRef, useState } from 'react'
import Image from 'next/image'

export function HeroSection() {
  const containerRef = useRef<HTMLDivElement>(null)
  const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 })
  const [rotateX, setRotateX] = useState(0)
  const [rotateY, setRotateY] = useState(0)

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!containerRef.current) return

    const rect = containerRef.current.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    const centerX = rect.width / 2
    const centerY = rect.height / 2

    const rotX = ((y - centerY) / centerY) * 15
    const rotY = ((x - centerX) / centerX) * -15

    setMousePosition({ x, y })
    setRotateX(Math.max(-15, Math.min(15, rotX)))
    setRotateY(Math.max(-15, Math.min(15, rotY)))
  }

  const handleMouseLeave = () => {
    setRotateX(0)
    setRotateY(0)
  }

  return (
    <section className="relative min-h-screen pt-24 pb-16 px-4 sm:px-6 lg:px-8 flex items-center bg-black overflow-hidden">
      <div className="max-w-7xl mx-auto w-full relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          {/* Left Column */}
          <motion.div
            initial={{ opacity: 0, x: -50 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
          >
            {/* Badge */}
            <div className="mb-8">
              <motion.div
                className="inline-flex items-center gap-2 px-4 py-2 rounded-full border border-[#1f1f1f] bg-[#0a0a0a]"
                whileHover={{ borderColor: '#888888' }}
              >
                <span className="text-sm text-[#888888]">Engineered for India 🇮🇳</span>
              </motion.div>
            </div>

            {/* Headline */}
            <h1 className="text-5xl sm:text-6xl lg:text-7xl font-bold text-white leading-tight mb-6 tracking-tighter">
              digital willpower is a lie.
            </h1>

            {/* Subheading */}
            <p className="text-lg sm:text-xl text-[#888888] mb-8 max-w-xl leading-relaxed">
              Software blockers fail because you can always click &apos;Ignore Limit&apos;. Halt is the un-bypassable physical circuit breaker for your screen addiction.
            </p>

            {/* CTA Button */}
            <motion.button
              className="px-8 py-4 bg-white text-black font-semibold rounded-lg hover:bg-gray-200 transition-colors text-lg"
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              Coming Soon ..........</motion.button>
          </motion.div>

          {/* Right Column - 3D Card */}
          <motion.div
            ref={containerRef}
            onMouseMove={handleMouseMove}
            onMouseLeave={handleMouseLeave}
            className="h-96 sm:h-full min-h-96 flex items-center justify-center perspective"
            style={{ perspective: '1200px' }}
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
          >
            <motion.div
              style={{
                rotateX: rotateX,
                rotateY: rotateY,
              }}
              animate={{
                y: [0, -20, 0],
              }}
              transition={{
                y: {
                  duration: 4,
                  repeat: Infinity,
                  ease: 'easeInOut',
                },
              }}
              className="w-full h-full flex items-center justify-center"
            >
              <Image
                src="/halt-card.png"
                alt="Halt Card"
                width={400}
                height={250}
                className="w-full max-w-md h-auto object-contain drop-shadow-2xl"
                style={{
                  filter: 'drop-shadow(0 20px 40px rgba(0, 0, 0, 0.8))',
                }}
              />
            </motion.div>
          </motion.div>
        </div>
      </div>
    </section>
  )
}
