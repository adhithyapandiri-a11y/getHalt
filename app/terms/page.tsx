import { Navigation } from '@/components/Navigation'
import DotMatrix from '@/components/DotMatrix'
import Link from 'next/link'

export const metadata = {
  title: 'halt. - Terms of Service',
  description: 'Terms of Service for the Halt Android application.',
}

export default function TermsOfService() {
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
              Terms of Service <br/>
              <span className="font-normal italic text-zinc-500">for Halt</span>
            </h1>
            <p className="text-xs font-mono text-zinc-500 mt-4">
              Last updated: June 26, 2026
            </p>
          </div>

          <div className="space-y-10 border-t border-white/[0.04] pt-10">
            <section className="space-y-4">
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                Welcome to Halt. These Terms of Service ("Terms") govern your use of the Halt mobile application and any related services provided by Halt Tech. By using the app, you agree to these Terms. If you do not agree, do not use the application.
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-mono text-zinc-300 uppercase tracking-wider">
                1. Nature of the Service
              </h2>
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                Halt is an extreme productivity and focus tool. When a blocking session is active, you intentionally surrender access to specified applications on your device. The only way to unlock restricted apps is by physically tapping the Halt NFC card to your device or by initiating the 24-hour emergency override. <strong className="text-white">We are not responsible for any consequences arising from your inability to access blocked applications.</strong>
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-mono text-zinc-300 uppercase tracking-wider">
                2. User Responsibilities
              </h2>
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                You are solely responsible for managing the physical Halt card. Losing the card without access to your backup emergency recovery phrase will result in the continued lockout of restricted applications until the system can be restored or the 24-hour emergency wait period elapses. You agree not to use Halt in any manner that could interfere with emergency communications or critical device functionality.
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-mono text-zinc-300 uppercase tracking-wider">
                3. Device Permissions
              </h2>
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                Halt requires deep system-level permissions (e.g., Package Usage Stats, System Alert Window, and NFC access) to function. By granting these permissions, you authorize the application to monitor foreground activity and display system-level overlays locally on your device.
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-mono text-zinc-300 uppercase tracking-wider">
                4. Limitation of Liability
              </h2>
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                To the maximum extent permitted by law, Halt Tech shall not be liable for any indirect, incidental, special, consequential, or punitive damages, or any loss of profits or revenues, whether incurred directly or indirectly, resulting from your use or inability to use the application.
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-mono text-zinc-300 uppercase tracking-wider">
                5. Changes to Terms
              </h2>
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                We reserve the right to modify these Terms at any time. We will notify users of any material changes by updating the "Last updated" date of these Terms. Continued use of the application after any such changes constitutes your consent to the updated Terms.
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-mono text-zinc-300 uppercase tracking-wider">
                6. Contact Us
              </h2>
              <p className="text-zinc-400 text-sm sm:text-base font-light leading-relaxed tracking-tight">
                If you have any questions about these Terms, please contact us at <a href="mailto:hello@gethalt.in" className="text-white hover:underline">hello@gethalt.in</a>.
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
