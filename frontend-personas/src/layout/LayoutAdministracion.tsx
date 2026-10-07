import AccountCircleOutlinedIcon from '@mui/icons-material/AccountCircleOutlined'
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined'
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined'
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined'
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined'
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded'
import ManageAccountsOutlinedIcon from '@mui/icons-material/ManageAccountsOutlined'
import TaskAltOutlinedIcon from '@mui/icons-material/TaskAltOutlined'
import {
  AppBar,
  Box,
  Button,
  Divider,
  Drawer,
  FormControl,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  MenuItem,
  Select,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material'
import { Navigate, NavLink, Outlet } from 'react-router-dom'
import { usarAutenticacion } from '../autenticacion/AutenticacionProvider'

const ancho = 248

const modulos = [
  {
    nombre: 'Inicio',
    icono: <DashboardOutlinedIcon />,
    ruta: '/',
    disponible: true,
  },
  {
    nombre: 'Personas y entidades',
    icono: <GroupsOutlinedIcon />,
    ruta: '/entidades',
    disponible: true,
  },
  {
    nombre: 'Cuentas y accesos',
    icono: <ManageAccountsOutlinedIcon />,
    ruta: '/cuentas',
    disponible: false,
  },
  {
    nombre: 'Calendarios',
    icono: <CalendarMonthOutlinedIcon />,
    ruta: '/calendarios',
    disponible: false,
  },
  {
    nombre: 'Solicitudes',
    icono: <TaskAltOutlinedIcon />,
    ruta: '/solicitudes',
    disponible: false,
  },
  {
    nombre: 'Documentos',
    icono: <DescriptionOutlinedIcon />,
    ruta: '/documentos',
    disponible: false,
  },
] as const

export function LayoutAdministracion() {
  const {
    contextoActivo,
    usuario,
    activarContexto,
    cerrarSesion,
  } = usarAutenticacion()

  if (!contextoActivo) return <Navigate to="/seleccionar-contexto" replace />

  const contextos = usuario?.contextos ?? []
  const puedeCambiarContexto = contextos.length > 1

  async function cambiarContexto(entidadContextoId: number) {
    if (entidadContextoId === contextoActivo?.entidadContextoId) return
    await activarContexto(entidadContextoId)
  }

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: '#f7f8fa' }}>
      <Drawer
        variant="permanent"
        sx={{
          width: ancho,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: ancho,
            boxSizing: 'border-box',
            borderRightColor: 'divider',
          },
        }}
      >
        <Toolbar sx={{ px: 2.5 }}>
          <Typography fontWeight={800}>Gestión de Personas</Typography>
        </Toolbar>

        <Divider />

        <List sx={{ px: 1.25, pt: 1.5 }}>
          {modulos.map((modulo) => (
            <ListItemButton
              key={modulo.nombre}
              component={modulo.disponible ? NavLink : 'div'}
              to={modulo.disponible ? modulo.ruta : undefined}
              disabled={!modulo.disponible}
              end={modulo.ruta === '/'}
              sx={{
                borderRadius: 2,
                mb: 0.4,
                '&.active': {
                  bgcolor: 'action.selected',
                  color: 'primary.main',
                  '& .MuiListItemIcon-root': { color: 'primary.main' },
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 38 }}>{modulo.icono}</ListItemIcon>
              <ListItemText
                primary={modulo.nombre}
                secondary={!modulo.disponible ? 'Próximo bloque' : undefined}
              />
            </ListItemButton>
          ))}
        </List>
      </Drawer>

      <Box component="main" sx={{ flex: 1, minWidth: 0 }}>
        <AppBar
          position="sticky"
          elevation={0}
          color="inherit"
          sx={{
            borderBottom: '1px solid',
            borderColor: 'divider',
            bgcolor: 'rgba(255,255,255,.96)',
            backdropFilter: 'blur(12px)',
          }}
        >
          <Toolbar sx={{ gap: 2, minHeight: 68 }}>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Stack
                direction="row"
                spacing={1.25}
                alignItems="center"
                sx={{ minWidth: 0 }}
              >
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ flexShrink: 0 }}
                >
                  Conectado a:
                </Typography>

                {puedeCambiarContexto ? (
                  <FormControl
                    size="small"
                    sx={{
                      minWidth: 220,
                      maxWidth: 360,
                      '& .MuiOutlinedInput-root': {
                        bgcolor: 'background.paper',
                        borderRadius: 2,
                      },
                    }}
                  >
                    <Select
                      value={contextoActivo.entidadContextoId}
                      onChange={(event) => {
                        void cambiarContexto(Number(event.target.value))
                      }}
                      aria-label="Contexto activo"
                    >
                      {contextos.map((contexto) => (
                        <MenuItem
                          key={contexto.contextoCuentaId}
                          value={contexto.entidadContextoId}
                        >
                          {contexto.nombreContexto}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                ) : (
                  <Typography fontWeight={700} noWrap>
                    {contextoActivo.nombreContexto}
                  </Typography>
                )}
              </Stack>
            </Box>

            <Button
              color="inherit"
              startIcon={<AccountCircleOutlinedIcon />}
              sx={{ textTransform: 'none' }}
            >
              {usuario?.nombreUsuario}
            </Button>

            <Button
              color="inherit"
              startIcon={<LogoutRoundedIcon />}
              onClick={() => void cerrarSesion()}
            >
              Salir
            </Button>
          </Toolbar>
        </AppBar>

        <Box sx={{ p: { xs: 2.5, md: 4 } }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  )
}
