package com.consultorio.agendausuarios.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.consultorio.agendausuarios.domain.entity.DisponibilidadOdontologo;
import com.consultorio.agendausuarios.domain.entity.Usuario;
import com.consultorio.agendausuarios.domain.enums.RolUsuario;
import com.consultorio.agendausuarios.dto.request.DisponibilidadRequestDTO;
import com.consultorio.agendausuarios.dto.response.DisponibilidadResponseDTO;
import com.consultorio.agendausuarios.repository.DisponibilidadRepository;
import com.consultorio.agendausuarios.repository.UsuarioRepository;
import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final DisponibilidadRepository disponibilidadRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public DisponibilidadResponseDTO asignarFranja(DisponibilidadRequestDTO request) {
        validarRangoHorario(request);
        Usuario odontologo = obtenerOdontologoActivo(request.odontologoId());

        // RN-02: Validar que no se superponga con otro horario del mismo profesional en el mismo día
        if (disponibilidadRepository.existeSuperposicionHoraria(
                odontologo.getId(), request.diaSemana(), request.horaInicio(), request.horaFin())) {
            throw new ConflictException("La franja horaria se superpone con otro horario activo del profesional en ese día");
        }

        DisponibilidadOdontologo disponibilidad = DisponibilidadOdontologo.builder()
                .odontologo(odontologo)
                .diaSemana(request.diaSemana())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .activo(true)
                .build();

        return mapToResponse(disponibilidadRepository.save(disponibilidad));
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadResponseDTO> listarPorOdontologo(Long odontologoId) {
        if (!usuarioRepository.existsById(odontologoId)) {
            throw new ResourceNotFoundException("Odontólogo no encontrado con ID: " + odontologoId);
        }
        return disponibilidadRepository.findByOdontologoIdAndActivoTrue(odontologoId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public DisponibilidadResponseDTO actualizarFranja(Long id, DisponibilidadRequestDTO request) {
        DisponibilidadOdontologo disponibilidad = disponibilidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidad no encontrada con ID: " + id));

        validarRangoHorario(request);
        Usuario odontologo = obtenerOdontologoActivo(request.odontologoId());

        // RN-02: Validar superposición excluyendo el registro actual
        if (disponibilidadRepository.existeSuperposicionHorariaExcluyendoId(
                odontologo.getId(), request.diaSemana(), request.horaInicio(), request.horaFin(), id)) {
            throw new ConflictException("La modificación se superpone con otra franja activa del profesional en ese día");
        }

        disponibilidad.setOdontologo(odontologo);
        disponibilidad.setDiaSemana(request.diaSemana());
        disponibilidad.setHoraInicio(request.horaInicio());
        disponibilidad.setHoraFin(request.horaFin());

        return mapToResponse(disponibilidadRepository.save(disponibilidad));
    }

    @Transactional
    public void inhabilitarFranja(Long id) {
        DisponibilidadOdontologo disp = disponibilidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidad no encontrada con ID: " + id));
        disp.setActivo(false);
        disponibilidadRepository.save(disp);
    }

    private void validarRangoHorario(DisponibilidadRequestDTO request) {
        if (!request.horaInicio().isBefore(request.horaFin())) {
            throw new ConflictException("La hora de inicio debe ser estrictamente anterior a la hora de finalización");
        }
    }

    private Usuario obtenerOdontologoActivo(Long odontologoId) {
        Usuario usuario = usuarioRepository.findById(odontologoId)
                .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado con ID: " + odontologoId));

        if (usuario.getRol() != RolUsuario.ODONTOLOGO || !usuario.getActivo()) {
            throw new ConflictException("Los horarios de consultorio solo pueden asignarse a odontólogos en actividad");
        }
        return usuario;
    }

    private DisponibilidadResponseDTO mapToResponse(DisponibilidadOdontologo d) {
        return new DisponibilidadResponseDTO(
                d.getId(),
                d.getOdontologo().getId(),
                d.getDiaSemana(),
                d.getHoraInicio(),
                d.getHoraFin(),
                d.getActivo()
        );
    }
}
