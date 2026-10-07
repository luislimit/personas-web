export interface ContextoAcceso {
  contextoCuentaId: number
  entidadContextoId: number
  nombreContexto: string
  tipoContexto: string
  rolSeguridadId: number
  codigoRol: string
  nombreRol: string
  alcance: string | null
  validoDesde: string | null
  validoHasta: string | null
}

export interface RespuestaInicioSesion {
  tokenAcceso: string
  tipoToken: string
  fechaExpiracion: string
  cuentaId: number
  personaEntidadId: number | null
  nombreUsuario: string
  contextos: ContextoAcceso[]
}

export interface RespuestaYo {
  cuentaId: number
  personaEntidadId: number | null
  nombreUsuario: string
  correoAcceso: string | null
  contextos: ContextoAcceso[]
}

export interface RespuestaContextoActivo {
  contexto: ContextoAcceso
  permisos: string[]
}

export interface ErrorApi {
  fechaHora?: string
  estadoHttp?: number
  error?: string
  mensaje?: string
  validacion?: Record<string, unknown>
}

export interface OpcionCanalMfa {
  canal: 'EMAIL' | 'SMS'
  destinoEnmascarado: string
  mecanismos: Array<'CODIGO' | 'ENLACE'>
}

export interface EstadoMfa {
  nivel: 'BASICO' | 'MFA'
  verificadaEn: string | null
  validaHasta: string | null
  canales: OpcionCanalMfa[]
}

export interface RespuestaDesafioMfa {
  desafioId: number
  canal: 'EMAIL' | 'SMS'
  mecanismo: 'CODIGO' | 'ENLACE'
  destinoEnmascarado: string
  fechaExpiracion: string
}

export interface RespuestaVerificacionMfa {
  verificada: boolean
  nivel: 'BASICO' | 'MFA'
  verificadaEn: string | null
  validaHasta: string | null
}
