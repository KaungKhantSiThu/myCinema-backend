package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.HallResponse;
import com.kkst.mycinema.repository.HallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HallService {

    private final HallRepository hallRepository;

    public List<HallResponse> getAllHalls() {
        return hallRepository.findAll().stream()
                .map(hall -> HallResponse.builder()
                        .id(hall.getId())
                        .name(hall.getName())
                        .totalRows(hall.getTotalRows())
                        .totalColumns(hall.getTotalColumns())
                        .build())
                .toList();
    }
}
