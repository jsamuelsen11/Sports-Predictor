package com.sportspredictor.entity;

import com.sportspredictor.entity.enums.BetLegStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** An individual leg within a multi-leg bet (parlay, teaser, SGP). */
@Entity
@Table(name = "bet_leg", uniqueConstraints = @UniqueConstraint(columnNames = {"bet_id", "leg_number"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BetLeg extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bet_id", nullable = false, updatable = false)
    private Bet bet;

    @Column(name = "leg_number", nullable = false)
    private int legNumber;

    @Column(name = "selection", nullable = false)
    private String selection;

    @Column(name = "odds", nullable = false)
    private BigDecimal odds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BetLegStatus status;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "sport", nullable = false)
    private String sport;

    @Column(name = "result_detail")
    private String resultDetail;
}
