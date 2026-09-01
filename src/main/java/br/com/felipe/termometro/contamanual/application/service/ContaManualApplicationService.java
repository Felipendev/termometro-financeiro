package br.com.felipe.termometro.contamanual.application.service;

import br.com.felipe.termometro.contamanual.application.api.request.ContaManualRequest;
import br.com.felipe.termometro.contamanual.application.repository.ContaManualRepository;
import br.com.felipe.termometro.contamanual.domain.ContaManual;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContaManualApplicationService {
    private final ContaManualRepository repository;
    public List<ContaManual> listaAtivas() { return repository.buscaAtivas(); }
    public ContaManual salva(UUID id, ContaManualRequest request) { return repository.salva(request.paraDominio(id)); }
    public void remove(UUID id) { repository.remove(id); }
}
