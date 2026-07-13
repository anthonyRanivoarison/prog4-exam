package api.poja.app.service.event;

import api.poja.app.endpoint.event.model.ImageProcessingRequested;
import api.poja.app.file.bucket.BucketComponent;
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

@Service
@AllArgsConstructor
public class ImageProcessingRequestedService implements Consumer<ImageProcessingRequested> {
  private final BucketComponent bucketComponent;
  private final Mailer mailer;

  private static final String CONVERTED_PREFIX = "converted";
  private static final ColorConvertOp BW_CONVERTER =
      new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

  @SneakyThrows
  @Override
  public void accept(ImageProcessingRequested event) {
    File original = bucketComponent.download(event.getS3Key());
    File bwImage = convertToBlackAndWhite(original, event.getFilename());
    String convertedKey = CONVERTED_PREFIX + "/" + event.getId() + "/" + event.getFilename();
    bucketComponent.upload(bwImage, convertedKey);
    String presignedUrl = bucketComponent.presign(convertedKey, Duration.ofMinutes(30)).toString();
    sendEmail(event.getEmail(), event.getFilename(), event.getId(), presignedUrl);
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
