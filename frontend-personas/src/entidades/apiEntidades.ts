import { peticionApi } from '../api/clienteApi'
import type {
  CrearEntidad,
  DetalleEntidad,
  ModificarEntidad,
  ResumenEntidad,
} from './tipos'

export interface FiltrosEntidades {
  nombre?: string
  codigoTipo?: string
  estado?: string
}

export async function buscarEntidades(
  filtros: FiltrosEntidades,
): Promise<ResumenEntidad[]> {
  const params = new URLSearchParams()

  if (filtros.nombre?.trim()) params.set('nombre', filtros.nombre.trim())
  if (filtros.codigoTipo) params.set('codigoTipo', filtros.codigoTipo)
  if (filtros.estado) params.set('estado', filtros.estado)

  const query = params.toString()
  return peticionApi<ResumenEntidad[]>(`/api/entidades${query ? `?${query}` : ''}`)
}

export function obtenerEntidad(id: number): Promise<DetalleEntidad> {
  return peticionApi<DetalleEntidad>(`/api/entidades/${id}`)
}

export function crearEntidad(entrada: CrearEntidad): Promise<DetalleEntidad> {
  return peticionApi<DetalleEntidad>('/api/entidades', {
    method: 'POST',
    body: JSON.stringify(entrada),
  })
}

export function modificarEntidad(
  id: number,
  entrada: ModificarEntidad,
): Promise<DetalleEntidad> {
  return peticionApi<DetalleEntidad>(`/api/entidades/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(entrada),
  })
}

export function archivarEntidad(id: number): Promise<void> {
  return peticionApi<void>(`/api/entidades/${id}`, {
    method: 'DELETE',
  })
}
