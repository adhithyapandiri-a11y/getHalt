import { Navigation } from '@/components/Navigation'
import { HeroSection } from '@/components/HeroSection'
import { PhilosophySection } from '@/components/PhilosophySection'
import { MechanismSection } from '@/components/MechanismSection'
import { ComingSoonSection } from '@/components/ComingSoonSection'
import DotMatrix from '@/components/DotMatrix'
import ParticleGridBackground from '@/components/ParticleGridBackground'
import { ComparisonSection } from '@/components/ComparisonSection'

export const metadata = {
  title: 'halt. - Digital Willpower is a Lie',
  description:
    'Halt is the un-bypassable physical circuit breaker for your screen addiction. The hardware solution to software weakness.',
}

export default function Home() {
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
      <div className="relative z-10">
        <Navigation />
        <HeroSection />
        <ComparisonSection />
        <PhilosophySection />
        <MechanismSection />
        <ComingSoonSection />
      </div>
    </main>
  )
}


