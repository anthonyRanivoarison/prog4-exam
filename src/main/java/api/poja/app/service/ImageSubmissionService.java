package api.poja.app.service;

import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.endpoint.event.model.ImageProcessingRequested;
import api.poja.app.file.bucket.BucketConf;
import api.poja.app.file.zip.FileTyper;
import api.poja.app.mapper.ImageSubmissionMapper;
import api.poja.app.model.ImageSubmission;
import api.poja.app.repository.ImageSubmissionRepository;
import api.poja.app.repository.model.JImageSubmission;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class ImageSubmissionService {
  private final ImageSubmissionRepository repository;
  private final ImageSubmissionMapper mapper;
  private final BucketConf bucketConf;
  private final EventProducer<ImageProcessingRequested> eventProducer;
  private final FileTyper fileTyper;

  public List<ImageSubmission> findAll() {
    return mapper.toModelList(repository.findAll());
  }

  @SneakyThrows
  public ImageSubmission create(MultipartFile file, String email) {
    File tempFile = saveToTempFile(file);
    validateFileType(tempFile);

    JImageSubmission entity =
        JImageSubmission.builder()
            .filename(file.getOriginalFilename())
            .email(email)
            .createdAt(Instant.now())
            .build();
    JImageSubmission saved = repository.save(entity);

    String s3Key = "original/" + saved.getId() + "/" + saved.getFilename();
    bucketConf
        .getS3Client()
        .putObject(
            PutObjectRequest.builder().bucket(bucketConf.getBucketName()).key(s3Key).build(),
            RequestBody.fromFile(tempFile));

    var event =
        ImageProcessingRequested.builder()
            .id(saved.getId())
            .filename(saved.getFilename())
            .email(saved.getEmail())
            .s3Key(s3Key)
            .build();
    eventProducer.accept(List.of(event));

    return mapper.toModel(saved);
  }

  private File saveToTempFile(MultipartFile file) throws IOException {
    String originalFilename = file.getOriginalFilename();
    String suffix =
        originalFilename != null && originalFilename.contains(".")
            ? originalFilename.substring(originalFilename.lastIndexOf('.'))
            : ".bin";
    File tempFile = File.createTempFile("upload-", suffix);
    file.transferTo(tempFile);
    return tempFile;
  }

  private void validateFileType(File file) {
    MediaType mediaType = fileTyper.apply(file);
    if (!MediaType.IMAGE_JPEG.equals(mediaType) && !MediaType.IMAGE_PNG.equals(mediaType)) {
      throw new IllegalArgumentException(
          "Only JPEG and PNG formats are accepted. Got: " + mediaType);
    }
  }
}
