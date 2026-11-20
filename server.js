import express from 'express'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const app = express()
const publico = path.join(path.dirname(fileURLToPath(import.meta.url)), 'public')
app.get('/', (req, res) => res.redirect(308, `/plataforma/${req.url.includes('?') ? req.url.slice(req.url.indexOf('?')) : ''}`))
app.use((req, res, next) => {
  if (/^\/(panel|registro|iniciar-sesion)(\/|$)/.test(req.path)) return res.redirect(308, `/plataforma${req.originalUrl}`)
  next()
})
app.use(express.static(publico, { index: false, redirect: false }))
app.use((req, res, next) => {
  if (req.method !== 'GET' && req.method !== 'HEAD') return next()
  if (path.posix.extname(req.path) || /\/(assets)(\/|$)/.test(req.path)) return next()
  if (/^\/plataforma(?:\/|$)/.test(req.path)) return res.sendFile(path.join(publico, 'plataforma/index.html'))
  if (/^\/tienda\/[^/]+(?:\/|$)/.test(req.path)) return res.sendFile(path.join(publico, 'tienda/index.html'))
  next()
})
app.use((req, res) => res.status(404).send('No encontrado'))
if (!process.env.VERCEL) app.listen(process.env.PORT || 3000, () => console.log(`Servidor disponible en el puerto ${process.env.PORT || 3000}`))
export default app
