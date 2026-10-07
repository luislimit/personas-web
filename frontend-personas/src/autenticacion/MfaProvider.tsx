import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { type PropsWithChildren, useCallback, useEffect, useRef, useState } from 'react'
import { configurarStepUpMfa, ErrorPeticionApi, peticionApi } from '../api/clienteApi'
import type { EstadoMfa, RespuestaDesafioMfa, RespuestaVerificacionMfa } from '../api/tipos'

export function MfaProvider({ children }: PropsWithChildren) {
  const [abierto, setAbierto] = useState(false)
  const [estado, setEstado] = useState<EstadoMfa | null>(null)
  const [canal, setCanal] = useState<'EMAIL' | 'SMS'>('EMAIL')
  const [mecanismo, setMecanismo] = useState<'CODIGO' | 'ENLACE'>('CODIGO')
  const [desafio, setDesafio] = useState<RespuestaDesafioMfa | null>(null)
  const [codigo, setCodigo] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [procesando, setProcesando] = useState(false)
  const resolver = useRef<((ok: boolean) => void) | null>(null)

  const solicitar = useCallback(() => {
    if (resolver.current) return Promise.resolve(false)
    setAbierto(true)
    setEstado(null)
    setDesafio(null)
    setCodigo('')
    setError(null)
    return new Promise<boolean>((resolve) => {
      resolver.current = resolve
    })
  }, [])

  useEffect(() => {
    configurarStepUpMfa(solicitar)
    return () => configurarStepUpMfa(null)
  }, [solicitar])

  useEffect(() => {
    if (!abierto) return
    void peticionApi<EstadoMfa>('/api/autenticacion/mfa/estado', {
      sinContexto: true,
      reintentarMfa: false,
    }).then((respuesta) => {
      setEstado(respuesta)
      const primera = respuesta.canales[0]
      if (primera) {
        setCanal(primera.canal)
        setMecanismo(primera.mecanismos[0] ?? 'CODIGO')
      }
    }).catch((e: unknown) => {
      setError(e instanceof Error ? e.message : 'No se pudo consultar MFA')
    })
  }, [abierto])

  function cerrar(ok: boolean) {
    setAbierto(false)
    resolver.current?.(ok)
    resolver.current = null
  }

  async function enviar() {
    setProcesando(true)
    setError(null)
    try {
      const respuesta = await peticionApi<RespuestaDesafioMfa>('/api/autenticacion/mfa/desafios', {
        method: 'POST',
        sinContexto: true,
        reintentarMfa: false,
        body: JSON.stringify({ canal, mecanismo }),
      })
      setDesafio(respuesta)
    } catch (e) {
      setError(e instanceof ErrorPeticionApi ? e.message : 'No se pudo enviar la verificación')
    } finally {
      setProcesando(false)
    }
  }

  async function verificarCodigo() {
    if (!desafio) return
    setProcesando(true)
    setError(null)
    try {
      const respuesta = await peticionApi<RespuestaVerificacionMfa>('/api/autenticacion/mfa/verificar-codigo', {
        method: 'POST',
        sinContexto: true,
        reintentarMfa: false,
        body: JSON.stringify({ desafioId: desafio.desafioId, codigo }),
      })
      if (respuesta.verificada) cerrar(true)
      else setError('El código no es válido')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo verificar el código')
    } finally {
      setProcesando(false)
    }
  }

  const opcion = estado?.canales.find((item) => item.canal === canal)

  return (
    <>
      {children}
      <Dialog open={abierto} onClose={() => cerrar(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Verificación de seguridad</DialogTitle>
        <DialogContent>
          <Stack spacing={2.2} sx={{ pt: 0.5 }}>
            <Typography color="text.secondary" variant="body2">
              Esta operación requiere autenticación reforzada. Solo tendrás que verificarte una vez mientras la validación MFA siga vigente.
            </Typography>
            {error && <Alert severity="error">{error}</Alert>}
            {!desafio && (
              <>
                <FormControl fullWidth size="small">
                  <InputLabel>Canal</InputLabel>
                  <Select
                    value={canal}
                    label="Canal"
                    onChange={(e) => {
                      const nuevo = e.target.value as 'EMAIL' | 'SMS'
                      setCanal(nuevo)
                      const canalNuevo = estado?.canales.find((c) => c.canal === nuevo)
                      setMecanismo(canalNuevo?.mecanismos[0] ?? 'CODIGO')
                    }}
                  >
                    {estado?.canales.map((item) => (
                      <MenuItem key={item.canal} value={item.canal}>
                        {item.canal === 'EMAIL' ? 'Correo electrónico' : 'SMS'} · {item.destinoEnmascarado}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <FormControl fullWidth size="small">
                  <InputLabel>Método</InputLabel>
                  <Select value={mecanismo} label="Método" onChange={(e) => setMecanismo(e.target.value as 'CODIGO' | 'ENLACE')}>
                    {opcion?.mecanismos.map((item) => (
                      <MenuItem key={item} value={item}>{item === 'CODIGO' ? 'Código de un solo uso' : 'Enlace de un solo uso'}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </>
            )}
            {desafio?.mecanismo === 'CODIGO' && (
              <TextField
                autoFocus
                label="Código"
                value={codigo}
                onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
                inputProps={{ inputMode: 'numeric', maxLength: 6 }}
                helperText={`Enviado a ${desafio.destinoEnmascarado}`}
              />
            )}
            {desafio?.mecanismo === 'ENLACE' && (
              <Box>
                <Alert severity="info">
                  Hemos enviado un enlace de un solo uso a {desafio.destinoEnmascarado}. Ábrelo en este navegador para completar la verificación.
                </Alert>
              </Box>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={() => cerrar(false)} color="inherit">Cancelar</Button>
          {!desafio && <Button variant="contained" onClick={() => void enviar()} disabled={procesando || !estado?.canales.length}>Enviar</Button>}
          {desafio?.mecanismo === 'CODIGO' && <Button variant="contained" onClick={() => void verificarCodigo()} disabled={procesando || codigo.length !== 6}>Verificar</Button>}
        </DialogActions>
      </Dialog>
    </>
  )
}
