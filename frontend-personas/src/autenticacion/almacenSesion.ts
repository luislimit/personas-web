const CLAVE_TOKEN = 'gestion-personas.token'
const CLAVE_CONTEXTO = 'gestion-personas.contexto'
const CLAVE_PERMISOS = 'gestion-personas.permisos'
const PREFIJO_ULTIMO_CONTEXTO = 'gestion-personas.ultimo-contexto.'

export function leerToken(): string | null {
  return localStorage.getItem(CLAVE_TOKEN)
}

export function guardarToken(token: string): void {
  localStorage.setItem(CLAVE_TOKEN, token)
}

export function leerContextoId(): number | null {
  const valor = localStorage.getItem(CLAVE_CONTEXTO)
  if (!valor) return null
  const numero = Number(valor)
  return Number.isFinite(numero) ? numero : null
}

export function guardarContextoId(id: number | null): void {
  if (id === null) localStorage.removeItem(CLAVE_CONTEXTO)
  else localStorage.setItem(CLAVE_CONTEXTO, String(id))
}

export function leerUltimoContextoCuenta(cuentaId: number): number | null {
  const valor = localStorage.getItem(`${PREFIJO_ULTIMO_CONTEXTO}${cuentaId}`)
  if (!valor) return null

  const numero = Number(valor)
  return Number.isFinite(numero) ? numero : null
}

export function guardarUltimoContextoCuenta(cuentaId: number, contextoId: number): void {
  localStorage.setItem(`${PREFIJO_ULTIMO_CONTEXTO}${cuentaId}`, String(contextoId))
}

export function guardarPermisos(permisos: string[]): void {
  localStorage.setItem(CLAVE_PERMISOS, JSON.stringify(permisos))
}

export function leerPermisos(): string[] {
  try {
    const raw = localStorage.getItem(CLAVE_PERMISOS)
    return raw ? (JSON.parse(raw) as string[]) : []
  } catch {
    return []
  }
}

export function limpiarSesionLocal(): void {
  localStorage.removeItem(CLAVE_TOKEN)
  localStorage.removeItem(CLAVE_CONTEXTO)
  localStorage.removeItem(CLAVE_PERMISOS)

  // Deliberadamente NO se elimina gestion-personas.ultimo-contexto.<cuentaId>.
  // Es una preferencia local de este navegador/equipo, no información de sesión.
}
