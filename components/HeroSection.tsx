'use client'

import { useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import InteractiveCard from '@/components/InteractiveCard'

export function HeroSection() {
  const [phraseNumber, setPhraseNumber] = useState(0)
  const phrases = useMemo(
    () => [
      "this is deep work.",
      "this is absolute focus.",
      "this is presence.",
      "this is undisrupted."
    ],
    []
  )

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (phraseNumber === phrases.length - 1) {
        setPhraseNumber(0)
      } else {
        setPhraseNumber(phraseNumber + 1)
      }
    }, 2500)
    return () => clearTimeout(timeoutId)
  }, [phraseNumber, phrases])

  const handleScrollToWaitlist = () => {
    const section = document.getElementById('coming-soon')
    if (section) {
      section.scrollIntoView({ behavior: 'smooth' })
    }
  }

  return (
    <section id="hero-viewport" className="relative min-h-screen pt-24 pb-16 px-4 sm:px-6 lg:px-8 flex items-center bg-transparent overflow-hidden select-none">
      <div className="max-w-7xl mx-auto w-full relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 items-center">
          {/* Left Column */}
          <motion.div
            className="lg:col-span-7 flex flex-col items-start gap-8"
            initial={{ opacity: 0, x: -50 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
          >
            {/* Headline */}
            <div className="flex flex-col gap-3 w-full">
              <h1 className="text-5xl sm:text-6xl lg:text-7xl font-light tracking-tighter leading-[0.95] text-white">
                digital willpower <br className="hidden md:block"/>
                is <span className="font-normal italic text-zinc-500">a lie.</span>
              </h1>
              
              {/* Cycling Brand Phrase */}
              <div className="relative h-8 w-full overflow-hidden">
                {phrases.map((phrase, index) => (
                  <motion.span
                    key={index}
                    className="absolute left-0 text-lg sm:text-xl font-light text-white tracking-tighter"
                    initial={{ opacity: 0, y: 20 }}
                    animate={
                      phraseNumber === index
                        ? {
                            y: 0,
                            opacity: 1,
                          }
                        : {
                            y: phraseNumber > index ? -20 : 20,
                            opacity: 0,
                          }
                    }
                    transition={{ type: "spring", stiffness: 80, damping: 15 }}
                  >
                    {phrase}
                  </motion.span>
                ))}
              </div>
            </div>

            {/* Subheading */}
            <p className="text-lg md:text-xl text-[#888888] max-w-xl font-light leading-relaxed tracking-tight">
              Software blockers fail because you can always click &apos;Ignore Limit&apos;. Halt is the un-bypassable physical, matte black NFC keycard paired with a hard-lock mobile client. 
              No digital bypasses. No passcodes. The only way back in is a physical tap.
            </p>

            {/* CTA Button */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 w-full sm:w-auto">
              <motion.button
                onClick={handleScrollToWaitlist}
                className="px-8 py-4 bg-white text-black font-semibold rounded-xl hover:bg-white/90 shadow-[0_0_20px_rgba(255,255,255,0.04)] hover:shadow-[0_0_25px_rgba(255,255,255,0.1)] transition-all duration-300 text-sm cursor-pointer"
                whileHover={{ scale: 1.01 }}
                whileTap={{ scale: 0.99 }}
              >
                Join Priority Waitlist
              </motion.button>
            </div>

            {/* Product Specs Footer */}
            <div className="flex flex-wrap items-center gap-3 pt-10 border-t border-white/[0.04] w-full max-w-lg mt-4">
              <span className="text-xs font-sans border border-white/10 px-3.5 py-1.5 rounded-full text-zinc-300 bg-white/[0.01] tracking-tight hover:border-white/20 transition-colors duration-300">
                matte black nfc
              </span>
              <span className="text-xs font-sans border border-white/10 px-3.5 py-1.5 rounded-full text-zinc-300 bg-white/[0.01] tracking-tight hover:border-white/20 transition-colors duration-300">
                zero battery
              </span>
              <span className="text-xs font-sans border border-white/10 px-3.5 py-1.5 rounded-full text-zinc-300 bg-white/[0.01] tracking-tight hover:border-white/20 transition-colors duration-300">
                zero loopholes
              </span>
            </div>
          </motion.div>

          {/* Right Column - 3D Card Viewport */}
          <motion.div
            className="lg:col-span-5 flex flex-col items-center justify-center relative py-12 lg:py-0"
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
          >
            {/* Soft Backlight Glow */}
            <div className="absolute inset-0 max-w-[500px] aspect-[1.67/1] bg-[radial-gradient(circle_at_center,rgba(255,255,255,0.03)_0%,transparent_60%)] pointer-events-none blur-xl z-0" />

            {/* The 3D Interactive Card Component */}
            <div className="z-10 w-full flex justify-center py-6">
              <InteractiveCard />
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  )
}


