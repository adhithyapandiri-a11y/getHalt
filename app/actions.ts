'use server';

import { Resend } from 'resend';

// Initialize Resend with the API key from environment variables
const resend = new Resend(process.env.RESEND_API_KEY);

export async function subscribeToWaitlist(formData: FormData) {
  const email = formData.get('email');

  if (!email || typeof email !== 'string') {
    return { success: false, error: 'Invalid email address.' };
  }

  try {
    const { error } = await resend.emails.send({
      from: 'Halt <onboarding@resend.dev>',
      to: [email],
      subject: 'welcome to the resistance.',
      html: '<h1>halt.</h1><p>Digital willpower is a lie. Your place on the priority waitlist for Batch 01 has been strictly secured.</p><p>We will contact you the exact moment the physical hardware architecture goes live in India.</p>',
    });

    if (error) {
      console.error('Resend API Error:', error);
      return { success: false, error: error.message };
    }

    return { success: true };
  } catch (err: any) {
    console.error('Subscription error:', err);
    return { success: false, error: err?.message || 'Failed to subscribe.' };
  }
}
