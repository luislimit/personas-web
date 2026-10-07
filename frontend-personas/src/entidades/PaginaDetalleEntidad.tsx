import ArchiveOutlinedIcon from '@mui/icons-material/ArchiveOutlined'
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { usarAutenticacion } from '../autenticacion/AutenticacionProvider'
import { ErrorPeticionApi } from '../api/clienteApi'
import { archivarEntidad, modificarEntidad, obtenerEntidad } from './apiEntidades'
import { DialogoEntidad } from './DialogoEntidad'
import type { CrearEntidad, ModificarEntidad } from './tipos'
import {
  etiquetaEstado,
  formatearFecha,
  iniciales,
  nombreTipoEntidad,
} from './utilEntidad'

function Campo({ etiqueta, valor }: { etiqueta: string; valor: string | null | undefined }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">
        {etiqueta}
      </Typography>
      <Typography sx={{ mt: 0.25 }}>{valor || '—'}</Typography>
    </Box>
  )
}

export function PaginaDetalleEntidad() {
  const { id } = useParams()
  const entidadId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { contextoActivo, tienePermiso } = usarAutenticacion()

  const [editar, setEditar] = useState(false)
  const [confirmarArchivo, setConfirmarArchivo] = useState(false)
  const [errorEdicion, setErrorEdicion] = useState<string | null>(null)
  const [errorArchivo, setErrorArchivo] = useState<string | null>(null)

  const consulta = useQuery({
    queryKey: ['entidad', contextoActivo?.entidadContextoId, entidadId],
    queryFn: () => obtenerEntidad(entidadId),
    enabled: Number.isFinite(entidadId) && entidadId > 0,
  })

  const mutacionEditar = useMutation({
    mutationFn: (entrada: ModificarEntidad) => modificarEntidad(entidadId, entrada),
    onSuccess: async () => {
      setEditar(false)
      setErrorEdicion(null)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['entidad'] }),
        queryClient.invalidateQueries({ queryKey: ['entidades'] }),
      ])
    },
    onError: (error) => {
      setErrorEdicion(
        error instanceof ErrorPeticionApi
          ? error.message
          : 'No se pudo modificar la entidad.',
      )
    },
  })

  const mutacionArchivar = useMutation({
    mutationFn: () => archivarEntidad(entidadId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['entidades'] })
      navigate('/entidades', { replace: true })
    },
    onError: (error) => {
      setErrorArchivo(
        error instanceof ErrorPeticionApi
          ? error.message
          : 'No se pudo archivar la entidad.',
      )
    },
  })

  if (consulta.isLoading) {
    return (
      <Box sx={{ minHeight: 320, display: 'grid', placeItems: 'center' }}>
        <CircularProgress size={32} />
      </Box>
    )
  }

  if (consulta.isError || !consulta.data) {
    return (
      <Stack spacing={2}>
        <Button
          startIcon={<ArrowBackRoundedIcon />}
          onClick={() => navigate('/entidades')}
          sx={{ alignSelf: 'flex-start' }}
        >
          Volver
        </Button>
        <Alert severity="error">
          {consulta.error instanceof Error
            ? consulta.error.message
            : 'No se pudo cargar la entidad.'}
        </Alert>
      </Stack>
    )
  }

  const entidad = consulta.data
  const persona = entidad.persona
  const puedeModificar =
    entidad.estado !== 'ARCHIVADO' && tienePermiso('ENTIDAD.MODIFICAR')
  const puedeArchivar =
    entidad.estado !== 'ARCHIVADO' && tienePermiso('ENTIDAD.ELIMINAR')

  async function guardar(entrada: CrearEntidad | ModificarEntidad) {
    await mutacionEditar.mutateAsync(entrada as ModificarEntidad)
  }

  return (
    <Stack spacing={3}>
      <Button
        startIcon={<ArrowBackRoundedIcon />}
        onClick={() => navigate('/entidades')}
        sx={{ alignSelf: 'flex-start' }}
      >
        Personas y entidades
      </Button>

      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ xs: 'stretch', sm: 'center' }}
        spacing={2}
      >
        <Stack direction="row" spacing={2} alignItems="center" sx={{ minWidth: 0 }}>
          <Avatar
            sx={{
              width: 54,
              height: 54,
              bgcolor: 'action.selected',
              color: 'text.primary',
              fontWeight: 750,
            }}
          >
            {iniciales(entidad.nombre)}
          </Avatar>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="h4" fontWeight={760} noWrap>
              {entidad.nombre}
            </Typography>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
              <Typography variant="body2" color="text.secondary">
                {nombreTipoEntidad(entidad.codigoTipo)}
              </Typography>
              <Typography variant="body2" color="text.disabled">
                ·
              </Typography>
              <Chip
                label={etiquetaEstado(entidad.estado)}
                size="small"
                variant="outlined"
              />
            </Stack>
          </Box>
        </Stack>

        <Stack direction="row" spacing={1} flexWrap="wrap">
          {puedeModificar && (
            <Button
              variant="outlined"
              startIcon={<EditOutlinedIcon />}
              onClick={() => {
                setErrorEdicion(null)
                setEditar(true)
              }}
            >
              Editar
            </Button>
          )}
          {puedeArchivar && (
            <Button
              color="error"
              variant="outlined"
              startIcon={<ArchiveOutlinedIcon />}
              onClick={() => {
                setErrorArchivo(null)
                setConfirmarArchivo(true)
              }}
            >
              Archivar
            </Button>
          )}
        </Stack>
      </Stack>

      <Paper
        elevation={0}
        sx={{
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 3,
          overflow: 'hidden',
        }}
      >
        <Box sx={{ px: 3, py: 2.25 }}>
          <Typography fontWeight={730}>Información básica</Typography>
        </Box>
        <Divider />

        <Box
          sx={{
            p: 3,
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
            gap: 3,
          }}
        >
          <Campo etiqueta="Tipo" valor={nombreTipoEntidad(entidad.codigoTipo)} />
          <Campo etiqueta="Estado" valor={etiquetaEstado(entidad.estado)} />

          {persona ? (
            <>
              <Campo etiqueta="Nombre" valor={persona.nombre} />
              <Campo etiqueta="Primer apellido" valor={persona.primerApellido} />
              <Campo etiqueta="Segundo apellido" valor={persona.segundoApellido} />
              <Campo
                etiqueta="Fecha de nacimiento"
                valor={formatearFecha(persona.fechaNacimiento)}
              />
            </>
          ) : (
            <Campo etiqueta="Nombre" valor={entidad.nombre} />
          )}
        </Box>
      </Paper>

      {entidad.codigoTipo === 'PERSONA' && persona === null && (
        <Alert severity="info">
          Puedes ver esta persona, pero sus datos personales no están disponibles con
          los permisos actuales.
        </Alert>
      )}

      <Typography variant="caption" color="text.secondary">
        Los identificadores, contactos, direcciones y relaciones se incorporarán en
        los siguientes incrementales del módulo.
      </Typography>

      <DialogoEntidad
        abierto={editar}
        entidad={entidad}
        enviando={mutacionEditar.isPending}
        error={errorEdicion}
        onCerrar={() => {
          if (!mutacionEditar.isPending) setEditar(false)
        }}
        onGuardar={guardar}
      />

      <Dialog
        open={confirmarArchivo}
        onClose={mutacionArchivar.isPending ? undefined : () => setConfirmarArchivo(false)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Archivar entidad</DialogTitle>
        <DialogContent>
          <Stack spacing={2}>
            {errorArchivo && <Alert severity="error">{errorArchivo}</Alert>}
            <Typography>
              Se archivará <strong>{entidad.nombre}</strong>. La información se
              conservará para mantener la trazabilidad.
            </Typography>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button
            onClick={() => setConfirmarArchivo(false)}
            disabled={mutacionArchivar.isPending}
          >
            Cancelar
          </Button>
          <Button
            color="error"
            variant="contained"
            disabled={mutacionArchivar.isPending}
            onClick={() => mutacionArchivar.mutate()}
          >
            {mutacionArchivar.isPending ? 'Archivando…' : 'Archivar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
