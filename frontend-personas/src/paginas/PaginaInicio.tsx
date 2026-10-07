import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded'
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined'
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined'
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined'
import ManageAccountsOutlinedIcon from '@mui/icons-material/ManageAccountsOutlined'
import TaskAltOutlinedIcon from '@mui/icons-material/TaskAltOutlined'
import { Box, Grid, Paper, Stack, Typography } from '@mui/material'
import { useNavigate } from 'react-router-dom'

const accesos = [
  {
    titulo: 'Personas y entidades',
    descripcion: 'Consulta y gestión de personas, organizaciones, grupos e instalaciones.',
    icono: <GroupsOutlinedIcon />,
    ruta: '/entidades',
    disponible: true,
  },
  {
    titulo: 'Cuentas y accesos',
    descripcion: 'Usuarios, contextos, roles, permisos y políticas de seguridad.',
    icono: <ManageAccountsOutlinedIcon />,
    disponible: false,
  },
  {
    titulo: 'Calendarios',
    descripcion: 'Calendarios, eventos y recurrencias de las entidades del contexto.',
    icono: <CalendarMonthOutlinedIcon />,
    disponible: false,
  },
  {
    titulo: 'Solicitudes',
    descripcion: 'Solicitudes, participantes, mensajes y seguimiento de estados.',
    icono: <TaskAltOutlinedIcon />,
    disponible: false,
  },
  {
    titulo: 'Documentos',
    descripcion: 'Documentación normal, personal y sensible con control de acceso.',
    icono: <DescriptionOutlinedIcon />,
    disponible: false,
  },
]

export function PaginaInicio() {
  const navigate = useNavigate()

  return (
    <Stack spacing={3.5}>
      <Box>
        <Typography variant="h4" fontWeight={760}>
          Inicio
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 0.6, maxWidth: 720 }}>
          Selecciona un área para trabajar con la información disponible en tu contexto actual.
        </Typography>
      </Box>

      <Grid container spacing={2}>
        {accesos.map((acceso) => (
          <Grid key={acceso.titulo} size={{ xs: 12, sm: 6, lg: 4 }}>
            <Paper
              component={acceso.disponible ? 'button' : 'div'}
              type={acceso.disponible ? 'button' : undefined}
              onClick={acceso.disponible ? () => navigate(acceso.ruta!) : undefined}
              elevation={0}
              sx={{
                p: 2.5,
                height: '100%',
                width: '100%',
                textAlign: 'left',
                font: 'inherit',
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 3,
                bgcolor: 'background.paper',
                cursor: acceso.disponible ? 'pointer' : 'default',
                opacity: acceso.disponible ? 1 : 0.58,
                transition: 'border-color .15s ease, background-color .15s ease',
                '&:hover': acceso.disponible
                  ? {
                      borderColor: 'primary.main',
                      bgcolor: 'rgba(36, 87, 214, 0.02)',
                    }
                  : undefined,
              }}
            >
              <Stack spacing={2}>
                <Box
                  sx={{
                    width: 42,
                    height: 42,
                    borderRadius: 2.25,
                    bgcolor: 'action.hover',
                    display: 'grid',
                    placeItems: 'center',
                    color: 'primary.main',
                  }}
                >
                  {acceso.icono}
                </Box>

                <Box>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Typography fontWeight={720}>{acceso.titulo}</Typography>
                    {acceso.disponible && (
                      <ArrowForwardRoundedIcon
                        sx={{ fontSize: 18, color: 'text.secondary' }}
                      />
                    )}
                  </Stack>
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ mt: 0.7, lineHeight: 1.55 }}
                  >
                    {acceso.descripcion}
                  </Typography>
                </Box>
              </Stack>
            </Paper>
          </Grid>
        ))}
      </Grid>
    </Stack>
  )
}
