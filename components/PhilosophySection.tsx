'use client'

import { motion } from 'framer-motion'
import { useInView } from 'react-intersection-observer'

export function PhilosophySection() {
  const { ref, inView } = useInView({
    threshold: 0.5,
    triggerOnce: false,
  })

  return (
    <section
      ref={ref}
      className="min-h-screen h-96 sm:h-[70vh] bg-black flex items-center justify-center px-4 sm:px-6 lg:px-8"
    >
      <motion.div
        initial={{ opacity: 0 }}
        animate={inView ? { opacity: 1 } : { opacity: 0 }}
        transition={{ duration: 1.2 }}
        className="text-center max-w-4xl"
      >
        <motion.h2
          initial={{ y: 50, opacity: 0 }}
          animate={inView ? { y: 0, opacity: 1 } : { y: 50, opacity: 0 }}
          transition={{ duration: 1.2, delay: 0.2 }}
          className="text-5xl sm:text-6xl lg:text-7xl font-bold text-white leading-tight tracking-tighter"
        >
          You don&apos;t need another app.
          <br />
          <motion.span
            initial={{ color: '#888888' }}
            animate={inView ? { color: '#ffffff' } : { color: '#888888' }}
            transition={{ duration: 1.5, delay: 0.5 }}
            className="block"
          >
            You need physical friction.
          </motion.span>
        </motion.h2>
      </motion.div>
    </section>
  )
}
