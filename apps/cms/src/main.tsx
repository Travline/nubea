import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@/styles/globals.css'
import { enrutador } from './rutas'
import { RouterProvider } from 'react-router-dom'
import { ProveedorSesion } from '@/autenticacion/sesion'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ProveedorSesion><RouterProvider router={enrutador} /></ProveedorSesion>
  </StrictMode>,
)
