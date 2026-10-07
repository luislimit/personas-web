import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { peticionApi, configurarNoAutorizado } from '../api/clienteApi'
import type {
  ContextoAcceso,
  RespuestaContextoActivo,
  RespuestaInicioSesion,
  RespuestaYo,
} from '../api/tipos'
import {
  guardarContextoId,
  guardarPermisos,
  guardarToken,
  guardarUltimoContextoCuenta,
  leerContextoId,
  leerPermisos,
  leerToken,
  leerUltimoContextoCuenta,
  limpiarSesionLocal,
} from './almacenSesion'

interface EstadoAutenticacion {
  cargando: boolean
  autenticado: boolean
  usuario: RespuestaYo | null
  contextoActivo: ContextoAcceso | null
  permisos: string[]
  iniciarSesion: (nombreUsuario: string, contrasena: string) => Promise<void>
  cerrarSesion: () => Promise<void>
  activarContexto: (entidadContextoId: number) => Promise<void>
  tienePermiso: (codigo: string) => boolean
}

const ContextoAutenticacion = createContext<EstadoAutenticacion | null>(null)

function resolverContextoInicial(usuario: RespuestaYo): number | null {
  if (usuario.contextos.length === 0) return null

  const ultimoContexto = leerUltimoContextoCuenta(usuario.cuentaId)
  if (
    ultimoContexto !== null &&
    usuario.contextos.some((contexto) => contexto.entidadContextoId === ultimoContexto)
  ) {
    return ultimoContexto
  }

  return usuario.contextos[0].entidadContextoId
}

export function AutenticacionProvider({ children }: PropsWithChildren) {
  const [cargando, setCargando] = useState(true)
  const [usuario, setUsuario] = useState<RespuestaYo | null>(null)
  const [contextoActivo, setContextoActivo] = useState<ContextoAcceso | null>(null)
  const [permisos, setPermisos] = useState<string[]>(leerPermisos())
  const cuentaActualRef = useRef<number | null>(null)

  const limpiar = useCallback(() => {
    limpiarSesionLocal()
    cuentaActualRef.current = null
    setUsuario(null)
    setContextoActivo(null)
    setPermisos([])
  }, [])

  useEffect(() => {
    configurarNoAutorizado(limpiar)
    return () => configurarNoAutorizado(null)
  }, [limpiar])

  const activarContexto = useCallback(async (entidadContextoId: number) => {
    const respuesta = await peticionApi<RespuestaContextoActivo>(
      `/api/autenticacion/contextos/${entidadContextoId}/activar`,
      { method: 'POST', sinContexto: true },
    )

    guardarContextoId(entidadContextoId)

    if (cuentaActualRef.current !== null) {
      guardarUltimoContextoCuenta(cuentaActualRef.current, entidadContextoId)
    }

    guardarPermisos(respuesta.permisos)
    setContextoActivo(respuesta.contexto)
    setPermisos(respuesta.permisos)
  }, [])

  useEffect(() => {
    let activo = true

    async function bootstrap() {
      const token = leerToken()
      if (!token) {
        if (activo) setCargando(false)
        return
      }

      try {
        const yo = await peticionApi<RespuestaYo>('/api/autenticacion/yo', {
          sinContexto: true,
        })

        if (!activo) return

        cuentaActualRef.current = yo.cuentaId
        setUsuario(yo)

        const contextoSesion = leerContextoId()
        const contextoSesionValido =
          contextoSesion !== null &&
          yo.contextos.some((contexto) => contexto.entidadContextoId === contextoSesion)

        const contextoInicial = contextoSesionValido
          ? contextoSesion
          : resolverContextoInicial(yo)

        if (contextoInicial !== null) {
          try {
            await activarContexto(contextoInicial)
          } catch {
            // La sesión sigue siendo válida aunque el contexto ya no pueda activarse.
            // La pantalla de selección queda como fallback excepcional.
            guardarContextoId(null)
            setContextoActivo(null)
            setPermisos([])
          }
        } else {
          guardarContextoId(null)
          setContextoActivo(null)
          setPermisos([])
        }
      } catch {
        if (activo) limpiar()
      } finally {
        if (activo) setCargando(false)
      }
    }

    void bootstrap()

    return () => {
      activo = false
    }
  }, [activarContexto, limpiar])

  const iniciarSesion = useCallback(async (nombreUsuario: string, contrasena: string) => {
    const respuesta = await peticionApi<RespuestaInicioSesion>(
      '/api/autenticacion/iniciar-sesion',
      {
        method: 'POST',
        sinContexto: true,
        reintentarMfa: false,
        body: JSON.stringify({ nombreUsuario, contrasena }),
      },
    )

    guardarToken(respuesta.tokenAcceso)

    const yo = await peticionApi<RespuestaYo>('/api/autenticacion/yo', {
      sinContexto: true,
    })

    cuentaActualRef.current = yo.cuentaId
    setContextoActivo(null)
    setPermisos([])
    guardarContextoId(null)

    const contextoInicial = resolverContextoInicial(yo)
    if (contextoInicial !== null) {
      try {
        await activarContexto(contextoInicial)
      } catch {
        // No exponemos durante unas décimas la pantalla antigua de selección.
        // Si la activación automática falla de verdad, dejamos la cuenta autenticada
        // sin contexto para que el fallback pueda mostrarse de forma estable.
        guardarContextoId(null)
        setContextoActivo(null)
        setPermisos([])
      }
    }

    // Marcamos la cuenta como autenticada solo cuando ya hemos terminado de
    // resolver el contexto inicial. Evita el parpadeo de /seleccionar-contexto.
    setUsuario(yo)
  }, [activarContexto])

  const cerrarSesion = useCallback(async () => {
    try {
      if (leerToken()) {
        await peticionApi<void>('/api/autenticacion/cerrar-sesion', {
          method: 'POST',
          sinContexto: true,
          reintentarMfa: false,
        })
      }
    } finally {
      limpiar()
    }
  }, [limpiar])

  const valor = useMemo<EstadoAutenticacion>(() => ({
    cargando,
    autenticado: usuario !== null,
    usuario,
    contextoActivo,
    permisos,
    iniciarSesion,
    cerrarSesion,
    activarContexto,
    tienePermiso: (codigo) => permisos.includes(codigo),
  }), [
    cargando,
    usuario,
    contextoActivo,
    permisos,
    iniciarSesion,
    cerrarSesion,
    activarContexto,
  ])

  return (
    <ContextoAutenticacion.Provider value={valor}>
      {children}
    </ContextoAutenticacion.Provider>
  )
}

export function usarAutenticacion(): EstadoAutenticacion {
  const valor = useContext(ContextoAutenticacion)
  if (!valor) {
    throw new Error('usarAutenticacion debe usarse dentro de AutenticacionProvider')
  }
  return valor
}
