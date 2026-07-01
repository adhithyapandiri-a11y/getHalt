'use client'

import { motion } from 'framer-motion'
import { FormEvent, useState } from 'react'
import { Navigation } from '@/components/Navigation'
import DotMatrix from '@/components/DotMatrix'
import ParticleGridBackground from '@/components/ParticleGridBackground'
import { submitSupportMessage } from '@/app/actions'
import Link from 'next/link'

export default function SupportPage() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setLoading(true)
    setErrorMsg('')

    const formData = new FormData()
    formData.append('name', name)
    formData.append('email', email)
    formData.append('message', message)

    try {
      const res = await submitSupportMessage(formData)
      if (res.success) {
        setSubmitted(true)
        setName('')
        setEmail('')
        setMessage('')
        // Reset success banner after 5 seconds
        setTimeout(() => setSubmitted(false), 5000)
      } else {
        setErrorMsg(res.error || 'Failed to send message.')
      }
    } catch (err: any) {
      setErrorMsg('Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="bg-black relative min-h-screen overflow-x-hidden">
      {/* Background Layers */}
      <div className="absolute inset-0 pointer-events-none z-0 overflow-hidden">
        {/* Canvas Background - constrained to hero (h-screen) */}
        <div className="absolute top-0 left-0 w-full h-screen overflow-hidden">
          <ParticleGridBackground />
        </div>
        {/* Page-wide Dot Matrix overlay */}
        <DotMatrix />
      </div>

      {/* Page Content */}
      <div className="relative z-10 flex flex-col min-h-screen">
        <Navigation />
        
        <div className="flex-grow flex items-center justify-center px-4 sm:px-6 lg:px-8 py-24">
          <div className="max-w-xl w-full mx-auto">
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8 }}
              className="text-center"
            >
              <h2 className="text-4xl sm:text-5xl lg:text-6xl font-light text-white mb-6 tracking-tighter leading-tight">
                how can we help?
              </h2>
              
              <p className="text-base sm:text-lg text-[#888888] mb-12 leading-relaxed font-light tracking-tight">
                Send us a message and our support team will get back to you as soon as possible.
              </p>

              <form onSubmit={handleSubmit} className="text-left space-y-6">
                <div>
                  <label htmlFor="name" className="block text-sm font-light text-zinc-500 mb-2">Name</label>
                  <input
                    id="name"
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                    className="w-full px-4 py-3 bg-transparent border border-white/10 text-white placeholder-zinc-700 rounded-xl focus:outline-none focus:border-white/30 transition-colors text-base font-light tracking-tight"
                    placeholder="Your name"
                  />
                </div>

                <div>
                  <label htmlFor="email" className="block text-sm font-light text-zinc-500 mb-2">Email</label>
                  <input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    className="w-full px-4 py-3 bg-transparent border border-white/10 text-white placeholder-zinc-700 rounded-xl focus:outline-none focus:border-white/30 transition-colors text-base font-light tracking-tight"
                    placeholder="your@email.com"
                  />
                </div>

                <div>
                  <label htmlFor="message" className="block text-sm font-light text-zinc-500 mb-2">Message</label>
                  <textarea
                    id="message"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    required
                    rows={5}
                    className="w-full px-4 py-3 bg-transparent border border-white/10 text-white placeholder-zinc-700 rounded-xl focus:outline-none focus:border-white/30 transition-colors text-base font-light tracking-tight resize-none"
                    placeholder="How can we assist you today?"
                  ></textarea>
                </div>

                {errorMsg && (
                  <p className="text-red-500/80 text-sm mt-3 text-center">
                    {errorMsg}
                  </p>
                )}

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-4 bg-white text-black font-semibold rounded-xl hover:bg-zinc-200 transition-colors disabled:opacity-50 text-sm cursor-pointer"
                >
                  {loading ? 'Sending message...' : 'Send Message'}
                </button>
              </form>

              {submitted && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  className="mt-8 text-center"
                >
                  <p className="text-zinc-500 text-sm font-sans tracking-tight">
                    message received. we'll be in touch.
                  </p>
                </motion.div>
              )}
            </motion.div>
          </div>
        </div>

        {/* Footer */}
        <motion.div
          className="pb-8 px-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.8, delay: 0.3 }}
        >
          <div className="flex justify-center gap-6 text-xs font-normal tracking-wide items-center">
            <Link href="/" className="text-zinc-500 hover:text-white transition-colors duration-200">
              Home
            </Link>
            <Link href="/privacy" className="text-zinc-500 hover:text-white transition-colors duration-200">
              Privacy Policy
            </Link>
            <Link href="/terms" className="text-zinc-500 hover:text-white transition-colors duration-200">
              Terms & Conditions
            </Link>
          </div>
        </motion.div>
      </div>
    </main>
  )
}
