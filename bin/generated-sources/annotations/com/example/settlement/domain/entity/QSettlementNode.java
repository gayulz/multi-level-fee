package com.example.settlement.domain.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSettlementNode is a Querydsl query type for SettlementNode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSettlementNode extends EntityPathBase<SettlementNode> {

    private static final long serialVersionUID = -624100768L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSettlementNode settlementNode = new QSettlementNode("settlementNode");

    public final ListPath<SettlementNode, QSettlementNode> children = this.<SettlementNode, QSettlementNode>createList("children", SettlementNode.class, QSettlementNode.class, PathInits.DIRECT2);

    public final NumberPath<java.math.BigDecimal> feeRate = createNumber("feeRate", java.math.BigDecimal.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final QOrganization organization;

    public final QSettlementNode parent;

    public QSettlementNode(String variable) {
        this(SettlementNode.class, forVariable(variable), INITS);
    }

    public QSettlementNode(Path<? extends SettlementNode> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSettlementNode(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSettlementNode(PathMetadata metadata, PathInits inits) {
        this(SettlementNode.class, metadata, inits);
    }

    public QSettlementNode(Class<? extends SettlementNode> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organization = inits.isInitialized("organization") ? new QOrganization(forProperty("organization"), inits.get("organization")) : null;
        this.parent = inits.isInitialized("parent") ? new QSettlementNode(forProperty("parent"), inits.get("parent")) : null;
    }

}

