import { Navigation } from '@/components/Navigation'
import DotMatrix from '@/components/DotMatrix'
import Link from 'next/link'

export const metadata = {
  title: 'halt. - Privacy Policy',
  description: 'Privacy Policy for the Halt Android application.',
}

export default function PrivacyPolicy() {
  return (
    <main className="bg-black relative min-h-screen overflow-x-hidden text-white font-sans selection:bg-white/10">
      {/* Background Layers */}
      <div className="absolute inset-0 pointer-events-none z-0 overflow-hidden">
        <DotMatrix />
      </div>

      {/* Page Content */}
      <div className="relative z-10 flex flex-col min-h-screen">
        <Navigation />

        <div className="flex-grow max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 pt-32 pb-24">
          <div className="mb-12">
            <span className="text-zinc-500 text-xs font-mono tracking-[0.35em] uppercase block mb-4">
              Legal & Trust
            </span>
            <h1 className="text-4xl sm:text-5xl font-light tracking-tighter leading-tight text-white mb-2">
              Privacy Policy <br/>
              <span className="font-normal italic text-zinc-500">for Halt</span>
            </h1>
            <p className="text-xs font-mono text-zinc-500 mt-4">
              Last updated: June 19, 2026
            </p>
          </div>

          <div className="space-y-10 border-t border-white/[0.04] pt-10">
            <section className="space-y-4">
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                At Halt, accessible from <Link href="https://gethalt.in" className="text-white hover:underline">https://gethalt.in</Link>, one of our main priorities is the privacy of our users. This Privacy Policy document outlines the types of information handled by the Halt Android application.
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-mono text-zinc-300 uppercase tracking-wider">
                1. Information Collection and Use
              </h2>
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                The Halt application functions as a local productivity tool. The application requires specific system-level permissions to operate:
              </p>
              <ul className="space-y-3 pl-4 border-l border-white/10 mt-4">
                <li className="text-sm font-light text-zinc-400">
                  <span className="text-white font-medium">Package Usage Stats (PACKAGE_USAGE_STATS)</span>: Used strictly on-device to detect when designated application packages enter the foreground.
                </li>
                <li className="text-sm font-light text-zinc-400">
                  <span className="text-white font-medium">System Alert Window (SYSTEM_ALERT_WINDOW)</span>: Used locally to draw the full-screen blocking overlay interface.
                </li>
                <li className="text-sm font-light text-zinc-400">
                  <span className="text-white font-medium">NFC (Near Field Communication)</span>: Used locally to scan the physical hardware card to release application locks.
                </li>
              </ul>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-mono text-zinc-300 uppercase tracking-wider">
                2. Zero Data Transmission
              </h2>
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                Halt does not collect, log, store, or transmit any personal data, usage history, application tracking logs, or hardware identifier payloads to external servers or cloud databases. All core monitoring routines operate purely in volatile memory on the local device and are discarded instantly.
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-mono text-zinc-300 uppercase tracking-wider">
                3. Third-Party Access
              </h2>
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                Because the application does not transmit data over the internet, no user information is shared with third-party analytics platforms, advertising networks, or data brokers.
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-mono text-zinc-300 uppercase tracking-wider">
                4. Contact Us
              </h2>
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                If you have additional questions or require more information about our Privacy Policy, do not hesitate to contact us at <a href="mailto:hello@gethalt.in" className="text-white hover:underline">hello@gethalt.in</a>.
              </p>
            </section>
          </div>

          {/* Simple back navigation */}
          <div className="mt-16 pt-8 border-t border-white/[0.04] flex justify-between items-center">
            <Link 
              href="/" 
              className="text-xs font-mono text-zinc-500 hover:text-white transition-colors duration-200"
            >
              [ ← Back to home ]
            </Link>
            <p className="text-[10px] font-mono text-zinc-600">
              © 2026 Halt Tech.
            </p>
          </div>
        </div>
      </div>
    </main>
  )
}
