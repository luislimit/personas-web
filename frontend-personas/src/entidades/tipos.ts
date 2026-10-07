export type EstadoEntidad = 'ACTIVO' | 'INACTIVO' | 'ARCHIVADO'

export interface ResumenEntidad {
  id: number
  codigoTipo: string
  nombre: string
  estado: string
}

export interface DatosPersona {
  nombre: string
  primerApellido: string
  segundoApellido: string | null
  fechaNacimiento: string | null
  genero: string | null
}

export interface DetalleEntidad {
  id: number
  codigoTipo: string
  nombre: string
  estado: string
  fechaCreacion: string
  fechaModificacion: string
  persona: DatosPersona | null
}

export interface EntradaPersona {
  nombre: string
  primerApellido: string
  segundoApellido: string | null
  fechaNacimiento: string | null
  genero: string | null
}

export interface CrearEntidad {
  codigoTipo: string
  nombre: string
  estado?: string
  persona?: EntradaPersona | null
}

export interface ModificarEntidad {
  nombre?: string | null
  estado?: string | null
  persona?: EntradaPersona | null
}
