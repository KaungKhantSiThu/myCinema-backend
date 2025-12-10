package com.kkst.mycinema.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, length = 50)
    private String genre;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The external data source from which this movie was imported.
     * Examples: "TMDb", "IMDb", "Manual", etc.
     * Null indicates the movie was created manually in the system.
     */
    @Column(name = "external_source", length = 50)
    private String externalSource;

    /**
     * The ID of this movie in the external data source.
     * Used to track the origin and prevent duplicate imports.
     * Null indicates the movie was created manually in the system.
     */
    @Column(name = "external_id", length = 100)
    private String externalId;
}
