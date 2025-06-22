package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponseDTO> getAllPatients() {
        //Get all patients
        List<Patient> patients = patientRepository.findAll();
//        List<PatientResponseDTO> patientResponseDTO = patients.
//                stream().map(patient -> PatientMapper.toPatientResponseDTO(patient))
//                .toList();
        //convert all patient objects to PatientResponseDTO objects and return its list
        return patients.stream()
                .map(PatientMapper::toPatientResponseDTO)
                .toList();
    }

}
