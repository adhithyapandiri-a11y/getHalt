'use client'

import { motion } from 'framer-motion'
import { FormEvent, useState } from 'react'
import Link from 'next/link'
import { subscribeToWaitlist } from '../app/actions'

const faqs = [
  {
    question: "Can't I just uninstall the Halt application to bypass the lock?",
    answer: "No. The Halt mobile client utilizes deep Android device administrator privileges and enterprise-grade Knox profile rules. Once a lock session is initiated, the application packages are hard-locked at the kernel level. Uninstallation is completely blocked until the physical card is detected.",
  },
  {
    question: "What happens if I lose my physical Halt card?",
    answer: "Every Halt card comes paired with a secure, 24-character backup emergency recovery phrase inside your physical package. Stash this phrase safely in a physical drawer at home to initiate a delayed, 24-hour emergency override sequence.",
  },
]

export function ComingSoonSection() {
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')
  const [openIndex, setOpenIndex] = useState<number | null>(null)

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setLoading(true)
    setErrorMsg('')

    const formData = new FormData()
    formData.append('email', email)

    try {
      const res = await subscribeToWaitlist(formData)
      if (res.success) {
        setSubmitted(true)
        setEmail('')
        // Reset success banner after 5 seconds
        setTimeout(() => setSubmitted(false), 5000)
      } else {
        setErrorMsg(res.error || 'Failed to subscribe.')
      }
    } catch (err: any) {
      setErrorMsg('Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section
      id="coming-soon"
      className="min-h-screen bg-transparent px-4 sm:px-6 lg:px-8 py-24 flex items-center border-t border-white/[0.04]"
    >
      <div className="max-w-3xl mx-auto w-full">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          viewport={{ once: false }}
          className="text-center"
        >
          {/* Main Heading */}
          <h2 className="text-5xl sm:text-6xl lg:text-7xl font-light text-white mb-6 tracking-tighter leading-tight">
            halt. is coming soon.
          </h2>

          {/* Subheading */}
          <p className="text-base sm:text-lg text-[#888888] mb-12 leading-relaxed max-w-xl mx-auto font-light tracking-tight">
            Securing uninterrupted deep work for a hyper-connected generation. Sign up to gain early access to our limited initial physical release.
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
            <div className="flex flex-col sm:flex-row gap-4 max-w-md mx-auto items-stretch sm:items-end">
              <input
                type="email"
                name="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your@email.com"
                required
                className="flex-1 px-2 py-3 bg-transparent border-b border-white/10 text-white placeholder-zinc-600 focus:outline-none focus:border-white transition-colors text-base font-light tracking-tight"
              />
              <button
                type="submit"
                disabled={loading}
                className="px-6 py-3 bg-white text-black font-semibold rounded-xl hover:bg-zinc-200 transition-colors disabled:opacity-50 text-sm whitespace-nowrap cursor-pointer"
              >
                {loading ? 'Securing seat...' : 'Join Waitlist'}
              </button>
            </div>
            {errorMsg && (
              <p className="text-red-500/80 text-xs mt-3 text-center">
                {errorMsg}
              </p>
            )}
          </motion.form>

          {/* Success Message */}
          {submitted && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="mb-8 text-center"
            >
              <p className="text-zinc-500 text-sm font-sans tracking-tight">
                seat secured. check your inbox.
              </p>
            </motion.div>
          )}

          {/* Technical Specifications Section */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.1 }}
            viewport={{ once: true }}
            className="max-w-2xl mx-auto text-left mb-24 mt-24"
          >
            <h3 className="text-zinc-500 text-xs tracking-widest uppercase font-mono mb-8">
              technical architecture.
            </h3>
            <div className="space-y-6">
              <div className="grid grid-cols-3 gap-4 border-t border-white/5 pt-6">
                <span className="text-zinc-500 text-sm font-light col-span-1">dimensions</span>
                <span className="text-white text-sm font-light col-span-2">85.6mm x 54mm (standard ID-1 credit card form factor)</span>
              </div>
              <div className="grid grid-cols-3 gap-4 border-t border-white/5 pt-6">
                <span className="text-zinc-500 text-sm font-light col-span-1">material</span>
                <span className="text-white text-sm font-light col-span-2">premium matte-finish brushed composite polymer</span>
              </div>
              <div className="grid grid-cols-3 gap-4 border-t border-white/5 pt-6">
                <span className="text-zinc-500 text-sm font-light col-span-1">architecture</span>
                <span className="text-white text-sm font-light col-span-2">embedded ultra-high-frequency passive NTAG213 micro-circuitry</span>
              </div>
              <div className="grid grid-cols-3 gap-4 border-t border-white/5 pt-6">
                <span className="text-zinc-500 text-sm font-light col-span-1">power</span>
                <span className="text-white text-sm font-light col-span-2">zero battery, zero charging required (powered entirely via mobile phone RF field induction)</span>
              </div>
              <div className="grid grid-cols-3 gap-4 border-t border-white/5 pt-6">
                <span className="text-zinc-500 text-sm font-light col-span-1">compatibility</span>
                <span className="text-white text-sm font-light col-span-2">Android 10+ with active NFC subsystem hardware</span>
              </div>
            </div>
          </motion.div>

          {/* FAQ Section */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.1 }}
            viewport={{ once: true }}
            className="max-w-2xl mx-auto text-left mb-24 mt-24"
          >
            <h3 className="text-zinc-500 text-xs tracking-widest uppercase font-mono mb-8">
              frequently asked questions.
            </h3>
            <div className="border-t border-white/5">
              {faqs.map((faq, index) => {
                const isOpen = openIndex === index;
                return (
                  <div key={index} className="border-b border-white/5 py-6">
                    <button
                      type="button"
                      onClick={() => setOpenIndex(isOpen ? null : index)}
                      className="w-full flex justify-between items-center text-left text-white hover:text-zinc-300 transition-colors focus:outline-none group cursor-pointer"
                    >
                      <span className="text-sm font-medium tracking-tight">
                        {faq.question}
                      </span>
                      <span className="text-zinc-500 group-hover:text-white transition-colors ml-4 text-xs font-mono">
                        {isOpen ? '[ - ]' : '[ + ]'}
                      </span>
                    </button>
                    <motion.div
                      initial={false}
                      animate={{
                        height: isOpen ? 'auto' : 0,
                        opacity: isOpen ? 1 : 0,
                        marginTop: isOpen ? 12 : 0
                      }}
                      transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
                      className="overflow-hidden"
                    >
                      <p className="text-zinc-400 text-sm leading-relaxed font-light">
                        {faq.answer}
                      </p>
                    </motion.div>
                  </div>
                );
              })}
            </div>
          </motion.div>

          {/* Footer */}
          <motion.div
            className="pt-12 border-t border-white/[0.04]"
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            transition={{ duration: 0.8, delay: 0.3 }}
            viewport={{ once: false }}
          >
            <p className="text-xs font-normal tracking-wide text-zinc-500 mb-4">
              © 2026 Halt Tech. All rights reserved. • Engineered in Jagtial, Telangana, India.
            </p>
            <div className="flex justify-center gap-6 text-xs font-normal tracking-wide items-center">
              <a href="#privacy" className="text-zinc-500 hover:text-white transition-colors duration-200">
                Privacy Policy
              </a>
              <a href="#terms" className="text-zinc-500 hover:text-white transition-colors duration-200">
                Terms of Service
              </a>
              <Link
                href="https://instagram.com/gethalt"
                target="_blank"
                rel="noopener noreferrer"
                className="text-zinc-500 hover:text-white transition-colors duration-200"
              >
                <svg
                  className="w-4 h-4"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  viewBox="0 0 24 24"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <rect x="2" y="2" width="20" height="20" rx="5" ry="5"/>
                  <circle cx="12" cy="12" r="3"/>
                  <circle cx="17.5" cy="6.5" r="1"/>
                </svg>
              </Link>
            </div>
          </motion.div>
        </motion.div>
      </div>
    </section>
  )
}
