package com.musicreviews.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "seguimiento", uniqueConstraints = @UniqueConstraint(columnNames = {"seguidor_id", "seguido_id"}))
@Data
public class Seguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seguidor_id", nullable = false)
    private Usuario seguidor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seguido_id", nullable = false)
    private Usuario seguido;

    @Column(name = "fecha_seguimiento")
    private LocalDateTime fechaSeguimiento;

    @PrePersist
    public void prePersist() {
        fechaSeguimiento = LocalDateTime.now();
    }
}
