import { Box, CircularProgress } from '@mui/material'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { usarAutenticacion } from './AutenticacionProvider'

export function ProtectorSesion() {
  const { cargando, autenticado } = usarAutenticacion()
  const location = useLocation()

  if (cargando) {
    return <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><CircularProgress size={28} /></Box>
  }

  if (!autenticado) return <Navigate to="/iniciar-sesion" replace state={{ desde: location.pathname }} />
  return <Outlet />
}
