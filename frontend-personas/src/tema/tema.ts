import { createTheme } from '@mui/material/styles'

export const tema = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#2457d6' },
    background: { default: '#f7f8fa', paper: '#ffffff' },
    text: { primary: '#18202b', secondary: '#667085' },
    divider: '#e5e7eb',
  },
  shape: { borderRadius: 12 },
  typography: {
    fontFamily: 'Inter, ui-sans-serif, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    button: { textTransform: 'none', fontWeight: 650 },
  },
  components: {
    MuiButton: { styleOverrides: { root: { borderRadius: 10 } } },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
    MuiChip: { styleOverrides: { root: { fontWeight: 600 } } },
  },
})
