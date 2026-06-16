'use client'

import { motion } from 'framer-motion'
import { useInView } from 'react-intersection-observer'

const steps = [
  {
    number: '01',
    title: 'Download & Lock.',
    description: 'Select addictive apps.',
  },
  {
    number: '02',
    title: 'Separate.',
    description: 'Leave the matte black card in another room.',
  },
  {
    number: '03',
    title: 'Reclaim.',
    description: 'Apps are hard-locked. No passcodes. You must physically tap the card to your phone to unlock.',
  },
]

export function MechanismSection() {
  const { ref, inView } = useInView({
    threshold: 0.2,
    triggerOnce: false,
  })

  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.2,
        delayChildren: 0.1,
      },
    },
  }

  const itemVariants = {
    hidden: { opacity: 0, y: 30 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { duration: 0.8, ease: 'easeOut' },
    },
  }

  return (
    <section
      ref={ref}
      className="py-32 px-4 sm:px-6 lg:px-8 bg-black border-t border-[#1f1f1f]"
    >
      <div className="max-w-7xl mx-auto">
        <motion.div
          variants={containerVariants}
          initial="hidden"
          animate={inView ? 'visible' : 'hidden'}
          className="grid grid-cols-1 md:grid-cols-3 gap-8"
        >
          {steps.map((step, index) => (
            <motion.div
              key={index}
              variants={itemVariants}
              className="p-8 border border-[#1f1f1f] rounded-lg bg-transparent hover:border-[#888888] transition-colors group"
            >
              <div className="text-5xl font-bold text-[#1f1f1f] group-hover:text-[#888888] transition-colors mb-4">
                {step.number}
              </div>
              <h3 className="text-2xl font-bold text-white mb-3">{step.title}</h3>
              <p className="text-[#888888] text-lg leading-relaxed">
                {step.description}
              </p>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  )
}
