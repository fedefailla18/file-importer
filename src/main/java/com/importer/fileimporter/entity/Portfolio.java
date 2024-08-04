package com.importer.fileimporter.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Type(type = "pg-uuid")
    private UUID id;

    private String name;

    private LocalDateTime creationDate;

    private LocalDateTime created;

    private String createdBy;

    private LocalDateTime modified;

    private String modifiedBy;

    @OneToMany(mappedBy = "portfolio", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Holding> holdings;

    @OneToMany(mappedBy = "portfolio", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Transaction> transactions;

}
