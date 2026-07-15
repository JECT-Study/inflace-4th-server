package com.example.inflace.domain.brand.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "brand", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    @Column(name = "admin_approved", nullable = false)
    private boolean adminApproved;

    public void approveByAdmin() {
        this.adminApproved = true;
    }
}
