import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@/styles/globals.css'
import { router } from './routes'
import { RouterProvider } from 'react-router-dom'
import { SessionProvider } from '@/auth/session'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <SessionProvider><RouterProvider router={router} /></SessionProvider>
  </StrictMode>,
)
