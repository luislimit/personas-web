import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { type PropsWithChildren, useState } from 'react'
import { AutenticacionProvider } from '../autenticacion/AutenticacionProvider'
import { MfaProvider } from '../autenticacion/MfaProvider'
import { tema } from '../tema/tema'

export function ProveedoresAplicacion({ children }: PropsWithChildren) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: { retry: 1, staleTime: 20_000 },
      mutations: { retry: 0 },
    },
  }))

  return (
    <ThemeProvider theme={tema}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <AutenticacionProvider>
          <MfaProvider>{children}</MfaProvider>
        </AutenticacionProvider>
      </QueryClientProvider>
    </ThemeProvider>
  )
}
