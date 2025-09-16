// src/main/java/com/dashboard/backend/entity/report/Report.java
package com.dashboard.backend.entity.report;

import com.dashboard.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    @NotNull(message = "Le type de rapport est obligatoire")
    private ReportType reportType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "author_id", nullable = false)
    @NotNull(message = "L'auteur est obligatoire")
    private Long authorId;

    // Paramètres du rapport stockés en JSON
    @Column(name = "parameters", columnDefinition = "JSON")
    private String parameters;

    // Période du rapport
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // Filtres
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "user_id")
    private Long userId;

    // Stockage du rapport généré
    @Column(name = "report_path")
    private String reportPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_format")
    @Builder.Default
    private ReportFormat reportFormat = ReportFormat.PDF;

    @Column(name = "file_size")
    private Long fileSize;

    // Statut et accès
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "generation_date")
    private LocalDate generationDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Builder.Default
    private Boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level")
    @Builder.Default
    private AccessLevel accessLevel = AccessLevel.AUTHOR_ONLY;

    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

    // Données du rapport en JSON (pour les rapports simples)
    @Column(name = "report_data", columnDefinition = "LONGTEXT")
    private String reportData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Types de rapport
     */
    public enum ReportType {
        ATTENDANCE("Rapport de Présence"),
        LEAVE("Rapport de Congés"),
        PERFORMANCE("Rapport de Performance"),
        EMPLOYEE("Rapport d'Employés"),
        DEPARTMENT("Rapport de Département"),
        PAYROLL("Rapport de Paie"),
        ABSENCE("Rapport d'Absences"),
        OVERTIME("Rapport d'Heures Supplémentaires"),
        CUSTOM("Rapport Personnalisé");

        private final String displayName;

        ReportType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Formats de rapport
     */
    public enum ReportFormat {
        PDF("PDF"),
        EXCEL("Excel"),
        CSV("CSV"),
        JSON("JSON"),
        HTML("HTML");

        private final String displayName;

        ReportFormat(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Statuts de rapport
     */
    public enum ReportStatus {
        PENDING("En attente"),
        GENERATING("Génération en cours"),
        COMPLETED("Terminé"),
        FAILED("Échec"),
        EXPIRED("Expiré");

        private final String displayName;

        ReportStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Niveaux d'accès
     */
    public enum AccessLevel {
        AUTHOR_ONLY("Auteur seulement"),
        DEPARTMENT("Département"),
        MANAGEMENT("Management"),
        HR_ONLY("RH seulement"),
        DIRECTOR_ONLY("Directeur seulement"),
        ALL("Tous");

        private final String displayName;

        AccessLevel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Vérifie si le rapport a expiré
     */
    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Incrémente le compteur de téléchargements
     */
    public void incrementDownloadCount() {
        this.downloadCount = this.downloadCount == null ? 1 : this.downloadCount + 1;
    }
}