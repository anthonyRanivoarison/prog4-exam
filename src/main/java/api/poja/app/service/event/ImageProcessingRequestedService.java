package api.poja.app.service.event;

import api.poja.app.endpoint.event.model.ImageProcessingRequested;
import api.poja.app.file.bucket.BucketConf;
import api.poja.app.mail.Email;
import api.poja.app.mail.Mailer;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@AllArgsConstructor
public class ImageProcessingRequestedService implements Consumer<ImageProcessingRequested> {
  private final BucketConf bucketConf;
  private final Mailer mailer;

  private static final String CONVERTED_PREFIX = "converted";
  private static final ColorConvertOp BW_CONVERTER =
      new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

  @SneakyThrows
  @Override
  public void accept(ImageProcessingRequested event) {
    File original = downloadFromS3(event.getS3Key());
    File bwImage = convertToBlackAndWhite(original, event.getFilename());
    String convertedKey = CONVERTED_PREFIX + "/" + event.getId() + "/" + event.getFilename();
    uploadToS3(bwImage, convertedKey);
    String presignedUrl = presignUrl(convertedKey, Duration.ofMinutes(30));
    sendEmail(event.getEmail(), event.getFilename(), event.getId(), presignedUrl);
  }

  @SneakyThrows
  private File downloadFromS3(String s3Key) {
    GetObjectRequest request =
        GetObjectRequest.builder().bucket(bucketConf.getBucketName()).key(s3Key).build();
    ResponseInputStream<GetObjectResponse> response = bucketConf.getS3Client().getObject(request);
    File tempFile = File.createTempFile("download-", "." + extensionFromKey(s3Key));
    try (var out = new java.io.FileOutputStream(tempFile)) {
      response.transferTo(out);
    }
    return tempFile;
  }

  private void uploadToS3(File file, String s3Key) {
    bucketConf
        .getS3Client()
        .putObject(
            PutObjectRequest.builder().bucket(bucketConf.getBucketName()).key(s3Key).build(),
            RequestBody.fromFile(file));
  }

  @SneakyThrows
  private String presignUrl(String s3Key, Duration expiration) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketConf.getBucketName()).key(s3Key).build();
    return bucketConf
        .getS3Presigner()
        .presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest)
                .build())
        .url()
        .toString();
  }

  private void sendEmail(String to, String filename, UUID submissionId, String presignedUrl)
      throws AddressException {
    var subject = "Your image \"%s\" is ready for download".formatted(filename);
    var htmlBody =
        """
<html>
  <body>
    <p>Hello,</p>
    <p>Good news — the black &amp; white conversion of your image is complete.</p>
    <p>
      <strong>File:</strong> %s<br>
      <strong>Submission ID:</strong> %s<br>
    </p>
    <p><a href="%s">Download your converted image</a></p>
    <p>Please note this download link is temporary and will expire after a period of time.</p>
    <p>Thank you for using our service.</p>
    <p>Best regards,</p>
    <p>The Team</p>
  </body>
</html>
"""
            .formatted(filename, submissionId, presignedUrl);
    var email =
        new Email(new InternetAddress(to), List.of(), List.of(), subject, htmlBody, List.of());
    mailer.accept(email);
  }

  private String extensionFromKey(String s3Key) {
    String filename = s3Key.contains("/") ? s3Key.substring(s3Key.lastIndexOf('/') + 1) : s3Key;
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex >= 0 ? filename.substring(dotIndex + 1) : "png";
  }

  private String extensionFromFilename(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex >= 0 ? filename.substring(dotIndex + 1) : "png";
  }

  private File convertToBlackAndWhite(File original, String filename) throws IOException {
    BufferedImage image = ImageIO.read(original);
    BufferedImage bwImage = BW_CONVERTER.filter(image, null);
    String ext = extensionFromFilename(filename);
    File output = File.createTempFile("bw-", "." + ext);
    ImageIO.write(bwImage, ext, output);
    return output;
  }
}
