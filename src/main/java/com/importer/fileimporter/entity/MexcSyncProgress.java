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
@Table(name = "mexc_sync_progress")
public class MexcSyncProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Type(type = "pg-uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "last_synced_order_id")
    private Long lastSyncedOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BinanceSyncStatus status; // Reusing BinanceSyncStatus as it's generic enough (PENDING, IN_PROGRESS, etc.)

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;
}
