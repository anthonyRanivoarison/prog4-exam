package api.poja.app.endpoint.rest.controller;

import api.poja.app.model.ImageSubmission;
import api.poja.app.service.ImageSubmissionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ImageSubmissionController {

  private final ImageSubmissionService service;

  @GetMapping("/images")
  public List<ImageSubmission> getAll() {
    return service.findAll();
  }
}
