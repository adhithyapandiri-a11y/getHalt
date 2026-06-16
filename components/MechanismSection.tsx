'use client';

import { motion } from 'framer-motion';
import { useInView } from 'react-intersection-observer';

const steps = [
  {
    number: '01',
    title: 'download & lock.',
    description: 'Select your distracting apps inside the software.',
  },
  {
    number: '02',
    title: 'separate.',
    description: 'Leave the physical keycard in another room to enforce distance.',
  },
  {
    number: '03',
    title: 'reclaim.',
    description: 'The apps are hard-locked. No digital loopholes. The only bypass is a physical NFC tap.',
  },
];

export function MechanismSection() {
  const { ref, inView } = useInView({
    threshold: 0.1,
    triggerOnce: true,
  });

  const containerVariants = {
    hidden: {},
    visible: {
      transition: {
        staggerChildren: 0.1,
      },
    },
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { duration: 0.6, ease: [0.16, 1, 0.3, 1] as const },
    },
  };

  return (
    <section
      ref={ref}
      className="py-48 bg-[#000000] w-full relative z-10 select-none"
    >
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate={inView ? 'visible' : 'hidden'}
        className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-7xl mx-auto px-6"
      >
        {steps.map((step, index) => (
          <motion.div
            key={index}
            variants={itemVariants}
            whileHover={{ y: -8 }}
            transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] as const }}
            className="relative overflow-hidden group bg-[#0A0A0A] border border-white/[0.04] hover:border-white/[0.15] transition-colors duration-500 rounded-2xl p-10"
          >
            {/* Spotlight Glow absolute background layer */}
            <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 bg-[radial-gradient(400px_circle_at_center,rgba(255,255,255,0.02),transparent_100%)] pointer-events-none" />

            {/* The Big Numbers at the top right */}
            <div className="absolute top-10 right-10 text-5xl font-light text-zinc-800 group-hover:text-zinc-600 transition-colors duration-300 select-none font-sans">
              {step.number}
            </div>

            {/* The Headings */}
            <h3 className="text-xl font-medium tracking-tight text-white mb-4 relative z-10">
              {step.title}
            </h3>

            {/* The Descriptions */}
            <p className="text-zinc-400 text-sm leading-relaxed relative z-10">
              {step.description}
            </p>
          </motion.div>
        ))}
      </motion.div>
    </section>
  );
}
