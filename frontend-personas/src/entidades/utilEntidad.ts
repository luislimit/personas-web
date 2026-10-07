export const TIPOS_ENTIDAD = [
  { codigo: 'PERSONA', nombre: 'Persona' },
  { codigo: 'ORGANIZACION', nombre: 'Organización' },
  { codigo: 'GRUPO', nombre: 'Grupo' },
  { codigo: 'INSTALACION', nombre: 'Instalación' },
] as const

export function nombreTipoEntidad(codigo: string): string {
  return TIPOS_ENTIDAD.find((tipo) => tipo.codigo === codigo)?.nombre ?? codigo
}

export function iniciales(nombre: string): string {
  const partes = nombre.trim().split(/\s+/).filter(Boolean)
  if (partes.length === 0) return '—'
  return partes.slice(0, 2).map((parte) => parte[0]?.toUpperCase()).join('')
}

export function etiquetaEstado(estado: string): string {
  switch (estado) {
    case 'ACTIVO':
      return 'Activo'
    case 'INACTIVO':
      return 'Inactivo'
    case 'ARCHIVADO':
      return 'Archivado'
    default:
      return estado
  }
}

export function formatearFecha(fecha: string | null | undefined): string {
  if (!fecha) return '—'
  const [year, month, day] = fecha.slice(0, 10).split('-')
  if (!year || !month || !day) return fecha
  return `${day}/${month}/${year}`
}
