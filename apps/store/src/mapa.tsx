/// <reference types="google.maps" />
import { APIProvider, Map, Marker, useMapsLibrary } from '@vis.gl/react-google-maps'
import { useEffect, useRef, useState } from 'react'

type Punto = { lat: number; lng: number }
function Contenido({ placeId, cambiar }: { placeId: string | null; cambiar?: (id: string | null) => void }) {
  const places = useMapsLibrary('places')
  const contenedor = useRef<HTMLDivElement>(null)
  const [punto, setPunto] = useState<Punto | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    if (!places || !cambiar || !contenedor.current) return
    const elemento = new places.PlaceAutocompleteElement()
    elemento.placeholder = 'Buscar un lugar'
    contenedor.current.appendChild(elemento)
    const seleccionar = async (evento: Event) => {
      try {
        const seleccion = evento as google.maps.places.PlacePredictionSelectEvent
        const lugar = seleccion.placePrediction.toPlace()
        await lugar.fetchFields({ fields: ['id', 'location'] })
        if (!lugar.id || !lugar.location) throw new Error()
        cambiar(lugar.id)
      } catch { setError('No se pudo obtener el lugar. Puedes usar la dirección escrita.') }
    }
    elemento.addEventListener('gmp-select', seleccionar)
    return () => { elemento.removeEventListener('gmp-select', seleccionar); elemento.remove() }
  }, [places, cambiar])
  useEffect(() => {
    let activo = true
    if (!places || !placeId) return
    setPunto(null); setError('')
    const lugar = new places.Place({ id: placeId })
    lugar.fetchFields({ fields: ['location'] }).then(() => {
      if (!activo) return
      if (!lugar.location) { setError('No se encontró la ubicación. La dirección escrita sigue disponible.'); return }
      setPunto({ lat: lugar.location.lat(), lng: lugar.location.lng() })
    }).catch(() => { if (activo) setError('No se pudo cargar la ubicación. La dirección escrita sigue disponible.') })
    return () => { activo = false }
  }, [places, placeId])
  return <div><div ref={contenedor} />{cambiar && placeId && <button type="button" onClick={() => { cambiar(null); setPunto(null); setError('') }}>Quitar ubicación</button>}{error && <p role="status">{error}</p>}{placeId && !error && !punto && <p>Cargando ubicación…</p>}{placeId && punto && <div style={{ height: 300, width: '100%' }}><Map key={placeId} defaultCenter={punto} defaultZoom={16} gestureHandling="greedy"><Marker position={punto} /></Map></div>}</div>
}
export function MapaLugar({ placeId, cambiar }: { placeId: string | null; cambiar?: (id: string | null) => void }) {
  const [error, setError] = useState(false)
  const clave = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined
  if (!clave || error) return <p>El mapa no está disponible. Puedes usar la dirección escrita.</p>
  return <APIProvider apiKey={clave} onError={() => setError(true)}><Contenido placeId={placeId} cambiar={cambiar} /></APIProvider>
}
