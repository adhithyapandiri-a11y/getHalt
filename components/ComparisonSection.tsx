'use client';

import React from 'react';
import { motion } from 'framer-motion';

export function ComparisonSection() {
  return (
    <section className="py-32 bg-[#000000] border-t border-white/[0.05] select-none relative z-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section title at the top */}
        <div className="mb-20 text-center lg:text-left">
          <span className="text-zinc-500 text-xs font-mono tracking-[0.35em] uppercase">
            The Friction Deficit
          </span>
        </div>
        
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 lg:gap-24 items-center">
          
          {/* Left Column - Side-by-side Android Phone Mockups */}
          <div className="lg:col-span-7 flex flex-row items-center justify-center gap-4 sm:gap-6 md:gap-8">
            
            {/* Phone A - No Loopholes */}
            <motion.div 
              className="flex flex-col items-center"
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
            >
              <span className="text-white text-xs sm:text-sm font-medium tracking-tight mb-4 text-center block">
                No Loopholes
              </span>
              
              {/* Device Container */}
              <div className="w-[145px] sm:w-[170px] md:w-[190px] aspect-[9/18.5] bg-black border-[4px] border-zinc-800 rounded-[28px] relative overflow-hidden flex flex-col justify-between p-4 shadow-[0_25px_50px_-12px_rgba(0,0,0,0.8)]">
                {/* Punch-hole camera */}
                <div className="absolute top-2.5 left-1/2 -translate-x-1/2 w-2 h-2 bg-zinc-900 rounded-full z-20" />
                
                {/* Status Bar */}
                <div className="flex justify-between items-center text-[8px] text-zinc-600 font-mono px-1 z-20">
                  <span>9:41</span>
                  <span>NFC</span>
                </div>
                
                {/* Content Panel */}
                <div className="flex-1 flex flex-col items-center justify-center text-center mt-6">
                  {/* Lock graphic / icon */}
                  <svg className="w-8 h-8 text-zinc-500 mb-4 opacity-80" fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                    <path d="M7 11V7a5 5 0 0110 0v4"/>
                  </svg>
                  <h4 className="text-white text-lg sm:text-xl font-bold tracking-tighter mb-2 leading-none">
                    halted.
                  </h4>
                  <p className="text-zinc-500 text-[10px] sm:text-[11px] leading-relaxed max-w-[130px] mx-auto font-light">
                    Tap your card to regain entry.
                  </p>
                </div>
                
                {/* Home Indicator */}
                <div className="w-12 h-0.5 bg-zinc-800 rounded-full mx-auto mt-2" />
              </div>
            </motion.div>

            {/* Separator - vs */}
            <span className="font-serif italic text-zinc-600 text-xl sm:text-2xl select-none mx-1 sm:mx-2 self-center pt-8">
              vs
            </span>

            {/* Phone B - Easy To Bypass */}
            <motion.div 
              className="flex flex-col items-center"
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-100px" }}
              transition={{ duration: 0.8, delay: 0.1, ease: [0.16, 1, 0.3, 1] }}
            >
              <span className="text-zinc-500 text-xs sm:text-sm font-medium tracking-tight mb-4 text-center block">
                Easy To Bypass
              </span>
              
              {/* Device Container */}
              <div className="w-[145px] sm:w-[170px] md:w-[190px] aspect-[9/18.5] bg-[#0A0A0A] border-[4px] border-zinc-900 rounded-[28px] relative overflow-hidden flex flex-col justify-between p-4 shadow-[0_25px_50px_-12px_rgba(0,0,0,0.8)] opacity-60">
                {/* Punch-hole camera */}
                <div className="absolute top-2.5 left-1/2 -translate-x-1/2 w-2 h-2 bg-zinc-900/60 rounded-full z-20" />
                
                {/* Status Bar */}
                <div className="flex justify-between items-center text-[8px] text-zinc-700 font-mono px-1 z-20">
                  <span>9:41</span>
                  <span>LTE</span>
                </div>
                
                {/* Content Panel */}
                <div className="flex-1 flex flex-col items-center justify-center mt-6">
                  {/* Android Style Warning Alert */}
                  <div className="bg-zinc-900/90 border border-zinc-800/80 rounded-xl p-3 w-full text-center shadow-lg">
                    <h5 className="text-zinc-300 text-[11px] font-medium tracking-tight mb-1">
                      App Limit
                    </h5>
                    <p className="text-zinc-500 text-[9px] leading-normal mb-3 font-light">
                      You&apos;ve reached your limit for Instagram.
                    </p>
                    
                    {/* Bypass Buttons */}
                    <button className="border border-zinc-800 text-zinc-400 text-[9px] py-1.5 px-2 rounded-md bg-zinc-950 font-medium w-full cursor-pointer hover:bg-zinc-900 transition-colors">
                      Ignore Limit
                    </button>
                    <span className="text-zinc-600 text-[9px] mt-2 block hover:text-zinc-500 cursor-pointer font-light">
                      One More Minute
                    </span>
                  </div>
                </div>
                
                {/* Home Indicator */}
                <div className="w-12 h-0.5 bg-zinc-900 rounded-full mx-auto mt-2" />
              </div>
            </motion.div>

          </div>
          
          {/* Right Column - The Copy Deck */}
          <div className="lg:col-span-5 flex flex-col items-start text-left">
            <span className="text-xs font-mono tracking-[0.2em] text-zinc-500 uppercase mb-4 block">
              REAL FRICTION
            </span>
            <h3 className="text-3xl sm:text-4xl md:text-5xl font-light mb-6 leading-tight tracking-tighter text-white">
              what makes halt. different?
            </h3>
            <p className="text-zinc-400 text-sm sm:text-base md:text-lg font-light leading-relaxed tracking-tight max-w-lg">
              Standard screen-time software can be completely bypassed in two clicks by hitting &apos;Ignore Limit&apos;. Halt introduces un-bypassable physical architecture. By separating the key to your digital landscape from your immediate environment, you introduce real physical friction to stay locked in.
            </p>
          </div>

        </div>
      </div>
    </section>
  );
}
