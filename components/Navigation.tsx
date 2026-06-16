'use client'

import { motion } from 'framer-motion'

export function Navigation() {
  const handleJoinWaitlist = () => {
    const comingSoonSection = document.getElementById('coming-soon')
    if (comingSoonSection) {
      comingSoonSection.scrollIntoView({ behavior: 'smooth' })
    }
  }

  return (
    <motion.nav
      className="fixed top-0 left-0 right-0 z-50 backdrop-blur-md bg-black/30 border-b border-[#1f1f1f]"
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      transition={{ duration: 0.5 }}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <motion.div
          className="text-2xl font-bold text-white tracking-tighter"
          whileHover={{ scale: 1.05 }}
        >
          halt.
        </motion.div>

        <motion.button
          onClick={handleJoinWaitlist}
          className="px-6 py-2 rounded-full bg-white text-black font-semibold text-sm hover:bg-gray-200 transition-colors"
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
        >
          Join Waitlist
        </motion.button>
      </div>
    </motion.nav>
  )
}
