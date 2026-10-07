import type { ErrorApi } from './tipos'
import { leerContextoId, leerToken } from '../autenticacion/almacenSesion'

const API_BASE_URL = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? ''

let alNoAutorizado: (() => void) | null = null
let solicitarMfa: (() => Promise<boolean>) | null = null

export class ErrorPeticionApi extends Error {
  constructor(
    public readonly estado: number,
    public readonly detalle: ErrorApi | null,
  ) {
    super(detalle?.mensaje ?? `Error HTTP ${estado}`)
  }
}

export class ErrorMfaRequerida extends ErrorPeticionApi {}

export function configurarNoAutorizado(handler: (() => void) | null): void {
  alNoAutorizado = handler
}

export function configurarStepUpMfa(handler: (() => Promise<boolean>) | null): void {
  solicitarMfa = handler
}

interface OpcionesPeticion extends RequestInit {
  sinContexto?: boolean
  reintentarMfa?: boolean
}

async function leerError(response: Response): Promise<ErrorApi | null> {
  const texto = await response.text()
  if (!texto) return null

  try {
    return JSON.parse(texto) as ErrorApi
  } catch {
    return {
      estadoHttp: response.status,
      error: response.statusText || `HTTP ${response.status}`,
      mensaje: texto,
      validacion: {},
    }
  }
}

function esMfaRequerida(error: ErrorApi | null): boolean {
  return error?.validacion?.codigo === 'MFA_REQUERIDA'
}

export async function peticionApi<T>(
  ruta: string,
  opciones: OpcionesPeticion = {},
  intentoMfa = false,
): Promise<T> {
  const headers = new Headers(opciones.headers)
  const token = leerToken()
  const contextoId = leerContextoId()

  if (!headers.has('Content-Type') && opciones.body) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (!opciones.sinContexto && contextoId !== null) headers.set('X-Contexto-Id', String(contextoId))

  const response = await fetch(`${API_BASE_URL}${ruta}`, { ...opciones, headers })

  if (response.status === 401) {
    alNoAutorizado?.()
    throw new ErrorPeticionApi(401, await leerError(response))
  }

  if (response.status === 403) {
    const error = await leerError(response)
    if (
      esMfaRequerida(error) &&
      opciones.reintentarMfa !== false &&
      !intentoMfa &&
      solicitarMfa &&
      (await solicitarMfa())
    ) {
      return peticionApi<T>(ruta, opciones, true)
    }
    if (esMfaRequerida(error)) throw new ErrorMfaRequerida(403, error)
    throw new ErrorPeticionApi(403, error)
  }

  if (!response.ok) throw new ErrorPeticionApi(response.status, await leerError(response))
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
