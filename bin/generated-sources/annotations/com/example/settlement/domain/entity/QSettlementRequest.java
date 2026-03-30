package com.example.settlement.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSettlementRequest is a Querydsl query type for SettlementRequest
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSettlementRequest extends EntityPathBase<SettlementRequest> {

    private static final long serialVersionUID = -691216015L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSettlementRequest settlementRequest = new QSettlementRequest("settlementRequest");

    public final DateTimePath<java.time.LocalDateTime> agencyApprovedAt = createDateTime("agencyApprovedAt", java.time.LocalDateTime.class);

    public final QUser agencyApprovedBy;

    public final StringPath agencyComment = createString("agencyComment");

    public final NumberPath<java.math.BigDecimal> amount = createNumber("amount", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> branchApprovedAt = createDateTime("branchApprovedAt", java.time.LocalDateTime.class);

    public final QUser branchApprovedBy;

    public final StringPath branchComment = createString("branchComment");

    public final DateTimePath<java.time.LocalDateTime> completedAt = createDateTime("completedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> currentApprovalLevel = createNumber("currentApprovalLevel", Integer.class);

    public final StringPath description = createString("description");

    public final NumberPath<java.math.BigDecimal> feeAmount = createNumber("feeAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> finalAmount = createNumber("finalAmount", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> hqApprovedAt = createDateTime("hqApprovedAt", java.time.LocalDateTime.class);

    public final QUser hqApprovedBy;

    public final StringPath hqComment = createString("hqComment");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath orderId = createString("orderId");

    public final QOrganization organization;

    public final DateTimePath<java.time.LocalDateTime> rejectedAt = createDateTime("rejectedAt", java.time.LocalDateTime.class);

    public final QUser rejectedBy;

    public final StringPath rejectReason = createString("rejectReason");

    public final QUser requester;

    public final EnumPath<com.example.settlement.domain.entity.enums.SettlementStatus> status = createEnum("status", com.example.settlement.domain.entity.enums.SettlementStatus.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QSettlementRequest(String variable) {
        this(SettlementRequest.class, forVariable(variable), INITS);
    }

    public QSettlementRequest(Path<? extends SettlementRequest> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSettlementRequest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSettlementRequest(PathMetadata metadata, PathInits inits) {
        this(SettlementRequest.class, metadata, inits);
    }

    public QSettlementRequest(Class<? extends SettlementRequest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.agencyApprovedBy = inits.isInitialized("agencyApprovedBy") ? new QUser(forProperty("agencyApprovedBy"), inits.get("agencyApprovedBy")) : null;
        this.branchApprovedBy = inits.isInitialized("branchApprovedBy") ? new QUser(forProperty("branchApprovedBy"), inits.get("branchApprovedBy")) : null;
        this.hqApprovedBy = inits.isInitialized("hqApprovedBy") ? new QUser(forProperty("hqApprovedBy"), inits.get("hqApprovedBy")) : null;
        this.organization = inits.isInitialized("organization") ? new QOrganization(forProperty("organization"), inits.get("organization")) : null;
        this.rejectedBy = inits.isInitialized("rejectedBy") ? new QUser(forProperty("rejectedBy"), inits.get("rejectedBy")) : null;
        this.requester = inits.isInitialized("requester") ? new QUser(forProperty("requester"), inits.get("requester")) : null;
    }

}

