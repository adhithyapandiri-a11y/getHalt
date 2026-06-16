'use client'

import { motion } from 'framer-motion'
import { useInView } from 'react-intersection-observer'

export function PhilosophySection() {
  const { ref, inView } = useInView({
    threshold: 0.4,
    triggerOnce: false,
  })

  return (
    <section
      ref={ref}
      className="min-h-[60vh] bg-transparent flex items-center justify-center px-4 sm:px-6 lg:px-8 select-none"
    >
      <motion.div
        initial={{ opacity: 0 }}
        animate={inView ? { opacity: 1 } : { opacity: 0 }}
        transition={{ duration: 1 }}
        className="text-center max-w-4xl"
      >
        <motion.h2
          initial={{ y: 30, opacity: 0 }}
          animate={inView ? { y: 0, opacity: 1 } : { y: 30, opacity: 0 }}
          transition={{ duration: 1, delay: 0.1 }}
          className="text-5xl sm:text-6xl lg:text-7xl font-light text-white leading-tight tracking-tighter"
        >
          you don&apos;t need another app.
          <br />
          <motion.span
            initial={{ color: '#555555' }}
            animate={inView ? { color: '#888888' } : { color: '#555555' }}
            transition={{ duration: 1.2, delay: 0.4 }}
            className="block font-normal italic mt-2"
          >
            you need physical friction.
          </motion.span>
        </motion.h2>
      </motion.div>
    </section>
  )
}

