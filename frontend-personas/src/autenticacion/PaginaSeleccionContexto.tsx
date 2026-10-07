import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded'
import BusinessRoundedIcon from '@mui/icons-material/BusinessRounded'
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded'
import { Alert, Box, Button, Chip, Container, Paper, Stack, Typography } from '@mui/material'
import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { usarAutenticacion } from './AutenticacionProvider'

export function PaginaSeleccionContexto() {
  const { usuario, contextoActivo, activarContexto, cerrarSesion } = usarAutenticacion()
  const [error, setError] = useState<string | null>(null)
  const [activando, setActivando] = useState<number | null>(null)
  const navigate = useNavigate()

  if (!usuario) return <Navigate to="/iniciar-sesion" replace />

  async function activar(id: number) {
    setActivando(id)
    setError(null)
    try {
      await activarContexto(id)
      navigate('/', { replace: true })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo activar el contexto')
    } finally {
      setActivando(null)
    }
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#f6f7f9', py: { xs: 4, md: 8 } }}>
      <Container maxWidth="md">
        <Stack spacing={3}>
          <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 2 }}>
            <Box>
              <Typography variant="h4" fontWeight={700}>Selecciona un contexto</Typography>
              <Typography color="text.secondary" sx={{ mt: 0.75 }}>Los permisos y el alcance se aplicarán dentro del contexto seleccionado.</Typography>
            </Box>
            <Button color="inherit" startIcon={<LogoutRoundedIcon />} onClick={() => void cerrarSesion()}>Salir</Button>
          </Box>
          {error && <Alert severity="error">{error}</Alert>}
          <Stack spacing={1.5}>
            {usuario.contextos.map((contexto) => (
              <Paper key={contexto.contextoCuentaId} elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3, p: 2.5 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Box sx={{ width: 44, height: 44, bgcolor: 'action.hover', borderRadius: 2.5, display: 'grid', placeItems: 'center' }}><BusinessRoundedIcon /></Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography fontWeight={700}>{contexto.nombreContexto}</Typography>
                    <Stack direction="row" spacing={1} sx={{ mt: 0.75, flexWrap: 'wrap', rowGap: 1 }}>
                      <Chip size="small" label={contexto.nombreRol} />
                      <Chip size="small" variant="outlined" label={contexto.alcance ?? 'Sin alcance'} />
                      <Chip size="small" variant="outlined" label={contexto.tipoContexto} />
                    </Stack>
                  </Box>
                  <Button
                    endIcon={<ArrowForwardRoundedIcon />}
                    variant={contextoActivo?.entidadContextoId === contexto.entidadContextoId ? 'outlined' : 'contained'}
                    disabled={activando !== null}
                    onClick={() => void activar(contexto.entidadContextoId)}
                  >
                    {activando === contexto.entidadContextoId ? 'Entrando…' : 'Entrar'}
                  </Button>
                </Box>
              </Paper>
            ))}
          </Stack>
        </Stack>
      </Container>
    </Box>
  )
}
