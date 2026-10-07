import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ProveedoresAplicacion } from './app/ProveedoresAplicacion'
import { RouterAplicacion } from './app/RouterAplicacion'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ProveedoresAplicacion>
      <RouterAplicacion />
    </ProveedoresAplicacion>
  </StrictMode>,
)
