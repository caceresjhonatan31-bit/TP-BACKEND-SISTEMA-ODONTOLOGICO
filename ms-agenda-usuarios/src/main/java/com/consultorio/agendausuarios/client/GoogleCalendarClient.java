package com.consultorio.agendausuarios.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.consultorio.agendausuarios.domain.entity.Usuario;
import com.consultorio.agendausuarios.dto.response.FeriadoResponseDTO;
import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ExternalServiceException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GoogleCalendarClient {

    private final RestClient restClient;
    private final String feriadosApiUrl;
    private final boolean enabled;
    private final String credentialsPath;
    private final String applicationName;
    private final String calendarId;
    private final String holidaysCalendarId;
    private final String zoneId;
    private final boolean strictMode;

    private Calendar calendarService;
    private final Map<Integer, List<FeriadoResponseDTO>> cacheFeriadosAnual = new ConcurrentHashMap<>();

    public GoogleCalendarClient(
            RestClient.Builder restClientBuilder,
            @Value("${clients.feriados-api.url:https://api.argentinadatos.com/v1/feriados}") String feriadosApiUrl,
            @Value("${google.calendar.enabled:true}") boolean enabled,
            @Value("${google.calendar.credentials-path:google-credentials.json}") String credentialsPath,
            @Value("${google.calendar.application-name:Plataforma-Turnos-Odontologicos}") String applicationName,
            @Value("${google.calendar.calendar-id:primary}") String calendarId,
            @Value("${google.calendar.holidays-calendar-id:es.ar#holiday@group.v.calendar.google.com}") String holidaysCalendarId,
            @Value("${google.calendar.zone-id:America/Argentina/Buenos_Aires}") String zoneId,
            @Value("${google.calendar.strict-mode:false}") boolean strictMode) {
        this.restClient = restClientBuilder.build();
        this.feriadosApiUrl = feriadosApiUrl;
        this.enabled = enabled;
        this.credentialsPath = credentialsPath;
        this.applicationName = applicationName;
        this.calendarId = calendarId;
        this.holidaysCalendarId = holidaysCalendarId;
        this.zoneId = zoneId;
        this.strictMode = strictMode;
    }

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("[GoogleCalendarClient] Integración con Google Calendar deshabilitada por configuración.");
            return;
        }
        try (InputStream credentialsStream = resolverCredencialesStream()) {
            if (credentialsStream == null) {
                log.info("[GoogleCalendarClient] Archivo '{}' no encontrado. Se utilizará validación de feriados vía API externa y generación de eventos en modo local.", credentialsPath);
                return;
            }
            byte[] bytes = credentialsStream.readAllBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (!content.contains("\"private_key\"") || content.contains("YOUR_PRIVATE_KEY")) {
                log.info("[GoogleCalendarClient] El archivo '{}' es una plantilla sin clave privada activa. Operando en modo local para eventos de Google Calendar.", credentialsPath);
                return;
            }

            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new java.io.ByteArrayInputStream(bytes))
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

            this.calendarService = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(applicationName)
                    .build();

            log.info("[GoogleCalendarClient] Cliente de Google Calendar inicializado exitosamente con Service Account.");
        } catch (Exception e) {
            log.warn("[GoogleCalendarClient] No se pudo inicializar el cliente de Google Calendar: {}. Se continuará en modo de respaldo.", e.getMessage());
        }
    }

    /**
     * RN-04: Valida que la fecha solicitada no corresponda a un día feriado en Argentina.
     * Consulta el calendario de feriados en Google Calendar y la API pública de feriados nacionales.
     */
    public void validarDiaNoFeriado(LocalDate fecha) {
        if (!enabled) {
            return;
        }

        // 1. Verificación mediante Google Calendar API (si la cuenta de servicio está activa)
        if (calendarService != null) {
            try {
                ZoneId zona = ZoneId.of(zoneId);
                ZonedDateTime inicioDia = fecha.atStartOfDay(zona);
                ZonedDateTime finDia = fecha.atTime(23, 59, 59).atZone(zona);

                Events events = calendarService.events().list(holidaysCalendarId)
                        .setTimeMin(new DateTime(inicioDia.toInstant().toEpochMilli()))
                        .setTimeMax(new DateTime(finDia.toInstant().toEpochMilli()))
                        .setSingleEvents(true)
                        .execute();

                if (events.getItems() != null && !events.getItems().isEmpty()) {
                    String nombreFeriado = events.getItems().get(0).getSummary();
                    throw new ConflictException(
                            "No se pueden asignar turnos en días feriados de Argentina: " + nombreFeriado + " (" + fecha + ") (RN-04)");
                }
            } catch (ConflictException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[GoogleCalendarClient] No se pudo consultar el calendario de feriados en Google Calendar ({}). Se verificará mediante el servicio de feriados nacionales.", e.getMessage());
            }
        }

        // 2. Verificación mediante API de Feriados de Argentina + padrón oficial nacional
        List<FeriadoResponseDTO> feriados = obtenerFeriadosPorAnio(fecha.getYear());
        String fechaIso = fecha.toString();
        for (FeriadoResponseDTO feriado : feriados) {
            if (fechaIso.equals(feriado.fecha())) {
                throw new ConflictException(
                        "No se pueden asignar turnos en días feriados de Argentina: " + feriado.nombre() + " (" + fecha + ") (RN-04)");
            }
        }
    }

    /**
     * Obtiene la nómina de feriados nacionales de Argentina para un año determinado.
     */
    public List<FeriadoResponseDTO> obtenerFeriadosPorAnio(int anio) {
        return cacheFeriadosAnual.computeIfAbsent(anio, this::consultarFeriadosDeAnio);
    }

    private List<FeriadoResponseDTO> consultarFeriadosDeAnio(int anio) {
        Map<String, FeriadoResponseDTO> mapaFeriados = new LinkedHashMap<>();

        // Cargar feriados nacionales fijos de base como garantía ante desconexión
        for (FeriadoResponseDTO fijo : obtenerFeriadosFijosNacionales(anio)) {
            mapaFeriados.put(fijo.fecha(), fijo);
        }

        // Consultar API externa de Feriados de Argentina (https://api.argentinadatos.com/v1/feriados/{anio})
        try {
            String url = feriadosApiUrl.endsWith("/")
                    ? feriadosApiUrl + anio
                    : feriadosApiUrl + "/" + anio;

            List<FeriadoResponseDTO> respuestaApi = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<FeriadoResponseDTO>>() {});

            if (respuestaApi != null) {
                for (FeriadoResponseDTO feriado : respuestaApi) {
                    if (feriado != null && feriado.fecha() != null) {
                        mapaFeriados.put(feriado.fecha(), feriado);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[GoogleCalendarClient] No se pudo conectar con la API pública de feriados para el año {}: {}. Se utiliza el calendario oficial nacional de respaldo.", anio, e.getMessage());
            if (strictMode) {
                throw new ExternalServiceException("Servicio externo de feriados de Argentina no disponible: " + e.getMessage());
            }
        }

        List<FeriadoResponseDTO> resultado = new ArrayList<>(mapaFeriados.values());
        resultado.sort(Comparator.comparing(FeriadoResponseDTO::fecha));
        return resultado;
    }

    /**
     * RN-04 y F-04: Registra el evento del turno en el Google Calendar del profesional
     * y devuelve el identificador del evento externo (google_event_id).
     */
    public String registrarEventoTurno(
            Usuario odontologo,
            Long pacienteId,
            Long practicaInicialId,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            String notaAclaratoria) {

        if (!enabled) {
            return null;
        }

        if (calendarService != null) {
            try {
                ZoneId zona = ZoneId.of(zoneId);
                ZonedDateTime inicioZoned = ZonedDateTime.of(fecha, horaInicio, zona);
                ZonedDateTime finZoned = ZonedDateTime.of(fecha, horaFin, zona);

                Event event = new Event()
                        .setSummary("Turno Odontológico - Dr/a. " + odontologo.getApellido() + ", " + odontologo.getNombre())
                        .setDescription(construirDescripcionEvento(odontologo, pacienteId, practicaInicialId, notaAclaratoria));

                EventDateTime start = new EventDateTime()
                        .setDateTime(new DateTime(inicioZoned.toInstant().toEpochMilli()))
                        .setTimeZone(zoneId);
                event.setStart(start);

                EventDateTime end = new EventDateTime()
                        .setDateTime(new DateTime(finZoned.toInstant().toEpochMilli()))
                        .setTimeZone(zoneId);
                event.setEnd(end);

                Event createdEvent = calendarService.events().insert(calendarId, event).execute();
                log.info("[GoogleCalendarClient] Evento creado en Google Calendar con ID: {}", createdEvent.getId());
                return createdEvent.getId();
            } catch (Exception e) {
                log.error("[GoogleCalendarClient] Error al insertar evento en Google Calendar: {}", e.getMessage());
                if (strictMode) {
                    throw new ExternalServiceException("Error al comunicarse con la API de Google Calendar para agendar el turno: " + e.getMessage());
                }
            }
        } else if (strictMode) {
            throw new ExternalServiceException("No se encontró una configuración activa de Google Calendar (google-credentials.json)");
        }

        // Generación de identificador de evento sincronizado para entorno local/desarrollo
        String generatedEventId = "gcal_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[GoogleCalendarClient] Evento de agenda registrado con ID externo: {}", generatedEventId);
        return generatedEventId;
    }

    /**
     * RN-09: Elimina o cancela el evento en Google Calendar cuando el turno es cancelado antes de su inicio.
     */
    public void eliminarEventoTurno(String googleEventId) {
        if (!enabled || googleEventId == null || googleEventId.isBlank()) {
            return;
        }

        if (calendarService != null && !googleEventId.startsWith("gcal_")) {
            try {
                calendarService.events().delete(calendarId, googleEventId).execute();
                log.info("[GoogleCalendarClient] Evento {} eliminado de Google Calendar.", googleEventId);
            } catch (Exception e) {
                log.warn("[GoogleCalendarClient] No se pudo eliminar el evento {} de Google Calendar: {}", googleEventId, e.getMessage());
                if (strictMode) {
                    throw new ExternalServiceException("Error al cancelar el evento en Google Calendar: " + e.getMessage());
                }
            }
        } else {
            log.info("[GoogleCalendarClient] Baja lógica del evento externo {} procesada.", googleEventId);
        }
    }

    private String construirDescripcionEvento(Usuario odontologo, Long pacienteId, Long practicaInicialId, String notaAclaratoria) {
        StringBuilder sb = new StringBuilder();
        sb.append("Paciente ID: ").append(pacienteId)
          .append(" | Práctica Inicial ID: ").append(practicaInicialId)
          .append(" | Odontólogo: ").append(odontologo.getApellido()).append(", ").append(odontologo.getNombre())
          .append(" (Matrícula: ").append(odontologo.getMatricula()).append(")");
        if (notaAclaratoria != null && !notaAclaratoria.isBlank()) {
            sb.append(" | Nota: ").append(notaAclaratoria);
        }
        return sb.toString();
    }

    private InputStream resolverCredencialesStream() {
        try {
            File fileDirecto = new File(credentialsPath);
            if (fileDirecto.exists() && fileDirecto.isFile()) {
                return new FileInputStream(fileDirecto);
            }
            File fileRaiz = new File("..", credentialsPath);
            if (fileRaiz.exists() && fileRaiz.isFile()) {
                return new FileInputStream(fileRaiz);
            }
            ClassPathResource resource = new ClassPathResource(credentialsPath);
            if (resource.exists()) {
                return resource.getInputStream();
            }
        } catch (Exception e) {
            log.debug("[GoogleCalendarClient] Error buscando archivo de credenciales: {}", e.getMessage());
        }
        return null;
    }

    private List<FeriadoResponseDTO> obtenerFeriadosFijosNacionales(int anio) {
        return List.of(
                new FeriadoResponseDTO(LocalDate.of(anio, 1, 1).toString(), "inamovible", "Año Nuevo"),
                new FeriadoResponseDTO(LocalDate.of(anio, 3, 24).toString(), "inamovible", "Día Nacional de la Memoria por la Verdad y la Justicia"),
                new FeriadoResponseDTO(LocalDate.of(anio, 4, 2).toString(), "inamovible", "Día del Veterano y de los Caídos en la Guerra de Malvinas"),
                new FeriadoResponseDTO(LocalDate.of(anio, 5, 1).toString(), "inamovible", "Día del Trabajador"),
                new FeriadoResponseDTO(LocalDate.of(anio, 5, 25).toString(), "inamovible", "Día de la Revolución de Mayo"),
                new FeriadoResponseDTO(LocalDate.of(anio, 6, 17).toString(), "trasladable", "Paso a la Inmortalidad del Gral. Don Martín Miguel de Güemes"),
                new FeriadoResponseDTO(LocalDate.of(anio, 6, 20).toString(), "inamovible", "Paso a la Inmortalidad del Gral. Manuel Belgrano"),
                new FeriadoResponseDTO(LocalDate.of(anio, 7, 9).toString(), "inamovible", "Día de la Independencia"),
                new FeriadoResponseDTO(LocalDate.of(anio, 8, 17).toString(), "trasladable", "Paso a la Inmortalidad del Gral. José de San Martín"),
                new FeriadoResponseDTO(LocalDate.of(anio, 10, 12).toString(), "trasladable", "Día del Respeto a la Diversidad Cultural"),
                new FeriadoResponseDTO(LocalDate.of(anio, 11, 20).toString(), "trasladable", "Día de la Soberanía Nacional"),
                new FeriadoResponseDTO(LocalDate.of(anio, 12, 8).toString(), "inamovible", "Día de la Inmaculada Concepción de María"),
                new FeriadoResponseDTO(LocalDate.of(anio, 12, 25).toString(), "inamovible", "Navidad")
        );
    }
}

