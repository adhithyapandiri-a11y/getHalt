import { PixelHero } from '@/components/ui/pixel-hero'

export const metadata = {
  title: 'halt. - Digital Willpower is a Lie',
  description:
    'Halt is the un-bypassable physical circuit breaker for your screen addiction. The hardware solution to software weakness.',
}

export default function Home() {
  return (
    <main className="">
      <PixelHero 
        word1="halt."
        word2="digital willpower is a lie."
        description="Halt is the un-bypassable physical circuit breaker for your screen addiction. The hardware solution to software weakness."
        primaryCta="Join Waitlist"
        primaryCtaMobile="Join"
        secondaryCta="Learn More"
        secondaryCtaMobile="Learn"
      />
    </main>
  )
}
