package api.poja.app.service;

import api.poja.app.mapper.ImageSubmissionMapper;
import api.poja.app.model.ImageSubmission;
import api.poja.app.repository.ImageSubmissionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageSubmissionService {
  private final ImageSubmissionRepository repository;
  private final ImageSubmissionMapper mapper;

  public List<ImageSubmission> findAll() {
    return mapper.toModelList(repository.findAll());
  }
}
