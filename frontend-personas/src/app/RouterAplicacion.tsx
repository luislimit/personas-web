import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { PaginaLogin } from '../autenticacion/PaginaLogin'
import { PaginaSeleccionContexto } from '../autenticacion/PaginaSeleccionContexto'
import { PaginaVerificarEnlaceMfa } from '../autenticacion/PaginaVerificarEnlaceMfa'
import { ProtectorSesion } from '../autenticacion/ProtectorSesion'
import { PaginaDetalleEntidad } from '../entidades/PaginaDetalleEntidad'
import { PaginaEntidades } from '../entidades/PaginaEntidades'
import { LayoutAdministracion } from '../layout/LayoutAdministracion'
import { PaginaInicio } from '../paginas/PaginaInicio'

const router = createBrowserRouter([
  { path: '/iniciar-sesion', element: <PaginaLogin /> },
  {
    element: <ProtectorSesion />,
    children: [
      { path: '/mfa/verificar', element: <PaginaVerificarEnlaceMfa /> },
      { path: '/seleccionar-contexto', element: <PaginaSeleccionContexto /> },
      {
        element: <LayoutAdministracion />,
        children: [
          { path: '/', element: <PaginaInicio /> },
          { path: '/entidades', element: <PaginaEntidades /> },
          { path: '/entidades/:id', element: <PaginaDetalleEntidad /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])

export function RouterAplicacion() {
  return <RouterProvider router={router} />
}
