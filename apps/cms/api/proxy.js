import http from 'node:http'
import https from 'node:https'

export const config = { api: { bodyParser: false } }

// El servidor de destino se configura en el servidor, no desde el navegador.
export default function proxy(peticion, respuesta) {
  let origen
  try {
    origen = new URL(process.env.BACKEND_URL)
    if (!['http:', 'https:'].includes(origen.protocol) || origen.username || origen.password || origen.pathname !== '/' || origen.search || origen.hash) throw new Error()
  } catch {
    return respuesta.status(503).json({ mensaje: 'El servidor no está configurado.' })
  }
  const url = new URL(peticion.url, 'http://localhost')
  const ruta = url.searchParams.get('__path')
  if (!ruta || ruta.split('/').some(parte => parte === '.' || parte === '..' || parte.includes('\\'))) return respuesta.status(400).end()
  url.searchParams.delete('__path')
  const destino = ruta === '__logout__' ? '/logout' : `/api/${ruta}`
  const cabeceras = { ...peticion.headers, host: origen.host }
  delete cabeceras.connection
  delete cabeceras['content-length']
  delete cabeceras.origin
  const conexion = (origen.protocol === 'https:' ? https : http).request({
    protocol: origen.protocol, hostname: origen.hostname, port: origen.port || undefined,
    method: peticion.method, path: destino + url.search, headers: cabeceras,
  }, respuestaOrigen => {
    respuesta.statusCode = respuestaOrigen.statusCode || 502
    for (const [nombre, valor] of Object.entries(respuestaOrigen.headers)) {
      if (valor !== undefined && !['connection', 'transfer-encoding'].includes(nombre)) respuesta.setHeader(nombre, valor)
    }
    respuestaOrigen.pipe(respuesta)
  })
  conexion.on('error', () => { if (!respuesta.headersSent) respuesta.status(502).json({ mensaje: 'No se pudo conectar con el servidor.' }); else respuesta.destroy() })
  peticion.pipe(conexion)
}
