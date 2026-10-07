import {
  Alert,
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
} from '@mui/material'
import { FormEvent, useEffect, useMemo, useState } from 'react'
import type {
  CrearEntidad,
  DetalleEntidad,
  EntradaPersona,
  ModificarEntidad,
} from './tipos'
import { TIPOS_ENTIDAD } from './utilEntidad'

interface Props {
  abierto: boolean
  entidad?: DetalleEntidad | null
  enviando?: boolean
  error?: string | null
  onCerrar: () => void
  onGuardar: (entrada: CrearEntidad | ModificarEntidad) => Promise<void>
}

function personaVacia(): EntradaPersona {
  return {
    nombre: '',
    primerApellido: '',
    segundoApellido: null,
    fechaNacimiento: null,
    genero: null,
  }
}

export function DialogoEntidad({
  abierto,
  entidad,
  enviando = false,
  error,
  onCerrar,
  onGuardar,
}: Props) {
  const esEdicion = Boolean(entidad)
  const [codigoTipo, setCodigoTipo] = useState('PERSONA')
  const [nombre, setNombre] = useState('')
  const [estado, setEstado] = useState('ACTIVO')
  const [persona, setPersona] = useState<EntradaPersona>(personaVacia())

  useEffect(() => {
    if (!abierto) return

    if (entidad) {
      setCodigoTipo(entidad.codigoTipo)
      setNombre(entidad.nombre)
      setEstado(entidad.estado)
      setPersona(
        entidad.persona
          ? {
              nombre: entidad.persona.nombre,
              primerApellido: entidad.persona.primerApellido,
              segundoApellido: entidad.persona.segundoApellido,
              fechaNacimiento: entidad.persona.fechaNacimiento,
              genero: entidad.persona.genero,
            }
          : personaVacia(),
      )
    } else {
      setCodigoTipo('PERSONA')
      setNombre('')
      setEstado('ACTIVO')
      setPersona(personaVacia())
    }
  }, [abierto, entidad])

  const esPersona = codigoTipo === 'PERSONA'

  const nombreCalculado = useMemo(() => {
    if (!esPersona) return nombre.trim()
    return [
      persona.nombre.trim(),
      persona.primerApellido.trim(),
      persona.segundoApellido?.trim(),
    ].filter(Boolean).join(' ')
  }, [esPersona, nombre, persona])

  async function submit(event: FormEvent) {
    event.preventDefault()

    if (esPersona) {
      const entradaPersona: EntradaPersona = {
        nombre: persona.nombre.trim(),
        primerApellido: persona.primerApellido.trim(),
        segundoApellido: persona.segundoApellido?.trim() || null,
        fechaNacimiento: persona.fechaNacimiento || null,
        genero: persona.genero?.trim() || null,
      }

      if (esEdicion) {
        await onGuardar({
          nombre: nombreCalculado,
          estado,
          persona: entradaPersona,
        })
      } else {
        await onGuardar({
          codigoTipo,
          nombre: nombreCalculado,
          estado,
          persona: entradaPersona,
        })
      }
      return
    }

    if (esEdicion) {
      await onGuardar({ nombre: nombre.trim(), estado })
    } else {
      await onGuardar({ codigoTipo, nombre: nombre.trim(), estado, persona: null })
    }
  }

  const formularioValido = esPersona
    ? persona.nombre.trim().length > 0 && persona.primerApellido.trim().length > 0
    : nombre.trim().length > 0

  return (
    <Dialog
      open={abierto}
      onClose={enviando ? undefined : onCerrar}
      fullWidth
      maxWidth="sm"
    >
      <form onSubmit={(event) => void submit(event)}>
        <DialogTitle>
          {esEdicion ? 'Editar entidad' : 'Nueva entidad'}
        </DialogTitle>

        <DialogContent dividers>
          <Stack spacing={2.25} sx={{ pt: 0.5 }}>
            {error && <Alert severity="error">{error}</Alert>}

            <FormControl fullWidth disabled={esEdicion}>
              <InputLabel id="tipo-entidad-label">Tipo</InputLabel>
              <Select
                labelId="tipo-entidad-label"
                label="Tipo"
                value={codigoTipo}
                onChange={(event) => setCodigoTipo(event.target.value)}
              >
                {TIPOS_ENTIDAD.map((tipo) => (
                  <MenuItem key={tipo.codigo} value={tipo.codigo}>
                    {tipo.nombre}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            {esPersona ? (
              <>
                <TextField
                  label="Nombre"
                  value={persona.nombre}
                  onChange={(event) => {
                    setPersona((actual) => ({ ...actual, nombre: event.target.value }))
                  }}
                  required
                  autoFocus
                />
                <TextField
                  label="Primer apellido"
                  value={persona.primerApellido}
                  onChange={(event) => {
                    setPersona((actual) => ({
                      ...actual,
                      primerApellido: event.target.value,
                    }))
                  }}
                  required
                />
                <TextField
                  label="Segundo apellido"
                  value={persona.segundoApellido ?? ''}
                  onChange={(event) => {
                    setPersona((actual) => ({
                      ...actual,
                      segundoApellido: event.target.value || null,
                    }))
                  }}
                />
                <TextField
                  label="Fecha de nacimiento"
                  type="date"
                  value={persona.fechaNacimiento ?? ''}
                  onChange={(event) => {
                    setPersona((actual) => ({
                      ...actual,
                      fechaNacimiento: event.target.value || null,
                    }))
                  }}
                  slotProps={{ inputLabel: { shrink: true } }}
                />
              </>
            ) : (
              <TextField
                label="Nombre"
                value={nombre}
                onChange={(event) => setNombre(event.target.value)}
                required
                autoFocus
              />
            )}

            <FormControl fullWidth>
              <InputLabel id="estado-entidad-label">Estado</InputLabel>
              <Select
                labelId="estado-entidad-label"
                label="Estado"
                value={estado}
                onChange={(event) => setEstado(event.target.value)}
              >
                <MenuItem value="ACTIVO">Activo</MenuItem>
                <MenuItem value="INACTIVO">Inactivo</MenuItem>
                {esEdicion && <MenuItem value="ARCHIVADO">Archivado</MenuItem>}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>

        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={onCerrar} disabled={enviando}>
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={enviando || !formularioValido}
          >
            {enviando ? 'Guardando…' : esEdicion ? 'Guardar cambios' : 'Crear'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
