'use client'

import { motion } from 'framer-motion'
import Link from 'next/link'

export function Navigation() {
  return (
    <motion.nav
      className="fixed top-0 left-0 right-0 z-50 backdrop-blur-md bg-black/40 border-b border-white/[0.04] select-none"
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/" className="text-xl font-bold tracking-tighter text-white hover:text-zinc-300 transition-colors cursor-pointer">
            halt.
          </Link>
        </div>
      </div>
    </motion.nav>
  )
}


