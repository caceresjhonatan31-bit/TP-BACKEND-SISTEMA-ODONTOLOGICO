package com.consultorio.agendausuarios.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.consultorio.agendausuarios.domain.enums.EstadoTurno;
import com.consultorio.agendausuarios.dto.request.TurnoRequestDTO;
import com.consultorio.agendausuarios.dto.response.FeriadoResponseDTO;
import com.consultorio.agendausuarios.dto.response.TurnoResponseDTO;
import com.consultorio.agendausuarios.service.TurnoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoResponseDTO otorgarTurno(@Valid @RequestBody TurnoRequestDTO request) {
        return turnoService.otorgarTurno(request);
    }

    @GetMapping
    public List<TurnoResponseDTO> consultarTurnos(
            @RequestParam(required = false) Long odontologoId,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoTurno estado) {
        return turnoService.consultarTurnos(odontologoId, pacienteId, fecha, estado);
    }

    @GetMapping("/feriados")
    public List<FeriadoResponseDTO> consultarFeriados(@RequestParam(required = false) Integer anio) {
        return turnoService.consultarFeriados(anio);
    }

    @GetMapping("/{id}")
    public TurnoResponseDTO obtenerPorId(@PathVariable Long id) {
        return turnoService.obtenerPorId(id);
    }

    @PatchMapping("/{id}/atencion")
    public TurnoResponseDTO marcarComoAtendido(@PathVariable Long id) {
        return turnoService.marcarComoAtendido(id);
    }

    @PatchMapping("/{id}/cancelacion")
    public TurnoResponseDTO cancelarTurno(@PathVariable Long id) {
        return turnoService.cancelarTurno(id);
    }

    @PatchMapping("/{id}/inasistencia")
    public TurnoResponseDTO marcarComoInasistido(@PathVariable Long id) {
        return turnoService.marcarComoInasistido(id);
    }

    @PatchMapping("/{id}/rectificacion")
    public TurnoResponseDTO rectificarInasistencia(@PathVariable Long id) {
        return turnoService.rectificarInasistencia(id);
    }
}
