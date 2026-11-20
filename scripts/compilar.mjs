import { execFileSync } from 'node:child_process'
import { cpSync, mkdirSync, rmSync } from 'node:fs'

for (const [aplicacion, destino] of [['cms', 'plataforma'], ['store', 'tienda']]) {
  const ruta = `apps/${aplicacion}`
  execFileSync('pnpm', ['install', '--frozen-lockfile'], { cwd: ruta, stdio: 'inherit' })
  execFileSync('pnpm', ['run', 'build'], { cwd: ruta, stdio: 'inherit' })
  rmSync(`public/${destino}`, { recursive: true, force: true })
  mkdirSync(`public/${destino}`, { recursive: true })
  cpSync(`${ruta}/dist`, `public/${destino}`, { recursive: true })
}
