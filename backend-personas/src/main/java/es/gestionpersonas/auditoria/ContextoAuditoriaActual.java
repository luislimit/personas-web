package es.gestionpersonas.auditoria;

public final class ContextoAuditoriaActual {
    private static final ThreadLocal<DatosAuditoria> HOLDER = new ThreadLocal<>();

    private ContextoAuditoriaActual() {}

    public static void iniciar(String ip) {
        HOLDER.set(new DatosAuditoria(null, normalizarIp(ip)));
    }

    public static void establecerCuenta(Long cuentaId) {
        DatosAuditoria actual = HOLDER.get();
        HOLDER.set(new DatosAuditoria(cuentaId, actual == null ? null : actual.ip()));
    }

    public static DatosAuditoria get() {
        return HOLDER.get();
    }

    public static void limpiar() {
        HOLDER.remove();
    }

    private static String normalizarIp(String ip) {
        if (ip == null || ip.isBlank()) return null;
        String valor = ip.trim();
        return valor.length() <= 45 ? valor : valor.substring(0, 45);
    }

    public record DatosAuditoria(Long cuentaId, String ip) {}
}
