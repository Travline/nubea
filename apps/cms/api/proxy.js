import http from 'node:http'
import https from 'node:https'

export const config = { api: { bodyParser: false } }

// The upstream is server-configured, never chosen by the browser.
export default function proxy(req, res) {
  let origin
  try {
    origin = new URL(process.env.BACKEND_URL)
    if (!['http:', 'https:'].includes(origin.protocol) || origin.username || origin.password || origin.pathname !== '/' || origin.search || origin.hash) throw new Error()
  } catch {
    return res.status(503).json({ mensaje: 'El servidor no está configurado.' })
  }
  const url = new URL(req.url, 'http://localhost')
  const path = url.searchParams.get('__path')
  if (!path || path.split('/').some(part => part === '.' || part === '..' || part.includes('\\'))) return res.status(400).end()
  url.searchParams.delete('__path')
  const target = path === '__logout__' ? '/logout' : `/api/${path}`
  const headers = { ...req.headers, host: origin.host }
  delete headers.connection
  delete headers['content-length']
  delete headers.origin
  const upstream = (origin.protocol === 'https:' ? https : http).request({
    protocol: origin.protocol, hostname: origin.hostname, port: origin.port || undefined,
    method: req.method, path: target + url.search, headers,
  }, response => {
    res.statusCode = response.statusCode || 502
    for (const [name, value] of Object.entries(response.headers)) {
      if (value !== undefined && !['connection', 'transfer-encoding'].includes(name)) res.setHeader(name, value)
    }
    response.pipe(res)
  })
  upstream.on('error', () => { if (!res.headersSent) res.status(502).json({ mensaje: 'No se pudo conectar con el servidor.' }); else res.destroy() })
  req.pipe(upstream)
}
