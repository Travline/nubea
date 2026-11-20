import { execFileSync } from 'node:child_process'
import { cpSync, mkdirSync, rmSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const pnpm = fileURLToPath(new URL('../node_modules/pnpm/bin/pnpm.cjs', import.meta.url))

for (const [aplicacion, destino] of [['cms', 'plataforma'], ['store', 'tienda']]) {
  const ruta = `apps/${aplicacion}`
  execFileSync(process.execPath, [pnpm, 'install', '--frozen-lockfile'], { cwd: ruta, stdio: 'inherit' })
  execFileSync(process.execPath, [pnpm, 'run', 'build'], { cwd: ruta, stdio: 'inherit' })
  rmSync(`public/${destino}`, { recursive: true, force: true })
  mkdirSync(`public/${destino}`, { recursive: true })
  cpSync(`${ruta}/dist`, `public/${destino}`, { recursive: true })
}
