'use client'

import { motion } from 'framer-motion'
import { FormEvent, useState } from 'react'
import Link from 'next/link'

export function ComingSoonSection() {
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setLoading(true)

    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000))

    setSubmitted(true)
    setEmail('')
    setLoading(false)

    // Reset after 3 seconds
    setTimeout(() => setSubmitted(false), 3000)
  }

  return (
    <section
      id="coming-soon"
      className="min-h-screen bg-black px-4 sm:px-6 lg:px-8 py-24 flex items-center border-t border-[#1f1f1f]"
    >
      <div className="max-w-3xl mx-auto w-full">
        <motion.div
          initial={{ opacity: 0, y: 50 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          viewport={{ once: false }}
          className="text-center"
        >
          {/* Main Heading */}
          <h2 className="text-5xl sm:text-6xl lg:text-7xl font-bold text-white mb-6 tracking-tighter leading-tight">
            halt. is coming soon.
          </h2>

          {/* Subheading */}
          <p className="text-lg sm:text-xl text-[#888888] mb-12 leading-relaxed">
            Batch 01 is strictly limited to 10 physical cards. Secure your place in the priority waitlist.
          </p>

          {/* Email Form */}
          <motion.form
            onSubmit={handleSubmit}
            className="mb-16"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            viewport={{ once: false }}
          >
            <div className="flex flex-col sm:flex-row gap-4 max-w-md mx-auto">
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your@email.com"
                required
                className="flex-1 px-0 py-3 bg-transparent border-b border-[#1f1f1f] text-white placeholder-[#888888] focus:outline-none focus:border-white transition-colors text-lg"
              />
              <motion.button
                type="submit"
                disabled={loading}
                className="px-8 py-3 bg-white text-black font-semibold rounded-lg hover:bg-gray-200 transition-colors disabled:opacity-50 text-lg whitespace-nowrap"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
              >
                {loading ? 'Joining...' : 'Join Waitlist'}
              </motion.button>
            </div>
          </motion.form>

          {/* Success Message */}
          {submitted && (
            <motion.div
              initial={{ opacity: 0, y: -20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="mb-8 text-center"
            >
              <p className="text-white text-lg font-semibold">
                You&apos;re on the list! Check your email for updates.
              </p>
            </motion.div>
          )}

          {/* Footer */}
          <motion.div
            className="pt-12 border-t border-[#1f1f1f]"
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            transition={{ duration: 0.8, delay: 0.3 }}
            viewport={{ once: false }}
          >
            <p className="text-[#888888] text-sm mb-4">© 2026 Halt Tech. All rights reserved</p>
            <div className="flex justify-center gap-6 text-sm items-center">
              <a href="#" className="text-[#888888] hover:text-white transition-colors">
                Privacy
              </a>
              <a href="#" className="text-[#888888] hover:text-white transition-colors">
                Terms
              </a>
              <Link
                href="https://instagram.com/gethalt"
                target="_blank"
                rel="noopener noreferrer"
                className="text-[#888888] hover:text-white transition-colors"
              >
                <svg
                  className="w-5 h-5"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <rect x="2" y="2" width="20" height="20" rx="5" ry="5" fill="none" stroke="currentColor" strokeWidth="2"/>
                  <circle cx="12" cy="12" r="3" fill="currentColor"/>
                  <circle cx="17.5" cy="6.5" r="1.5" fill="currentColor"/>
                </svg>
              </Link>
            </div>
          </motion.div>
        </motion.div>
      </div>
    </section>
  )
}
