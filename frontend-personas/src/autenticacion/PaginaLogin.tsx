import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import { Alert, Box, Button, Paper, Stack, TextField, Typography } from '@mui/material'
import { FormEvent, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { usarAutenticacion } from './AutenticacionProvider'

export function PaginaLogin() {
  const { autenticado, iniciarSesion, contextoActivo } = usarAutenticacion()
  const [usuario, setUsuario] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const navigate = useNavigate()

  if (autenticado) {
    return <Navigate to={contextoActivo ? '/' : '/seleccionar-contexto'} replace />
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setEnviando(true)
    setError(null)

    try {
      await iniciarSesion(usuario.trim(), contrasena)
      navigate('/', { replace: true })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo iniciar sesión')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: '#f6f7f9',
        display: 'grid',
        placeItems: 'center',
        px: 2,
      }}
    >
      <Paper
        elevation={0}
        sx={{
          width: '100%',
          maxWidth: 420,
          p: { xs: 3, sm: 4 },
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 4,
        }}
      >
        <Stack spacing={3}>
          <Box>
            <Box
              sx={{
                width: 42,
                height: 42,
                borderRadius: 2.5,
                bgcolor: 'primary.main',
                color: 'primary.contrastText',
                display: 'grid',
                placeItems: 'center',
                mb: 2.5,
              }}
            >
              <LockOutlinedIcon fontSize="small" />
            </Box>

            <Typography variant="h4" fontWeight={700}>
              Gestión de Personas
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 0.75 }}>
              Accede a tu espacio de administración.
            </Typography>
          </Box>

          {error && <Alert severity="error">{error}</Alert>}

          <Box component="form" onSubmit={(e) => void submit(e)}>
            <Stack spacing={2}>
              <TextField
                label="Usuario"
                autoComplete="username"
                autoFocus
                value={usuario}
                onChange={(e) => setUsuario(e.target.value)}
                required
              />
              <TextField
                label="Contraseña"
                type="password"
                autoComplete="current-password"
                value={contrasena}
                onChange={(e) => setContrasena(e.target.value)}
                required
              />
              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={enviando || !usuario || !contrasena}
              >
                {enviando ? 'Entrando…' : 'Entrar'}
              </Button>
            </Stack>
          </Box>
        </Stack>
      </Paper>
    </Box>
  )
}
