package api.poja.app.repository.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "image_submission")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class JImageSubmission {
  @UuidGenerator @GeneratedValue @Id private UUID id;

  @Column(name = "filename", nullable = false)
  private String filename;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
