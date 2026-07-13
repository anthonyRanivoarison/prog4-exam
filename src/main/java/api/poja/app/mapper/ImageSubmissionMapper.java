package api.poja.app.mapper;

import api.poja.app.model.ImageSubmission;
import api.poja.app.repository.model.JImageSubmission;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ImageSubmissionMapper {

  public ImageSubmission toModel(JImageSubmission entity) {
    if (entity == null) {
      return null;
    }
    return ImageSubmission.builder()
        .id(entity.getId())
        .filename(entity.getFilename())
        .email(entity.getEmail())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  public JImageSubmission toEntity(ImageSubmission model) {
    if (model == null) {
      return null;
    }
    return JImageSubmission.builder()
        .id(model.getId())
        .filename(model.getFilename())
        .email(model.getEmail())
        .createdAt(model.getCreatedAt())
        .build();
  }

  public List<ImageSubmission> toModelList(List<JImageSubmission> entities) {
    if (entities == null) {
      return List.of();
    }
    return entities.stream().map(this::toModel).toList();
  }
}
