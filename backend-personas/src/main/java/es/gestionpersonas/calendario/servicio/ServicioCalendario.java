package es.gestionpersonas.calendario.servicio;

import es.gestionpersonas.calendario.dto.CalendarioDtos;
import es.gestionpersonas.calendario.modelo.*;
import es.gestionpersonas.calendario.repositorio.*;
import es.gestionpersonas.comun.ExcepcionNoEncontrado;
import es.gestionpersonas.seguridad.servicio.ServicioControlAcceso;
import es.gestionpersonas.seguridad.web.AccesoActual;
import es.gestionpersonas.seguridad.web.ContextoAccesoActual;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Consumer;

@Service
@Transactional(readOnly = true)
public class ServicioCalendario {
    private static final int MAX_GENERATED_OCCURRENCES = 10_000;

    private final RepositorioCalendario calendarRepository;
    private final RepositorioEventoCalendario eventRepository;
    private final RepositorioRecurrenciaEvento recurrenceRepository;
    private final RepositorioExcepcionRecurrenciaEvento exceptionRepository;
    private final EntityManager em;
    private final ServicioControlAcceso controlAcceso;

    public ServicioCalendario(RepositorioCalendario calendarRepository,
                           RepositorioEventoCalendario eventRepository,
                           RepositorioRecurrenciaEvento recurrenceRepository,
                           RepositorioExcepcionRecurrenciaEvento exceptionRepository,
                           EntityManager em,
                           ServicioControlAcceso controlAcceso) {
        this.calendarRepository = calendarRepository;
        this.eventRepository = eventRepository;
        this.recurrenceRepository = recurrenceRepository;
        this.exceptionRepository = exceptionRepository;
        this.em = em;
        this.controlAcceso = controlAcceso;
    }

    public List<CalendarioDtos.VistaCalendario> calendars(Long ownerEntityId) {
        AccesoActual acceso = ContextoAccesoActual.require();

        if (ownerEntityId != null) {
            controlAcceso.requirePermissionForEntity(acceso, "CALENDARIO.LEER", ownerEntityId);
        }

        var calendars = ownerEntityId == null
            ? calendarRepository.findAllByOrderByNameAsc()
            : calendarRepository.findByOwnerEntityIdOrderByNameAsc(ownerEntityId);

        return calendars.stream()
            .filter(c -> controlAcceso.hasPermissionForEntity(acceso, "CALENDARIO.LEER", c.getOwnerEntityId()))
            .map(this::toCalendarView)
            .toList();
    }

    public CalendarioDtos.VistaCalendario calendario(Long calendarId) {
        Calendario calendario = calendarRequired(calendarId);
        requireCalendarPermission(calendario, "CALENDARIO.LEER");
        return toCalendarView(calendario);
    }

    @Transactional
    public CalendarioDtos.VistaCalendario createCalendar(CalendarioDtos.GuardarCalendario input) {
        ensureEntityExists(input.entidadPropietariaId(), "ownerEntityId");
        controlAcceso.requirePermissionForEntity(ContextoAccesoActual.require(), "CALENDARIO.CREAR", input.entidadPropietariaId());
        Calendario calendario = new Calendario();
        applyCalendar(calendario, input);
        calendario.setActive(input.activo() == null || input.activo());
        return toCalendarView(calendarRepository.save(calendario));
    }

    @Transactional
    public CalendarioDtos.VistaCalendario updateCalendar(Long calendarId, CalendarioDtos.GuardarCalendario input) {
        Calendario calendario = calendarRequired(calendarId);
        requireCalendarPermission(calendario, "CALENDARIO.MODIFICAR");
        ensureEntityExists(input.entidadPropietariaId(), "ownerEntityId");
        controlAcceso.requirePermissionForEntity(ContextoAccesoActual.require(), "CALENDARIO.MODIFICAR", input.entidadPropietariaId());
        applyCalendar(calendario, input);
        if (input.activo() != null) calendario.setActive(input.activo());
        return toCalendarView(calendarRepository.save(calendario));
    }

    @Transactional
    public void deactivateCalendar(Long calendarId) {
        Calendario calendario = calendarRequired(calendarId);
        requireCalendarPermission(calendario, "CALENDARIO.ELIMINAR");
        calendario.setActive(false);
        calendarRepository.save(calendario);
    }

    public List<CalendarioDtos.VistaEvento> events(Long calendarId, LocalDateTime from, LocalDateTime to) {
        Calendario calendario = calendarRequired(calendarId);
        requireCalendarPermission(calendario, "EVENTO.LEER");
        LocalDateTime windowFrom = from != null ? from : LocalDateTime.now().minusMonths(1);
        LocalDateTime windowTo = to != null ? to : LocalDateTime.now().plusYears(1);
        if (!windowTo.isAfter(windowFrom)) {
            throw new IllegalArgumentException("'to' debe ser posterior a 'from'");
        }

        List<EventoCalendario> all = eventRepository.findByCalendarIdOrderByStartsAtAsc(calendarId);
        if (all.isEmpty()) return List.of();

        Map<Long, EventoCalendario> byId = new HashMap<>();
        all.forEach(e -> byId.put(e.getId(), e));

        List<RecurrenciaEvento> recurrences = recurrenceRepository.findByEventIdInAndActiveTrue(byId.keySet());
        Map<Long, RecurrenciaEvento> recurrenceByEvent = new HashMap<>();
        recurrences.forEach(r -> recurrenceByEvent.put(r.getEventId(), r));

        List<ExcepcionRecurrenciaEvento> exceptions = recurrences.isEmpty()
            ? List.of()
            : exceptionRepository.findByRecurrenceIdIn(recurrences.stream().map(RecurrenciaEvento::getId).toList());

        Map<Long, Map<LocalDateTime, ExcepcionRecurrenciaEvento>> exceptionsByRecurrence = new HashMap<>();
        Set<Long> replacementIds = new HashSet<>();
        for (ExcepcionRecurrenciaEvento ex : exceptions) {
            exceptionsByRecurrence.computeIfAbsent(ex.getRecurrenceId(), ignored -> new HashMap<>())
                .put(ex.getOriginalStartsAt(), ex);
            if (ex.getReplacementEventId() != null) replacementIds.add(ex.getReplacementEventId());
        }

        List<CalendarioDtos.VistaEvento> result = new ArrayList<>();
        for (EventoCalendario event : all) {
            if (replacementIds.contains(event.getId())) continue;
            RecurrenciaEvento recurrence = recurrenceByEvent.get(event.getId());
            if (recurrence == null) {
                if (overlaps(event.getStartsAt(), event.getEndsAt(), windowFrom, windowTo)) {
                    result.add(toEventView(event, null, event.getStartsAt(), false, null, calendario.getTimezone()));
                }
                continue;
            }
            expandSeries(event, recurrence, exceptionsByRecurrence.getOrDefault(recurrence.getId(), Map.of()),
                byId, windowFrom, windowTo, calendario.getTimezone(), result);
        }

        result.sort(Comparator.comparing(CalendarioDtos.VistaEvento::fechaInicio));
        return result;
    }

    public CalendarioDtos.VistaEvento event(Long eventId, LocalDateTime occurrenceStartsAt) {
        EventoCalendario event = eventRequired(eventId);
        requireCalendarPermission(calendarRequired(event.getCalendarId()), "EVENTO.LEER");
        RecurrenciaEvento recurrence = recurrenceRepository.findByEventId(eventId).filter(RecurrenciaEvento::isActive).orElse(null);
        Calendario calendario = calendarRequired(event.getCalendarId());
        if (recurrence == null) {
            return toEventView(event, null, event.getStartsAt(), false, null, calendario.getTimezone());
        }
        LocalDateTime occurrence = occurrenceStartsAt != null ? occurrenceStartsAt : event.getStartsAt();
        ExcepcionRecurrenciaEvento ex = exceptionRepository
            .findByRecurrenceIdAndOriginalStartsAt(recurrence.getId(), occurrence).orElse(null);
        if (ex != null && "CANCELADA".equals(ex.getExceptionType())) {
            throw new ExcepcionNoEncontrado("La ocurrencia está cancelada");
        }
        EventoCalendario visible = ex != null && ex.getReplacementEventId() != null
            ? eventRequired(ex.getReplacementEventId()) : shiftedEventCopy(event, occurrence);
        return toEventView(visible, recurrence, occurrence, ex != null, event.getId(), calendario.getTimezone());
    }

    @Transactional
    public CalendarioDtos.VistaEvento createEvent(Long calendarId, CalendarioDtos.GuardarEvento input) {
        Calendario calendario = calendarRequired(calendarId);
        requireCalendarPermission(calendario, "EVENTO.CREAR");
        validateEventTimes(input.fechaInicio(), input.fechaFin());
        if (input.entidadUbicacionId() != null) ensureEntityExists(input.entidadUbicacionId(), "locationEntityId");

        EventoCalendario event = new EventoCalendario();
        event.setCalendarId(calendarId);
        applyEvent(event, input.titulo(), input.descripcion(), input.fechaInicio(), input.fechaFin(), input.diaCompleto(),
            input.zonaHoraria(), input.entidadUbicacionId(), input.estado(), input.creadoPorCuentaId(), calendario.getTimezone());
        event = eventRepository.save(event);

        RecurrenciaEvento recurrence = null;
        if (input.recurrencia() != null) {
            recurrence = createRecurrence(event.getId(), input.recurrencia(), event.getStartsAt(), event.getTimezone());
        }
        return toEventView(event, recurrence, event.getStartsAt(), false, recurrence == null ? null : event.getId(), calendario.getTimezone());
    }

    @Transactional
    public CalendarioDtos.VistaEvento updateEvent(Long eventId,
                                               LocalDateTime occurrenceStartsAt,
                                               CalendarioDtos.AlcanceRecurrencia alcance,
                                               CalendarioDtos.ModificarEvento patch) {
        EventoCalendario master = eventRequired(eventId);
        requireCalendarPermission(calendarRequired(master.getCalendarId()), "EVENTO.MODIFICAR");
        RecurrenciaEvento recurrence = recurrenceRepository.findByEventId(eventId).filter(RecurrenciaEvento::isActive).orElse(null);
        Calendario calendario = calendarRequired(master.getCalendarId());

        if (recurrence == null) {
            applyPatch(master, patch, calendario.getTimezone());
            validateEventTimes(master.getStartsAt(), master.getEndsAt());
            master = eventRepository.save(master);
            if (patch.recurrencia() != null) {
                recurrence = createRecurrence(master.getId(), patch.recurrencia(), master.getStartsAt(), master.getTimezone());
            }
            return toEventView(master, recurrence, master.getStartsAt(), false, recurrence == null ? null : master.getId(), calendario.getTimezone());
        }

        CalendarioDtos.AlcanceRecurrencia effectiveScope = alcance == null ? CalendarioDtos.AlcanceRecurrencia.SOLO_ESTE : alcance;
        LocalDateTime occurrence = occurrenceStartsAt != null ? occurrenceStartsAt : master.getStartsAt();
        return switch (effectiveScope) {
            case SOLO_ESTE -> updateSingleOccurrence(master, recurrence, occurrence, patch, calendario.getTimezone());
            case ESTE_Y_SIGUIENTES -> updateThisAndFollowing(master, recurrence, occurrence, patch, calendario.getTimezone());
            case TODA_LA_SERIE -> updateWholeSeries(master, recurrence, patch, calendario.getTimezone());
        };
    }

    @Transactional
    public void deleteEvent(Long eventId, LocalDateTime occurrenceStartsAt, CalendarioDtos.AlcanceRecurrencia alcance) {
        EventoCalendario master = eventRequired(eventId);
        requireCalendarPermission(calendarRequired(master.getCalendarId()), "EVENTO.ELIMINAR");
        RecurrenciaEvento recurrence = recurrenceRepository.findByEventId(eventId).filter(RecurrenciaEvento::isActive).orElse(null);
        if (recurrence == null) {
            master.setStatus("CANCELADO");
            eventRepository.save(master);
            return;
        }

        CalendarioDtos.AlcanceRecurrencia effectiveScope = alcance == null ? CalendarioDtos.AlcanceRecurrencia.SOLO_ESTE : alcance;
        LocalDateTime occurrence = occurrenceStartsAt != null ? occurrenceStartsAt : master.getStartsAt();
        switch (effectiveScope) {
            case SOLO_ESTE -> cancelOccurrence(recurrence, occurrence);
            case ESTE_Y_SIGUIENTES -> {
                if (!occurrence.isAfter(master.getStartsAt())) {
                    recurrence.setActive(false);
                    master.setStatus("CANCELADO");
                    eventRepository.save(master);
                } else {
                    recurrence.setUntilAt(occurrence.minusSeconds(1));
                    recurrence.setOccurrenceCount(null);
                }
                recurrenceRepository.save(recurrence);
            }
            case TODA_LA_SERIE -> {
                recurrence.setActive(false);
                recurrenceRepository.save(recurrence);
                master.setStatus("CANCELADO");
                eventRepository.save(master);
            }
        }
    }

    private CalendarioDtos.VistaEvento updateSingleOccurrence(EventoCalendario master, RecurrenciaEvento recurrence,
                                                           LocalDateTime occurrence, CalendarioDtos.ModificarEvento patch,
                                                           String calendarTimezone) {
        ExcepcionRecurrenciaEvento existing = exceptionRepository
            .findByRecurrenceIdAndOriginalStartsAt(recurrence.getId(), occurrence).orElse(null);
        EventoCalendario replacement;
        if (existing != null && existing.getReplacementEventId() != null) {
            replacement = eventRequired(existing.getReplacementEventId());
        } else {
            replacement = shiftedEventCopy(master, occurrence);
        }
        applyPatch(replacement, patch, calendarTimezone);
        validateEventTimes(replacement.getStartsAt(), replacement.getEndsAt());
        replacement = eventRepository.save(replacement);

        ExcepcionRecurrenciaEvento ex = existing == null ? new ExcepcionRecurrenciaEvento() : existing;
        ex.setRecurrenceId(recurrence.getId());
        ex.setOriginalStartsAt(occurrence);
        ex.setExceptionType("MODIFICADA");
        ex.setReplacementEventId(replacement.getId());
        exceptionRepository.save(ex);
        return toEventView(replacement, recurrence, occurrence, true, master.getId(), calendarTimezone);
    }

    private CalendarioDtos.VistaEvento updateThisAndFollowing(EventoCalendario master, RecurrenciaEvento recurrence,
                                                           LocalDateTime occurrence, CalendarioDtos.ModificarEvento patch,
                                                           String calendarTimezone) {
        if (!occurrence.isAfter(master.getStartsAt())) {
            return updateWholeSeries(master, recurrence, patch, calendarTimezone);
        }

        CalendarioDtos.EntradaRecurrencia inheritedRule = recurrenceToInput(recurrence, recurrence.getUntilAt());
        if (recurrence.getOccurrenceCount() != null) {
            int consumedBefore = (int) generateOccurrences(master.getStartsAt(), recurrence, occurrence)
                .stream().filter(start -> start.isBefore(occurrence)).count();
            int remaining = Math.max(1, recurrence.getOccurrenceCount() - consumedBefore);
            inheritedRule = new CalendarioDtos.EntradaRecurrencia(
                inheritedRule.frecuencia(), inheritedRule.intervalo(), inheritedRule.diasSemana(), inheritedRule.diasMes(),
                null, remaining, inheritedRule.zonaHoraria());
        }
        recurrence.setUntilAt(occurrence.minusSeconds(1));
        recurrence.setOccurrenceCount(null);
        recurrenceRepository.save(recurrence);

        EventoCalendario newMaster = shiftedEventCopy(master, occurrence);
        applyPatch(newMaster, patch, calendarTimezone);
        validateEventTimes(newMaster.getStartsAt(), newMaster.getEndsAt());
        newMaster = eventRepository.save(newMaster);

        CalendarioDtos.EntradaRecurrencia newRuleInput = patch.recurrencia() != null
            ? patch.recurrencia()
            : inheritedRule;
        RecurrenciaEvento newRecurrence = createRecurrence(newMaster.getId(), newRuleInput, newMaster.getStartsAt(), newMaster.getTimezone());
        return toEventView(newMaster, newRecurrence, newMaster.getStartsAt(), false, newMaster.getId(), calendarTimezone);
    }

    private CalendarioDtos.VistaEvento updateWholeSeries(EventoCalendario master, RecurrenciaEvento recurrence,
                                                      CalendarioDtos.ModificarEvento patch, String calendarTimezone) {
        applyPatch(master, patch, calendarTimezone);
        validateEventTimes(master.getStartsAt(), master.getEndsAt());
        master = eventRepository.save(master);
        if (patch.recurrencia() != null) applyRecurrence(recurrence, patch.recurrencia(), master.getTimezone());
        recurrenceRepository.save(recurrence);
        return toEventView(master, recurrence, master.getStartsAt(), false, master.getId(), calendarTimezone);
    }

    private void cancelOccurrence(RecurrenciaEvento recurrence, LocalDateTime occurrence) {
        ExcepcionRecurrenciaEvento ex = exceptionRepository
            .findByRecurrenceIdAndOriginalStartsAt(recurrence.getId(), occurrence)
            .orElseGet(ExcepcionRecurrenciaEvento::new);
        if (ex.getReplacementEventId() != null) {
            EventoCalendario replacement = eventRequired(ex.getReplacementEventId());
            replacement.setStatus("CANCELADO");
            eventRepository.save(replacement);
        }
        ex.setRecurrenceId(recurrence.getId());
        ex.setOriginalStartsAt(occurrence);
        ex.setExceptionType("CANCELADA");
        ex.setReplacementEventId(null);
        exceptionRepository.save(ex);
    }

    private void expandSeries(EventoCalendario master,
                              RecurrenciaEvento recurrence,
                              Map<LocalDateTime, ExcepcionRecurrenciaEvento> exceptions,
                              Map<Long, EventoCalendario> byId,
                              LocalDateTime from,
                              LocalDateTime to,
                              String calendarTimezone,
                              List<CalendarioDtos.VistaEvento> target) {
        List<LocalDateTime> starts = generateOccurrences(master.getStartsAt(), recurrence, to);
        for (LocalDateTime occurrence : starts) {
            if (!occurrence.isBefore(to)) break;
            LocalDateTime occurrenceEnd = shiftedEnd(master, occurrence);
            if (!overlaps(occurrence, occurrenceEnd, from, to)) continue;

            ExcepcionRecurrenciaEvento ex = exceptions.get(occurrence);
            if (ex != null && "CANCELADA".equals(ex.getExceptionType())) continue;
            if (ex != null && ex.getReplacementEventId() != null) {
                EventoCalendario replacement = byId.get(ex.getReplacementEventId());
                if (replacement == null) replacement = eventRepository.findById(ex.getReplacementEventId()).orElse(null);
                if (replacement != null && !"CANCELADO".equals(replacement.getStatus())) {
                    target.add(toEventView(replacement, recurrence, occurrence, true, master.getId(), calendarTimezone));
                }
            } else if (!"CANCELADO".equals(master.getStatus())) {
                EventoCalendario shifted = shiftedEventCopy(master, occurrence);
                target.add(toEventView(shifted, recurrence, occurrence, false, master.getId(), calendarTimezone));
            }
        }
    }

    private List<LocalDateTime> generateOccurrences(LocalDateTime anchor, RecurrenciaEvento recurrence, LocalDateTime queryTo) {
        List<LocalDateTime> result = new ArrayList<>();
        int maxByCount = recurrence.getOccurrenceCount() == null ? MAX_GENERATED_OCCURRENCES : recurrence.getOccurrenceCount();
        LocalDateTime naturalEnd = recurrence.getUntilAt() != null && recurrence.getUntilAt().isBefore(queryTo)
            ? recurrence.getUntilAt().plusNanos(1) : queryTo;

        switch (recurrence.getFrequency()) {
            case "DIARIA" -> {
                LocalDateTime current = anchor;
                for (int i = 0; i < maxByCount && i < MAX_GENERATED_OCCURRENCES && current.isBefore(naturalEnd); i++) {
                    result.add(current);
                    current = current.plusDays(recurrence.getIntervalValue());
                }
            }
            case "SEMANAL" -> generateWeekly(anchor, recurrence, naturalEnd, maxByCount, result);
            case "MENSUAL" -> generateMonthly(anchor, recurrence, naturalEnd, maxByCount, result);
            case "ANUAL" -> {
                LocalDateTime current = anchor;
                for (int i = 0; i < maxByCount && i < MAX_GENERATED_OCCURRENCES && current.isBefore(naturalEnd); i++) {
                    result.add(current);
                    current = current.plusYears(recurrence.getIntervalValue());
                }
            }
            default -> throw new IllegalArgumentException("Frecuencia de recurrencia no soportada: " + recurrence.getFrequency());
        }
        return result;
    }

    private void generateWeekly(LocalDateTime anchor, RecurrenciaEvento recurrence, LocalDateTime end,
                                int maxCount, List<LocalDateTime> result) {
        Set<DayOfWeek> days = parseDays(recurrence.getByDay());
        if (days.isEmpty()) days = Set.of(anchor.getDayOfWeek());
        LocalDate anchorDate = anchor.toLocalDate();
        LocalDate weekZero = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate date = anchorDate;
        int guard = 0;
        while (result.size() < maxCount && result.size() < MAX_GENERATED_OCCURRENCES && guard++ < 200_000) {
            LocalDateTime candidate = LocalDateTime.of(date, anchor.toLocalTime());
            if (!candidate.isBefore(end)) break;
            LocalDate candidateWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long weeks = ChronoUnit.WEEKS.between(weekZero, candidateWeek);
            if (weeks % recurrence.getIntervalValue() == 0 && days.contains(date.getDayOfWeek()) && !candidate.isBefore(anchor)) {
                result.add(candidate);
            }
            date = date.plusDays(1);
        }
    }

    private void generateMonthly(LocalDateTime anchor, RecurrenciaEvento recurrence, LocalDateTime end,
                                 int maxCount, List<LocalDateTime> result) {
        List<Integer> days = parseMonthDays(recurrence.getByMonthDay());
        if (days.isEmpty()) days = List.of(anchor.getDayOfMonth());
        YearMonth month = YearMonth.from(anchor);
        int monthIndex = 0;
        while (result.size() < maxCount && result.size() < MAX_GENERATED_OCCURRENCES) {
            YearMonth current = month.plusMonths((long) monthIndex * recurrence.getIntervalValue());
            LocalDateTime first = current.atDay(1).atTime(anchor.toLocalTime());
            if (!first.isBefore(end)) break;
            for (Integer day : days) {
                if (day <= current.lengthOfMonth()) {
                    LocalDateTime candidate = current.atDay(day).atTime(anchor.toLocalTime());
                    if (!candidate.isBefore(anchor) && candidate.isBefore(end)) result.add(candidate);
                    if (result.size() >= maxCount || result.size() >= MAX_GENERATED_OCCURRENCES) break;
                }
            }
            monthIndex++;
        }
        result.sort(Comparator.naturalOrder());
    }

    private RecurrenciaEvento createRecurrence(Long eventId, CalendarioDtos.EntradaRecurrencia input,
                                             LocalDateTime anchor, String eventTimezone) {
        validateRecurrence(input, anchor);
        RecurrenciaEvento recurrence = new RecurrenciaEvento();
        recurrence.setEventId(eventId);
        applyRecurrence(recurrence, input, eventTimezone);
        return recurrenceRepository.save(recurrence);
    }

    private void applyRecurrence(RecurrenciaEvento recurrence, CalendarioDtos.EntradaRecurrencia input, String eventTimezone) {
        validateRecurrence(input, null);
        recurrence.setFrequency(input.frecuencia().name());
        recurrence.setIntervalValue(input.intervalo() == null ? 1 : input.intervalo());
        recurrence.setByDay(input.diasSemana() == null || input.diasSemana().isEmpty() ? null : String.join(",", input.diasSemana()));
        recurrence.setByMonthDay(input.diasMes() == null || input.diasMes().isEmpty() ? null
            : input.diasMes().stream().map(String::valueOf).reduce((a,b) -> a + "," + b).orElse(null));
        recurrence.setUntilAt(input.hasta());
        recurrence.setOccurrenceCount(input.numeroOcurrencias());
        recurrence.setTimezone(firstNonBlank(input.zonaHoraria(), eventTimezone, "Europe/Madrid"));
        recurrence.setActive(true);
    }

    private void validateRecurrence(CalendarioDtos.EntradaRecurrencia input, LocalDateTime anchor) {
        if (input.hasta() != null && input.numeroOcurrencias() != null) {
            throw new IllegalArgumentException("La recurrencia no puede tener a la vez 'until' y 'count'");
        }
        if (anchor != null && input.hasta() != null && input.hasta().isBefore(anchor)) {
            throw new IllegalArgumentException("'until' no puede ser anterior al inicio del evento");
        }
        if (input.frecuencia() == CalendarioDtos.FrecuenciaRecurrencia.SEMANAL && input.diasMes() != null && !input.diasMes().isEmpty()) {
            throw new IllegalArgumentException("diasMes no aplica a una recurrencia SEMANAL");
        }
    }

    private CalendarioDtos.EntradaRecurrencia recurrenceToInput(RecurrenciaEvento r, LocalDateTime untilOverride) {
        return new CalendarioDtos.EntradaRecurrencia(
            CalendarioDtos.FrecuenciaRecurrencia.valueOf(r.getFrequency()), r.getIntervalValue(),
            splitCsv(r.getByDay()), parseMonthDays(r.getByMonthDay()),
            untilOverride != null ? untilOverride : r.getUntilAt(), r.getOccurrenceCount(), r.getTimezone());
    }

    private EventoCalendario shiftedEventCopy(EventoCalendario source, LocalDateTime newStart) {
        EventoCalendario copy = new EventoCalendario();
        copy.setCalendarId(source.getCalendarId());
        copy.setTitle(source.getTitle());
        copy.setDescription(source.getDescription());
        copy.setStartsAt(newStart);
        copy.setEndsAt(shiftedEnd(source, newStart));
        copy.setAllDay(source.isAllDay());
        copy.setTimezone(source.getTimezone());
        copy.setLocationEntityId(source.getLocationEntityId());
        copy.setStatus(source.getStatus());
        copy.setCreatedByAccountId(source.getCreatedByAccountId());
        return copy;
    }

    private LocalDateTime shiftedEnd(EventoCalendario source, LocalDateTime newStart) {
        if (source.getEndsAt() == null) return null;
        return newStart.plus(Duration.between(source.getStartsAt(), source.getEndsAt()));
    }

    private void applyCalendar(Calendario calendario, CalendarioDtos.GuardarCalendario input) {
        calendario.setOwnerEntityId(input.entidadPropietariaId());
        calendario.setName(input.nombre());
        calendario.setDescription(input.descripcion());
        calendario.setDefaultColor(input.colorPredeterminado());
        calendario.setTimezone(firstNonBlank(input.zonaHoraria(), "Europe/Madrid"));
    }

    private void applyEvent(EventoCalendario event, String title, String description,
                            LocalDateTime startsAt, LocalDateTime endsAt, Boolean allDay,
                            String zona_horaria, Long locationEntityId, String status,
                            Long createdByAccountId, String calendarTimezone) {
        event.setTitle(title);
        event.setDescription(description);
        event.setStartsAt(startsAt);
        event.setEndsAt(endsAt);
        event.setAllDay(Boolean.TRUE.equals(allDay));
        event.setTimezone(firstNonBlank(zona_horaria, calendarTimezone, "Europe/Madrid"));
        event.setLocationEntityId(locationEntityId);
        event.setStatus(firstNonBlank(status, "PROGRAMADO"));
        event.setCreatedByAccountId(createdByAccountId);
    }

    private void applyPatch(EventoCalendario event, CalendarioDtos.ModificarEvento patch, String calendarTimezone) {
        if (patch.titulo() != null) event.setTitle(patch.titulo());
        if (patch.descripcion() != null) event.setDescription(patch.descripcion());
        if (patch.fechaInicio() != null) {
            Duration duration = event.getEndsAt() == null ? null : Duration.between(event.getStartsAt(), event.getEndsAt());
            event.setStartsAt(patch.fechaInicio());
            if (patch.fechaFin() == null && duration != null) event.setEndsAt(patch.fechaInicio().plus(duration));
        }
        if (patch.fechaFin() != null) event.setEndsAt(patch.fechaFin());
        if (patch.diaCompleto() != null) event.setAllDay(patch.diaCompleto());
        if (patch.zonaHoraria() != null) event.setTimezone(firstNonBlank(patch.zonaHoraria(), calendarTimezone, "Europe/Madrid"));
        if (patch.entidadUbicacionId() != null) {
            ensureEntityExists(patch.entidadUbicacionId(), "locationEntityId");
            event.setLocationEntityId(patch.entidadUbicacionId());
        }
        if (patch.estado() != null) event.setStatus(patch.estado());
    }

    private void validateEventTimes(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null) throw new IllegalArgumentException("fechaInicio es obligatoria");
        if (endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("fechaFin debe ser posterior a fechaInicio");
        }
    }

    private boolean overlaps(LocalDateTime start, LocalDateTime end, LocalDateTime from, LocalDateTime to) {
        LocalDateTime effectiveEnd = end == null ? start.plusNanos(1) : end;
        return start.isBefore(to) && effectiveEnd.isAfter(from);
    }

    private CalendarioDtos.VistaCalendario toCalendarView(Calendario c) {
        return new CalendarioDtos.VistaCalendario(c.getId(), c.getOwnerEntityId(), entityName(c.getOwnerEntityId()),
            c.getName(), c.getDescription(), c.getDefaultColor(), c.getTimezone(), c.isActive());
    }

    private CalendarioDtos.VistaEvento toEventView(EventoCalendario e, RecurrenciaEvento recurrence,
                                                LocalDateTime originalStart, boolean modified,
                                                Long seriesEventId, String calendarTimezone) {
        Long visibleId = e.getId() != null ? e.getId() : seriesEventId;
        return new CalendarioDtos.VistaEvento(
            visibleId, seriesEventId, e.getCalendarId(), e.getTitle(), e.getDescription(),
            e.getStartsAt(), e.getEndsAt(), originalStart, e.isAllDay(),
            firstNonBlank(e.getTimezone(), calendarTimezone, "Europe/Madrid"),
            e.getLocationEntityId(), e.getLocationEntityId() == null ? null : entityName(e.getLocationEntityId()),
            e.getStatus(), recurrence != null, modified, recurrence == null ? null : toRecurrenceView(recurrence));
    }

    private CalendarioDtos.VistaRecurrencia toRecurrenceView(RecurrenciaEvento r) {
        return new CalendarioDtos.VistaRecurrencia(r.getId(), CalendarioDtos.FrecuenciaRecurrencia.valueOf(r.getFrequency()),
            r.getIntervalValue(), splitCsv(r.getByDay()), parseMonthDays(r.getByMonthDay()),
            r.getUntilAt(), r.getOccurrenceCount(), r.getTimezone(), r.isActive());
    }

    private void requireCalendarPermission(Calendario calendario, String permission) {
        controlAcceso.requirePermissionForEntity(
            ContextoAccesoActual.require(),
            permission,
            calendario.getOwnerEntityId()
        );
    }

    private Calendario calendarRequired(Long id) {
        return calendarRepository.findById(id).orElseThrow(() -> new ExcepcionNoEncontrado("No existe el calendario " + id));
    }

    private EventoCalendario eventRequired(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new ExcepcionNoEncontrado("No existe el evento " + id));
    }

    private void ensureEntityExists(Long id, String field) {
        Number count = (Number) em.createNativeQuery("select count(*) from entidad where id=:id").setParameter("id", id).getSingleResult();
        if (count.longValue() == 0) throw new IllegalArgumentException(field + " no referencia una entidad existente: " + id);
    }

    private String entityName(Long id) {
        Object value = em.createNativeQuery("select nombre from entidad where id=:id").setParameter("id", id).getSingleResult();
        return value == null ? null : value.toString();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private static Set<DayOfWeek> parseDays(String value) {
        Map<String, DayOfWeek> map = Map.of("MO", DayOfWeek.MONDAY, "TU", DayOfWeek.TUESDAY,
            "WE", DayOfWeek.WEDNESDAY, "TH", DayOfWeek.THURSDAY, "FR", DayOfWeek.FRIDAY,
            "SA", DayOfWeek.SATURDAY, "SU", DayOfWeek.SUNDAY);
        Set<DayOfWeek> result = new LinkedHashSet<>();
        for (String token : splitCsv(value)) {
            DayOfWeek day = map.get(token);
            if (day != null) result.add(day);
        }
        return result;
    }

    private static List<Integer> parseMonthDays(String value) {
        if (value == null || value.isBlank()) return List.of();
        return splitCsv(value).stream().map(Integer::valueOf).toList();
    }
}
