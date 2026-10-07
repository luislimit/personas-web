import { Alert, Box, Button, CircularProgress, Paper, Stack, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { peticionApi } from '../api/clienteApi'
import type { RespuestaVerificacionMfa } from '../api/tipos'
import { usarAutenticacion } from './AutenticacionProvider'

export function PaginaVerificarEnlaceMfa() {
  const { autenticado } = usarAutenticacion()
  const [params] = useSearchParams()
  const [estado, setEstado] = useState<'cargando' | 'ok' | 'error'>('cargando')
  const [mensaje, setMensaje] = useState('Validando enlace…')

  const desafioId = Number(params.get('desafio'))
  const token = params.get('token')

  useEffect(() => {
    if (!autenticado || !Number.isFinite(desafioId) || !token) {
      setEstado('error')
      setMensaje('El enlace de verificación no es válido o la sesión no está iniciada.')
      return
    }
    void peticionApi<RespuestaVerificacionMfa>('/api/autenticacion/mfa/verificar-enlace', {
      method: 'POST',
      sinContexto: true,
      reintentarMfa: false,
      body: JSON.stringify({ desafioId, token }),
    }).then((respuesta) => {
      if (respuesta.verificada) {
        setEstado('ok')
        setMensaje('Verificación completada. La sesión ya está reforzada.')
      } else {
        setEstado('error')
        setMensaje('No se pudo validar el enlace.')
      }
    }).catch((e: unknown) => {
      setEstado('error')
      setMensaje(e instanceof Error ? e.message : 'No se pudo validar el enlace.')
    })
  }, [autenticado, desafioId, token])

  if (!autenticado) return <Navigate to="/iniciar-sesion" replace />

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#f6f7f9', display: 'grid', placeItems: 'center', px: 2 }}>
      <Paper elevation={0} sx={{ width: '100%', maxWidth: 480, p: 4, border: '1px solid', borderColor: 'divider', borderRadius: 4 }}>
        <Stack spacing={2.5} alignItems="flex-start">
          <Typography variant="h5" fontWeight={700}>Verificación MFA</Typography>
          {estado === 'cargando' && <CircularProgress size={26} />}
          {estado !== 'cargando' && <Alert severity={estado === 'ok' ? 'success' : 'error'} sx={{ width: '100%' }}>{mensaje}</Alert>}
          {estado !== 'cargando' && <Button component={Link} to="/" variant="contained">Volver a la aplicación</Button>}
        </Stack>
      </Paper>
    </Box>
  )
}
