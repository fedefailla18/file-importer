package com.importer.fileimporter.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "external_api_raw_response")
public class ExternalApiRawResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Type(type = "pg-uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_name", nullable = false, length = 20)
    private ExchangeName exchangeName;

    @Column(name = "response_type", nullable = false, length = 50)
    private String responseType;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "raw_json", nullable = false, columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}
