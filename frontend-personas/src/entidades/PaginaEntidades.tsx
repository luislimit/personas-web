import AddRoundedIcon from '@mui/icons-material/AddRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  CircularProgress,
  FormControl,
  InputAdornment,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usarAutenticacion } from '../autenticacion/AutenticacionProvider'
import { ErrorPeticionApi } from '../api/clienteApi'
import { buscarEntidades, crearEntidad } from './apiEntidades'
import { DialogoEntidad } from './DialogoEntidad'
import type { CrearEntidad, ModificarEntidad } from './tipos'
import {
  etiquetaEstado,
  iniciales,
  nombreTipoEntidad,
  TIPOS_ENTIDAD,
} from './utilEntidad'

export function PaginaEntidades() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { contextoActivo, tienePermiso } = usarAutenticacion()

  const [textoBusqueda, setTextoBusqueda] = useState('')
  const [codigoTipo, setCodigoTipo] = useState('')
  const [estado, setEstado] = useState('ACTIVO')
  const [dialogoCrear, setDialogoCrear] = useState(false)
  const [errorCrear, setErrorCrear] = useState<string | null>(null)

  const filtros = useMemo(
    () => ({
      nombre: textoBusqueda.trim() || undefined,
      codigoTipo: codigoTipo || undefined,
      estado: estado || undefined,
    }),
    [textoBusqueda, codigoTipo, estado],
  )

  const consulta = useQuery({
    queryKey: ['entidades', contextoActivo?.entidadContextoId, filtros],
    queryFn: () => buscarEntidades(filtros),
    enabled: Boolean(contextoActivo),
  })

  const mutacionCrear = useMutation({
    mutationFn: (entrada: CrearEntidad) => crearEntidad(entrada),
    onSuccess: async (entidad) => {
      setDialogoCrear(false)
      setErrorCrear(null)
      await queryClient.invalidateQueries({ queryKey: ['entidades'] })
      navigate(`/entidades/${entidad.id}`)
    },
    onError: (error) => {
      setErrorCrear(
        error instanceof ErrorPeticionApi
          ? error.message
          : 'No se pudo crear la entidad.',
      )
    },
  })

  const puedeCrear = tienePermiso('ENTIDAD.CREAR')

  async function guardarNueva(entrada: CrearEntidad | ModificarEntidad) {
    await mutacionCrear.mutateAsync(entrada as CrearEntidad)
  }

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ xs: 'stretch', sm: 'flex-start' }}
        spacing={2}
      >
        <Box>
          <Typography variant="h4" fontWeight={760}>
            Personas y entidades
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 0.6 }}>
            Personas, grupos, organizaciones e instalaciones accesibles en el contexto actual.
          </Typography>
        </Box>

        {puedeCrear && (
          <Button
            variant="contained"
            startIcon={<AddRoundedIcon />}
            onClick={() => {
              setErrorCrear(null)
              setDialogoCrear(true)
            }}
          >
            Nueva entidad
          </Button>
        )}
      </Stack>

      <Paper
        elevation={0}
        sx={{
          p: 2,
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 3,
        }}
      >
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={1.5}
          alignItems={{ md: 'center' }}
        >
          <TextField
            size="small"
            fullWidth
            placeholder="Buscar por nombre"
            value={textoBusqueda}
            onChange={(event) => setTextoBusqueda(event.target.value)}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchRoundedIcon fontSize="small" />
                  </InputAdornment>
                ),
              },
            }}
          />

          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel id="filtro-tipo-label">Tipo</InputLabel>
            <Select
              labelId="filtro-tipo-label"
              label="Tipo"
              value={codigoTipo}
              onChange={(event) => setCodigoTipo(event.target.value)}
            >
              <MenuItem value="">Todos</MenuItem>
              {TIPOS_ENTIDAD.map((tipo) => (
                <MenuItem key={tipo.codigo} value={tipo.codigo}>
                  {tipo.nombre}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel id="filtro-estado-label">Estado</InputLabel>
            <Select
              labelId="filtro-estado-label"
              label="Estado"
              value={estado}
              onChange={(event) => setEstado(event.target.value)}
            >
              <MenuItem value="">Todos</MenuItem>
              <MenuItem value="ACTIVO">Activos</MenuItem>
              <MenuItem value="INACTIVO">Inactivos</MenuItem>
              <MenuItem value="ARCHIVADO">Archivados</MenuItem>
            </Select>
          </FormControl>
        </Stack>
      </Paper>

      {consulta.isError && (
        <Alert severity="error">
          {consulta.error instanceof Error
            ? consulta.error.message
            : 'No se pudieron cargar las entidades.'}
        </Alert>
      )}

      <Paper
        elevation={0}
        sx={{
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 3,
          overflow: 'hidden',
        }}
      >
        {consulta.isLoading ? (
          <Box sx={{ minHeight: 260, display: 'grid', placeItems: 'center' }}>
            <CircularProgress size={30} />
          </Box>
        ) : (
          <TableContainer>
            <Table sx={{ minWidth: 720 }}>
              <TableHead>
                <TableRow sx={{ bgcolor: 'rgba(15, 23, 42, 0.025)' }}>
                  <TableCell>Nombre</TableCell>
                  <TableCell width={170}>Tipo</TableCell>
                  <TableCell width={130}>Estado</TableCell>
                  <TableCell width={56} />
                </TableRow>
              </TableHead>
              <TableBody>
                {(consulta.data ?? []).map((entidad) => (
                  <TableRow
                    key={entidad.id}
                    hover
                    onClick={() => navigate(`/entidades/${entidad.id}`)}
                    sx={{ cursor: 'pointer' }}
                  >
                    <TableCell>
                      <Stack direction="row" spacing={1.5} alignItems="center">
                        <Avatar
                          sx={{
                            width: 36,
                            height: 36,
                            fontSize: 13,
                            bgcolor: 'action.selected',
                            color: 'text.primary',
                          }}
                        >
                          {iniciales(entidad.nombre)}
                        </Avatar>
                        <Typography fontWeight={650}>{entidad.nombre}</Typography>
                      </Stack>
                    </TableCell>
                    <TableCell>{nombreTipoEntidad(entidad.codigoTipo)}</TableCell>
                    <TableCell>
                      <Chip
                        label={etiquetaEstado(entidad.estado)}
                        size="small"
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell align="right">
                      <ChevronRightRoundedIcon color="disabled" />
                    </TableCell>
                  </TableRow>
                ))}

                {!consulta.isLoading && (consulta.data?.length ?? 0) === 0 && (
                  <TableRow>
                    <TableCell colSpan={4}>
                      <Box sx={{ py: 7, textAlign: 'center' }}>
                        <Typography fontWeight={650}>
                          No hay resultados
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                          Prueba con otros filtros o términos de búsqueda.
                        </Typography>
                      </Box>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>

      <DialogoEntidad
        abierto={dialogoCrear}
        enviando={mutacionCrear.isPending}
        error={errorCrear}
        onCerrar={() => {
          if (!mutacionCrear.isPending) setDialogoCrear(false)
        }}
        onGuardar={guardarNueva}
      />
    </Stack>
  )
}
